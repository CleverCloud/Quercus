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

package com.caucho.servlets;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Use ServletResponse.sendError() to send an error to the client.
 *
 * <pre>
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;forbidden&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;com.caucho.servlets.ErrorStatusServlet&lt;/servlet-class&gt;
 *   &lt;init&gt;
 *     &lt;status-code&gt;403&lt;/status-code&gt;
 *     &lt;message&gt;You cannot look at that.&lt;/message&gt;
 *   &lt;/init&gt;
 * &lt;/servlet&gt;
 *
 * &lt;servlet-mapping url-pattern="*.properties" servlet-name="forbidden"/&gt;
 * &lt;servlet-mapping url-pattern="/config/*" servlet-name="forbidden"/&gt;
 * </pre>
 */
public class ErrorStatusServlet extends GenericServlet {
  private int _statusCode = 404;
  private String _message;

  /**
   * The status code to send, default 404 (Not Found).
   */
  public void setStatusCode(int code)
  {
    _statusCode = code;
  }

  /**
   * The message to send, default is to send no message.
   */
  public void setMessage(String message)
  {
    _message = message;
  }

  public void service(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    HttpServletResponse res = (HttpServletResponse) response;

    if (_message != null)
      res.sendError(_statusCode, _message);
    else
      res.sendError(_statusCode);
  }
}
