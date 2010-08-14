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

package com.caucho.servlet.comet;

import java.io.*;
import javax.servlet.*;

/**
 * Servlet to handle comet requests.
 */
public interface CometServlet extends Servlet
{
  /**
   * Services the initial request.
   *
   * @param request the servlet request object
   * @param response the servlet response object
   * @param controller the controller to be passed to application code
   *
   * @return true for keepalive, false for the end of the request
   */
  abstract public boolean service(ServletRequest request,
                                  ServletResponse response,
                                  CometController controller)
    throws IOException, ServletException;

  /**
   * Resumes service the initial request.
   *
   * @param request the servlet request object
   * @param response the servlet response object
   * @param controller the controller to be passed to application code
   *
   * @return true for keepalive, false for the end of the request
   */
  abstract public boolean resume(ServletRequest request,
                                 ServletResponse response,
                                 CometController controller)
    throws IOException, ServletException;
}
