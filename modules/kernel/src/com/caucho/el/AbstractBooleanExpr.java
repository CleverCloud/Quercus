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
import java.io.IOException;

/**
 * Base implementation for a boolean-valued expression.
 */
abstract public class AbstractBooleanExpr extends Expr {
  
  /**
   * Evaluate the expression as a boolean
   *
   * @param env the variable environment
   *
   * @return the value as a booleanstring
   */
  @Override
  abstract public boolean evalBoolean(ELContext env)
    throws ELException;
  
  /**
   * Evaluate the expression as an object.
   *
   * @param env the variable environment
   *
   * @return the value as an object
   */
  @Override
  public Object getValue(ELContext env)
    throws ELException
  {
    return evalBoolean(env) ? Boolean.TRUE : Boolean.FALSE;
  }
  
  /**
   * Evaluate the expression as a string
   *
   * @param env the variable environment
   *
   * @return the value as a string
   */
  @Override
  public String evalString(ELContext env)
    throws ELException
  {
    return evalBoolean(env) ? "true" : "false";
  }
  
  /**
   * Evaluate the expression as a long
   *
   * @param env the variable environment
   *
   * @return the value as a long
   */
  @Override
  public long evalLong(ELContext env)
    throws ELException
  {
    ELException e = new ELException(L.l("'{0}': boolean expressions can not be converted to long values.", this));
    
    error(e, env);

    return 0;
  }
  
  /**
   * Evaluate the expression as a double
   */
  @Override
  public double evalDouble(ELContext env)
    throws ELException
  {
    ELException e = new ELException(L.l("`{0}': boolean expressions can not be converted to double values.", this));
    
    error(e, env);

    return 0;
  }

  /**
   * Evalutes directly to the output.
   *
   * @param out the output stream
   * @param env the variable environment
   */
  @Override
  public boolean print(WriteStream out, ELContext env, boolean isEscaped)
    throws IOException, ELException
  {
    if (evalBoolean(env))
      out.print("true");
    else
      out.print("false");

    return false;
  }
}
