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
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.Value;

/**
 * Represents a PHP expression.
 */
abstract public class AbstractLongValuedExpr extends Expr {
  public AbstractLongValuedExpr(Location location)
  {
    super(location);
  }

  public AbstractLongValuedExpr()
  {
  }

  /**
   * Evaluates the expression as a value.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public Value eval(Env env)
  {
    return LongValue.create(evalLong(env));
  }

  /**
   * Evaluates the expression as a boolean.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public boolean evalBoolean(Env env)
  {
    return evalLong(env) != 0;
  }

  /**
   * Evaluates the expression as double
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  public double evalDouble(Env env)
  {
    return evalLong(env);
  }

  /**
   * Evaluates the expression as a long.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  abstract public long evalLong(Env env);
}

