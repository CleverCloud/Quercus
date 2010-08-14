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

import java.io.IOException;

/**
 * Expr is an intermediate form representing an expression.
 */
class IdExpr extends Expr {
  private static ESId ARGUMENTS = ESId.intern("arguments");
  private Variable var;

  IdExpr(Block block, Variable var)
  {
    super(block);
    
    this.var = var;
    if (var.getId() == ARGUMENTS) {
      function.setArguments();
      function.setUseAllVariables();
    }

    if (var.getTypeExpr() != null) {
      type = var.getType();
      javaType = var.getTypeExpr().getJavaClass();
    }

    if (! var.isLocal() && ! function.isGlobalScope() && ! var.isJavaGlobal())
      function.setNeedsScope();
  }

  void setType(int type)
  {
    var.setType(type);
  }
  
  boolean isLocal()
  {
    return var.isLocal() && function.allowLocals();
  }
  
  boolean isJavaLocal()
  {
    return var.isJavaLocal() && function.allowLocals();
  }

  /**
   * Returns true if the variable is a global represented as a Java
   * field.
   */
  boolean isJavaGlobal()
  {
    return var.isJavaGlobal();
  }

  void setUsed()
  {
    var.setUsed();
  }

  int getType()
  {
    if (! isLocal() && ! isJavaGlobal())
      return TYPE_ES;
    else
      return var.getType();
  }

  Expr getTypeExpr()
  {
    if (! isLocal())
      return null;
    else
      return var.getTypeExpr();
  }

  boolean isSimple()
  {
    return isJavaLocal();
  }

  boolean isGlobalScope()
  {
    return function.isGlobalScope() && ! var.isScope();
  }

  /**
   * Returns the underlying variable.
   */
  Variable getVar()
  {
    return var;
  }

  boolean isUsed()
  {
    return var.isUsed() || function.useAllVariables();
  }
  
  void setLocal()
  {
    var.setLocal();
  }
  
  ESId getId()
  {
    return var.getId();
  }
  
  Expr delete()
  {
    return new DeleteExpr(block, this);
  }

  Expr postfix(int op)
  {
    if (op == '+')
      return new PostfixExpr(block, PostfixExpr.POSTINC, this);
    else
      return new PostfixExpr(block, PostfixExpr.POSTDEC, this);
  }

  Expr prefix(int op)
  {
    if (op == '+')
      return new PostfixExpr(block, PostfixExpr.PREINC, this);
    else
      return new PostfixExpr(block, PostfixExpr.PREDEC, this);
  }

  /**
   * Assigns the identifier to a value.
   */
  Expr assign(Expr value)
    throws ESException
  {
    int valueType = value.getType();
    Expr typeExpr = value.getTypeExpr();
    // XXX: needs to differ from getType to make type inference work
    int type = var.type; 

    if (isLocal() || isJavaGlobal()) {
      if (valueType == TYPE_UNKNOWN)
        valueType = TYPE_ES;

      if (typeExpr != null)
        var.setType(TYPE_JAVA, typeExpr);
      else if (type == TYPE_UNKNOWN)
        var.setType(valueType);
      else if (type == valueType) {
      }
      else if ((type == TYPE_INTEGER || type == TYPE_NUMBER) &&
               (valueType == TYPE_INTEGER || valueType == TYPE_NUMBER))
        var.setType(TYPE_NUMBER);
      else
        var.setType(TYPE_ES, null);

      type = var.getType();
      Expr newTypeExpr = var.getTypeExpr();
      if (newTypeExpr == null)
        javaType = null;
      else
        javaType = newTypeExpr.getJavaClass();
    }
    
    return new AssignExpr(block, this, value);
  }

  /**
   * Creates a call expression from this id.
   */
  CallExpr startCall()
    throws ESException
  {
    var.setUsed();
    
    return new CallExpr(block, this, null, false);
  }

  CallExpr startNew()
    throws ESException
  {
    var.setUsed();

    return new CallExpr(block, this, null, true);
  }

  void exprStatement(Function fun) throws ESException
  {
    doVoid().exprStatement(fun);
  }

  void printNumImpl() throws IOException
  {
    printImpl();
  }

  void printBooleanImpl() throws IOException
  {
    printImpl();
  }

  void printInt32Impl() throws IOException
  {
    printImpl();
  }

  void printStringImpl() throws IOException
  {
    printImpl();
  }

  void printImpl() throws IOException
  {
    cl.setLine(getFilename(), getLine());
    
    if (isJavaLocal()) {
      cl.print(getId());
    }
    else if (isJavaGlobal()) {
      cl.print(getId());
    }
    else if (isGlobalScope()) {
      // Need to use getGlobalVariable so an undefined variable will
      // throw an exception
      cl.print("_env.getGlobalVariable(");
      printLiteral(getId());
      cl.print(")");
    } else {
      cl.print("_env.getScopeProperty(");
      printLiteral(getId());
      cl.print(")");
    }
  }

  void printJavaImpl() throws IOException
  {
    cl.setLine(getFilename(), getLine());
    
    cl.print(getId());
  }

  public String toString()
  {
    return "[IdExpr " + getId() + "]";
  }
}
