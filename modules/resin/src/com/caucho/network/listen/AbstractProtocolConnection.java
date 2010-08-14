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
 * Protocol specific information for each request.  ServerRequest
 * is reused to reduce memory allocations.
 *
 * <p>ServerRequests are created by Server.createRequest()
 */
public abstract  class AbstractProtocolConnection implements ProtocolConnection {
  /**
   * Initialize the connection.  At this point, the current thread is the
   * connection thread.
   */
  @Override
  public void init()
  {
  }
  
  @Override
  public void onStartConnection()
  {
    
  }
  
  /**
   * Returns a default debugging identifier for the connection
   */
  @Override
  public String getProtocolRequestURL()
  {
    return null;
  }
  
  /**
   * Handles a new connection.  The controlling TcpServer may call
   * handleConnection again after the connection completes, so 
   * the implementation must initialize any variables for each connection.
   *
   * @param conn Information about the connection, including buffered
   * read and write streams.
   */
  @Override
  public abstract boolean handleRequest() throws IOException;

  /**
   * Handles a resumption of the connection for an async/comet request.
   */
  @Override
  public boolean handleResume()
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Handles a close event when the connection is closed.
   */
  @Override
  public void onCloseConnection()
  {
  }
}
