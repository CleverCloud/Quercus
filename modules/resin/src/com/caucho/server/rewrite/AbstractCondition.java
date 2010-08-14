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
 * @author Sam
 */

package com.caucho.server.rewrite;

import com.caucho.config.ConfigException;
import com.caucho.server.http.CauchoResponse;
import com.caucho.util.L10N;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

abstract public class AbstractCondition
  implements Condition
{
  private static final L10N L =  new L10N(AbstractCondition.class);

  public void init()
  {
  }

  public String getTagName()
  {
    return getClass().getName();
  }

  /**
   * Throws an exception if the passed value is null.
   */
  protected void required(Object value, String name)
    throws ConfigException
  {
    if (value == null)
      throw new ConfigException(L.l("{0} requires '{1}' attribute.",
                                    getTagName(), name));
  }

  protected void addHeaderValue(HttpServletResponse response, String header, String value)
  {
    while (response instanceof HttpServletResponseWrapper)
      response = (HttpServletResponse) ((HttpServletResponseWrapper) response).getResponse();

    if (response instanceof CauchoResponse) {
      CauchoResponse res = (CauchoResponse) response;

      String currentValue = res.getHeader(header);

      if (currentValue != null) {
        if (currentValue.equals(value)
            || (currentValue.contains(value + ","))
            || (currentValue.contains(", " + value))) {
        }
        else {
          res.setHeader(header, currentValue + ", " + value);
        }
      }
      else {
        res.setHeader(header, value);
      }
    }
    else {
      response.addHeader(header, value);
    }
  }

  public void destroy()
  {
  }
}
