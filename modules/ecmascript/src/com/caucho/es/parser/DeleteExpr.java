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
class DeleteExpr extends Expr {
  private Expr lhs;
  private IdExpr var;
  private ESId id;
  private Expr field;
  private boolean isTop;

  DeleteExpr(Block block, IdExpr var)
  {
    super(block);
    
    this.var = var;
    var.setUsed();
  }

  DeleteExpr(Block block, Expr lhs, ESId id)
  {
    super(block);
    
    this.lhs = lhs;
    this.id = id;

    if (lhs != null)
      lhs.setUsed();
  }

  DeleteExpr(Block block, Expr lhs, Expr field)
  {
    super(block);
    
    this.lhs = lhs;
    this.field = field;
    
    if (lhs != null)
      lhs.setUsed();
    if (field != null)
      field.setUsed();
  }

  void exprStatement(Function fun) throws ESException
  {
    isTop = true;

    fun.addExpr(this);
  }

  /**
   * The assignment operator
   */
  void print()
    throws IOException
  {
    if (var != null && var.isLocal()) {
      if (! isTop)
        cl.print("ESBoolean.FALSE");
    }
    else if (var != null) {
      if (function.isGlobalScope())
        cl.print("_env.global.delete(");
      else
        cl.print("_env.deleteScopeProperty(");
      printLiteral(var.getId());
      cl.print(")");
      if (isTop)
        cl.println(";");
    }
    else if (id != null) {
      lhs.print();
      cl.print(".delete(");
      printLiteral(id);
      cl.print(")");
      if (isTop)
        cl.println(";");
    } else {
      lhs.print();
      cl.print(".delete(");
      field.printStr();
      cl.print(")");
      if (isTop)
        cl.println(";");
    }
  }
}
