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

package com.caucho.security;

import java.security.Principal;

/**
 * Defines the interface any security provider must implement.
 */
public interface SecurityContextProvider {
  /**
   * Returns the Principal for the context.
   *
   * @return the principal in the context or null.
   */
  public Principal getUserPrincipal()
    throws SecurityContextException;
  
  /**
   * Returns true if the user principal plays the named role.
   *
   * @param permission the permission to test against.
   */
  public boolean isUserInRole(String permission);
  
  /**
   * Sets the current runAs role/principal.  This should affect
   * <code>isUserInRole</code> and <code>getUserPrincipal</code>.
   *
   * @param roleName the new role
   * @return the old run-as role
   */
  public String runAs(String roleName);
  
  /**
   * Returns true if the transport context is secure (SSL).
   */
  public boolean isTransportSecure()
    throws SecurityContextException;
  
  /**
   * Logs the principal out.
   */
  /*
  public void logout()
    throws SecurityContextException;
  */
}
