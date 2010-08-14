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

package com.caucho.servlet;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Bidirectional TCP connection based on a HTTP upgrade, e.g. WebSocket.
 *
 * The context and its values are not thread safe.  The DuplexListener
 * thread normally is the only thread reading from the input stream.
 */
public interface WebSocketContext {
  /**
   * Returns the input stream
   */
  public InputStream getInputStream()
    throws IOException;

  /**
   * Returns the output stream.
   */
  public OutputStream getOutputStream()
    throws IOException;

  /**
   * Sets the read timeout.
   */
  public void setTimeout(long timeout);

  /**
   * Gets the read timeout.
   */
  public long getTimeout();

  /**
   * Complete and close the connection.
   */
  public void complete();
}
