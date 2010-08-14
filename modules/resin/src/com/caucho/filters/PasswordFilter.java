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
 * @author Sam
 */

package com.caucho.filters;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Save a password that the user submits as the session attribute
 * 'java.naming.security.credentials'.
 * <p>
 * Enable with:
 * <pre>
 * &lt;filter  filter-name='password' 
 *          filter-class='com.caucho.filters.PasswordFilter'/&gt;
 *
 * &lt;filter-mapping filter-name='password'
 *                 url-pattern='j_security_check'/&gt;
 * </pre>
 *
 * Test with this in a JSP:
 * <pre>
 * &lt;% if (request.getUserPrincipal() != null) { %&gt;
 *   username: &lt;%= request.getRemoteUser() %&gt; 
 *   password: &lt;%= session.getAttribute("java.naming.security.credentials") %&gt;
 * &lt;% } %&gt;
 * </pre>
 * This will work with a <b>form</b> based login.
 */

public class PasswordFilter implements Filter {
    
  public void init(FilterConfig config)
  {
  }
    
  public void doFilter(ServletRequest request,
                       ServletResponse response,
                       FilterChain next)
    throws IOException, ServletException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    
    String password = req.getParameter("j_password");
    req.getSession().setAttribute("java.naming.security.credentials", password);
    
    next.doFilter(request, response);
  }
    
  public void destroy()
  {
  }
}
