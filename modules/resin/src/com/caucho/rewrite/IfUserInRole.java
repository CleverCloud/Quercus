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
 * @author Sam
 */

package com.caucho.rewrite;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.util.L10N;

import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.annotation.PostConstruct;

/**
 * Match if the user is in any of the given roles,
 * i.e. if request.isUserInRole() matches.
 *
 * <pre>
 * &lt;resin:Allow url-pattern="/admin/*"&gt;
 *                  xmlns:resin="urn:java:com.caucho.resin"&gt;
 *   &lt;resin:IfUserInRole role="admin"/>
 * &lt;/resin:Allow>
 * </pre>
 *
 * <p>RequestPredicates may be used for security and rewrite actions.
 */
@Configurable
public class IfUserInRole implements RequestPredicate
{
  private static final Logger log
    = Logger.getLogger(IfUserInRole.class.getName());

  private String []_roles = new String[0];

  /**
   * Adds a role to check.  The user must match one of the roles.
   */
  @Configurable
  public void addRole(String role)
  {
    String []newRoles = new String[_roles.length + 1];
    System.arraycopy(_roles, 0, newRoles, 0, _roles.length);
    newRoles[_roles.length] = role;
    _roles = newRoles;
  }

  /**
   * True if the predicate matches.
   *
   * @param request the servlet request to test
   */
  @Override
  public boolean isMatch(HttpServletRequest request)
  {
    Principal user = request.getUserPrincipal();
    
    if (user == null)
      return false;
    
    for (String role : _roles) {
      if (role.equals("*"))
        return true;

      if (request.isUserInRole(role)) {
        if (log.isLoggable(Level.FINER))
          log.finer("IfUserInRole[" + role + "] " + user);

        return true;
      }
    }

    if (_roles.length == 0)
      return true;
    
    if (log.isLoggable(Level.FINER))
      log.finer(this + " does not match " + user);
    
    return false;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");
    for (int i = 0; i < _roles.length; i++) {
      if (i != 0)
        sb.append(',');
      sb.append(_roles[i]);
    }
    sb.append("]");
    
    return sb.toString();
  }
}
