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
 * FieldExpr is an intermediate form representing a field reference.
 *
 * <p>In JavaScript, foo.bar and foo["bar"] are equivalent.
 */
class FieldExpr extends Expr {
  Expr lhs;
  Expr field;

  FieldExpr(Block block, Expr lhs, Expr field)
  {
    super(block);
    
    this.lhs = lhs;
    this.field = field;

    if (lhs != null)
      lhs.setUsed();
    if (field != null)
      field.setUsed();
  }

  /**
   * Returns the base expression of the field. For foo.bar it returns foo.
   */
  Expr getExpr()
  {
    return lhs;
  }

  /**
   * Returns the field reference.
   */
  Expr getField()
  {
    return field;
  }

  /**
   * Type is always ES.
   */
  int getType()
  {
    return TYPE_ES;
  }
  
  Expr assign(Expr value)
    throws ESException
  {
    return new AssignExpr(block, lhs, field, value);
  }
  
  Expr delete()
  {
    return new DeleteExpr(block, lhs, field);
  }

  Expr prefix(int op)
  {
    if (op == '+')
      return new PostfixExpr(block, PostfixExpr.PREINC, this);
    else
      return new PostfixExpr(block, PostfixExpr.PREDEC, this);
  }
  
  Expr postfix(int op)
  {
    if (op == '+')
      return new PostfixExpr(block, PostfixExpr.POSTINC, this);
    else
      return new PostfixExpr(block, PostfixExpr.POSTDEC, this);
  }
  
  CallExpr startCall()
    throws ESException
  {
    return new CallExpr(block, lhs, field, false);
  }

  CallExpr startNew()
    throws ESException
  {
    return new CallExpr(block, lhs, field, true);
  }

  void printImpl() throws IOException
  {
    lhs.print();
    cl.print(".getProperty(");
    field.printStr();
    cl.print(")");
  }
}
