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

import com.caucho.config.ConfigException;
import com.caucho.config.types.InstantiationConfig;
import com.caucho.util.L10N;

/**
 * Permission configuration.
 */
public class PermissionConfig {
  static final L10N L = new L10N(PermissionConfig.class);

  private InstantiationConfig _type;

  private Permission _perm;

  /**
   * Sets the permission type.
   */
  public void setType(InstantiationConfig type)
    throws ConfigException
  {
    if (! Permission.class.isAssignableFrom(type.getType()))
      throw new ConfigException(L.l("`{0}' must extend java.security.Permission",
                                    type));

    _type = type;
  }

  /**
   * Adds an argument to the type.
   */
  public void addArg(Object obj)
  {
    _type.addArg(obj);
  }

  /**
   * Creates the permission.
   */
  public void init()
    throws Exception
  {
    _perm = (Permission) _type.create();
  }

  /**
   * Returns the permission.
   */
  public Permission getPermission()
  {
    return _perm;
  }
  
}
