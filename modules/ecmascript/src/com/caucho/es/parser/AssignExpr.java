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

import java.io.IOException;

/**
 * Expr is an intermediate form representing an expression.
 */
class AssignExpr extends Expr {
  private Expr lhs;
  private IdExpr var;
  private Expr field;
  private Expr rhs;

  AssignExpr(Block block, IdExpr var, Expr rhs)
  {
    super(block);
    
    this.var = var;
    this.rhs = rhs;
    rhs.getType();
    if (! (rhs instanceof LiteralExpr || rhs instanceof IdExpr))
      var.setUsed();
  }

  AssignExpr(Block block, Expr lhs, Expr field, Expr rhs)
  {
    super(block);
    
    this.lhs = lhs;
    this.field = field;
    this.rhs = rhs;
    rhs.getType();
    lhs.setUsed();
  }

  void exprStatement(Function fun) throws ESException
  {
    isTop = true;
    noValue = true;

    fun.addExpr(this);
  }

  /**
   * The assignment operator
   */
  void printImpl()
    throws IOException
  {
    cl.setLine(getFilename(), getLine());

    if (var != null && (var.isJavaLocal() || var.isJavaGlobal())) {
      if (! isTop && ! noValue) {
        switch (var.getType()) {
        case TYPE_NUMBER:
        case TYPE_INTEGER:
          cl.print("ESNumber.create(");
          break;
        
        case TYPE_BOOLEAN:
          cl.print("ESBoolean.create(");
          break;
        
        default:
          cl.print("(");
          break;
        }
      }
      else if (noValue && ! var.isUsed() &&
               (rhs instanceof LiteralExpr || rhs instanceof IdExpr))
        return;
      else if (! noValue)
        cl.print("(");
      
      cl.print(var.getId());
      cl.print(" = ");
      switch (var.getType()) {
      case TYPE_NUMBER:
        rhs.printNum();
        break;
        
      case TYPE_INTEGER:
        rhs.printInt32();
        break;
        
      case TYPE_BOOLEAN:
        rhs.printBoolean();
        break;
        
      case TYPE_JAVA:
        if (var.getType() == TYPE_JAVA &&
            ! var.getJavaClass().isAssignableFrom(rhs.getJavaClass()))
          cl.print("(" + var.getJavaClass().getName() + ") ");
        rhs.printJava();
        break;
        
      default:
        rhs.print();
        break;
      }

      if (! noValue)
        cl.print(")");
    }
    else if (noValue && var != null) {
      if (var.isGlobalScope())
        cl.print("_env.global.setProperty(");
      // XXX: should expand to anything without a side-effect
      else if (! var.isUsed() && var.isLocal() &&
               (rhs instanceof LiteralExpr || rhs instanceof IdExpr))
        return;
      else if (var.getVar().hasInit())
        cl.print("_arg.setProperty(");
      else
        cl.print("_env.setScopeProperty(");

      printLiteral(var.getId());
      cl.print(", ");
      rhs.print();
      cl.print(")");
    }
    else if (var != null) {
      if (var.isGlobalScope())
        cl.print("_env.setGlobalProperty(");
      else if (! var.isUsed() && var.isLocal() && 
               (rhs instanceof LiteralExpr || rhs instanceof IdExpr))
        return;
      else if (var.getVar().hasInit())
        cl.print("_arg.setProperty(");
      else
        cl.print("_env.setScopeProperty(");

      printLiteral(var.getId());
      cl.print(", ");
      rhs.print();
      cl.print(")");
    }
    else if (noValue) {
      lhs.print();
      cl.print(".setProperty(");
      if (field.getType() == TYPE_INTEGER)
        field.printInt32();
      else
        field.printStr();
      cl.print(", ");
      rhs.print();
      cl.print(")");
    }
    else {
      cl.print("_env.setProperty(");
      lhs.print();
      cl.print(", ");
      field.printStr();
      cl.print(", ");
      rhs.print();
      cl.print(")");
    }
  }
}
