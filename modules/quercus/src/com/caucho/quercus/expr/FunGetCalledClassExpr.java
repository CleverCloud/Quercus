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
import com.caucho.quercus.env.*;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.util.L10N;

/**
 * Represents returns the current called class.
 */
public class FunGetCalledClassExpr extends Expr {
  private static final L10N L = new L10N(FunGetCalledClassExpr.class);
  
  public FunGetCalledClassExpr(Location location)
  {
    super(location);
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
    Value qThis = env.getThis();
    
    if (qThis.isNull()) {
      env.warning(L.l("get_called_class() must be called in a class context"));
      return BooleanValue.FALSE;
    }
    else {
      return env.createString(qThis.getClassName());
    }
  }

  public String toString()
  {
    return "get_called_class()";
  }
}

