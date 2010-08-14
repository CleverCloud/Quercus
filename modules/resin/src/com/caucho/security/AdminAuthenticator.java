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

import javax.inject.Named;
import javax.enterprise.inject.Default;

import com.caucho.config.Admin;
import com.caucho.config.CauchoDeployment;
import com.caucho.config.Service;
import com.caucho.util.Base64;
import com.caucho.util.Crc64;

/**
 * The admin authenticator provides authentication for Resin admin/management
 * purposes.  It's typically defined at the &lt;resin> level.
 *
 * <code><pre>
 * &lt;security:AdminAuthenticator path="WEB-INF/admin-users.xml"/>
 * </pre></code>
 *
 * <p>The format of the static file is as follows:
 *
 * <code><pre>
 * &lt;users>
 *   &lt;user name="h.potter" password="quidditch" roles="user,captain"/>
 *   ...
 * &lt;/users>
 * </pre></code>
 */
@Service
@Admin
@Named("resinAdmin")
@Default
@CauchoDeployment  
@SuppressWarnings("serial")
public class AdminAuthenticator extends XmlAuthenticator
{
  private String _remoteCookie;
  

  public AdminAuthenticator()
  {
  }
  
  /**
   * Abstract method to return a user based on the name
   *
   * @param userName the string user name
   *
   * @return the populated PasswordUser value
   */
  @Override
  protected PasswordUser getPasswordUser(String userName)
  {
    if ("admin.resin".equals(userName)) {
      String hash = getHash();
      PasswordDigest digest = getPasswordDigest();

      if (digest != null)
        hash = digest.getPasswordDigest(userName, hash);
      
      return new PasswordUser(userName, hash);
    }
    else
      return super.getPasswordUser(userName);
  }

  /**
   * Creates a cookie based on the user hash.
   */
  public String getHash()
  {
    if (_remoteCookie == null) {
      long crc64 = 0;

      for (PasswordUser user : getUserMap().values()) {
        if (user.isDisabled())
          continue;

        crc64 = Crc64.generate(crc64, user.getPrincipal().getName());
        crc64 = Crc64.generate(crc64, ":");
        crc64 = Crc64.generate(crc64, new String(user.getPassword()));
      }

      if (crc64 != 0) {
        StringBuilder cb = new StringBuilder();
        Base64.encode(cb, crc64);

        _remoteCookie = cb.toString();
      }
    }

    return _remoteCookie;
  }

  @Override
  public String getDefaultGroup()
  {
    return "resin-admin";
  }
}
