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

import com.caucho.es.ESBase;
import com.caucho.es.ESException;
import com.caucho.es.ESId;
import com.caucho.util.CharBuffer;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Block is an intermediate form representing an expression.
 */
class Block {
  private static HashMap specialNames;

  Function function;
  private Block parent;
  private Expr lastExpr;
  boolean isDead;
  private boolean hasStatementValue;
  private Parser parser;
  private boolean isLoop;
  private boolean canExit;
  private boolean hasDefault;
  private int withDepth;
  private ESId id;
  private Expr mark;
  
  private Object switchTop;
  private Object top;
  private int topMark;

  
  private Block()
  {
  }

  Block create()
    throws ESException
  {
    evalExpr();
    
    Block block = new Block();
    
    block.function = function;
    block.parent = this;
    block.lastExpr = null;
    block.isDead = false;
    block.hasStatementValue = hasStatementValue;
    block.parser = parser;
    block.isLoop = false;
    block.canExit = false;
    block.withDepth = withDepth;
    block.id = null;
    block.mark = null;
    block.switchTop = null;
    block.top = null;

    block.setTop();

    return block;
  }

  static Block create(Parser parser, Function function)
  {
    Block block = new Block();

    block.function = function;
    block.parent = null;
    block.lastExpr = null;
    block.isDead = false;
    block.hasStatementValue = function.needsStatementResults();
    block.parser = parser;
    block.isLoop = false;
    block.canExit = false;
    block.withDepth = 0;
    block.id = null;

    block.top = null;
    block.topMark = 0;

    function.setVars();

    return block;
  }

  ClassLoader getClassLoader()
  {
    return parser.getClassLoader();
  }

  void setTop()
  {
    top = function.getTop();
    if (top instanceof CharBuffer) {
      CharBuffer cb = (CharBuffer) top;
      topMark = cb.length();
    }
    else
      topMark = 0;
  }

  Block pop()
  {
    Block parent = this.parent;
    function.setVars();
    free();

    return parent;
  }

  boolean isGlobal()
  {
    return function.isGlobal();
  }

  int getDepth()
  {
    return withDepth;
  }

  boolean allowSpecial()
  {
    return parent == null;
  }

  String getFilename()
  {
    String filename = parser.lexer.getLastFilename();
    int p = filename.lastIndexOf('/');
    if (p > 0)
      filename = filename.substring(p + 1);
    p = filename.lastIndexOf('\\');
    if (p > 0)
      filename = filename.substring(p + 1);
    
    return filename;
  }

  int getLine()
  {
    int line = parser.lexer.getLastLine();
    
    return line;
  }

  void setLine(int line)
  {
  }

  /**
   * Returns true if the variable is already declared.
   */
  boolean hasVar(ESId name)
  {
    return function.hasVar(name) || specialNames.get(name) != null;
  }

  /**
   * Returns an expression for a new variable
   */
  IdExpr newVar(ESId name)
  {
    return newVar(name, null);
  }

  IdExpr newVar(ESId name, Expr type)
  {
    if (withDepth > 0)
      return new IdExpr(this, new Variable(this, name, type, false));
    
    IdExpr expr = function.newVar(this, name, type);

    // Kill variables inside an if
    if (parent != null)
      expr.getType();

    return expr;
  }

  /**
   * Define a new variable.
   */
  void defVar(ESId name)
  {
    function.addVariable(this, name, null);
  }

  /**
   * Define a new variable with the given type.
   */
  void defVar(ESId name, Expr type)
  {
    function.addVariable(this, name, type);
  }

  Expr newLiteral(ESBase value)
  {
    return new LiteralExpr(this, value);
  }

  Expr newRegexp(ESBase value, String flags)
    throws ESException
  {
    return new RegexpExpr(this, value, flags);
  }

  Expr newThis()
  {
    return new SpecialExpr(this, SpecialExpr.THIS);
  }

  Expr newArray(Expr expr)
  {
    return new SpecialExpr(this, SpecialExpr.ARRAY, expr);
  }
  
  Expr hasNext(String iter)
  {
    return new SpecialExpr(this, SpecialExpr.HAS_NEXT, iter);
  }

  Expr newType(ESId name)
  {
    return TypeExpr.create(this, name);
  }
  
  void addExpr(Expr expr)
    throws ESException
  {
    if (isDead)
      throw error("Statement is unreachable.");
    if (lastExpr != null)
      lastExpr.exprStatement(function);
    
    if (hasStatementValue && ! void.class.equals(expr.getJavaClass()))
      lastExpr = expr;
    else {
      lastExpr = null;
      expr.exprStatement(function);
    }
  }
  
  Block startBlock()
    throws ESException
  {
    evalExpr();
    function.println("{");
    return create();
  }
  
  Block startBlock(ESId id)
    throws ESException
  {
    if (findBlock(id) != null)
      throw error("duplicate label `" + id + "'");
    
    evalExpr();
    Block block = create();
    block.id = id;
    function.println(id + ": {");
    block.setTop();
    return block;
  }

  Block finishBlock()
    throws ESException
  {
    evalExpr();
    function.println("}");
    this.id = null;
    Block old = pop();
    if (isDead && ! canExit)
      old.isDead = true;

    return old;
  }

  void endBlock()
    throws ESException
  {
    evalExpr();
    function.println("}");
    this.id = null;
  }
  
  void startIf(Expr expr, boolean isElse)
    throws ESException
  {
    evalExpr();
    if (isElse)
      function.print(" else ");
    function.print("if (");
    function.addBoolean(expr);
    function.println(") {");
    setTop();
  }
  
  void startElse()
    throws ESException
  {
    evalExpr();
    function.println(" else {");
    setTop();
  }
  
  Block startWhile(ESId id, Expr expr)
    throws ESException
  {
    evalExpr();
    if (id != null)
      function.println(id + ":");
    function.print("while (");
    function.addBoolean(expr);
    function.println(") {");

    Block block = create();
    startLoop(id);

    if (! (expr instanceof LiteralExpr) ||
        ! ((LiteralExpr) expr).getLiteral().toBoolean())
      canExit = true;
        
    return block;
  }
  
  Block startFor(ESId id, Expr test, Expr incr)
    throws ESException
  {
    evalExpr();
    if (id != null)
      function.println(id + ":");
    function.print("for (;");
    if (test != null)
      function.addBoolean(test);
    function.print(";");
    if (incr != null)
      function.addExpr(incr);
    function.println(") {");
    function.cl.pushDepth();

    Block block = create();
    startLoop(id);

    if (test == null)
      canExit = false;
    else if (! (test instanceof LiteralExpr) ||
             ! ((LiteralExpr) test).getLiteral().toBoolean())
      canExit = true;
        
    return block;
  }
  
  Block startDo(ESId id)
    throws ESException
  {
    evalExpr();
    if (id != null)
      function.println(id + ":");
    function.print("do {");
    
    Block block = create();
    startLoop(id);
    return block;
  }
  
  Block endDo(Expr expr)
    throws ESException
  {
    evalExpr();
    
    Block old = endLoop();
    
    if (! (expr instanceof LiteralExpr) ||
        ! ((LiteralExpr) expr).getLiteral().toBoolean())
      old.canExit = true;

    if (old.canExit)
      old.isDead = false;
    
    function.print("while (");
    function.addBoolean(expr);
    function.println(");");

    return old;
  }

  void startLoop(ESId id)
  {
    String oldVar = function.getStatementVar();
    function.pushStatementLoop();
    String newVar = function.getStatementVar();
    if (oldVar != null)
      function.println(newVar + " = " + oldVar + ";");
    this.id = id;
    isLoop = true;
    canExit = false;
  }

  Block endLoop()
    throws ESException
  {
    evalExpr();
    String newVar = function.getStatementVar();
    function.popStatementLoop();
    String oldVar = function.getStatementVar();
    if (oldVar != null && ! isDead)
      function.println(oldVar + " = " + newVar + ";");
    function.cl.popDepth();
    function.println("}");

    Block old = pop();
    if (! old.canExit)
      old.isDead = true;

    return old;
  }

  Block startSwitch(Expr test)
    throws ESException
  {
    ESId id = ESId.intern("_switchtemp");
    
    function.print("_switchtemp = ");
    function.addExpr(test);
    function.println(";");
    
    Block block = create();
    block.switchTop = function.getSwitch();
    block.isLoop = true;
    block.hasDefault = false;
    
    function.println("switch (_switchcode) {");
    return block;
  }

  void doCase(int i)
    throws ESException
  {
    isDead = false;
    evalExpr();
    function.println("case " + i + ":");
  }

  void doDefault()
    throws ESException
  {
    isDead = false;
    hasDefault = true;
    evalExpr();
    function.println("default:");
  }

  Block fillSwitch(ArrayList exprs)
    throws ESException
  {
    evalExpr();
    if (! hasDefault && ! isDead) {
      function.println("default:");
      function.println("  break;");
    }
    else if (! isDead)
      function.println("break;");
    function.println("}");

    int mark = function.mark();
    
    for (int i = 0; i < exprs.size(); i++) {
      if (i != 0)
        function.print("else ");

      Expr test = (Expr) exprs.get(i);

      function.print("if (_switchtemp.equals(");
      function.addExpr(test);
      function.println(")) _switchcode = " + i + ";");
    }
    if (exprs.size() > 0)
      function.print("else ");
    function.println("_switchcode = -1;");

    function.moveChunk(switchTop, mark);

    Block old = pop();
    if (isDead && ! canExit && hasDefault)
      old.isDead = true;

    return old;
  }
  
  void doBreak(ESId id)
    throws ESException
  {
    Block block = this;
    for (; block != null; block = block.parent) {
      if (block.id == id) {
        block.canExit = true;
        break;
      }
    }
    
    if (block == null)
      throw error("break needs enclosing loop");
    
    function.setVars();
    evalExpr();
    function.println("break " + id + ";");
    isDead = true;
  }
  
  void doBreak()
    throws ESException
  {
    Block block = this;
    for (; block != null; block = block.parent) {
      if (block.isLoop) {
        block.canExit = true;
        break;
      }
    }
    
    if (block == null)
      throw error("break needs enclosing loop");
    
    function.setVars();
    evalExpr();
    function.println("break;");
    isDead = true;
  }
  
  void doContinue(ESId id)
    throws ESException
  {
    Block block = this;
    for (; block != null; block = block.parent) {
      if (block.id == id && block.isLoop)
        break;
      /*
      else
        block.canExit = true;
      */
    }
    if (block == null)
      throw error("continue needs enclosing loop");
    
    function.setVars();
    evalExpr();
    function.println("continue " + id + ";");
    isDead = true;
  }
  
  void doContinue()
    throws ESException
  {
    if (findBlock(null) == null)
      throw error("continue needs enclosing loop");
    
    function.setVars();
    evalExpr();
    function.println("continue;");
    isDead = true;
  }

  private Block findBlock(ESId id)
  {
    for (Block block = this; block != null; block = block.parent) {
      if (id != null && block.id == id)
        return block;
      else if (id == null && block.isLoop)
        return block;
    }
    
    return null;
  }
  
  Block startWith(Expr expr)
    throws ESException
  {
    function.setArguments();
    function.setUseAllVariables();
    evalExpr();
    withDepth++;
    function.println("try {");
    function.print("_env.pushScope(");
    function.addExpr(expr);
    function.println(");");
    setTop();
    
    return this;
  }
  
  Block endWith()
    throws ESException
  {
    evalExpr();
    withDepth--;
    function.println("} finally {");
    function.println("_env.popScope();");
    function.println("}");
    
    return this;
  }

  int getWithDepth()
  {
    return withDepth;
  }
  
  Block startTry()
    throws ESException
  {
    function.setVars();
    evalExpr();
    function.println("try {");
    return this;
  }
  
  Block endTry()
    throws ESException
  {
    function.setVars();
    evalExpr();
    function.println("}");
    
    return this;
  }
  
  void doTry()
    throws ESException
  {
    evalExpr();

    int i = 0;
    for (; i < function.data.size(); i++) {
      Object o = function.data.get(i);
      if (o != top) {
      }
      else if (o instanceof CharBuffer) {
        CharBuffer cb = (CharBuffer) o;
        cb.insert(topMark, " try {\n");
        break;
      }
      else {
        function.data.add(i + 1, new CharBuffer(" try {\n"));
        break;
      }
    }
    if (i < function.data.size()) {
    }
    else if (function.tail != null && top == function.tail)
      function.tail.insert(topMark, " try {\n");
    else
      function.data.add(0, new CharBuffer(" try {\n"));

    function.println("}");
  }
  
  Block startCatch(String exn, Expr var)
    throws ESException
  {
    evalExpr();
    String temp = "_e" + function.getTemp();
    function.println("catch (" + exn + " " + temp + ") {");
    if (var != null) {
      Expr expr = new SpecialExpr(this, SpecialExpr.EXCEPTION, temp);
      var.assign(expr).exprStatement(function);
    }
      
    isDead = false;
    setTop();
    
    return this;
  }
  
  Block endCatch()
    throws ESException
  {
    evalExpr();
    function.println("}");
    return this;
  }
  
  Block startFinally()
    throws ESException
  {
    evalExpr();
    Block block = create();
    function.println("finally {");
    function.pushStatementLoop();
    block.setTop();

    return block;
  }
  
  Block endFinally()
    throws ESException
  {
    evalExpr();
    function.println("}");
    function.popStatementLoop();
    return pop();
  }
  
  Block startSynchronized(Expr expr)
    throws ESException
  {
    evalExpr();
    function.print("synchronized (");
    function.addExpr(expr);
    function.println(".toJavaObject()) {");

    return create();
  }
  
  Block endSynchronized()
    throws ESException
  {
    evalExpr();

    function.println("}");
    
    Block old = pop();
    old.isDead = isDead;
    
    return old;
  }
  
  void doThrow(Expr expr)
    throws ESException
  {
    function.print("throw (Exception)");
    function.addExpr(expr);
    function.println(".toJavaObject();");
    isDead = true;
  }
  
  void doReturn(Expr value)
    throws ESException
  {
    evalExpr();
    function.print("return ");

    value.setUsed();
    if (function.getReturnType() != null)
      function.addExpr(new TopExpr(this, value,
                                   function.getReturnType()));
    else
      function.addExpr(value);
    
    function.println(";");
    isDead = true;
    /* can't break
    for (Block block = this; block != null; block = block.parent)
      block.canExit = true;
    */
  }
  
  void doReturn()
    throws ESException
  {
    evalExpr();
    
    if (function.getReturnType() != null)
      function.print("return 0;");
    else
      function.print("return ESBase.esUndefined;");
    isDead = true;
    /*
    for (Block block = this; block != null; block = block.parent)
      block.canExit = true;
    */
  }

  void finish()
    throws ESException
  {
    if (isDead)
      return;

    if (lastExpr != null) {
      function.print("return ");
      function.addExpr(lastExpr);
      function.println(";");
      lastExpr = null;
    }
    else if (hasStatementValue)
      function.println("return _val0;");
    else
      function.println("return ESBase.esUndefined;");
  }

  String newIterator(ESId id, Expr expr)
    throws ESException
  {
    evalExpr();
    
    String iter = "iter" + function.getIter();
    function.print(iter + " = ");
    function.addExpr(expr);
    function.println(".keys();");
    
    return iter;
  }

  void evalExpr() throws ESException
  {
    if (lastExpr == null)
      return;

    function.print(function.getStatementVar() + " = ");
    function.addExpr(lastExpr);
    function.println(";");
    
    lastExpr = null;
  }

  private static Block allocate()
  {
    Block block = new Block();

    return block;
  }

  ESException error(String message)
  {
    return parser.lexer.error(message);
  }
  
  void free()
  {
  }

  static {
    specialNames = new HashMap();
    specialNames.put(ESId.intern("Object"), "Object");
    specialNames.put(ESId.intern("Date"), "Date");
    specialNames.put(ESId.intern("String"), "String");
    specialNames.put(ESId.intern("Number"), "Number");
    specialNames.put(ESId.intern("Array"), "Array");
    specialNames.put(ESId.intern("Boolean"), "Boolean");
    specialNames.put(ESId.intern("Math"), "Math");
  };
}
