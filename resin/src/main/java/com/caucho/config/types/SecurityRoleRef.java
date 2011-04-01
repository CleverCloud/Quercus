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

package com.caucho.config.types;

import com.caucho.util.L10N;

/**
 * Configuration for the ejb-ref
 */
public class SecurityRoleRef {
  private static final L10N L = new L10N(SecurityRoleRef.class);

  private String _roleName;
  private String _roleLink;

  /**
   * Sets the config file id attribute.
   */
  public void setId(String id)
  {
  }

  /**
   * Adds a description
   */
  public void addDescription(String description)
  {
  }

  /**
   * Sets the role name which is an alias of the linked value
   */
  public void setRoleName(String roleName)
  {
    _roleName = roleName;
  }

  /**
   * Sets the role link
   */
  public void setRoleLink(String roleLink)
  {
    _roleLink = roleLink;
  }

  public String toString()
  {
    return "SecurityRoleRef[" + _roleName + "]";
  }
}
