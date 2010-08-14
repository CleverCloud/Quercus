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

package com.caucho.hemp.servlet;

import com.caucho.hmtp.AuthQuery;

import java.io.Serializable;

/**
 * Authentication query merges the Hmtp authentication and bind steps
 */
public class LoginQuery implements Serializable {
  private final AuthQuery _auth;
  private final String _ipAddress;
  private final transient boolean _isLocal;

  /**
   * login packet
   */
  public LoginQuery(AuthQuery auth, String ipAddress)
  {
    _auth = auth;
    _ipAddress = ipAddress;
    _isLocal = true;
  }

  public AuthQuery getAuth()
  {
    return _auth;
  }

  public String getAddress()
  {
    if (_isLocal)
      return _ipAddress;
    else
      return "xx.xx.xx.xx";
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");
    sb.append(_auth);

    sb.append(", ");
    sb.append(_ipAddress);
    
    sb.append("]");

    return sb.toString();
  }
}
