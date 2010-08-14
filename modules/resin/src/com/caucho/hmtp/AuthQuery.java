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

package com.caucho.hmtp;

import java.io.Serializable;

/**
 * Authentication query merges the Hmtp authentication and bind steps
 */
@SuppressWarnings("serial")
public class AuthQuery implements Serializable {
  private final String _uid;
  private final Serializable _credentials;
  
  private final String _resource;

  /**
   * null constructor for Hessian.
   */
  @SuppressWarnings("unused")
  private AuthQuery()
  {
    _uid = null;
    _credentials = null;
    _resource = null;
  }

  /**
   * login packet
   */
  public AuthQuery(String uid, Serializable credentials)
  {
    _uid = uid;
    _credentials = credentials;
    _resource = null;
  }

  /**
   * login packet
   */
  public AuthQuery(String uid, Serializable credentials, String resource)
  {
    _uid = uid;
    _credentials = credentials;
    _resource = resource;
  }

  public String getUid()
  {
    return _uid;
  }

  public Serializable getCredentials()
  {
    return _credentials;
  }

  public String getResource()
  {
    return _resource;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");
    sb.append(_uid);

    if (_credentials != null) {
      sb.append(",");
      sb.append(_credentials.getClass().getSimpleName());
    }

    if (_resource != null) {
      sb.append(",resource=");
      sb.append(_resource);
    }
    
    sb.append("]");

    return sb.toString();
  }
}
