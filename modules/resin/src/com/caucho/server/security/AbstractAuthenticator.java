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

import java.security.Principal;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.security.PasswordCredentials;
import com.caucho.server.http.CauchoRequest;

/**
 * All applications should extend AbstractAuthenticator to implement
 * their custom authenticators.  While this isn't absolutely required,
 * it protects implementations from API changes.
 *
 * <p>The AbstractAuthenticator provides a single-signon cache.  Users
 * logged into one web-app will share the same principal.
 */
@SuppressWarnings("serial")
public class AbstractAuthenticator
  extends com.caucho.security.AbstractAuthenticator
{

  //
  // basic password authentication
  //

  /**
   * Main authenticator API.
   */
  @Override
  public Principal authenticate(Principal principal,
                                PasswordCredentials cred,
                                Object details)
  {
    HttpServletRequest request = (HttpServletRequest) details;

    String userName = principal.getName();
    String password = new String(cred.getPassword());

    ServletContext webApp = request.getServletContext();
    HttpServletResponse response
      = (HttpServletResponse) ((CauchoRequest) request).getServletResponse();

    try {
      Principal user = getUserPrincipal(request, response, webApp);

      if (user != null)
        return user;
      
      return login(request, response, webApp, userName, password);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Backward compatiblity call
   */
  protected Principal login(HttpServletRequest request,
                                HttpServletResponse response,
                                ServletContext app,
                                String userName, String password)
    throws ServletException
  {
    return loginImpl(request, response, app, userName, password);
  }

  /**
   * Backward compatiblity call
   */
  protected Principal loginImpl(HttpServletRequest request,
                                HttpServletResponse response,
                                ServletContext app,
                                String userName, String password)
    throws ServletException
  {
    Principal user = getUserPrincipal(request, response, app);

    if (user == null)
      user = login(request, response, app);

    return user;
  }

  /**
   * Backward compatiblity call
   */
  protected Principal getUserPrincipal(HttpServletRequest request,
                                       HttpServletResponse response,
                                       ServletContext app)
    throws ServletException
  {
    return null;
  }

  /**
   * Backward compatiblity call
   */
  protected Principal login(HttpServletRequest request,
                            HttpServletResponse response,
                            ServletContext app)
    throws ServletException
  {
    return null;
  }

  //
  // user in role

  /**
   * Returns true if the user plays the named role.
   *
   * @param request the servlet request
   * @param user the user to test
   * @param role the role to test
   */
  @Override
  public boolean isUserInRole(Principal user, String role)
  {
    try {
      HttpServletRequest request = null;
      HttpServletResponse response = null;
      ServletContext webApp = null;
      
      return isUserInRole(request, response, webApp, user, role);
    } catch (ServletException e) {
      throw new RuntimeException(e);
    }
  }
      
  public boolean isUserInRole(HttpServletRequest request,
                              HttpServletResponse response,
                              ServletContext application,
                              Principal user, String role)
    throws ServletException
  {
    return "user".equals(role);
  }
}
