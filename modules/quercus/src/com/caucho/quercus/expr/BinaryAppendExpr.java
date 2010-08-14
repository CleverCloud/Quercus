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

package com.caucho.quercus.expr;

import com.caucho.quercus.env.BinaryBuilderValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

/**
 * Represents a PHP append ('.') expression.
 */
public class BinaryAppendExpr extends Expr
{
  private final Expr _value;
  private BinaryAppendExpr _next;

  protected BinaryAppendExpr(Expr value, BinaryAppendExpr next)
  {
    _value = value;
    _next = next;
  }

  /**
   * Returns the value expression.
   */
  public Expr getValue()
  {
    return _value;
  }

  /**
   * Returns the next value in the append chain.
   */
  public BinaryAppendExpr getNext()
  {
    return _next;
  }

  /**
   * Returns the next value in the append chain.
   */
  void setNext(BinaryAppendExpr next)
  {
    _next = next;
  }

  /**
   * Returns true for a string.
   */
  public boolean isString()
  {
    return true;
  }

  @Override
  public Value eval(Env env)
  {
    Value value = _value.eval(env);

    StringValue sb = value.toStringBuilder(env);

    for (BinaryAppendExpr ptr = _next; ptr != null; ptr = ptr._next) {
      Value ptrValue = ptr._value.eval(env);

      sb = sb.appendUnicode(ptrValue);
    }

    return sb;
  }

  @Override
  public String evalString(Env env)
  {
    Value value = _value.eval(env);

    StringValue sb = value.toStringBuilder(env);

    for (BinaryAppendExpr ptr = _next; ptr != null; ptr = ptr._next) {
      sb = sb.appendUnicode(ptr._value.eval(env));
    }

    return sb.toString();
  }

  /**
   * Returns the first constant string, or null.
   */
  @Override
  public Value evalConstantPrefix()
  {
    return _value.evalConstantPrefix();
  }

  /**
   * Returns the tail constant string, or null.
   */
  @Override
  public Value evalConstantSuffix()
  {
    if (_next != null)
      return _next.evalConstantSuffix();
    else
      return null;
  }

  public String toString()
  {
    if (_next != null)
      return "(" + _value + " . " + _next + ")";
    else
      return String.valueOf(_value);
  }
}

