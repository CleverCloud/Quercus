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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;

/**
 * Implements the "CLIENT-CERT" auth-method.  CLIENT-CERT uses the
 * SSL authentication with WWW-Authenticate and SC_UNAUTHORIZE.
 */
public class ClientCertLogin extends AbstractLogin {
  /**
   * Returns the authentication type.
   */
  public String getAuthType()
  {
    return "CLIENT_CERT";
  }
  
  /**
   * Logs a user in with a user name and a password.  Basic authentication
   * extracts the user and password from the authorization header.  If
   * the user/password is missing, authenticate will send a basic challenge.
   *
   * @param request servlet request
   * @param response servlet response, in case any cookie need sending.
   * @param application servlet application
   *
   * @return the logged in principal on success, null on failure.
   */
  public Principal authenticate(HttpServletRequest request,
                                HttpServletResponse response,
                                ServletContext application)
    throws ServletException, IOException
  {
    return getUserPrincipal(request, response, application);
  }

  @Override
  public Principal getUserPrincipal(HttpServletRequest request)
  {
    return getUserPrincipal(request, null, null);
  }

  /**
   * Returns the current user with the user name and password.
   *
   * @param request servlet request
   * @param response servlet response, in case any cookie need sending.
   * @param application servlet application
   *
   * @return the logged in principal on success, null on failure.
   */
  public Principal getUserPrincipal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    ServletContext application)
  {
    X509Certificate []certs;

    certs = (X509Certificate []) request.getAttribute("javax.servlet.request.X509Certificate");

    if (certs != null)
      return certs[0].getSubjectDN();
    else
      return null;
  }
}
