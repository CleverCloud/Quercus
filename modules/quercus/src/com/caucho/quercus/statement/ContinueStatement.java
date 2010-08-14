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

package com.caucho.quercus.statement;

import java.util.ArrayList;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.ContinueValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.Expr;

/**
 * Represents a continue expression statement in a PHP program.
 */
public class ContinueStatement extends Statement {
  //public static final ContinueStatement CONTINUE = new ContinueStatement();
  
  protected final Expr _target;
  protected final ArrayList<String> _loopLabelList;
  
  public ContinueStatement(Location location,
                           Expr target,
                           ArrayList<String> loopLabelList)
  {
    super(location);
    _target = target;
    _loopLabelList = loopLabelList;
  }

  /**
   * Executes the statement, returning the expression value.
   */
  public Value execute(Env env)
  {
    if (_target == null)
      return ContinueValue.CONTINUE;
    else
      return new ContinueValue(_target.eval(env).toInt());
  }
}

