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

package com.caucho.server.cluster;


/**
 * Dynamic clustered representation of a server
 */
@SuppressWarnings("serial")
public class DynamicServerResult implements java.io.Serializable {
  private String _id;
  private String _address;
  private int _port;
  private int _index;

  @SuppressWarnings("unused")
  private DynamicServerResult()
  {
  }

  public DynamicServerResult(String id,
                             int index,
                             String address,
                             int port)
  {
    _id = id;
    _index = index;
    _address = address;
    _port = port;
  }

  public String getId()
  {
    return _id;
  }

  public String getAddress()
  {
    return _address;
  }

  public int getPort()
  {
    return _port;
  }

  public int getIndex()
  {
    return _index;
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "[" + _id
            + ",index=" + _index
            + ",address=" + _address + ":" + _port
            + "]");
  }
}
