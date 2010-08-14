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

package com.caucho.jms.jca;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.security.auth.Subject;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The managed factory implementation.
 */
public class MessageSenderManager
  implements ManagedConnectionFactory, ResourceAdapterAssociation {
  protected static final Logger log
    = Logger.getLogger(MessageSenderManager.class.getName());
  private static final L10N L = new L10N(MessageSenderManager.class);

  private ResourceAdapterImpl _ra;

  public MessageSenderManager()
  {
  }

  public ResourceAdapter getResourceAdapter()
  {
    return _ra;
  }

  public void setResourceAdapter(ResourceAdapter adapter)
    throws ResourceException
  {
    if (! (adapter instanceof ResourceAdapterImpl))
      throw new ResourceException(L.l("'{0}' is not a valid resource-adapter for MessageSenderManager.",
                                      adapter.getClass().getName()));

    _ra = (ResourceAdapterImpl) adapter;
  }

  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_ra == null)
      throw new ConfigException(L.l("MessageSenderManager must be configured with a resource adapter"));
  }
  
  /**
   * Creates the data source the user sees.
   */
  public Object createConnectionFactory(ConnectionManager connManager)
    throws ResourceException
  {
    return new MessageSenderImpl(this, connManager);
  }

  /**
   * Creates the data source the user sees.  Not needed in this case,
   * since ManagedFactoryImpl is only allowed in Resin.
   */
  public Object createConnectionFactory()
    throws ResourceException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates the underlying managed connection.
   */
  public ManagedConnection
    createManagedConnection(Subject subject,
                            ConnectionRequestInfo requestInfo)
    throws ResourceException
  {
    ResourceAdapterImpl ra = _ra;
    
    return new ManagedSessionImpl(ra.getConnectionFactory(),
                                  ra.getDestination());
  }

  /**
   * Creates the underlying managed connection.
   */
  public ManagedConnection
    matchManagedConnections(Set connSet,
                            Subject subject,
                            ConnectionRequestInfo requestInfo)
    throws ResourceException
  {
    Iterator<ManagedSessionImpl> iter = (Iterator<ManagedSessionImpl>) connSet.iterator();

    if (iter.hasNext()) {
      ManagedSessionImpl mConn;
      mConn = (ManagedSessionImpl) iter.next();

      return mConn;
    }
    
    return null;
  }

  /**
   * Returns the dummy log writer.
   */
  public PrintWriter getLogWriter()
  {
    return null;
  }

  /**
   * Sets the dummy log writer.
   */
  public void setLogWriter(PrintWriter log)
  {
  }
}

