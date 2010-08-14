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

package com.caucho.server.admin;

import com.caucho.config.*;
import com.caucho.security.BasicPrincipal;
import com.caucho.security.AbstractAuthenticator;
import com.caucho.security.PasswordUser;
import com.caucho.util.*;

import java.security.Principal;
import java.util.*;
import java.util.logging.*;

/**
 * Special authenticator for management
 */
public class ManagementAuthenticator extends AbstractAuthenticator {
  private static final Logger log
    = Logger.getLogger(ManagementAuthenticator.class.getName());
  
  private TreeMap<String,PasswordUser> _userMap
    = new TreeMap<String,PasswordUser>();

  private String _remoteCookie;

  /**
   * Adds a password user from the configuration.
   */
  public void addUser(String name, PasswordUser user)
  {
    _userMap.put(name, user);
    _remoteCookie = null;
  }
  
  /**
   * Returns the PasswordUser
   */
  @Override
  protected PasswordUser getPasswordUser(String userName)
  {
    if (userName == null)
      return null;

    // The caller should clear the password in the returned PasswordUser,
    // so we need to return a copy
    PasswordUser user = _userMap.get(userName);

    if (user != null)
      return user.copy();
    else
      return null;
  }

  /**
   * Creates a cookie based on the user hash.
   */
  public String getHash()
  {
    if (_remoteCookie == null) {
      long crc64 = 0;

      for (PasswordUser user : _userMap.values()) {
        if (user.isDisabled())
          continue;

        String item = (user.getPrincipal().getName()
                       + ":" + new String(user.getPassword()));

        crc64 = Crc64.generate(crc64, item);
      }

      if (crc64 != 0) {
        CharBuffer cb = new CharBuffer();
        Base64.encode(cb, crc64);

        _remoteCookie = cb.toString();
      }
    }

    return _remoteCookie;
  }
}
