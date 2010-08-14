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

import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.util.L10N;

import java.security.*;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Defines the policy for the current security context.
 */
public class PolicyImpl extends Policy {
  private static Logger _log;
  private static L10N _L;

  private static final PolicyImpl _policy = new PolicyImpl();

  private ClassLoader _systemClassLoader;
  private Policy _parent;

  private PolicyImpl()
  {
    _parent = Policy.getPolicy();
    _systemClassLoader = ClassLoader.getSystemClassLoader();
  }

  public static PolicyImpl getPolicy()
  {
    return _policy;
  }

  public static void init()
  {
    Policy.setPolicy(_policy);
  }

  public PermissionCollection getPermissions(CodeSource codesource)
  {
    PermissionCollection perms = new Permissions();
    perms.add(new AllPermission());
    
    return perms;
  }

  public PermissionCollection getPermissions(ProtectionDomain domain)
  {
    PermissionCollection perms = new Permissions();
    perms.add(new AllPermission());
    
    return perms;
  }

  public boolean implies(ProtectionDomain domain, Permission permission)
  {
    /*
    if (domain == null)
      return true; // handle null value passed from RMI
    */
    
    ClassLoader loader = domain.getClassLoader();

    if (loader == _systemClassLoader)
      return true;

    // XXX: temporary to restore default security-manager
    if (true)
      return true;
    if (true && _parent != null)
      return _parent.implies(domain, permission);
    else if (true)
      return true;

    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        EnvironmentClassLoader envLoader;
        envLoader = (EnvironmentClassLoader) loader;

        ArrayList<Permission> perms = envLoader.getPermissions();

        if (perms == null)
          return _parent.implies(domain, permission);

        for (int i = perms.size() - 1; i >= 0; i--) {
          Permission perm = perms.get(i);

          if (permission.implies(perm))
            return true;
        }

        return _parent.implies(domain, permission);
      }
    }

    if (loader == null)
      return true;
    
    return true;
  }

  public void refresh()
  {
  }

  private Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(PolicyImpl.class.getName());

    return _log;
  }

  private L10N L()
  {
    if (_L == null)
      _L = new L10N(PolicyImpl.class);

    return _L;
  }

  public String toString()
  {
    return "PolicyImpl[]";
  }
}
