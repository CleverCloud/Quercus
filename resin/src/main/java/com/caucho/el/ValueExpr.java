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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.el;

import com.caucho.vfs.WriteStream;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ValueExpression;
import java.io.IOException;

/**
 * ValueExpression expression.
 */
public class ValueExpr extends Expr {
  // The identifier name
  private final String _name;
  
  private final ValueExpression _valExpr;

  /**
   * Creates the identifier
   */
  public ValueExpr(String name, ValueExpression valExpr)
  {
    _name = name;
    _valExpr = valExpr;
  }

  /**
   * Creates a field reference using this expression as the base object.
   *
   * @param field the string reference for the field.
   */
  @Override
  public Expr createField(String field)
  {
    if (_valExpr instanceof FieldGenerator) {
      FieldGenerator gen = (FieldGenerator) _valExpr;
      
      ValueExpression fieldExpr = gen.createField(field);

      if (fieldExpr != null)
        return new ValueExpr(field, fieldExpr);
    }
    
    Expr arrayExpr = createField(new StringLiteral(field));

    return new PathExpr(arrayExpr, _name + '.' + field);
  }

  /**
   * Evaluate the expr as an object.
   *
   * @param env the variable environment
   *
   * @return the value as an object
   */
  @Override
  public Object getValue(ELContext env)
    throws ELException
  {
    return _valExpr.getValue(env);
  }

  /**
   * Sets teh value.
   *
   * @param env the variable environment
   *
   * @return the value as an object
   */
  @Override
  public void setValue(ELContext env, Object value)
    throws ELException
  {
    _valExpr.setValue(env, value);
  }

  /**
   * Prints the code to create an IdExpr.
   */
  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.el.ValueExpr(\"");
    printEscapedString(os, _name);
    os.print("\")");
  }

  public boolean equals(Object o)
  {
    if (o == null || ! o.getClass().equals(ValueExpr.class))
      return false;

    ValueExpr expr = (ValueExpr) o;

    return _valExpr.equals(expr._valExpr);
  }

  public String toString()
  {
    return _name;
  }
}
