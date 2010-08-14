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

package javax.resource.spi;

import java.util.EventObject;

/**
 * A listener for connection events, implemented by the application
 * server.
 */
public class ConnectionEvent extends EventObject {
  public final static int CONNECTION_CLOSED = 1;
  public final static int CONNECTION_ERROR_OCCURRED = 5;
  public final static int LOCAL_TRANSACTION_COMMITTED = 3;
  public final static int LOCAL_TRANSACTION_ROLLEDBACK = 4;
  public final static int LOCAL_TRANSACTION_STARTED = 2;

  protected int id;
  private Exception exception;
  private Object connectionHandle;

  /**
   * Creates an event.
   */
  public ConnectionEvent(ManagedConnection source, int eid)
  {
    super(source);

    this.id = eid;
  }

  /**
   * Creates an event.
   */
  public ConnectionEvent(ManagedConnection source, int eid,
                         Exception exception)
  {
    super(source);

    this.id = eid;
    this.exception = exception;
  }

  /**
   * Returns the connection handle.
   */
  public Object getConnectionHandle()
  {
    return this.connectionHandle;
  }

  /**
   * Sets the connection handle.
   */
  public void setConnectionHandle(Object handle)
  {
    this.connectionHandle = handle;
  }

  /**
   * Returns the exception
   */
  public Exception getException()
  {
    return this.exception;
  }

  /**
   * Returns the type of the event.
   */
  public int getId()
  {
    return this.id;
  }
}
