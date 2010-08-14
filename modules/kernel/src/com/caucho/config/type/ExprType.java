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

package com.caucho.config.type;

import com.caucho.config.ConfigELContext;
import com.caucho.config.types.*;
import com.caucho.el.ELParser;
import com.caucho.el.Expr;

/**
 * Represents a String type.
 */
public final class ExprType extends ConfigType<Expr>
{
  public static final ExprType TYPE = new ExprType();
  
  /**
   * The StringType is a singleton
   */
  private ExprType()
  {
  }
  
  /**
   * Returns the Java type.
   */
  @Override
  public Class<Expr> getType()
  {
    return Expr.class;
  }

  /**
   * Return true for non-trim.
   */
  @Override
  public boolean isNoTrim()
  {
    return true;
  }

  /**
   * Return true for EL
   */
  @Override
  public boolean isEL()
  {
    return false;
  }
  
  /**
   * Converts the string to a value of the type.
   */
  @Override
  public Object valueOf(String text)
  {
    ELParser parser = new ELParser(getELContext(), text);
    parser.setCheckEscape(true);
    Expr expr = parser.parse();
    
    return expr;
  }

  /**
   * Returns the variable resolver.
   */
  public ConfigELContext getELContext()
  {
    return ConfigELContext.EL_CONTEXT;
  }
}
