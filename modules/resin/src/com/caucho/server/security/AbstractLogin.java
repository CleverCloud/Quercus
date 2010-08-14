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

package com.caucho.server.security;

import com.caucho.network.listen.TcpSocketLink;
import com.caucho.server.http.CauchoRequest;
import com.caucho.server.session.SessionImpl;
import com.caucho.util.LruCache;

import javax.annotation.PostConstruct;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.security.Principal;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Backwards compatibility
 *
 * @since Resin 2.0.2
 * @deprecated
 * @see com.caucho.security.AbstractLogin
 */
public abstract class AbstractLogin extends com.caucho.security.AbstractLogin {
  /**
   * Authentication
   */
  @Override
  public Principal getUserPrincipalImpl(HttpServletRequest request)
  {
    ServletContext app = request.getServletContext();

    HttpServletResponse response = null;
    
    return getUserPrincipal(request, response, app);
  }

  protected Principal getUserPrincipal(HttpServletRequest request,
                                       HttpServletResponse response,
                                       ServletContext app)
  {
    return null;
  }
  
  /**
   * Authentication
   */
  @Override
  public Principal login(HttpServletRequest request,
                         HttpServletResponse response,
                         boolean isFail)
  {
    try {
      ServletContext app = request.getServletContext();
    
      return authenticate(request, response, app);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected Principal authenticate(HttpServletRequest request,
                                   HttpServletResponse response,
                                   ServletContext app)
    throws ServletException, IOException
  {
    return null;
  }

  /**
   * Returns true if the current user plays the named role.
   * <code>isUserInRole</code> is called in response to the
   * <code>HttpServletRequest.isUserInRole</code> call.
   *
   * @param user the logged in user
   * @param role the role to check
   *
   * @return true if the user plays the named role
   */
  @Override
  public boolean isUserInRole(Principal user, String role)
  {
    CauchoRequest request
      = (CauchoRequest) TcpSocketLink.getCurrentRequest();

    return isUserInRole(request,
                        null, // request.getResponse(),
                        request.getServletContext(),
                        user,
                        role);
  }

  protected boolean isUserInRole(HttpServletRequest request,
                                 HttpServletResponse response,
                                 ServletContext app,
                                 Principal user, String role)
  {
    return false;
  }
}

