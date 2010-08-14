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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.security.Principal;

/**
 * Used in conjunction with AbstractLogin to authenticate users in
 * a servlet request.  The ServletAuthenticator is typically responsible for
 * the actual authentication and AbstractLogin is responsible for extracting
 * credentials (user and password) from the request and returning any
 * error pages.  Since Login classes typically delegate to the Authenticator,
 * the same authenticator can be used for "basic", "form" or a custom login.
 *
 * <p>In general, applications should extend AbstractAuthenticator instead
 * to protect from API changes in the Authenticator.
 *
 * <p>The authenticator is configured using init-param in the resin.conf.
 * For example, if test.MyAuthenticator defines a <code>setFoo</code> method,
 * it can be configured with &lt;init-param foo='bar'/>.
 *
 * <code><pre>
 * &lt;authenticator url='scheme:param1=value1;param2=value2'>
 *   &lt;init>
 *     &lt;param3>value4&lt;/param3>
 *   &lt;/init>
 * &lt;/authenticator>
 * </pre></code>
 *
 * <p>Authenticator instances can be specific to a web-app, host, or
 * server-wide.  If the authenticator is configured for the host, it
 * is shared for all web-apps in that host, enabling single-signon.
 *
 * <code><pre>
 * &lt;host id='foo'>
 *   &lt;authenticator id='myauth'>...&lt;/authenticator>
 *
 *   &lt;web-app id='/a'>
 *     ...
 *   &lt;/web-app>
 *
 *   &lt;web-app id='/a'>
 *     ...
 *   &lt;/web-app>
 * &lt;/host>
 * </pre></code>
 */
public interface ServletAuthenticator {
  /**
   * Initialize the authenticator.  <code>init()</code> is called after all
   * the bean parameter have been set.
   */
  public void init()
    throws ServletException;
  
  /**
   * Logs a user in with a user name and a password.  The login method
   * is generally called during servlet security checks.  The
   * ServletRequest.getUserPrincipal call will generally call
   * getUserPrincipal.
   *
   * <p>The implementation may only use the response to set cookies
   * and headers.  It may not write output or set the response status.
   * If the application needs to send a custom error reponse,
   * it must implement a custom AbstractLogin instead.
   *
   * @param request servlet request
   * @param response servlet response, in case any cookie need sending.
   * @param application servlet application
   * @param user the user name.
   * @param password the users input password.
   *
   * @return the logged in principal on success, null on failure.
   */
  public Principal login(HttpServletRequest request,
                         HttpServletResponse response,
                         ServletContext application,
                         String user, String password)
    throws ServletException;
  
  /**
   * Gets the authenticated user for the current request.  If the user
   * has not logged in, just returns null.
   *
   * <p>getUserPrincipal is called in response to an application's call to
   * HttpServletRequest.getUserPrincipal.
   *
   * <p>The implementation may only use the response to set cookies
   * and headers.  It may not write output.
   *
   * @param request the request trying to authenticate.
   * @param response the response for setting headers and cookies.
   * @param application the servlet context
   *
   * @return the authenticated user or null if none has logged in
   */
  public Principal getUserPrincipal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    ServletContext application)
    throws ServletException;
  
  /**
   * Validates the user when using HTTP Digest authentication.
   * DigestLogin will call this method.  Most other AbstractLogin
   * implementations, like BasicLogin and FormLogin, will use
   * getUserPrincipal instead.
   *
   * <p>The HTTP Digest authentication uses the following algorithm
   * to calculate the digest.  The digest is then compared to
   * the client digest.
   *
   * <code><pre>
   * A1 = MD5(username + ':' + realm + ':' + password)
   * A2 = MD5(method + ':' + uri)
   * digest = MD5(A1 + ':' + nonce + A2)
   * </pre></code>
   *
   * @param request the request trying to authenticate.
   * @param response the response for setting headers and cookies.
   * @param app the servlet context
   * @param user the username
   * @param realm the authentication realm
   * @param nonce the nonce passed to the client during the challenge
   * @param uri te protected uri
   * @param qop
   * @param nc
   * @param cnonce the client nonce
   * @param clientDigest the client's calculation of the digest
   *
   * @return the logged in principal if successful
   */
  public Principal loginDigest(HttpServletRequest request,
                               HttpServletResponse response,
                               ServletContext app,
                               String user, String realm,
                               String nonce, String uri,
                               String qop, String nc, String cnonce,
                               byte []clientDigset)
    throws ServletException;
  
  /**
   * Returns true if the user plays the named role.
   *
   * <p>This method is called in response to the
   * HttpServletResponse.isUserInRole call and for security-constraints
   * that check the use role.
   *
   * @param request the request testing the role.
   * @param application the owning application
   * @param user the user's Principal.
   * @param role role name.
   */
  public boolean isUserInRole(HttpServletRequest request,
                              HttpServletResponse response,
                              ServletContext application,
                              Principal user, String role)
    throws ServletException;
  
  /**
   * Logs the user out from the given request.
   *
   * <p>Called via the session.logout() method.
   *
   * @param session for timeout, the session timing out. null if force logout
   */
  public void logout(ServletContext application,
                     HttpSession session,
                     String sessionId,
                     Principal user)
    throws ServletException;
}
