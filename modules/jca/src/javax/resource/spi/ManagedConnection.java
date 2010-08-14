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

import javax.resource.ResourceException;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import java.io.PrintWriter;

/**
 * Interface for the resource adapter's connection instance.  This
 * interface is used by the app server and is normally not visible
 * to the application itself.
 */
public interface ManagedConnection {
  /**
   * Creates a new connection handle for the underlying physical connection.
   */
  public Object getConnection(Subject subject,
                              ConnectionRequestInfo requestInfo)
    throws ResourceException;

  /**
   * Associates an application-level connection handle with a ManagedConnection
   */
  public void associateConnection(Object connection)
    throws ResourceException;

  /**
   * Adds a new listener.
   */
  public void addConnectionEventListener(ConnectionEventListener listener);

  /**
   * Removes an old new listener.
   */
  public void removeConnectionEventListener(ConnectionEventListener listener);

  /**
   * Returns an XAResource for the connection.
   */
  public XAResource getXAResource()
    throws ResourceException;

  /**
   * Returns a LocalTransaction.
   */
  public LocalTransaction getLocalTransaction()
    throws ResourceException;

  /**
   * Returns the meta data for the connection.
   */
  public ManagedConnectionMetaData getMetaData()
    throws ResourceException;

  /**
   * Sets the log stream.
   */
  public void setLogWriter(PrintWriter log)
    throws ResourceException;

  /**
   * Returns the log stream.
   */
  public PrintWriter getLogWriter()
    throws ResourceException;
  
  /**
   * Cleanup the physical connection.
   */
  public void cleanup()
    throws ResourceException;
  
  /**
   * Destroys the physical connection.
   */
  public void destroy()
    throws ResourceException;
}
