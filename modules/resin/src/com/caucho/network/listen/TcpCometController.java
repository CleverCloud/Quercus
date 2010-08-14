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

package com.caucho.network.listen;

import com.caucho.inject.Module;
import com.caucho.util.Alarm;

/**
 * Public API to control a comet connection.
 */
@Module
public class TcpCometController extends AsyncController {
  private TcpSocketLink _conn;

  private CometHandler _cometHandler;

  private boolean _isTimeout;

  TcpCometController(TcpSocketLink conn,
                     CometHandler cometHandler)
  {
    _conn = conn;
    _cometHandler = cometHandler;
  }

  public TcpSocketLink getConnection()
  {
    return _conn;
  }

  /**
   * Sets the max idle time.
   */
  public void setMaxIdleTime(long idleTime)
  {
    if (idleTime < 0 || Long.MAX_VALUE / 2 < idleTime)
      idleTime = Long.MAX_VALUE / 2;
    
    _conn.setIdleTimeout(idleTime);
  }

  /**
   * Gets the max idle time.
   */
  public long getMaxIdleTime()
  {
    return _conn.getIdleTimeout();
  }

  /**
   * Complete the connection
   */
  public final void complete()
  {
    TcpSocketLink conn = _conn;

    if (conn != null)
      conn.toCometComplete();

    // _cometHandler.onComplete();

    wake();
  }

  /**
   * Wakes the connection.
   */
  public final boolean wake()
  {
    TcpSocketLink conn = _conn;

    if (conn != null)
      return conn.wake();
    else
      return false;
  }

  /**
   * Returns true for a duplex controller
   */
  public boolean isDuplex()
  {
    return false;
  }

  /**
   * Sets the timeout.
   */
  public final void timeout()
  {
    _cometHandler.onTimeout();

    _conn.toCometComplete();

    wake();
  }

  /**
   * Return true if timed out
   */
  public final boolean isTimeout()
  {
    return _isTimeout;
  }

  /**
   * Returns true if the connection is active.
   */
  public final boolean isActive()
  {
    return _conn != null;
  }

  /**
   * Returns true for an active comet connection.
   */
  public boolean isComet()
  {
    TcpSocketLink conn = _conn;

    return conn != null && ! conn.isCometComplete();
  }

  /**
   * Returns true if the connection is active.
   */
  public final boolean isClosed()
  {
    TcpSocketLink conn = _conn;

    return conn == null || conn.isCometComplete();
  }

  @Override
  public void closeImpl()
  {
    // complete();

    TcpSocketLink conn = _conn;
    _conn = null;

    if (conn != null)
      conn.closeController(this);
  }

  public String toString()
  {
    TcpSocketLink conn = _conn;

    if (conn == null)
      return getClass().getSimpleName() + "[closed]";

    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append("[");

    if (Alarm.isTest())
      sb.append("test");
    else
      sb.append(conn.getId());

    TcpSocketLink tcpConn = null;

    if (_conn instanceof TcpSocketLink)
      tcpConn = (TcpSocketLink) _conn;

    if (tcpConn != null && tcpConn.isCometComplete())
      sb.append(",complete");

    if (tcpConn != null && tcpConn.isCometSuspend())
      sb.append(",suspended");

    if (tcpConn != null && tcpConn.isWakeRequested())
      sb.append(",wake");

    sb.append("]");

    return sb.toString();
  }
}
