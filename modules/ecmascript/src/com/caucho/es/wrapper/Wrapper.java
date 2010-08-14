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

package com.caucho.es.wrapper;

import com.caucho.es.ESArrayWrapper;
import com.caucho.es.ESBase;
import com.caucho.es.ESBeanWrapper;
import com.caucho.es.Global;
import com.caucho.java.JavaCompiler;
import com.caucho.loader.SimpleLoader;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CharBuffer;
import com.caucho.util.IntMap;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.MergePath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

public class Wrapper {
  private static final Integer LOCK = new Integer(0);
  private static final Logger log
    = Logger.getLogger(Wrapper.class.getName());
  
  private String name;
  private String javaClassName;
  private Class cl;
  private boolean isPublic;
  private Path dest;
  private WriteStream os;
  private ClassLoader loader;
  private JavaCompiler compiler;

  private ESBeanInfo beanInfo;
  private IntMap hasDispatch;
  private IntMap staticHasDispatch;
  private IntMap setDispatch;
  private IntMap staticSetDispatch;
  private IntMap methodDispatch;
  private IntMap staticMethodDispatch;
  private HashMap namedProperties;
  
  private ArrayList overloadDispatch;
  
  private int depth;
  private boolean isNewline;
  private Class esBase;

  /**
   * Creates the instance of the wrapper generator.
   *
   * @param resin the global parent object
   * @param cl the class to wrap.
   */
  private Wrapper(Global resin, Class cl)
  {
    name = cl.getName().replace('/', '.');

    MergePath mergePath = new MergePath();
    mergePath.addClassPath(cl.getClassLoader());
    Path destClass = mergePath.lookup(name.replace('.', '/') + ".class");

    // technically, need to resort to dynamic.  This is a cheat.
    if (! destClass.exists() && cl.getInterfaces().length > 0) {
      cl = cl.getInterfaces()[0];
      name = cl.getName().replace('/', '.');
    }
    
    javaClassName = toJavaClassName(name);

    CharBuffer cb = new CharBuffer();
    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);

      if (ch == '$')
        cb.append("_0");
      else if (ch == '_')
        cb.append("__");
      else
        cb.append(ch);
    }
      
    name = "_jsbean." + cb + "_es";

    this.cl = cl;

    isPublic =  Modifier.isPublic(cl.getModifiers());

    //this.loader = resin.getParentLoader();
    loader = cl.getClassLoader();

    compiler = JavaCompiler.create(loader);
    //compiler.setEncoding("utf8");
    Path workPath = CauchoSystem.getWorkPath();
    
    dest = workPath.lookup(name.replace('.', '/') + ".java");

    hasDispatch = new IntMap();
    staticHasDispatch = new IntMap();
    setDispatch = new IntMap();
    staticSetDispatch = new IntMap();
    methodDispatch = new IntMap();
    staticMethodDispatch = new IntMap();
    namedProperties = new HashMap();
    
    overloadDispatch = new ArrayList();

    try {
      esBase = Class.forName("com.caucho.es.ESBase");
    } catch (Exception e) {
    }
  }

  public static ESBase []bean(Global resin, Class cl)
    throws Throwable
  {
    Wrapper wrapper = null;
    
    if (cl.isArray()) {
      ESBase arrayWrapper = ESArrayWrapper.wrapper(resin, cl);
      return new ESBase[] { arrayWrapper.getProperty("CONSTRUCTOR"),
                            arrayWrapper };
    }

    synchronized (LOCK) {
      wrapper = new Wrapper(resin, cl);
      ESBeanWrapper beanWrapper = wrapper.wrap();
      beanWrapper.n = 0;
      
      return new ESBase[] { beanWrapper.wrapStatic(), beanWrapper };
    }
  }

  /**
   * Creates the wrapper for a class
   */
  private ESBeanWrapper wrap() throws Throwable
  {
    dest.getParent().mkdirs();

    Path workPath = CauchoSystem.getWorkPath();
    Path destClass = workPath.lookup(name.replace('.', '/') + ".class");

    ClassLoader beanLoader;
    beanLoader = SimpleLoader.create(loader,
                                          CauchoSystem.getWorkPath(),
                                          name);
    ESBeanWrapper wrapper;

    try {
      Class cl = CauchoSystem.loadClass(name, false, beanLoader);
      wrapper = (ESBeanWrapper) cl.newInstance();

      if (! wrapper.isModified() && wrapper.getVersionId() == CauchoSystem.getVersionId())
        return wrapper;
    } catch (Throwable e) {
    }
    
    destClass.remove();
    os = dest.openWrite();

    beanInfo = ESIntrospector.getBeanInfo(cl);

    try {
      printHeader();
      printConstructors();
      printHasProperty();
      printSetProperty();
      printKeys();
      printDeletes();
      printMethods();
      printInit();
      printFooter();
    } finally {
      os.close();
    }

    compiler.compile(name.replace('.', '/') + ".java", null);

    beanLoader = SimpleLoader.create(loader,
                                          CauchoSystem.getWorkPath(),
                                          name);
    
    try {
      Class cl = CauchoSystem.loadClass(name, false, beanLoader);
      wrapper = (ESBeanWrapper) cl.newInstance();
    } catch (NoClassDefFoundError e) {
      e.printStackTrace();
      throw e;
    }

    return wrapper;
  }

  private long getSourceLastModified(Class cl)
  {
    long lastModified = 0;
    String classPath;
    
    URL resource = null;
    String clName = cl.getName().replace('.', '/') + ".class";
    String name;

    if (loader != null)
      resource = loader.getResource(clName);

    String fileName = resource != null ? resource.toExternalForm() : null;

    // XXX: need to implement jar: filesystem
    if (resource == null ||
        fileName.startsWith("systemresource:") ||
        fileName.startsWith("jar:"))
      return getClassPathLastModified(cl);

    Path path = Vfs.lookup(fileName);

    if (path != null && path.canRead())
      return path.getLastModified();
    else
      return 0;
  }

  private long getClassPathLastModified(Class cl)
  {
    String clName = cl.getName().replace('.', '/') + ".class";

    String classPath = System.getProperty("java.class.path");
        
    char sep = CauchoSystem.getPathSeparatorChar();
    int head = 0;
    int tail;
    for (; (tail = classPath.indexOf(sep, head)) >= 0; head = tail + 1) {
      String name = classPath.substring(head, tail);
      Path path = Vfs.lookupNative(name);

      if (name.endsWith(".jar") || name.endsWith(".zip"))
        path = JarPath.create(path);

      if (path != null && path.lookup(clName).canRead()) {
        return path.lookup(clName).getLastModified();
      }
    }

    String name = classPath.substring(head);
    Path path = Vfs.lookupNative(name);

    if (name.endsWith(".jar") || name.endsWith(".zip"))
      path = JarPath.create(path);

    if (path != null && path.lookup(clName).canRead())
      return path.lookup(clName).getLastModified();
    
    return 0;
  }

  private void printHeader() throws IOException
  {
    int p = name.lastIndexOf('.');
    String pkg = name.substring(0, p);
    String clName = name.substring(p + 1);

    println("package " + pkg + ";");
    println("import com.caucho.es.*;");
    Iterator iter = beanInfo.getNonPkgClasses().iterator();
    while (iter.hasNext()) {
      String name = (String) iter.next();
      println("import " + name + ";");
    }
    println();
    println("public class " + clName + " extends ESBeanWrapper {");
    pushDepth();
    if (isPublic)
      println("private " + javaClassName + " _value;");

    println();
    println("public long getVersionId() { return " +
            CauchoSystem.getVersionId() + "L; }");
    
    println();
    println("protected ESBeanWrapper dup()");
    println("{");
    println("  return new " + clName + "();");
    println("}");

    println();
    println("public ESBeanWrapper wrap(Object value)");
    println("{");
    pushDepth();
    println("if (value == null) throw new NullPointerException();");
    println(name + " child = new " + name + "();");
    println("child.value = value;");
    if (isPublic)
      println("child._value = (" + javaClassName + ") value;");
    println("child.hasDispatch = instanceHasDispatch;");
    println("child.setDispatch = instanceSetDispatch;");
    println("child.methodDispatch = instanceMethodDispatch;");
    println("child.n = -1;");
    println("return child;");
    popDepth();
    println("}");
    
    println();
    println("public ESBeanWrapper wrapStatic()");
    println("{");
    pushDepth();
    println(name + " child = new " + name + "();");
    println("child.hasDispatch = staticHasDispatch;");
    println("child.setDispatch = staticSetDispatch;");
    println("child.methodDispatch = staticMethodDispatch;");
    println("child.n = -2;");
    println("child.name = \"" + javaClassName + "\";");
    println("try {");
    println("  child.value = Class.forName(child.name);");
    println("} catch (Exception e) {}");
    println("return child;");
    popDepth();
    println("}");

    println();
    println("public Class getJavaType()");
    println("{");
    pushDepth();
    println("return value.getClass();");
    popDepth();
    println("}");
  }
  
  private void printConstructors() throws IOException
  {
    println();
    println("public ESBase construct(Call call, int length)");
    println("  throws Throwable");
    println("{");
    pushDepth();
    println("if (n != -2)");
    println("  throw new ESException(\"can't create `" + javaClassName + "'\");");
    println();

    if (printMethodConstructor()) {
      popDepth();
      println("}");
      return;
    }
    
    ArrayList overload = beanInfo.getConstructors();
    if (Modifier.isAbstract(cl.getModifiers()))
      overload = null;
    
    if (overload == null || overload.size() == 0) {
      println("  throw new ESException(\"can't create `" + javaClassName + "'\");");
    }
    else {

    Constructor last = null; 
    for (int i = 0; i < overload.size(); i++) {
      if (overload.get(i) instanceof Constructor)
        last = (Constructor) overload.get(i);
    }

    for (int i = 0; i < overload.size(); i++) {
      Object o = overload.get(i);
      if (! (o instanceof Constructor))
        continue;

      Constructor constructor = (Constructor) o;
      if (constructor != last) {
        println("if (length <= " + i + ")");
        print("  ");
      }

      print("return wrap(new " + javaClassName + "(");
      Class []param = constructor.getParameterTypes();
      for (int j = 0; j < param.length; j++) {
        if (j > 0)
          print(", ");

        printArgToJava(param[j], j);
      }
      println("));");
    }
    
    if (last == null)
      println("throw new ESException(\"can't create `" + javaClassName + "'\");");
    }

    popDepth();
    println("}");
  }

  private boolean printMethodConstructor() throws IOException
  {
    ArrayList overload = (ArrayList) beanInfo._staticMethodMap.get("create");
    if (overload != null) {
      printMethod(Integer.MIN_VALUE, "create", overload, null);
      return true;
    }
    else
      return false;
  }

  /**
   * Print the code for accessing properties.
   */
  private void printHasProperty() throws IOException
  {
    println();
    println("public ESBase hasProperty(ESString name)");
    println("  throws Throwable");
    println("{");
    pushDepth();
    println("ESBase temp;");
    println("switch (hasDispatch.get(name)) {");
    
    PropertyDescriptor []props = beanInfo.getPropertyDescriptors();

    int index = 1;
    for (int i = 0; i < props.length; i++) {
      if (props[i] instanceof NamedPropertyDescriptor)
        index = doHasNamedProperty(index, (NamedPropertyDescriptor) props[i]);
      else if (props[i] instanceof ESIndexedPropertyDescriptor)
        index = doHasIndexProperty(index,
                                   (ESIndexedPropertyDescriptor) props[i]);
      else if (props[i] instanceof ESPropertyDescriptor)
        index = doHasProperty(index, (ESPropertyDescriptor) props[i]);
      else
        throw new RuntimeException();
    }
    println("default:");
    println("  return ESBase.esEmpty;");
    println("}");
    popDepth();
    println("}");
  }

  private int doHasIndexProperty(int i, ESIndexedPropertyDescriptor prop)
    throws IOException
  {
    Named named = new Named(prop.getName(), namedProperties.size());
    int n = named.n;
    
    namedProperties.put(prop.getName(), named);
    hasDispatch.put(prop.getName(), i);

    println("case " + i + ":");
    pushDepth();
    println("if (name" + n + " == null) {");
    println("  name" + n + " = new " + name + "();");
    println("  name" + n + ".value = value;");
    if (isPublic)
      println("  name" + n + "._value = _value;");
    println("  name" + n + ".hasDispatch = has" + n + ";");
    println("  name" + n + ".setDispatch = set" + n + ";");
    println("  name" + n + ".delId = " + n + ";");
    println("}");
    println("return name" + n + ";");
    popDepth();

    i += 1;

    ESMethodDescriptor md = prop.getESReadMethod();
    if (md == null)
      return i;

    println("case " + i + ":");
    pushDepth();
    named.get = i;
    
    ESMethodDescriptor size = prop.getESSizeMethod();

    if (size != null) {
      println("if (name.equals(LENGTH)) {");
      pushDepth();
      Method method = size.getMethod();
      Class resultClass = method.getReturnType();
      print("return ");
      startJavaToES(resultClass);
      startProp(size);
      print(")");
      endJavaToES(resultClass);
      println(";");
      popDepth();
      println("} else {");
      pushDepth();
    }
    Method method = md.getMethod();
    Class resultClass = method.getReturnType();
    print("return ");
    startJavaToES(resultClass);
    int p = startProp(md);
    if (p > 0)
      print(", ");
    print("name.toInt32())");
    endJavaToES(resultClass);
    println(";");
    if (size != null) {
      popDepth();
      println("}");
    }

    popDepth();

    return i + 1;
  }

  private int doHasNamedProperty(int i, NamedPropertyDescriptor prop)
    throws IOException
  {
    Named named = new Named(prop.getName(), namedProperties.size());
    int n = named.n;
    
    namedProperties.put(prop.getName(), named);
    hasDispatch.put(prop.getName(), i);

    println("case " + i + ":");
    pushDepth();
    println("if (name" + n + " == null) {");
    println("  name" + n + " = new " + name + "();");
    println("  name" + n + ".value = value;");
    if (isPublic)
      println("  name" + n + "._value = _value;");
    println("  name" + n + ".hasDispatch = has" + n + ";");
    println("  name" + n + ".setDispatch = set" + n + ";");
    println("  name" + n + ".delId = " + n + ";");
    println("}");
    println("return name" + n + ";");
    popDepth();

    i += 1;

    ESMethodDescriptor md = prop.getNamedReadMethod();
    if (md == null)
      return i;

    println("case " + i + ":");
    pushDepth();
    named.get = i;

    Method method = md.getMethod();
    if (Modifier.isStatic(method.getModifiers()) && ! md.isStaticVirtual())
      staticHasDispatch.put(prop.getName(), i - 1);
    
    Class resultClass = method.getReturnType();
    print("return ");
    startJavaToES(resultClass);
    int p = startProp(md);
    if (p > 0)
      print(", ");
    print("name.toJavaString())");
    endJavaToES(resultClass);
    println(";");
    popDepth();
 
    return i + 1;
  }

  private int doHasProperty(int i, ESPropertyDescriptor prop)
    throws IOException
  {
    Field field = prop.getESField();
    ESMethodDescriptor md = prop.getESReadMethod();

    if (field != null && ! Modifier.isPublic(field.getModifiers()))
      field = null;

    if (md == null && field == null)
      return i;
    
    hasDispatch.put(prop.getName(), i);
    
    println("case " + i + ":");
    pushDepth();

    if (field != null) {
      Class resultClass = field.getType();
      print("return ");
      startJavaToES(resultClass);
      if (isPublic && field.getDeclaringClass().getName().equals(cl.getName()))
        print("_value.");
      else if (Modifier.isStatic(field.getModifiers()))
        print(toJavaClassName(field.getDeclaringClass().getName()) + ".");
      else
        print("((" + toJavaClassName(field.getDeclaringClass().getName()) + ") value).");
      print(field.getName());
      endJavaToES(resultClass);
      println(";");
      popDepth();
      
      if (Modifier.isStatic(field.getModifiers()))
        staticHasDispatch.put(prop.getName(), i);
      
      return i + 1;
    }
    
    Method method = md.getMethod();
    
    if (Modifier.isStatic(method.getModifiers()) && ! md.isStaticVirtual())
      staticHasDispatch.put(prop.getName(), i);
      
    print("return ");

    Class resultClass = method.getReturnType();
    
    startJavaToES(resultClass);
    int p = startProp(md);
    print(")");
    endJavaToES(resultClass);
    println(";");
    popDepth();

    return i + 1;
  }
  
  private void printSetProperty() throws IOException
  {
    println();
    println("public void setProperty(ESString name, ESBase newValue)");
    println("  throws Throwable");
    println("{");
    pushDepth();
    println("ESBase temp;");
    println("switch (setDispatch.get(name)) {");
    
    PropertyDescriptor []props = beanInfo.getPropertyDescriptors();

    int index = 0;
    for (int i = 0; i < props.length; i++) {
      if (props[i] instanceof NamedPropertyDescriptor) {
        index = doSetNamedProperty(index,
                                   (NamedPropertyDescriptor) props[i]);
      }
      else if (props[i] instanceof ESIndexedPropertyDescriptor) {
        index = doSetIndexProperty(index,
                                   (ESIndexedPropertyDescriptor) props[i]);
      }
      else if (props[i] instanceof ESPropertyDescriptor)
        index = doSetProperty(index, (ESPropertyDescriptor) props[i]);
      else
        throw new RuntimeException();
    }
    println("default:");
    println("  return;");
    println("}");
    popDepth();
    println("}");
  }

  private int doSetNamedProperty(int i, NamedPropertyDescriptor prop)
    throws IOException
  {
    Named named = (Named) namedProperties.get(prop.getName());
    if (named == null)
      return i;
    
    int n = named.n;

    ESMethodDescriptor md = prop.getNamedWriteMethod();
    if (md == null)
      return i;

    println("case " + i + ":");
    pushDepth();
    named.set = i;
    
    int p = startProp(md);
    Class []param = md.getParameterTypes();
    
    if (p != 0)
      print(", ");
    print("name.toJavaString(), ");
    printValueToJava(param[1], "newValue");
    println(");");

    println("return;");
    popDepth();

    return i + 1;
  }

  private int doSetIndexProperty(int i, ESIndexedPropertyDescriptor prop)
    throws IOException
  {
    Named named = (Named) namedProperties.get(prop.getName());
    if (named == null)
      return i;
    
    int n = named.n;

    ESMethodDescriptor md = prop.getESWriteMethod();
    if (md == null)
      return i;

    println("case " + i + ":");
    pushDepth();
    named.set = i;
    
    int p = startProp(md);
    Class []param = md.getParameterTypes();
    
    if (p != 0)
      print(", ");
    print("name.toInt32(), ");
    printValueToJava(param[1], "newValue");
    println(");");

    println("return;");
    popDepth();

    return i + 1;
  }

  private int doSetProperty(int i, ESPropertyDescriptor prop)
    throws IOException
  {
    ESMethodDescriptor md = prop.getESWriteMethod();
    Field field = prop.getESField();
    if (field != null && Modifier.isFinal(field.getModifiers()))
      field = null;

    if (md == null && field == null)
      return i;

    println("case " + i + ":");
    pushDepth();
    
    setDispatch.put(prop.getName(), i);

    if (field != null) {
      Class resultClass = field.getType();
      if (isPublic)
        print("_value.");
      else
        print("((" + field.getDeclaringClass().getName() + ") value).");
      print(field.getName());
      print(" = ");
      printValueToJava(resultClass, "newValue");
      println(";");
      println("return;");
      popDepth();
      return i + 1;
    }
    
    Method method = md.getMethod();
    
    if (Modifier.isStatic(method.getModifiers()) && ! md.isStaticVirtual())
      staticSetDispatch.put(prop.getName(), i);
    
    Class []param = md.getParameterTypes();
    
    int p = startProp(md);
    if (p != 0)
      print(", ");
    printValueToJava(param[0], "newValue");
    println(");");

    println("return;");
    
    popDepth();

    return i + 1;
  }
  
  private void printKeys() throws IOException
  {
    println();
    println("public java.util.Iterator keys()");
    println("  throws Throwable");
    println("{");
    pushDepth();
    println("switch (delId) {");

    ESMethodDescriptor md = beanInfo.iterator;
    if (md != null) {
      println("case -1:");
      print("  return Call.toESIterator(");
      startProp(md);
      println("));");
    }

    PropertyDescriptor []props = beanInfo.getPropertyDescriptors();
    for (int i = 0; i < props.length; i++) {
      if (props[i] instanceof NamedPropertyDescriptor)
        printNamedKey((NamedPropertyDescriptor) props[i]);
    }
    
    println("default:");
    println("  return super.keys();");
    println("}");
    popDepth();
    println("}");
  }

  private void printNamedKey(NamedPropertyDescriptor prop)
    throws IOException
  {
    ESMethodDescriptor md = prop.getNamedIteratorMethod();
    if (md == null)
      return;

    Named named = (Named) namedProperties.get(prop.getName());
    println("case " + named.n + ":");
    pushDepth();
    print("return Call.toESIterator(");
    int p = startProp(md);
    println("));");
    popDepth();
  }
  
  private void printDeletes() throws IOException
  {
    println();
    println("public ESBase delete(ESString key)");
    println("  throws Throwable");
    println("{");
    pushDepth();
    println("switch (delId) {");

    PropertyDescriptor []props = beanInfo.getPropertyDescriptors();
    for (int i = 0; i < props.length; i++) {
      if (props[i] instanceof NamedPropertyDescriptor)
        printNamedDelete((NamedPropertyDescriptor) props[i]);
    }
    
    println("default:");
    println("  return ESBoolean.FALSE;");
    println("}");
    popDepth();
    println("}");
  }

  private void printNamedDelete(NamedPropertyDescriptor prop)
    throws IOException
  {
    ESMethodDescriptor md = prop.getNamedRemoveMethod();
    if (md == null)
      return;

    Named named = (Named) namedProperties.get(prop.getName());
    println("case " + named.n + ":");
    pushDepth();
    int p = startProp(md);
    if (p > 0)
      print(", ");
    println("key.toJavaString());");
    println("return ESBoolean.TRUE;");
    popDepth();
  }

  /**
   * Prints all the accessible methods in this object.
   */
  private void printMethods() throws IOException
  {
    println();
    println("public ESBase call(Call call, int length, int n)");
    println("  throws Throwable");
    println("{");
    pushDepth();
    println("ESBase temp;");
    println("switch (n) {");
    
    ArrayList overload = (ArrayList) beanInfo._methodMap.get("call");
    if (overload != null)
      printMethod(-1, "call", overload, null);

    // Print the constructor (code -2)
    ArrayList create = (ArrayList) beanInfo._staticMethodMap.get("create");
    ArrayList call = (ArrayList) beanInfo._staticMethodMap.get("call");
    if (create != null)
      printMethod(-2, "create", create, null);
    else if (call != null)
      printMethod(-2, "call", create, null);
    else {
      println("case -2:");
      println("  return construct(call, length);");
    }
    
    Iterator iter = beanInfo._methodMap.entrySet().iterator();
    int i = 0;
    while (iter.hasNext()) {
      Map.Entry entry = (Map.Entry) iter.next();
      overload = (ArrayList) entry.getValue();
      String name = (String) entry.getKey();

      i = printMethod(i, name, overload, methodDispatch);
    }
    
    iter = beanInfo._staticMethodMap.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry entry = (Map.Entry) iter.next();
      overload = (ArrayList) entry.getValue();
      String name = (String) entry.getKey();

      i = printMethod(i, name, overload, staticMethodDispatch);
    }

    println("}");
    println("return ESBase.esUndefined;");
    popDepth();
    println("}");
  }

  /**
   * Prints a method in a method dispatch.
   */
  private int printMethod(int i, String name, ArrayList overload,
                          IntMap dispatch)
    throws IOException
  {
    ESMethodDescriptor []last = null;

    if (overload == null)
      return i;
    
    for (int j = 0; j < overload.size(); j++) {
      last = (ESMethodDescriptor []) overload.get(j);
    }

    if (last == null) {
      return i;
    }

    if (i > -100) {
      println("case " + i + ":");
      pushDepth();
      if (dispatch != null)
        dispatch.put(name, i++);
    }

    if (overload.size() > 2) {
      ESMethodDescriptor []mds = (ESMethodDescriptor []) overload.get(2);

      for (int j = 0; mds != null && j < mds.length; j++) {
        Class []cl = mds[j].getParameterTypes();
        int p = cl.length - 2;

        if (cl[0].getName().equals("com.caucho.es.Call") &&
            cl[1].getName().equals("int")) {
          printMethod(mds[j], dispatch == null);
          popDepth();
          return i;
        }
      }
    }

    for (int j = 0; j < overload.size(); j++) {
      Object o = overload.get(j);

      if (o == null)
        continue;

      ESMethodDescriptor []mds = (ESMethodDescriptor []) o;
      if (mds != last) {
        println("if (length <= " + j + ") {");
        pushDepth();
      }

      if (mds.length == 1)
        printMethod(mds[0], dispatch == null);
      else {
        String var = "dispatch" + overloadDispatch.size();
        
        overloadDispatch.add(mds);

        println("switch (" + var + ".select(call, length)) {");
        for (int k = 0; k < mds.length; k++) {
          println("case " + k + ":");
          pushDepth();
          printMethod(mds[k], dispatch == null);
          popDepth();
        }

        println("default:");
        println("  throw new ESException(\"no matching method " + mds[0].getName() + "\");");
        println("}");
      }
      
      if (mds != last) {
        popDepth();
        println("}");
      }
    }

    if (i > -100) {
      popDepth();
    }
    
    return i;
  }

  /**
   * Print a single method based on a method descriptor.  The arguments
   * are assumed to come from a Call object.
   *
   * @param md the method descriptor.
   */
  private void printMethod(ESMethodDescriptor md, boolean isProp)
    throws IOException
  {
    boolean hasThrowable = hasException(md.getMethod().getExceptionTypes(),
                                        Throwable.class);

    /*
    if (hasThrowable) {
      println("try {");
      pushDepth();
    }
    */
    
    Class returnCl = md.getReturnType();
    if (! returnCl.getName().equals("void")) {
      print("return ");
      startJavaToES(returnCl);
    }

    Class []param = md.getParameterTypes();
    
    int p;

    if (isProp)
      p = startProp(md);
    else
      p = startCall(md);

    if (param.length == 2 &&
        param[0].getName().equals("com.caucho.es.Call") &&
        param[1].getName().equals("int")) {
      if (p > 0)
        print(", ");
      print("call, length");
    }
    else {
      for (int j = 0; j < param.length; j++) {
        if (j + p > 0)
          print(", ");
      
        printArgToJava(param[j], j);
      }
    }

    if (returnCl.getName().equals("void")) {
      println(");");
      println("return ESBase.esUndefined;");
    }
    else {
      print(")");
      endJavaToES(returnCl);
      println(";");
    }

    /*
    if (hasThrowable) {
      popDepth();
      println("} catch (Exception e) {");
      println("  throw e;");
      println("} catch (RuntimeException e) {");
      println("  throw e;");
      println("} catch (Error e) {");
      println("  throw e;");
      println("} catch (Throwable e) {");
      println("  throw new com.caucho.es.ESException(String.valueOf(e));");
      println("}");
    }
    */
  }

  private boolean hasException(Class []exn, Class cl)
  {
    for (int i = 0; i < exn.length; i++)
      if (exn[i].isAssignableFrom(cl))
        return true;

    return false;
  }

  /**
   * Starts a method call that overloads a property.
   */
  private int startProp(ESMethodDescriptor md)
    throws IOException
  {
    Method method = md.getMethod();
    
    int p = 0;
    if (md.isStaticVirtual()) {
      print(md.getMethodClassName());
      print(".");
      print(md.getMethod().getName());
      if (isPublic)
        print("(_value");
      else
        print("((" + toJavaClassName(md.getObjectClassName()) + ") value");
      p = 1;
    } else if (Modifier.isStatic(method.getModifiers())) {
      print(md.getMethodClassName());
      print(".");
      print(md.getMethod().getName());
      print("(");
    } else {
      if (isPublic)
        print("_value.");
      else
        print("((" + toJavaClassName(md.getObjectClassName()) + ") value).");
      print(md.getMethod().getName());
      print("(");
    }

    return p;
  }

  private int startCall(ESMethodDescriptor md)
    throws IOException
  {
    Method method = md.getMethod();

    int p = 0;
    if (md.isStaticVirtual()) {
      print(toJavaClassName(md.getMethodClassName()));
      print(".");
      print(md.getMethod().getName());
      print("((" + md.getObjectClassName() + ") call.getThisWrapper()");
      p = 1;
    } else if (Modifier.isStatic(method.getModifiers())) {
      print(toJavaClassName(md.getMethodClassName()));
      print(".");
      print(md.getMethod().getName());
      print("(");
    } else {
      print("((" + toJavaClassName(md.getObjectClassName()) + ") call.getThisWrapper()).");
      print(md.getMethod().getName());
      print("(");
    }

    return p;
  }

  private void startJavaToES(Class cl)
    throws IOException
  {
    String name = cl.getName();
    
    switch (classTypes.get(name)) {
    case T_V: 
      //addJsUndefinedRef();
      break;

    case T_Z:
      print("ESBoolean.create(");
      break;

    case T_C:
      print("ESString.createFromCharCode(");
      break;
      
    case T_B: case T_S: case T_I: case T_L:
    case T_F: case T_D:
      print("ESNumber.create(");
      break;

    case T_STRING:
      print("ESString.toStr(");
      break;

    default:
      if (esBase.isAssignableFrom(cl))
        print("((temp = ");
      else
        print("Global.wrap(");
      break;
    }
  }
  
  private void endJavaToES(Class cl)
    throws IOException
  {
    String name = cl.getName();
    
    switch (classTypes.get(name)) {
    case T_V: 
      //addJsUndefinedRef();
      break;

    case T_Z:
    case T_C:
    case T_B: case T_S: case T_I: case T_L:
    case T_F: case T_D:
    case T_STRING:
      print(")");
      break;

    default:
      if (esBase.isAssignableFrom(cl))
        print(") == null ? ESBase.esNull : temp)");
      else
        print(")");
      break;
    }
  }

  private void printValueToJava(Class cl, String value)
    throws IOException
  {
    String name = cl.getName();
    
    switch (classTypes.get(name)) {
    case T_V: 
      throw new RuntimeException();

    case T_Z:
      print(value + ".toBoolean()");
      break;

    case T_C:
      print("(char) " + value + ".toStr().carefulCharAt(0)");
      break;
      
    case T_B:
      print("(byte) " + value + ".toInt32()");
      break;
      
    case T_S:
      print("(short) " + value + ".toInt32()");
      break;
      
    case T_I:
      print(value + ".toInt32()");
      break;
      
    case T_L:
      print("(long)" + value + ".toNum()");
      break;
      
    case T_F:
      print("(float)" + value + ".toNum()");
      break;
      
    case T_D:
      print(value + ".toNum()");
      break;

    case T_STRING:
      print("(" + value + ").toJavaString()");
      break;
      
    default:
      if (cl.isAssignableFrom(esBase))
        print(value);
      else if (esBase.isAssignableFrom(cl)) {
        print("(");
        printClassType(cl);
        print(") " + value);
      }
      else {
        print("(");
        printClassType(cl);
        print(") " + value + ".toJavaObject()");
      }
      break;
    }
  }

  private void printArgToJava(Class cl, int i)
    throws IOException
  {
    String name = cl.getName();
    
    switch (classTypes.get(name)) {
    case T_V: 
      throw new RuntimeException();

    case T_Z:
      print("call.getArg(" + i + ", length).toBoolean()");
      break;

    case T_C:
      print("(char) call.getArg(" + i + ", length).toStr().carefulCharAt(0)");
      break;
      
    case T_B:
      print("(byte) call.getArgInt32(" + i + ", length)");
      break;
      
    case T_S:
      print("(short) call.getArgInt32(" + i + ", length)");
      break;
      
    case T_I:
      print("call.getArgInt32(" + i + ", length)");
      break;
      
    case T_L:
      print("(long) call.getArgNum(" + i + ", length)");
      break;
      
    case T_F:
      print("(float)call.getArgNum(" + i + ", length)");
      break;
      
    case T_D:
      print("call.getArgNum(" + i + ", length)");
      break;

    case T_STRING:
      print("call.getArgString(" + i + ", length)");
      break;
      
    default:
      if (cl.isAssignableFrom(esBase) &&
          ! cl.getName().equals("java.lang.Object"))
        print("call.getArg(" + i + ", length)");
      else if (esBase.isAssignableFrom(cl)) {
        print("(");
        printClassType(cl);
        print(") ");
        print("call.getArg(" + i + ", length)");
      }
      else {
        print("(");
        printClassType(cl);
        print(") call.getArgObject(" + i + ", length)");
      }
      break;
    }
  }

  private void printClassType(Class cl)
    throws IOException
  {
    if (cl.isArray()) {
      printClassType(cl.getComponentType());
      print("[]");
    }
    else {
      print(cl.getName().replace('$', '.'));
    }
  }

  private void printInit() throws IOException
  {
    println("private int delId = -1;");
    println();
    println("private static com.caucho.util.IntMap instanceHasDispatch;");
    println("private static com.caucho.util.IntMap instanceSetDispatch;");
    println("private static com.caucho.util.IntMap instanceMethodDispatch;");
    println("private static com.caucho.util.IntMap staticMethodDispatch;");
    println("private static com.caucho.util.IntMap staticHasDispatch;");
    println("private static com.caucho.util.IntMap staticSetDispatch;");
    Iterator iter = namedProperties.values().iterator();
    while (iter.hasNext()) {
      Named named = (Named) iter.next();
      println(name + " name" + named.n + ";");
      print("private static ConstIntMap has" + named.n);
      println(" = new ConstIntMap(" + named.get + ");");
      print("private static ConstIntMap set" + named.n);
      println(" = new ConstIntMap(" + named.set + ");");
    }
    
    for (int i = 0; i < overloadDispatch.size(); i++) {
      print("private static com.caucho.es.wrapper.MethodDispatcher dispatch" + i);
      println(" = new com.caucho.es.wrapper.MethodDispatcher(new Class[][] {");
      pushDepth();

      ESMethodDescriptor []mds;
      mds = (ESMethodDescriptor []) overloadDispatch.get(i);

      for (int j = 0; j < mds.length; j++) {
        print("new Class[] {");

        Class []param = mds[j].getParameterTypes();

        for (int k = 0; k < param.length; k++) {
          printClass(param[k]);
          print(", ");
        }
        println("},");
      }
      popDepth();
      println("});");
    }
    
    println();
    println("static { _init(); }");

    println();
    println("public boolean isModified()");
    println("{");
    pushDepth();
    com.caucho.make.ClassDependency dep = new com.caucho.make.ClassDependency(cl);
    println("try {");
    println("  Class cl = Class.forName(\"" + cl.getName() + "\", false, Thread.currentThread().getContextClassLoader());");
    println("  return new com.caucho.make.ClassDependency(\""
            + cl.getName() + "\", " + dep.getDigest() + "L).isModified();");
    println("} catch (Throwable e) {");
    println("  return true;");
    println("}");
    popDepth();
    println("}");
    
    println();
    println("private static void _init()");
    println("{");
    
    pushDepth();
    println("instanceHasDispatch = new com.caucho.util.IntMap();");
    iter = hasDispatch.iterator();
    while (iter.hasNext()) {
      String key = (String) iter.next();
      
      println("instanceHasDispatch.put(ESId.intern(\"" + key + "\"), " +
              hasDispatch.get(key) + ");");
    }

    println();
    println("staticHasDispatch = new com.caucho.util.IntMap();");
    iter = staticHasDispatch.iterator();
    while (iter.hasNext()) {
      String key = (String) iter.next();
      
      println("staticHasDispatch.put(ESId.intern(\"" + key + "\"), " +
              staticHasDispatch.get(key) + ");");
    }
    
    println();
    println("instanceSetDispatch = new com.caucho.util.IntMap();");
    iter = setDispatch.iterator();
    while (iter.hasNext()) {
      String key = (String) iter.next();
      
      println("instanceSetDispatch.put(ESId.intern(\"" + key + "\"), " +
              setDispatch.get(key) + ");");
    }
    
    println();
    println("staticSetDispatch = new com.caucho.util.IntMap();");
    iter = staticSetDispatch.iterator();
    while (iter.hasNext()) {
      String key = (String) iter.next();
      
      println("staticSetDispatch.put(ESId.intern(\"" + key + "\"), " +
              staticSetDispatch.get(key) + ");");
    }
    
    println();
    println("instanceMethodDispatch = new com.caucho.util.IntMap();");
    iter = methodDispatch.iterator();
    while (iter.hasNext()) {
      String key = (String) iter.next();
      
      println("instanceMethodDispatch.put(ESId.intern(\"" + key + "\"), " +
              methodDispatch.get(key) + ");");
    }
    
    println();
    println("staticMethodDispatch = new com.caucho.util.IntMap();");
    iter = staticMethodDispatch.iterator();
    while (iter.hasNext()) {
      String key = (String) iter.next();
      
      println("staticMethodDispatch.put(ESId.intern(\"" + key + "\"), " +
              staticMethodDispatch.get(key) + ");");
    }
    
    popDepth();
    println("}");
  }

  private void printClass(Class cl)
    throws IOException
  {
    if (! cl.isArray()) {
      print(cl.getName() + ".class");
      return;
    }

    print("(new ");
    printArrayClass(cl.getComponentType());
    print("[0]).getClass()");
  }

  private void printArrayClass(Class cl)
    throws IOException
  {
    if (cl.isArray()) {
      printArrayClass(cl.getComponentType());
      print("[]");
    }
    else
      print(cl.getName());
  }
  
  private void printFooter() throws IOException
  {
    popDepth();
    println("}");
  }

  private void pushDepth()
  {
    depth += 2;
  }

  private void popDepth()
  {
    depth -= 2;
  }
    
  private void print(String s) throws IOException
  {
    if (isNewline)
      printDepth();
    os.print(s);
  }

  private void println(String s) throws IOException
  {
    if (isNewline)
      printDepth();
    os.println(s);
    isNewline = true;
  }

  private void println() throws IOException
  {
    if (isNewline)
      printDepth();
    os.println();
    isNewline = true;
  }

  private void printDepth() throws IOException
  {
    for (int i = 0; i < depth; i++)
      os.print(' ');
    isNewline = false;
  }

  private String toJavaClassName(String name)
  {
    CharBuffer cb = CharBuffer.allocate();
    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);

      if (ch == '$' && i > 0 && name.charAt(i - 1) != '.')
        cb.append(".");
      else
        cb.append(ch);
    }
    return cb.close();
  }    

  static HashMap<String,String> classNames;
  static {
    classNames = new HashMap<String,String>();
    classNames.put("void", "V");
    classNames.put("boolean", "Z");
    classNames.put("byte", "B");
    classNames.put("short", "S");
    classNames.put("char", "C");
    classNames.put("int", "I");
    classNames.put("long", "J");
    classNames.put("float", "F");
    classNames.put("double", "D");
  }

  static IntMap classTypes;
  static final int T_V = 0;
  static final int T_Z = T_V + 1;
  static final int T_B = T_Z + 1;
  static final int T_S = T_B + 1;
  static final int T_C = T_S + 1;
  static final int T_I = T_C + 1;
  static final int T_L = T_I + 1;
  static final int T_F = T_L + 1;
  static final int T_D = T_F + 1;
  static final int T_STRING = T_D + 1;
  static {
    classTypes = new IntMap();
    classTypes.put("void", T_V);
    classTypes.put("boolean", T_Z);
    classTypes.put("byte", T_B);
    classTypes.put("short", T_S);
    classTypes.put("char", T_C);
    classTypes.put("int", T_I);
    classTypes.put("long", T_L);
    classTypes.put("float", T_F);
    classTypes.put("double", T_D);
    classTypes.put("java.lang.String", T_STRING);
  }

  static class Named {
    String name;
    int n;
    int get = -1;
    int set = -1;
    int keys = -1;
    int remove = -1;
    
    Named(String name, int n)
    {
      this.name = name;
      this.n = n;
    }
  }
}

