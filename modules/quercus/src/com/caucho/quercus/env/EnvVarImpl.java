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

package com.caucho.quercus.env;

/**
 * Encapsulates an environment entry for a variable.  The EnvVar is a
 * container for Vars.
 */
public final class EnvVarImpl extends EnvVar
{
  private Var _var;

  public EnvVarImpl(Var var)
  {
    if (var == null)
      throw new NullPointerException();
    
    _var = var;
  }
  
  /**
   * Returns the current value.
   */
  public Value get()
  {
    return _var.toValue();
  }

  /**
   * Sets the current value.
   */
  public Value set(Value value)
  {
    return _var.set(value);
  }

  /**
   * Returns the current Var.
   */
  public Var getVar()
  {
    return _var;
  }

  /**
   * Sets the var.
   */
  public Var setVar(Var var)
  {
    _var = var;

    return var;
  }
}

