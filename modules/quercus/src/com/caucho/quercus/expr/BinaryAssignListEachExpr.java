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

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

/**
 * Represents a PHP list() = each() assignment expression.
 */
public class BinaryAssignListEachExpr extends Expr {
  private static final L10N L = new L10N(BinaryAssignListEachExpr.class);
  
  protected final ListHeadExpr _listHead;
  protected final Expr _value;
  
  public BinaryAssignListEachExpr(ListHeadExpr listHead, Expr value)
  {
    _listHead = listHead;
    _value = value;
  }

  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public Value eval(Env env)
  {
    if (! _value.isVar()) {
      env.error(L.l("each() argument must be a variable at '{0}'", _value));
      return NullValue.NULL;
    }

    Value value = _value.eval(env);

    _listHead.evalAssignEachValue(env, value);

    return value;
  }
  
  /**
   * Evaluates the expression.
   *
   * @param env the calling environment.
   *
   * @return the expression value.
   */
  @Override
  public boolean evalBoolean(Env env)
  {
    if (! _value.isVar()) {
      env.error(L.l("each() argument must be a variable at '{0}'", _value));
      return false;
    }

    Value value = _value.eval(env);

    return _listHead.evalEachBoolean(env, value);
  }
}

