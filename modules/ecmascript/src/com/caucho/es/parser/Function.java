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

import com.caucho.es.ESId;
import com.caucho.util.CharBuffer;
import com.caucho.util.IntMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Function is an intermediate form representing an expression.
 */
class Function {
  static ESId PROTOTYPE = ESId.intern("prototype");

  Parser parser;
  ParseClass cl;
  private Function parent;

  int funDepth;
  private int lambdaCount = 0;

  ArrayList formals;
  Expr returnType;
  
  ArrayList variables = new ArrayList();

  // child closures 
  ArrayList functions;
  private IntMap funMap;
  

  boolean isClass;
  ESId classProto; // null for non-classes
  Function constructor;

  String name;
  ESId id;
  int num;
  boolean isSilent;
  boolean hasCall;
  boolean hasThis;
  boolean allowLocals;
  boolean allowJavaLocals;
  boolean needsScope;
  
  ArrayList data = new ArrayList();
  private HashMap vars = new HashMap();
  CharBuffer tail;
  private int iterCount;
  private int tempCount;
  private int stmtCount;
  private int stmtTop;
  private HashMap usedVars;
  private boolean isGlobal;
  // True if an arguments variable needs to be created.  eval, closures,
  // the arguments variable and "with" will set this.
  private boolean needsArguments;
  private boolean needsStatementResults;
  // True if any variable can be used even if not explicitly referenced
  // e.g. if the "arguments" variable is used.
  private boolean useAllVariables;
  // True if the function contains an eval
  private boolean isEval;
  // True if the function contains a switch
  private boolean hasSwitch;

  Function(ParseClass cl, Function parent,
           String name, ESId id, boolean isClass)
  {
    this.id = id;
    this.name = name;
    this.parent = parent;
    this.cl = cl;
    
    if (parent == null || isClass)
      funDepth = 0;
    else
      funDepth = parent.funDepth + 1;

    num = -1;

    isGlobal = parent == null;
    allowLocals = funDepth >= 1 || isClass;
    allowJavaLocals = allowLocals;
    needsStatementResults = parent == null;
  }

  void setFast(boolean isFast)
  {
    if (isFast) {
      allowLocals = true;
      allowJavaLocals = true;
    }
  }
  
  Function getParent()
  {
    return parent;
  }

  boolean isGlobalScope()
  {
    return ! needsScope && (getFunctionDepth() == 0 ||
                            getFunctionDepth() == 1 && ! needsArguments());
  }

  boolean needsStatementResults()
  {
    return needsStatementResults;
  }

  void setNeedsResults()
  {
    needsStatementResults = true;
  }

  void setEval()
  {
    setArguments();
    needsStatementResults = true;
    isEval = true;
  }

  boolean useAllVariables()
  {
    return isEval || useAllVariables;
  }

  void setUseAllVariables()
  {
    useAllVariables = true;
  }

  String getStatementVar()
  {
    if (! needsStatementResults())
      return null;
    else
      return "_val" + stmtCount;
  }

  void pushStatementLoop()
  {
    if (! needsStatementResults())
      return;

    stmtCount++;
    if (stmtCount > stmtTop)
      stmtTop = stmtCount;
  }

  void popStatementLoop()
  {
    if (! needsStatementResults())
      return;

    stmtCount--;
  }

  void setCall()
  {
    this.hasCall = true;
  }

  void setThis()
  {
    this.hasThis = true;
  }

  /**
   * Force all unassigned variables to become object vars.
   */
  void setVars()
  {
    Iterator iter = vars.values().iterator();
    while (iter.hasNext()) {
      Variable var = (Variable) iter.next();
      var.getType();
    }
  }

  boolean isGlobal()
  {
    return isGlobal;
  }

  int getFunctionDepth()
  {
    return funDepth;
  }

  void disableGlobal()
  {
    isGlobal = false;
  }

  void disallowLocal()
  {
    if (parent != null) {
      allowJavaLocals = false;
      allowLocals = false;
      needsScope = true;
    }
  }

  void disallowJavaLocal()
  {
    allowJavaLocals = false;
  }

  void setNeedsScope()
  {
    if (parent != null)
      needsScope = true;
  }

  void setArguments()
  {
    needsArguments = true;
    setNeedsScope();
    disallowLocal();
  }

  boolean needsArguments()
  {
    return needsArguments;
  }
  
  boolean allowLocals()
  {
    return allowLocals;
  }
  
  boolean allowJavaLocals()
  {
    return allowLocals && allowJavaLocals;
  }

  void useClosureVar(ESId name)
  {
    for (Function fun = parent; fun != null; fun = fun.parent) {
      Variable var = (Variable) fun.vars.get(name);

      fun.needsArguments = true;
      
      if (var != null)
        var.setUsedByClosure();
      else {
        if (fun.usedVars == null)
          fun.usedVars = new HashMap();
        fun.usedVars.put(name, name);
      }
    }
  }

  /**
   * Returns true if the variable is declared.
   */
  boolean hasVar(ESId name)
  {
    return vars.get(name) != null;
  }
  
  /**
   * Returns a new variable.
   */
  IdExpr newVar(Block block, ESId name)
  {
    return newVar(block, name, null);
  }
  
  /**
   * Returns a new variable.
   */
  IdExpr newVar(Block block, ESId name, Expr type)
  {
    Variable var = (Variable) vars.get(name);

    if (var == null && type == null) {
      var = cl.getVariable(name);
      if (var != null) {
        return new IdExpr(block, var);
      }
    }
    
    if (var == null) {
        
      var = new Variable(block, name, type, false);
      vars.put(name, var);
      
      if (usedVars != null && usedVars.get(name) != null)
        var.setUsedByClosure();
    }

    useClosureVar(name);

    return new IdExpr(block, var);
  }

  /**
   * Add a variable to the function.  If the function is the global
   * function, add it to the class.
   */
  void addVariable(Block block, ESId id, Expr type)
  {
    if (variables == null)
      variables = new ArrayList();

    Variable var = (Variable) vars.get(id);
    if (var == null) {
      var = new Variable(block, id, type, allowLocals);
      vars.put(id, var);
      
      if (usedVars != null && usedVars.get(id) != null)
        var.setUsedByClosure();

      // Only add global variables if they're declared, not if defined
      // by use.
      if (parent == null && type != null)
        cl.addVariable(id, var);
    }
    else if (parent != null)
      var.setLocal();

    if (! variables.contains(var) &&
        (formals == null || ! formals.contains(var)))
      variables.add(var);
    
    useClosureVar(id);
  }

  int getIter()
  {
    return iterCount++;
  }

  String getTemp()
  {
    return "temp" + tempCount++;
  }

  void setConstructor(Function constructor)
  {
    isClass = true;
    this.constructor = constructor;
  }

  void setClassProto(ESId classProto)
  {
    isClass = true;
    this.classProto = classProto;
  }

  void setCodeNumber(int num)
  {
    this.num = num;
  }

  void addFormal(Block block, ESId id, Expr type)
  {
    if (formals == null)
      formals = new ArrayList();

    Variable var = new Variable(block, id, type, true);
    var.setUsed();
    formals.add(var);
    vars.put(id, var);
  }

  int getFormalSize()
  {
    return formals == null ? 0 : formals.size();
  }

  Variable getFormal(int j)
  {
    return (Variable) formals.get(j);
  }

  Expr getReturnType()
  {
    return returnType;
  }

  void setReturnType(Expr type)
  {
    returnType = type;
  }

  int getVariableSize()
  {
    return variables == null ? 0 : variables.size();
  }

  void addFunction(Function function)
  {
    if (functions == null) {
      functions = new ArrayList();
      funMap = new IntMap();
    }
    int pos = funMap.get(function.id);

    if (pos < 0) {
      funMap.put(function.id, functions.size());
      functions.add(function);
    }
    else
      functions.set(pos, function);
  }

  int getFunctionSize()
  {
    return functions == null ? 0 : functions.size();
  }

  Function getFunction(int i)
  {
    return (Function) functions.get(i);
  }

  Function getFunction(ESId id)
  {
    if (funMap == null)
      return null;

    int index = funMap.get(id);

    if (index >= 0) {
      return (Function) functions.get(index);
    }
    else
      return null;
  }

  void print(Object value)
  {
    if (tail == null)
      tail = CharBuffer.allocate();
    tail.append(String.valueOf(value));
  }

  void println(String value)
  {
    if (tail == null)
      tail = CharBuffer.allocate();
    tail.append(String.valueOf(value));
    tail.append('\n');
  }

  void addExpr(Expr expr)
  {
    if (tail != null)
      data.add(tail);
    tail = null;
    data.add(expr);
    expr.setUsed();
  }

  void addBoolean(Expr expr)
  {
    if (tail != null)
      data.add(tail);
    tail = null;

    data.add(expr.setBoolean());
    expr.setUsed();
  }

  Object getTop()
  {
    if (tail != null)
      return tail;
    else if (data.size() > 0)
      return data.get(data.size() - 1);
    else {
      tail = new CharBuffer();
      return tail;
    }
  }

  Object getSwitch()
  {
    if (tail != null)
      data.add(tail);
    tail = null;

    hasSwitch = true;
    
    return data.get(data.size() - 1);
  }
  
  int mark()
  {
    if (tail != null)
      data.add(tail);
    tail = null;
    
    return data.size();
  }

  void moveChunk(Object topObject, int mark)
  {
    if (tail != null)
      data.add(tail);
    tail = null;

    int top;
    for (top = 0; top < mark; top++) {
      if (data.get(top) == topObject)
        break;
    }
    top++;

    int here = data.size();

    for (int i = 0; i < here - mark; i++) {
      Object chunk = data.remove(data.size() - 1);
      data.add(top, chunk);
    }
  }

  void writeCode(ParseClass cl) throws IOException
  {
    cl.print("public ");

    if (returnType == null)
      cl.print("ESBase ");
    else if (returnType.getType() == Expr.TYPE_INTEGER)
      cl.print("int ");
    else
      cl.print("ESBase ");
    
    cl.print(name + "(Call _env, int _length");

    for (int i = 0; i < getFormalSize(); i++) {
      Variable formal = (Variable) formals.get(i);
      
      if (formal.getTypeExpr() instanceof TypeExpr) {
        TypeExpr type = (TypeExpr) formal.getTypeExpr();

        if (type.getTypeName() != null)
          cl.print(", " + type.getTypeName() + " " + formal.getId());
      }
    }
    cl.println(")");
    
    cl.println("throws Throwable");
    cl.println("{");
    cl.pushDepth();
    
    if (hasCall)
      cl.println("Call _call = _env.getCall();");

    if (hasThis)
      cl.println("ESObject _this = _env.getThis();");

    if (parent != null && functions != null && functions.size() > 0) {
      needsArguments = true;
      setNeedsScope();
    }

    // Eval just uses the calling scope
    if (isEval) {
      cl.println("ESObject _arg = _env.getEval();");
    }
    else {
      if (needsScope && parent != null)
        cl.println("_env.fillScope();");

      if (needsArguments && parent != null) {
        cl.print("ESObject _arg = _env.createArg(");
        if (getFormalSize() > 0)
          cl.print("_js._a_" + num);
        else
          cl.print("_js._a_null");
        cl.println(", _length);");
      }
    }

    printFormals();
    printLocalVariables();

    // do closures
    for (int i = 0;
         needsArguments && functions != null && i < functions.size();
         i++) {
      if (i == 0)
        cl.println("ESClosure _closure;");
        
      Function fun = (Function) functions.get(i);
      Variable var = (Variable) vars.get(fun.id);

      if (! isGlobal && ! isEval &&
          var != null && ! var.isUsed() && ! useAllVariables) {
        continue;
      }
      
      cl.print("_closure = new ESClosure(");
      cl.printLiteral(fun.id);
      cl.println(", _js, null, " + fun.num + ", _js._a_null, null);");
      cl.println("_closure.closure(_env);");
        
      if (! isEval && allowLocals &&
          var != null && var.isUsed() && var.isJavaLocal()) {
        cl.println("ESBase " + fun.id + " = _closure;");
        continue;
      }
      else if (! isEval && parent != null)
        cl.print("_arg.put(");
      else
        cl.print("_env.global.put(");
      cl.printLiteral(fun.id);
      cl.print(", _closure, ");
      if (! isEval)
        cl.print("ESBase.DONT_ENUM|ESBase.DONT_DELETE");
      else
        cl.print("0");
      cl.println(");");
    }
    
    for (int i = 0; i < iterCount; i++)
      cl.println("java.util.Iterator iter" + i + ";");
    for (int i = 0; i < tempCount; i++)
      cl.println("ESBase temp" + i + ";");
    if (needsStatementResults())
      cl.println("ESBase _val0 = ESBase.esUndefined;");
    for (int i = 1; i <= stmtTop; i++)
      cl.println("ESBase _val" + i + " = ESBase.esUndefined;");
    if (hasSwitch) {
      cl.println("int _switchcode;");
      cl.println("ESBase _switchtemp;");
    }

    for (int i = 0; i < data.size(); i++) {
      Object d = data.get(i);
      if (d instanceof CharBuffer)
        cl.print((CharBuffer) d);
      else if (d instanceof Expr) {
        Expr expr = (Expr) d;
        //cl.setLine(expr.getFilename(), expr.getLine());
        expr.printExpr();
      }
    }

    if (tail != null)
      cl.print(tail);

    cl.popDepth();
    cl.println("}");
  }

  void printFormals()
    throws IOException
  {
    formal:
    for (int i = 0; formals != null && i < formals.size(); i++) {
      Variable formal = (Variable) formals.get(i);
      
      // Functions have priority
      if (funMap != null && funMap.get(formal.getId()) >= 0)
        continue formal;

      // Duplicate formals use the last one
      for (int j = i + 1; j < formals.size(); j++) {
        if (formal.getId() == ((Variable) formals.get(j)).getId())
          continue formal;
      }

      if (! allowLocals) {
      }
      else if (formal.getTypeExpr() instanceof JavaTypeExpr) {
      }
      else if (formal.getType() != Expr.TYPE_INTEGER) {
        cl.print("ESBase " + formal.getId());
        cl.println(" = _env.getArg(" + i + ", _length);");
      }
    }
  }

  /**
   * Initializes the local variables for a function.
   */
  void printLocalVariables()
    throws IOException
  {
    for (int i = 0; i < variables.size(); i++) {
      Variable var = (Variable) variables.get(i);

      if (funMap != null && funMap.get(var.getId()) >= 0) {
        var.killLocal();
        continue;
      }

      if (var.isJavaGlobal()) {
        // already initialized
      }
      else if ((! allowLocals || ! var.isJavaLocal()) && isGlobal()) {
        cl.print("_env.global.setProperty(");
        cl.printLiteral(var.getId());
        cl.println(", ESBase.esUndefined);");
      }
      else if (! var.isUsed() && ! useAllVariables()) {
      }
      else if (! allowLocals || ! var.isJavaLocal()) {
        if (isEval) {
          cl.print("if (_arg.getProperty(");
          cl.printLiteral(var.getId());
          cl.println(") == ESBase.esEmpty)");
          cl.print("  ");
        }
        if (! var.hasInit()) {
          cl.print("_arg.put(");
          cl.printLiteral(var.getId());
          cl.print(", ESBase.esUndefined, ");
          if (! isEval)
            cl.print("ESBase.DONT_ENUM|ESBase.DONT_DELETE");
          else
            cl.print("0");
          cl.println(");");
        }
      }
      else if (var.getType() == Expr.TYPE_BOOLEAN) {
        cl.println("boolean " + var.getId() + ";");
        if (! var.hasInit())
          cl.println(var.getId() + " = false;");
      }
      else if (var.getType() == Expr.TYPE_INTEGER) {
        cl.println("int " + var.getId() + ";");
        if (! var.hasInit())
          cl.println(var.getId() + " = 0;");
      }
      else if (var.getType() == Expr.TYPE_NUMBER) {
        cl.println("double " + var.getId() + ";");
        if (! var.hasInit())
          cl.println(var.getId() + " = Double.NaN;");
      }
      else if (var.getType() == Expr.TYPE_STRING) {
        cl.println("ESString " + var.getId() + ";");
        if (! var.hasInit())
          cl.println(var.getId() + " = null;");
      }
      else if (var.getType() == Expr.TYPE_JAVA &&
               var.getTypeExpr() != null) {
        TypeExpr type = (TypeExpr) var.getTypeExpr();
        
        cl.println(type.getTypeName() + " " + var.getId() + " = null;");
      }
      else if (var.isLocal()) {
        if (! var.hasInit())
          cl.println("ESBase " + var.getId() + " = ESBase.esUndefined;");
        else
          cl.println("ESBase " + var.getId() + ";");
      }
      else {
        cl.print("  _env.global.setProperty(");
        cl.printLiteral(var.getId());
        cl.println(", ESBase.esUndefined);");
      }
    }
  }
}
