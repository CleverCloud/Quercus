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
import javax.el.ELResolver;
import javax.el.PropertyNotFoundException;
import java.io.IOException;

/**
 * Identifier expression.
 */
public class IdExpr extends Expr {
  // The identifier name
  private String _id;

  /**
   * Creates the identifier
   */
  public IdExpr(String id)
  {
    _id = id;
  }

  /**
   * Returns true if the expression is read-only.
   */
  @Override
  public boolean isReadOnly(ELContext env)
  {
    env.getELResolver().getValue(env, null, _id);

    if (! env.isPropertyResolved())
      throw new PropertyNotFoundException(L.l(
        "'{0}' not found in context '{1}'.",
        _id, env));
        
    return false;
  }

  /**
   * Creates a field reference using this expression as the base object.
   *
   * @param field the string reference for the field.
   */
  @Override
  public Expr createField(String field)
  {
    Expr arrayExpr = createField(new StringLiteral(field));

    return new PathExpr(arrayExpr, _id + '.' + field);
  }

  /**
   * Evaluate the expr as an object.
   *
   * @param env the variable environment
   *
   * @return the value as an object
   */
  @Override
  public Class getType(ELContext env)
    throws ELException
  {
    return env.getELResolver().getType(env, null, _id);
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
    return env.getELResolver().getValue(env, null, _id);
  }

  /**
   * Evaluates the expression, setting an object.
   *
   * @param env the variable environment
   *
   * @return the value of the expression as an object
   */
  @Override
  public void setValue(ELContext env, Object value)
    throws ELException
  {
    ELResolver resolver = env.getELResolver();

    resolver.getValue(env, null, _id);

    if (env.isPropertyResolved())
      resolver.setValue(env, null, _id, value);
    else
      throw new PropertyNotFoundException(L.l(
        "'{0}' not found in context '{1}'.",
        _id, env));
  }

  /**
   * Prints the code to create an IdExpr.
   */
  @Override
  public void printCreate(WriteStream os)
    throws IOException
  {
    os.print("new com.caucho.el.IdExpr(\"");
    printEscapedString(os, _id);
    os.print("\")");
  }

  public boolean equals(Object o)
  {
    if (o == null || ! o.getClass().equals(IdExpr.class))
      return false;

    IdExpr expr = (IdExpr) o;

    return _id.equals(expr._id);
  }

  public String toString()
  {
    return _id;
  }
}
