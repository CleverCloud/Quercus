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

package com.caucho.security;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Used to login and logout users in a servlet request.  AbstractLogin handles
 * the different login types like "basic" or "form".  Normally, a Login
 * will delegate the actual authentication to a ServletAuthenticator.
 *
 * @since Resin 4.0.0
 */
public interface Login {
  public static final String LOGIN_NAME = "caucho.login";
  public static final String LOGIN_USER = "caucho.user";
  public static final String LOGIN_PASSWORD = "caucho.password";

  /**
   * Returns the authentication type.  <code>getAuthType</code> is called
   * by <code>HttpServletRequest.getAuthType</code>.
   */
  public String getAuthType();

  /**
   * Returns the configured authenticator
   */
  public Authenticator getAuthenticator();
  
  /**
   * Returns true if the login can be used for this request. This lets
   * webapps use multiple login methods.
   */
  public boolean isLoginUsedForRequest(HttpServletRequest request);
  
  /**
   * Returns the Principal associated with the current request.
   * getUserPrincipal is called in response to the Request.getUserPrincipal
   * call.  Login.getUserPrincipal can't modify the response or return
   * an error page.
   *
   * @param request servlet request
   *
   * @return the logged in principal on success, null on failure.
   */
  public Principal getUserPrincipal(HttpServletRequest request);
  
  /**
   * Logs a user in.  The authenticate method is called during the
   * security check.  If the user does not exist, <code>authenticate</code>
   * sets the reponse error page and returns null.
   *
   * @param request servlet request
   * @param response servlet response for a failed authentication.
   * @param isFail true if the authorization has failed
   *
   * @return the logged in principal on success, null on failure.
   */
  public Principal login(HttpServletRequest request,
                         HttpServletResponse response,
                         boolean isFail);

  /**
   * Returns true if username and password based authentication is supported.
   * @see BasicLogin
   * @return
   */
  public boolean isPasswordBased();

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
  public boolean isUserInRole(Principal user, String role);
  
  /**
   * Logs the user out from the given request.
   *
   * <p>Since there is no servlet API for logout, this must be called
   * directly from user code.  Resin stores the web-app's login object
   * in the ServletContext attribute "caucho.login".
   */
  public void logout(Principal user,
                     HttpServletRequest request,
                     HttpServletResponse response);
  
  /**
   * Called when the session invalidates.
   */
  public void sessionInvalidate(HttpSession session,
                                boolean isTimeout);
}
