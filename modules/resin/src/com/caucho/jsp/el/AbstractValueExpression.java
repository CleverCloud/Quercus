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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jsp.el;

import javax.el.*;

abstract public class AbstractValueExpression extends ValueExpression
{
  public boolean isReadOnly(ELContext env)
  {
    env.setPropertyResolved(true);
    
    return true;
  }

  public boolean isLiteralText()
  {
    return false;
  }

  public Class getType(ELContext env)
  {
    Object value = getValue(env);

    if (env.isPropertyResolved())
      return value.getClass();
    else
      return null;
  }

  public Class getExpectedType()
  {
    return Object.class;
  }
  
  public void setValue(ELContext env, Object value)
  {
    throw new PropertyNotWritableException(getClass().getName());
  }
  
  public int hashCode()
  {
    return getExpressionString().hashCode();
  }
  
  public boolean equals(Object o)
  {
    return (this == o);
  }

  public String toString()
  {
    return getExpressionString();
  }
}
