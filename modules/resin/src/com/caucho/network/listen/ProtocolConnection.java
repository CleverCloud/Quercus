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

import java.io.IOException;

/**
 * Protocol specific information for each connection.  ProtocolConnection
 * is reused to reduce memory allocations.
 *
 * <p>ProtocolConnections are created by Protocol.createConnection
 */
public interface ProtocolConnection {
  /**
   * Initialize the connection.  At this point, the current thread is the
   * connection thread.
   */
  public void init();

  /**
   * Return true if the connection should wait for a read before
   * handling the request.
   */
  public boolean isWaitForRead();

  /**
   * Called when the connection starts, i.e. just after the accept
   */
  public void onStartConnection();

  /**
   * Handles a new request.  The controlling TcpServer may call
   * handleRequest again after the connection completes, so
   * the implementation must initialize any variables for each connection.
   */
  public boolean handleRequest() throws IOException;
  
  /**
   * Returns a request URL for debugging/management.
   */
  public String getProtocolRequestURL();

  /**
   * Handles a resumption of the connection for an async/comet request.
   */
  public boolean handleResume() throws IOException;

  /**
   * Handles a close event when the connection is closed.
   */
  public void onCloseConnection();
}
