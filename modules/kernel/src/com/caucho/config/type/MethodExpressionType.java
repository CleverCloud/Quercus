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

import javax.el.*;
import com.caucho.config.*;
import com.caucho.config.xml.XmlConfigContext;
import com.caucho.el.*;

/**
 * Represents a MethodExpression type.
 */
public final class MethodExpressionType extends ConfigType
{
  public static final MethodExpressionType TYPE = new MethodExpressionType();
  
  /**
   * The MethodExpressionType is a singleton
   */
  private MethodExpressionType()
  {
  }
  
  /**
   * Returns the Java type.
   */
  public Class getType()
  {
    return MethodExpression.class;
  }

  /**
   * Return false to disable EL
   */
  @Override
  public boolean isEL()
  {
    return false;
  }
  
  /**
   * Converts the string to a value of the type.
   */
  public Object valueOf(String text)
  {
    ELContext elContext = XmlConfigContext.getCurrent().getELContext();
    
    ELParser parser = new ELParser(elContext, text);

    Expr expr = parser.parse();

    return new MethodExpressionImpl(expr, text,
                                    Object.class, new Class[0]);
  }
}
