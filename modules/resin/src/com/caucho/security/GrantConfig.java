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

import java.security.Permission;
import java.security.Policy;
import java.util.ArrayList;

import com.caucho.loader.Environment;
import com.caucho.util.L10N;

/**
 * Grants permissions.
 */
public class GrantConfig {
  static final L10N L = new L10N(GrantConfig.class);

  private ArrayList<Permission> _permissionList =
    new ArrayList<Permission>();

  public void addPermission(PermissionConfig permission)
  {
    _permissionList.add(permission.getPermission());
  }

  public void init()
  {
    if (! (Policy.getPolicy() instanceof PolicyImpl)) {
      Policy.setPolicy(PolicyImpl.getPolicy());
    }
    
    for (int i = 0; i < _permissionList.size(); i++) {
      Environment.addPermission(_permissionList.get(i));
    }
  }
}
