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

package com.caucho.quercus.program;

import com.caucho.quercus.Location;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.expr.Expr;
import com.caucho.quercus.expr.ExprFactory;
import com.caucho.quercus.statement.*;
import com.caucho.util.L10N;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Declaration for an abstract function or interface.
 */
public class MethodDeclaration extends Function
{
  private static final Logger log = Logger.getLogger(
    MethodDeclaration.class.getName());
  private static final L10N L = new L10N(MethodDeclaration.class);

  private final ClassDef _qClass;

  public MethodDeclaration(ExprFactory exprFactory,
                           Location location,
                           ClassDef qClass,
                           String name,
                           FunctionInfo info,
                           Arg []argList)
  {
    super(exprFactory, location,
          name, info, argList,
          new Statement[0]);

    _qClass = qClass;
  }

  public boolean isAbstract()
  {
    return true;
  }

  public boolean isObjectMethod()
  {
    return true;
  }

  /**
   * Binds the user's arguments to the actual arguments.
   *
   * @param args the user's arguments
   * @return the user arguments augmented by any defaults
   */
  public Expr []bindArguments(Env env, Expr fun, Expr []args)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Evaluates the function.
   */
  public Value call(Env env, Value []args)
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "]";
  }
}

