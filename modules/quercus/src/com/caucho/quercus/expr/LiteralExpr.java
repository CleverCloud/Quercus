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

import com.caucho.quercus.Location;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.Value;

/**
 * Represents a literal expression.
 */
public class LiteralExpr extends Expr {
  private final Value _value;

  public LiteralExpr(Value value)
  {
    _value = value;
  }

  protected Value getValue()
  {
    return _value;
  }

  /**
   * Returns true for a literal expression.
   */
  @Override
  public boolean isLiteral()
  {
    return true;
  }

  /**
   * Returns true if a static true value.
   */
  @Override
  public boolean isTrue()
  {
    if (_value == BooleanValue.TRUE)
      return true;
    else if (_value instanceof LongValue)
      return _value.toLong() != 0;
    else
      return false;
  }

  /**
   * Returns true if a static true value.
   */
  @Override
  public boolean isFalse()
  {
    if (_value == BooleanValue.FALSE)
      return true;
    else if (_value instanceof LongValue)
      return _value.toLong() == 0;
    else
      return false;
  }

  /**
   * Returns true for a long value.
   */
  public boolean isLong()
  {
    return _value.isLongConvertible();
  }

  /**
   * Returns true for a double value.
   */
  public boolean isDouble()
  {
    return _value.isDoubleConvertible();
  }

  /*
   *
   */
  public boolean isBoolean()
  {
    return _value.isBoolean();
  }

  /**
   * Evaluates the expression as a constant.
   *
   * @return the expression value.
   */
  public Value evalConstant()
  {
    return _value;
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value eval(Env env)
  {
    return _value;
  }

  public String toString()
  {
    return _value.toString();
  }
}

