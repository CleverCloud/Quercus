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

package com.caucho.vfs;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.Selector;

/**
 * Abstract socket to handle both normal sockets and bin/resin sockets.
 */
abstract public class QServerSocket {
  public void setTcpNoDelay(boolean delay)
  {
  }
  
  public boolean isTcpNoDelay()
  {
    return false;
  }
  
  public boolean isJni()
  {
    return false;
  }

  public boolean setSaveOnExec()
  {
    return false;
  }

  public int getSystemFD()
  {
    return -1;
  }

  /**
   * Sets the socket's listen backlog.
   */
  public void listen(int backlog)
  {
  }

  /**
   * Sets the connection read timeout.
   */
  abstract public void setConnectionSocketTimeout(int ms);
  
  abstract public boolean accept(QSocket socket)
    throws IOException;
  
  abstract public QSocket createSocket()
    throws IOException;

  abstract public InetAddress getLocalAddress();

  abstract public int getLocalPort();

  public Selector getSelector()
  {
    return null;
  }

  public boolean isClosed()
  {
    return false;
  }
  
  abstract public void close()
    throws IOException;
}

