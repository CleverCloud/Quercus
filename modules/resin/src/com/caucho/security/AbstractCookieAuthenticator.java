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

/**
 * All applications should extend AbstractAuthenticator to implement
 * their custom authenticators.  While this isn't absolutely required,
 * it protects implementations from API changes.
 *
 * <p>The AbstractAuthenticator provides a single-signon cache.  Users
 * logged into one web-app will share the same principal.
 */
@SuppressWarnings("serial")
public class AbstractCookieAuthenticator extends AbstractAuthenticator
  implements CookieAuthenticator
{
  //
  // CookieAuthenticator API
  //

  /**
   * Test if cookie-based authentication is supported.
   */
  @Override
  public boolean isCookieSupported(String jUseCookieAuth)
  {
    return false;
  }

  /**
   * Associate the user with a cookie
   */
  @Override
  public boolean associateCookie(Principal user, String cookieValue)
  {
    return false;
  }

  /**
   * Authenticates the user based on the cookie
   */
  @Override
  public Principal authenticateByCookie(String cookieValue)
  {
    return null;
  }
}
