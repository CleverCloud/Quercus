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


/**
 * Abstract implementation of the Protocol.
 */
abstract public class AbstractProtocol implements Protocol {
  // The owning port
  //private Port _port;
  
  private ClassLoader _classLoader;

  // The protocol name
  private String _name = "tcp";

  protected AbstractProtocol()
  {
    _classLoader = Thread.currentThread().getContextClassLoader();
  }
  
  /**
   * Sets the protocol name.
   */
  public void setProtocolName(String name)
  {
    _name = name;
  }

  /**
   * Returns the protocol name.
   */
  public String getProtocolName()
  {
    return _name;
  }
  
  /**
   * Sets the containing port
   */
  /*
  public void setPort(Port port)
  {
    _port = port;
  }
  */

  /**
   * Gets the parent port.
   */
  /*
  public Port getPort()
  {
    return _port;
  }
  */
  
  /**
   * Returns the protocol owning classloader
   */
  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }
  
  /**
   * Create a Request object for the new thread.
   */
  abstract public ProtocolConnection createConnection(SocketLink conn);
}
