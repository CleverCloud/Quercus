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

import com.caucho.el.*;
import com.caucho.jsp.PageContextImpl;
import com.caucho.vfs.WriteStream;

import javax.el.*;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.util.*;

public class HeaderValuesExpression extends AbstractValueExpression
  implements FieldGenerator
{
  public static final ValueExpression EXPR
    = new HeaderValuesExpression();

  /**
   * Creates a field reference using this expression as the base object.
   *
   * @param field the string reference for the field.
   */
  public ValueExpression createField(String field)
  {
    return new HeaderValuesFieldExpression(field);
  }
  
  /**
   * Evaluate the expr as an object.
   *
   * @param env the page context
   */
  @Override
  public Object getValue(ELContext env)
    throws ELException
  {
    if (! (env instanceof ServletELContext))
      return env.getELResolver().getValue(env, null, "headerValues");

    ServletELContext servletEnv = (ServletELContext) env;
    
    return servletEnv.getHeaderValues();
  }

  public String getExpressionString()
  {
    return "headerValues";
  }
}
