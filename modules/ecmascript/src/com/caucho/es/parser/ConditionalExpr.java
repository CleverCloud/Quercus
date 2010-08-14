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
class ConditionalExpr extends Expr {
  Expr cond;
  Expr left;
  Expr right;
  
  ConditionalExpr(Block block, Expr cond, Expr left, Expr right)
  {
    super(block);
    
    this.cond = cond;
    this.left = left;
    this.right = right;
    this.type = TYPE_ES;

    left.setUsed();
    right.setUsed();
  }

  void exprStatement(Function fun) throws ESException
  {
    isTop = true;
    fun.addExpr(this);
  }

  void printImpl() throws IOException
  {
    if (isTop)
      cl.print("_env.doVoid(");

    cl.print("(");
    cond.printBoolean();
    cl.print("?");
    left.print();
    cl.print(":");
    right.print();
    cl.print(")");
    
    if (isTop)
      cl.println(")");
  }
}
