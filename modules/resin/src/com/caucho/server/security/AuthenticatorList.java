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

package com.caucho.server.security;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.ArrayList;

/**
 * The AuthenticatorList is used to configure more than one authenticators in a
 * list, each authenticator is tried in turn and if the authentication fails the
 * next authenticator in the list is attempted.
 *
 * <code><pre>
 *  &lt;authenticator type="com.caucho.server.security.AuthenticatorList"&gt;
 *    &lt;init&gt;
 *      &lt;authenticator resin:type="com.caucho.server.security.XmlAuthenticator"&gt;
 *        &lt;user&gt;admin:NIHlOSafJN2H7emQCkOQ2w==:user,admin&lt;/user&gt;
 *      &lt;/authenticator&gt;
 *
 *      &lt;authenticator resin:type='com.caucho.server.security.JdbcAuthenticator'&gt;
 *        &lt;data-source&gt;jdbc/users&lt;/data-source&gt;
 *        &lt;password-query&gt;
 *          SELECT password FROM LOGIN WHERE username=?
 *        &lt;/password-query&gt;
 *        &lt;cookie-auth-query&gt;
 *          SELECT username FROM LOGIN WHERE cookie=?
 *        &lt;/cookie-auth-query&gt;
 *        &lt;cookie-auth-update&gt;
 *          UPDATE LOGIN SET cookie=? WHERE username=?
 *        &lt;/cookie-auth-update&gt;
 *        &lt;role-query&gt;
 *          SELECT role FROM LOGIN WHERE username=?
 *        &lt;/role-query&gt;
 *      &lt;/authenticator&gt;
 *    &lt;/init&gt;
 *  &lt;/authenticator&gt;
 *
 *  &lt;login-config auth-method='basic'/&gt;
 *
 *  &lt;security-constraint url-pattern='/users/*' role-name='user'/&gt;
 *  &lt;security-constraint url-pattern='/admin/*' role-name='admin'/&gt;
 *
 * </pre></code>
 */
public class AuthenticatorList implements ServletAuthenticator {
  private ArrayList<ServletAuthenticator> _authenticators 
    = new ArrayList<ServletAuthenticator>();

  /**
   * Sets the path to the XML file.
   */
  public void addAuthenticator(ServletAuthenticator authenticator)
  {
    _authenticators.add(authenticator);
  }

  @PostConstruct
  public void init()
    throws ServletException
  {
  }
  
  public Principal login(HttpServletRequest request,
                         HttpServletResponse response,
                         ServletContext application,
                         String user, String password)
    throws ServletException
  {
    Principal result = null;

    for (ServletAuthenticator authenticator : _authenticators) {
      result = authenticator.login( request, 
                                    response, 
                                    application, 
                                    user, 
                                    password );

      if (result != null)
        break;
    }

    return result;
  }
  
  public Principal getUserPrincipal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    ServletContext application)
    throws ServletException
  {
    Principal result = null;

    for (ServletAuthenticator authenticator : _authenticators) {
      result = authenticator.getUserPrincipal( request, 
                                               response, 
                                               application );

      if (result != null)
        break;
    }

    return result;
  }
  
  public Principal loginDigest(HttpServletRequest request,
                               HttpServletResponse response,
                               ServletContext app,
                               String user, String realm,
                               String nonce, String uri,
                               String qop, String nc, String cnonce,
                               byte []clientDigset)
    throws ServletException
  {
    Principal result = null;

    for (ServletAuthenticator authenticator : _authenticators) {
      result = authenticator.loginDigest( request, 
                                          response, 
                                          app, 
                                          user, 
                                          realm, 
                                          nonce, 
                                          uri, 
                                          qop, 
                                          nc, 
                                          cnonce, 
                                          clientDigset );

      if (result != null)
        break;
    }

    return result;
  }
  
  public boolean isUserInRole(HttpServletRequest request,
                              HttpServletResponse response,
                              ServletContext application,
                              Principal user, String role)
    throws ServletException
  {
    boolean result = false;

    for (ServletAuthenticator authenticator : _authenticators) {
      result = authenticator.isUserInRole( request, 
                                           response, 
                                           application, 
                                           user, 
                                           role );

      if (result)
        break;
    }

    return result;
  }
  
  public void logout(ServletContext application,
                     HttpSession timeoutSession,
                     String sessionId,
                     Principal user)
    throws ServletException
  {
    for (ServletAuthenticator authenticator : _authenticators) {
      authenticator.logout(application,
                           timeoutSession,
                           sessionId,
                           user );
    }
  }
}
