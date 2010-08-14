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

package com.caucho.management.server;

import java.io.Serializable;

/**
 * Information about a tcp connection
 */
public class TcpConnectionInfo implements java.io.Serializable
{
  // the connection id
  private final int _id;
  // the thread id
  private final long _threadId;
  
  // the port id
  private final String _portName;

  // the connection state
  private final String _state;
  // the request time
  private final long _requestTime;

  /**
   * null-arg constructor for Hessian.
   */
  private TcpConnectionInfo()
  {
    _id = 0;
    _threadId = 0;
    _portName = null;
    _state = null;
    _requestTime = 0;
  }

  public TcpConnectionInfo(int id,
                           long threadId,
                           String portName,
                           String state,
                           long requestTime)
  {
    _id = id;
    _threadId = threadId;
    _portName = portName;
    _state = state;
    _requestTime = requestTime;
  }

  public int getId()
  {
    return _id;
  }

  public long getThreadId()
  {
    return _threadId;
  }

  public String getPortName()
  {
    return _portName;
  }

  public String getState()
  {
    return _state;
  }

  public long getRequestTime()
  {
    return _requestTime;
  }

  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _id
            + "," + _portName
            + "]");
  }
}
