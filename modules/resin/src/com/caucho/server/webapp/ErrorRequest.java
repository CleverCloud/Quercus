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

package com.caucho.server.webapp;

import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.server.dispatch.Invocation;
import com.caucho.util.IntMap;

public class ErrorRequest extends ForwardRequest {
  private static final IntMap _errorAttributeMap = new IntMap();

  private static final int REQUEST_URI_CODE = 1;
  
  public ErrorRequest()
  {
  }
  
  public ErrorRequest(HttpServletRequest request,
                      HttpServletResponse response,
                      Invocation invocation)
  {
    super(request, response, invocation);
  }

  public DispatcherType getDispatcherType()
  {
    return DispatcherType.ERROR;
  }

  //
  // attributes
  //

  public Object getAttribute(String name)
  {
    switch (_errorAttributeMap.get(name)) {
    case REQUEST_URI_CODE:
      return unwrapRequest().getRequestURI();
      
    default:
      return super.getAttribute(name);
    }
  }

  static {
    _errorAttributeMap.put(RequestDispatcher.ERROR_REQUEST_URI,
                           REQUEST_URI_CODE);
  }
}
