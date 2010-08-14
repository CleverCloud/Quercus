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
import java.io.PrintWriter;
import java.util.Set;

/**
 * Interface for the resource adapter's connection factory.
 */
public interface ManagedConnectionFactory extends java.io.Serializable {
  /**
   * Creates a Connection Factory instance.  The ConnectionFactory
   * instance is initialized with the ConnectionManager.
   */
  public Object createConnectionFactory(ConnectionManager manager)
    throws ResourceException;
  
  /**
   * Creates a Connection Factory instance, using a default
   * ConnectionManager from the resource adapter.
   * instance is initialized with the ConnectionManager.
   */
  public Object createConnectionFactory()
    throws ResourceException;

  /**
   * Creates physical connection to the EIS resource manager.
   */
  public ManagedConnection createManagedConnection(Subject subject,
                                                   ConnectionRequestInfo info)
    throws ResourceException;

  /**
   * Returns a matched connection.
   */
  public ManagedConnection
    matchManagedConnections(Set connectionSet,
                            Subject subject,
                            ConnectionRequestInfo info)
    throws ResourceException;

  /**
   * Sets the log writer for the ManagedConnectionFactory.
   */
  public void setLogWriter(PrintWriter out)
    throws ResourceException;

  /**
   * Gets the log writer for the ManagedConnectionFactory.
   */
  public PrintWriter getLogWriter()
    throws ResourceException;

  public boolean equals(Object o);

  public int hashCode();
}
