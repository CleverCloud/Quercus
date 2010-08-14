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

package com.caucho.resin;

import com.caucho.server.cluster.*;

/**
 * Embeddable version of a Resin port
 */
abstract public class PortEmbed
{
  private int _port = -1;
  private String _address;

  /**
   * The TCP port
   */
  public void setPort(int port)
  {
    _port = port;
  }

  /**
   * The TCP port
   */
  public int getPort()
  {
    return _port;
  }

  /**
   * Returns the local, bound port
   */
  abstract public int getLocalPort();

  /**
   * The binding address
   */
  public void setAddress(String address)
  {
    _address = address;
  }

  /**
   * The binding address
   */
  public String getAddress()
  {
    return _address;
  }

  /**
   * Binds the port to the server
   */
  abstract public void bindTo(Server server);
}
