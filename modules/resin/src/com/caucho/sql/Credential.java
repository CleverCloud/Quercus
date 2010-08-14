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

package com.caucho.sql;

import javax.resource.spi.ConnectionRequestInfo;

/**
 * The credentials
 */
public class Credential implements ConnectionRequestInfo
{
  private final String _userName;
  private final String _password;

  Credential(String userName, String password)
  {
    if (userName == null)
      userName = "";
    
    if (password == null)
      password = "";
    
    _userName = userName;
    _password = password;
  }

  /**
   * Returns the user name.
   */
  public String getUserName()
  {
    return _userName;
  }

  /**
   * Returns the password
   */
  public String getPassword()
  {
    return _password;
  }

  /**
   * Returns the hash code.
   */
  public int hashCode()
  {
    return getUserName().hashCode() * 65521 + getPassword().hashCode();
  }

  /**
   * Returns true for equality.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof Credential))
      return false;

    Credential credential = (Credential) o;

    return (getUserName().equals(credential.getUserName())
            && getPassword().equals(credential.getPassword()));
  }


  public String toString()
  {
    return getClass().getSimpleName() + "[userName=" + _userName + "]";
  }
}

