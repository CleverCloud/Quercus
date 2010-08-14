/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.es.parser;

import com.caucho.VersionFactory;
import com.caucho.es.*;
import com.caucho.java.LineMap;
import com.caucho.util.CharBuffer;
import com.caucho.util.IntMap;
import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

class ParseClass {
  private String srcFilename;
  private LineMap lineMap;

  private Parser parser;

  // The full classname of the generated script.
  private String className;
  // The package name of the generated script.
  private String pkg;
  // The class name.
  private String name;
  private ESId proto;

  // the global parse class
  private ParseClass root;

  // class instance variables
  private HashMap variables = new HashMap();

  // child classes
  private ArrayList classes = new ArrayList();

  // class methods
  private ArrayList functions = new ArrayList();

  private IntMap funMap = new IntMap();
  private HashMap names = new HashMap();
  private HashMap literals = new HashMap();
  private HashMap mangledSet = new HashMap();
  private Function global;
  private int unique;
  private int callDepth;
  private ArrayList imports = new ArrayList();
  private ArrayList javaImports = new ArrayList();
  private WriteStream os;

  private int printDepth;
  private boolean isFirst;

  private int destLine;
  private Path sourcePath;

  /**
   * Creates a new parse class.
   *
   * @param srcFilename the name of the source (*.js)
   * @param name the classname.
   */
  ParseClass(String srcFilename, String name)
  {
    this.srcFilename = srcFilename;
    this.root = this;

    className = name;

    int p = name.lastIndexOf('.');

    if (p >= 0) {
      pkg = name.substring(0, p);
      this.name = name.substring(p + 1);
    }
    else {
      this.name = name;
    }

    lineMap = new LineMap(name, srcFilename);
    lineMap.add(1, 1);
    destLine = 1;
  }

  /**
   * Sets the source path for searching import classes.
   */
  void setSourcePath(Path sourcePath)
  {
    this.sourcePath = sourcePath;
  }

  /**
   * Gets the script search path
   */
  Path getScriptPath()
  {
    return root.parser.getScriptPath();
  }

  void setParser(Parser parser)
  {
    root.parser = parser;
  }

  void setWriteStream(WriteStream os)
  {
    this.os = os;
  }

  /**
   * Creates a child subclasses of this class.
   *
   * @param id the new class name.
   */
  ParseClass newClass(ESId id)
  {
    ParseClass cl = new ParseClass(srcFilename, id.toString());

    classes.add(cl);
    cl.root = root;

    return cl;
  }

  Function newFunction(Function oldFun, ESId id, boolean isClass)
  {
    if (id == null)
      id = ESId.intern("$lambda" + unique++);
    String realId = "_f_" + id.toString();

    while (names.get(realId) != null) {
      realId = realId + unique++;
    }
    names.put(realId, realId);

    Function fun = new Function(this, oldFun, realId, id, isClass);
    setFunction(fun);

    return fun;
  }

  void setFunction(Function function)
  {
    int index;
    if ((index = funMap.get(function.name)) >= 0) {
      function.num = index;
      functions.set(index, function);
    }
    else {
      function.num = functions.size();
      funMap.put(function.name, functions.size());
      functions.add(function);
    }
  }

  void addImport(String importName)
  {
    if (! imports.contains(importName))
      imports.add(importName);
  }

  /**
   * Adds a Java class/package import to the list.
   */
  void addJavaImport(String importName)
  {
    if (! javaImports.contains(importName))
      javaImports.add(importName);
  }

  /**
   * Gets a class variable.
   */
  Variable getVariable(ESId id)
  {
    return (Variable) variables.get(id);
  }

  /**
   * Sets a new class variable.
   */
  void addVariable(ESId id, Variable var)
  {
    variables.put(id, var);
    var.setJavaGlobal(true);
  }

  void setGlobal(Function global)
  {
    this.global = global;
  }

  void setProto(ESId proto)
  {
    this.proto = proto;
  }

  void addFunction(Function function)
  {
    setFunction(function);
  }

  void writeCode(WriteStream os) throws IOException
  {
    this.os = os;

    println("/**");
    println(" * Generated by " + VersionFactory.getFullVersion());
    println(" */");
    println();
    println("package " + pkg + ";");
    println("import java.io.*;");
    println("import com.caucho.es.*;");
    println("import com.caucho.util.*;");
    for (int i = 0; i < javaImports.size(); i++)
      println("import " + javaImports.get(i) + ";");

    println("public class " + name + " extends com.caucho.es.Script {");

    pushDepth();

    writeExecute();

    writeClassContents();

    writeLastModified();
    printLineMap();

    popDepth();

    println("}");
  }

  /**
   * Writes the contents of the class.  Actually generates two classes.
   * One is a straight Java class, the other is the wrapper.
   */
  void writeClassContents() throws IOException
  {
    if (os == null)
      os = root.os;

    String name = this == root ? "js_global" : this.name;

    println();
    println("public static class " + name + " {");
    pushDepth();
    println(name + "_es _js;");

    Iterator iter = variables.values().iterator();
    while (iter.hasNext()) {
      Variable var = (Variable) iter.next();

      writeVarDecl(var, true);
    }

    for (int i = 0; i < functions.size(); i++) {
      Function fun = (Function) functions.get(i);

      println();
      fun.writeCode(this);
    }

    popDepth();
    println("}");

    println();
    if (name.equals("js_global"))
      println("public static class js_global_es extends ESGlobal {");
    else
      println("public static class " + name + "_es extends ESClass {");
    pushDepth();
    println(name + " _js_object;");
    println();

    println();
    if (name.equals("js_global")) {
      println("js_global_es(Global resin)");
      println("{");
      println("  super(resin);");
      println("  resin.setGlobal(this);");
    }
    else {
      println(name + "_es()");
      println("{");
    }
    println("  _js_object = new " + name + "();");
    println("  _js_object._js = this;");
    println("}");

    writeMap();

    printGetProperty();
    printPropNames();

    writeInit();
    writeWrapperInit(name);
    writeStaticInit();
    writeExport();

    popDepth();
    println("}");

    for (int i = 0; i < classes.size(); i++) {
      ParseClass subClass = (ParseClass) classes.get(i);

      subClass.setWriteStream(os);
      subClass.writeClassContents();
    }
  }

  void writeExport() throws IOException
  {
    println();
    println("public void export(ESObject dst) throws Throwable");
    println("{");

    println("  ESBase tmp;");

    for (int i = 0;
         global.functions != null && i < global.functions.size();
         i++) {
      Function fun = (Function) global.functions.get(i);

      println("  tmp = getProperty(\"" + fun.id + "\");");
      println("  dst.put(\"" + fun.id + "\", tmp, ESBase.DONT_ENUM);");
    }

    for (int i = 0; i < classes.size(); i++) {
      ParseClass cl = (ParseClass) classes.get(i);

      Function fun = cl.getFunction(ESId.intern(cl.name));

      println("  tmp = getProperty(\"" + cl.name + "\");");
      println("  dst.put(\"" + cl.name + "\", tmp, ESBase.DONT_ENUM);");
    }

    println("}");
  }

  void writeExecute()
    throws IOException
  {
    println("public ESGlobal initClass(Global resin) throws Throwable");
    println("{");
    pushDepth();
    println("resin.addScript(\"" + className + "\", this);");

    println("js_global_es test = new js_global_es(resin);");
    println("test._init(resin, test);");
    println("return test;");
    popDepth();
    println("}");
  }

  void writeWrapperInit(String name)
    throws IOException
  {
  }

  void writeVarDecl(Variable var, boolean init)
    throws IOException
  {
    if (var.getType() == Expr.TYPE_BOOLEAN) {
      print("boolean " + var.getId());
      if (init)
        println(" = false;");
      else
        println(";");
    }
    else if (var.getType() == Expr.TYPE_INTEGER) {
      print("int " + var.getId());
      if (init)
        println(" = 0;");
      else
        println(";");
    }
    else if (var.getType() == Expr.TYPE_NUMBER) {
      print("double " + var.getId());
      if (init)
        println(" = Double.NaN;");
      else
        println(";");
    }
    else if (var.getType() == Expr.TYPE_STRING) {
      print("ESString " + var.getId());
      if (init)
        println(" = ESString.create(\"\");");
      else
        println(";");
    }
    else if (var.getType() == Expr.TYPE_JAVA &&
             var.getTypeExpr() != null) {
      TypeExpr type = (TypeExpr) var.getTypeExpr();

      println(type.getTypeName() + " " + var.getId() + ";");
    }
    else {
      if (init)
        println("ESBase " + var.getId() + " = ESBase.esUndefined;");
      else
        println("ESBase " + var.getId() + ";");
    }
  }

  void printId(ESId id) throws IOException
  {
    print("js_global_es.");
    print(getMangledLiteral(id));
  }

  String getMangledLiteral(ESBase value)
  {
    String literal = (String) literals.get(value);

    if (literal == null) {
      literal = mangleLiteral(value);
      literals.put(value, literal);
    }

    return literal;
  }

  // XXX: hacks
  void pushCall()
  {
    callDepth++;
  }

  void popCall(int n)
  {
    callDepth -= n;
  }

  int getCallDepth()
  {
    return callDepth;
  }

  void printLiteral(ESBase obj) throws IOException
  {
    if (obj == null)
      print("ESBase.esNull");
    else if (obj instanceof ESNull)
      print("ESBase.esNull");
    else if (obj instanceof ESUndefined)
      print("ESBase.esUndefined");
    else if (obj == ESBoolean.TRUE)
      print("ESBoolean.TRUE");
    else if (obj == ESBoolean.FALSE)
      print("ESBoolean.FALSE");
    else {
      String literal = (String) literals.get(obj);

      if (literal == null) {
        try {
          if (obj instanceof ESNumber && Double.isNaN(obj.toNum())) {
            print("ESNumber.NaN");
            return;
          }
        } catch (Throwable e) {
        }
        literal = mangleLiteral(obj);
        literals.put(obj, literal);
      }

      print("js_global_es.");
      print(literal);
    }
  }

  private String mangleLiteral(Object obj)
  {
    String s = obj.toString();
    CharBuffer cb = new CharBuffer("_l_");
    for (int i = 0; i < s.length() && i < 32; i++) {
      char ch = s.charAt(i);
      // Java can't really deal with non-ascii?
      if (Character.isJavaIdentifierPart(ch) && ch >= ' ' && ch < 127)
        cb.append(ch);
      else if (cb.getLastChar() != '_')
        cb.append("_");
    }

    if (mangledSet.get(cb) != null)
      cb.append("_" + unique++);
    mangledSet.put(cb, cb);

    return cb.toString();
  }

  /**
   * Writes the method map.
   */
  void writeMap() throws IOException
  {
    println("public ESBase call(int n, Call call, int length)");
    println("  throws Throwable");
    println("{");
    println("  switch(n) {");

    for (int i = 0; i < functions.size(); i++) {
      Function fun = (Function) functions.get(i);

      println("  case " + i + ":");
      print("    return ");

      Expr expr = fun.getReturnType();
      boolean hasCoerce = false;
      if (expr == null) {
      }
      else if (expr.getType() == Expr.TYPE_INTEGER ||
               expr.getType() == Expr.TYPE_NUMBER) {
        hasCoerce = true;
        print("ESNumber.create(");
      }
      else if (expr instanceof JavaTypeExpr) {
        print("call.global.wrap(");
      }

      print("_js_object.");
      print(fun.name + "(call, length");
      for (int j = 0; j < fun.getFormalSize(); j++) {
        Variable formal = fun.getFormal(j);

        if (formal.getTypeExpr() instanceof JavaTypeExpr) {
          TypeExpr type = (TypeExpr) formal.getTypeExpr();

          print(", (" + type.getTypeName() + ") call.getArgObject(" + j + ", length)");
        }
        else if (formal.getType() == Expr.TYPE_INTEGER)
          print(", call.getArgInt32(" + j + ", length)");
      }
      print(")");
      if (hasCoerce)
        print(")");
      println(";");
    }

    println("  default:");
    println("    throw new RuntimeException();");
    println("  }");
    println("}");
  }

  void printGetProperty() throws IOException
  {
    if (variables.size() == 0)
      return;

    println();
    println("public ESBase getProperty(ESString key) throws Throwable");
    println("{");
    pushDepth();
    println("switch (propNames.get(key)) {");

    Iterator iter = variables.values().iterator();
    int i = 0;
    while (iter.hasNext()) {
      Variable var = (Variable) iter.next();

      println("case " + i + ":");
      Class javaClass = var.getTypeExpr().getJavaClass();
      if (ESBase.class.isAssignableFrom(javaClass))
        println("  return _js_object." + var.getId() + ";");
      else
        println("  return wrap(_js_object." + var.getId() + ");");

      i++;
    }

    println("default:");
    println("  return super.getProperty(key);");
    println("}");
    popDepth();
    println("}");
  }

  void printSetProperty() throws IOException
  {
    if (variables.size() == 0)
      return;

    println();
    println("public void setProperty(ESString key, ESBase value) throws Throwable");
    println("{");
    pushDepth();
    println("switch (propNames.get(key)) {");

    Iterator iter = variables.values().iterator();
    int i = 0;
    while (iter.hasNext()) {
      Variable var = (Variable) iter.next();

      println("case " + i + ":");
      Class javaClass = var.getTypeExpr().getJavaClass();
      if (ESBase.class.isAssignableFrom(javaClass))
        println("  _js_object." + var.getId() + " = (" + javaClass.getName() + ") value.toJavaObject();");
      else
        println("  _js_object." + var.getId() + " = value;");
      println("  break;");

      i++;
    }

    println("default:");
    println("  return super.setProperty(key, value);");
    println("}");
    popDepth();
    println("}");
  }

  void printPropNames() throws IOException
  {
    if (variables.size() == 0)
      return;

    println();
    println("private static com.caucho.util.IntMap propNames;");
    println();
    println("static {");
    pushDepth();
    println("propNames = new com.caucho.util.IntMap();");

    Iterator iter = variables.values().iterator();
    int i = 0;
    while (iter.hasNext()) {
      Variable var = (Variable) iter.next();

      println("propNames.put(ESId.intern(\"" + var.getId() + "\"), " + i + ");");

      i++;
    }

    popDepth();
    println("}");
  }

  void writeInit() throws IOException
  {
    println();
    println("public void _init(Global resin, ESObject global) throws Throwable");
    println("{");

    pushDepth();

    for (int i = 0; i < imports.size(); i++) {
      String importName = (String) imports.get(i);

      print("resin.importScript(this, \"");
      printString(importName);
      println("\");");
    }

    println("ESClosure fun;");

    for (int i = 0; global.functions != null && i < global.functions.size(); i++) {
      Function fun = (Function) global.functions.get(i);

      print("fun = new ESClosure(");
      print(getMangledLiteral(fun.id));
      print(", this, null, " + fun.num + ", ");
      if (fun.getFormalSize() == 0)
        print("_a_null");
      else
        print("_a_" + fun.num);
      println(", global);");
      println("global.put(\"" + fun.id + "\", fun, ESBase.DONT_ENUM);");
    }

    println("ESObject protoProto;");
    println("ESObject proto;");
    for (int i = 0; i < classes.size(); i++) {
      ParseClass cl = (ParseClass) classes.get(i);

      Function fun = cl.getFunction(ESId.intern(cl.name));

      println("{");
      pushDepth();
      println(cl.name + "_es jsClass;");

      println("jsClass = new " + cl.name + "_es();");
      if (cl.proto != null) {
        print("proto = new ESObject(\"Object\", getProperty(");
        print(getMangledLiteral(cl.proto));
        println(").getProperty(");
        printLiteral(ESId.intern("prototype"));
        println("));");
      }
      else
        println("proto = resin.createObject();");
      println("jsClass._init(resin, proto);");
      print("fun = new ESClosure(");
      print(getMangledLiteral(ESId.intern(cl.name)));
      print(", jsClass, proto, " + fun.num + ", ");
      if (fun.getFormalSize() == 0)
        print("_a_null");
      else
        print("_a_" + (i + functions.size()));
      println(", null);");
      println("setProperty(\"" + cl.name + "\", fun);");

      popDepth();
      println("}");
    }

    popDepth();
    println("}");
  }

  void writeLastModified() throws IOException
  {
    println();
    println("public boolean isModified()");
    println("{");
    if (sourcePath == null || sourcePath.getLastModified() <= 0)
      println("  return false;");
    else if (getScriptPath().lookup(sourcePath.getUserPath()).exists()) {
      print("  return scriptPath.lookup(\"");
      printString(sourcePath.getUserPath());
      println("\").getLastModified() != " + sourcePath.getLastModified() + "L;");
    }
    else {
      print("  return com.caucho.vfs.Vfs.lookup(\"");
      printString(sourcePath.getFullPath());
      println("\").getLastModified() != " + sourcePath.getLastModified() + "L;");
    }

    println("}");
  }

  Function getFunction(ESId name)
  {
    for (int i = 0; i < functions.size(); i++) {
      Function fun = (Function) functions.get(i);
      if (fun.id == name)
        return fun;
    }

    return null;
  }

  boolean hasFunction(ArrayList functions, ESId var)
  {
    for (int i = 2; i < functions.size(); i++) {
      Function fun = (Function) functions.get(i);
      if (fun.id == var)
        return true;
    }

    return false;
  }

  void writeStaticInit() throws IOException
  {
    Iterator iter = literals.keySet().iterator();

    if (iter.hasNext())
      println();

    while (iter.hasNext()) {
      Object o = iter.next();
      String name = (String) literals.get(o);

      if (o instanceof ESId) {
        print("static ESId " + name);
        print(" = ESId.intern(\"");
        printString(String.valueOf(o));
        println("\");");
      }
      else if (o instanceof ESString) {
        print("static ESString " + name);
        print(" = ESString.create(\"");
        printString(String.valueOf(o));
        println("\");");
      }
      else if (o instanceof ESNumber) {
        print("static ESNumber " + name);
        double v = ((ESNumber) o).toNum();
        if (Double.isInfinite(v))
          println(" = ESNumber.create(Double.POSITIVE_INFINITY);");
        else if (Double.isInfinite(-v))
          println(" = ESNumber.create(Double.NEGATIVE_INFINITY);");
        else if (Double.isNaN(v))
          throw new RuntimeException();
        else
          println(" = ESNumber.create(" + o + "D);");
      }
      else
        throw new RuntimeException();
    }

    println("static ESId[] _a_null = new ESId[0];");
    for (int i = 0; i < functions.size(); i++) {
      Function fun = (Function) functions.get(i);

      printFormals(fun, i);
    }

    for (int i = 0; i < classes.size(); i++) {
      ParseClass cl = (ParseClass) classes.get(i);

      Function fun = cl.getFunction(ESId.intern(cl.name));

      printFormals(fun, i + functions.size());
    }
  }

  void setLine(String filename, int line)
  {
    lineMap.add(filename, line, destLine);
  }

  /**
   * Prints the mapping between java lines and the original *.js lines.
   */
  void printLineMap() throws IOException
  {
    String dst = name + ".java";
    String src = srcFilename;
    String srcTail = srcFilename;

    int p = srcTail.lastIndexOf('/');
    if (p >= 0)
      srcTail = srcTail.substring(p + 1);
    p = srcTail.lastIndexOf('\\');
    if (p >= 0)
      srcTail = srcTail.substring(p + 1);

    println();
    println("public com.caucho.java.LineMap getLineMap()");
    println("{");
    pushDepth();
    print("com.caucho.java.LineMap lineMap = new com.caucho.java.LineMap(\"");
    printString(dst);
    print("\", \"");
    printString(srcTail);
    println("\");");
    println("lineMap.add(1, 1);");
    Iterator iter = lineMap.iterator();
    while (iter.hasNext()) {
      LineMap.Line line = (LineMap.Line) iter.next();

      if (line.getSourceFilename() == src) {
        println("lineMap.add(" + line.getSourceLine() + ", " +
                line.getDestinationLine() + ");");
      }
      else {
        src = line.getSourceFilename();
        srcTail = src;
        p = srcTail.lastIndexOf('/');
        if (p >= 0)
          srcTail = srcTail.substring(p + 1);
        p = srcTail.lastIndexOf('\\');
        if (p >= 0)
          srcTail = srcTail.substring(p + 1);

        print("lineMap.add(\"");
        printString(srcTail);
        println("\", " +
                line.getSourceLine() + ", " +
                line.getDestinationLine() + ");");
      }
    }

    println("return lineMap;");
    popDepth();
    println("}");
  }

  private void printFormals(Function fun, int i)
    throws IOException
  {
    if (fun.getFormalSize() > 0) {
      print("  private static ESId[] _a_" + i + " = new ESId[] {");
      for (int j = 0; fun.formals != null && j < fun.formals.size(); j++) {

        if (j != 0)
          print(",");
        Variable var = (Variable) fun.formals.get(j);
        print(" ESId.intern(\"" + var.getId() + "\")");
      }
      println("};");
    }
  }

  /**
   * Escapes the string so the Java compiler can properly understand it.
   */
  void printString(String s) throws IOException
  {
    for (int i = 0; i < s.length(); i++) {
      if (i > 0 && i % (16 * 1024) == 0)
        os.print("\" + \"");

      char ch = s.charAt(i);
      switch (ch) {
      case '\\':
        os.print("\\\\");
        break;
      case '\n':
        os.print("\\n");
        break;
      case '\r':
        os.print("\\r");
        break;
      case '\t':
        os.print("\\t");
        break;
      case '"':
        os.print("\\\"");
        break;
      default:
        if (ch >= 32 && ch < 127)
          os.print(ch);
        else {
          os.print("\\u");
          printHex(ch >> 12);
          printHex(ch >> 8);
          printHex(ch >> 4);
          printHex(ch >> 0);
        }
        break;
      }
    }
  }

  void printHex(int i) throws IOException
  {
    i &= 0xf;
    if (i < 10) {
      os.print(i);
    }
    else {
      os.print((char) ('a' + i - 10));
    }
  }

  void pushDepth()
  {
    printDepth += 2;
  }

  void popDepth()
  {
    printDepth -= 2;
  }

  void print(boolean b) throws IOException
  {
    if (isFirst)
      printSpaces();
    root.os.print(b);
  }

  void print(int i) throws IOException
  {
    if (isFirst)
      printSpaces();
    root.os.print(i);
  }

  void print(char c) throws IOException
  {
    if (isFirst)
      printSpaces();

    root.os.print(c);
    if (c == '\n')
      destLine++;
  }

  void print(String s) throws IOException
  {
    if (isFirst)
      printSpaces();

    if (s == null)
      s = "null";

    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) == '\n') {
        isFirst = true;
        destLine++;
      }
      else
        isFirst = false;
    }

    root.os.print(s);
  }

  void print(Object o) throws IOException
  {
    if (isFirst)
      printSpaces();

    print(String.valueOf(o));
  }

  void println() throws IOException
  {
    if (isFirst)
      printSpaces();

    root.os.println();
    destLine++;
    isFirst = true;
  }

  void println(String s) throws IOException
  {
    print(s);
    println();
  }

  void println(Object o) throws IOException
  {
    print(String.valueOf(o));
    println();
  }

  void printSpaces() throws IOException
  {
    for (int i = 0; i < printDepth; i++)
      root.os.print(' ');
    isFirst = false;
  }

  static class Location {
    String filename;
    int line;

    Location(String filename, int line)
    {
      this.filename = filename;
      this.line = line;
    }
  }
}
