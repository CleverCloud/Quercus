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

import com.caucho.es.ESException;
import com.caucho.es.ESId;
import com.caucho.es.wrapper.ESMethodDescriptor;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a method call.  Both Java calls and JavaScript calls are
 * handled here.
 */
class CallExpr extends Expr {
  protected Expr term;
  private Expr field;
  private boolean isNew;
  protected boolean isTop;
  protected ArrayList args = new ArrayList();

  protected TypeExpr typeExpr;
  protected boolean isCalculated;
  
  // The Java method if we can determine direct Java call.
  protected ESMethodDescriptor method;

  CallExpr(Block block, Expr term, Expr field, boolean isNew)
    throws ESException
  {
    super(block);
    
    this.term = term;
    this.field = field;
    this.isNew = isNew;

    if (term != null)
      term.setUsed();
    if (field != null)
      field.setUsed();

    if (term == null || ! (term.getTypeExpr() instanceof JavaTypeExpr))
      block.function.setCall();
  }

  void exprStatement(Function fun) throws ESException
  {
    isTop = true;
    noValue = true;

    fun.addExpr(this);
  }

  /**
   * Returns the javascript type for the expression
   *
   * @return a javascript type.
   */
  int getType()
  {
    calculateType();
    
    return type;
  }

  /**
   * Returns the type expression of the call.
   *
   * @return a type expression.
   */
  Expr getTypeExpr()
  {
    calculateType();

    return typeExpr;
  }

  /**
   * Calculate the return type of the expression.
   */
  private void calculateType()
  {
    boolean isStatic = false;

    if (isCalculated)
      return;
    
    isCalculated = true;

    Class javaClass = term.getJavaClass();
    if (term instanceof JavaClassExpr) {
      isStatic = true;
      javaClass = ((JavaClassExpr) term).getJavaClass();
    }

    if (javaClass == null || field == null ||
        ! (field instanceof LiteralExpr) ||
        term.getType() != TYPE_JAVA && ! isStatic) {
      return;
    }
    
    LiteralExpr lit = (LiteralExpr) field;
    String name = lit.getLiteral().toString();

    method = JavaMethod.bestMethod(javaClass, name, isStatic, args);

    // exception if can't find method
    if (method != null) {
      Class returnType = method.getReturnType();

      if (returnType.equals(void.class))
        type = TYPE_VOID;
      else if (returnType.equals(int.class))
        type = TYPE_INTEGER;
      else if (returnType.equals(double.class))
        type = TYPE_NUMBER;
      else if (returnType.equals(boolean.class))
        type = TYPE_BOOLEAN;
      else {
        type = TYPE_JAVA;
        this.javaType = returnType;
      }
      
      typeExpr = new JavaTypeExpr(block, returnType);
    }
  }

  void printBooleanImpl() throws IOException
  {
    printJavaImpl();
  }

  void printInt32Impl() throws IOException
  {
    printJavaImpl();
  }

  void printNumImpl() throws IOException
  {
    printJavaImpl();
  }

  /**
   * Generates code for the call, producing a JavaScript result.
   */
  void printImpl() throws IOException
  {
    if (term instanceof IdExpr && field == null &&
        ! ((IdExpr) term).isJavaLocal()) {
      ESId id = ((IdExpr) term).getId();

      field = new LiteralExpr(block, id);
      term = null;
    }

    if (term != null) {
      if (isNew)
        cl.print("_call.doNew(");
      else
        cl.print("_call.call(");

      term.print();

      if (field != null) {
        cl.print(", ");
        field.printStr();
      }
    }
    else if (function.isGlobalScope() && withDepth == 0) {
      if (isNew)
        cl.print("_call.doNew(_env.global, ");
      else
        cl.print("_call.call(_env.global, ");

      field.printStr();
    }
    else {
      if (isNew)
        cl.print("_call.newScope(");
      else
        cl.print("_call.callScope(");
      
      field.printStr();
    }

    int argCount = function.cl.getCallDepth();
    
    cl.print(", " + argCount);
    
    for (int i = 0; i < args.size() && i < 3; i++) {
      Expr expr = (Expr) args.get(i);
      cl.print(", ");
      function.cl.pushCall();
      expr.print();
    }
    if (args.size() >= 3)
      cl.print(", 3");
    for (int i = 3; i < args.size(); i++) {
      Expr expr = (Expr) args.get(i);
      cl.print("+ _call.arg(" + (i + argCount) + ", ");
      function.cl.pushCall();
      expr.print();
      cl.print(")");
    }
    function.cl.popCall(args.size());
    cl.print(")");
    if (isTop)
      cl.println(";");
  }

  void printStringImpl()
    throws IOException
  {
    if (String.class.equals(getJavaClass()))
      printJavaImpl();
    else {
      cl.print("String.valueOf(");
      printJavaImpl();
      cl.print(")");
    }
  }

  /**
   * Generates code for the call, producing a Java result.
   */
  void printJavaImpl()
    throws IOException
  {
    if (method.isStaticVirtual()) {
      cl.print(method.getMethodClassName());
    }
    else {
      if (term instanceof JavaClassExpr)
        cl.print(method.getMethodClassName());
      else {
        term.printJava();
      }
    }
    
    cl.print('.');
    cl.print(method.getName());
    cl.print("(");

    boolean isFirst = true;
    if (method.isStaticVirtual()) {
      cl.print("(");
      printJavaClass(method.getDeclaringClass());
      cl.print(")");
      isFirst = false;
      term.printJava();
    }

    Class []params = null;
    if (method != null)
      params = method.getParameterTypes();
      
    for (int i = 0; i < args.size(); i++) {
      Expr expr = (Expr) args.get(i);
      
      if (! isFirst)
        cl.print(", ");
      isFirst = false;

      if (params == null)
        expr.printJava();
      else if (params[i].equals(String.class)) {
        expr.printJavaString();
      }
      else if (! params[i].isPrimitive()) {
        cl.print("(");
        printJavaClass(params[i]);
        cl.print(")");

        expr.printJava();
      }
      else if (params[i].equals(int.class))
        expr.printInt32();
      else if (params[i].equals(long.class))
        expr.printInt64();
      else if (params[i].equals(boolean.class))
        expr.printBoolean();
      else if (params[i].equals(double.class))
        expr.printNum();
      else
        expr.printJava();
    }
    cl.print(")");
    if (isTop)
      cl.println(";");
  }
  
  /**
   * adds a new call parameter
   */
  void addCallParam(Expr param)
  {
    param.setUsed();
    args.add(param);
  }
}
