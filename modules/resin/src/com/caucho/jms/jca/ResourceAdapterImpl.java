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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jms.jca;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAResource;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * The JCA resource adapter.
 */
public class ResourceAdapterImpl implements ResourceAdapter {
  private static final Logger log
    = Logger.getLogger(ResourceAdapterImpl.class.getName());
  private static final L10N L = new L10N(ResourceAdapterImpl.class);

  private BootstrapContext _ctx;
  
  private ConnectionFactory _connectionFactory;
  private Destination _destination;

  private ArrayList<MessageListenerSpec> _listeners =
    new ArrayList<MessageListenerSpec>();

  /**
   * Sets the connection factory.
   */
  public void setConnectionFactory(ConnectionFactory factory)
  {
    _connectionFactory = factory;
  }

  /**
   * Gets the connection factory.
   */
  public ConnectionFactory getConnectionFactory()
  {
    return _connectionFactory;
  }

  /**
   * Sets the destination
   */
  public void setDestination(Destination destination)
  {
    _destination = destination;
  }

  /**
   * Gets the destination
   */
  public Destination getDestination()
  {
    return _destination;
  }

  /**
   * Initialization.
   */
  public void init()
    throws ConfigException
  {
    if (_connectionFactory == null)
      throw new ConfigException(L.l("connection-factory is not configured.  The JMS resource adapter needs a connection factory."));
    
    if (_destination == null)
      throw new ConfigException(L.l("destination is not configured.  The JMS resource adapter needs a destination."));
  }
  
  /**
   * Called when the resource adapter is started.
   */
  public void start(BootstrapContext ctx)
    throws ResourceAdapterInternalException
  {
    _ctx = ctx;
  }
  
  /**
   * Called when the resource adapter is stopped.
   */
  public void stop()
  {
    _ctx = null;
  }

  /**
   * Returns the work manager.
   */
  WorkManager getWorkManager()
  {
    return _ctx.getWorkManager();
  }

  /**
   * Called during activation of a message endpoint.
   */
  public void endpointActivation(MessageEndpointFactory endpointFactory,
                                 ActivationSpec spec)
    throws NotSupportedException, ResourceException
  {
    MessageListenerSpec listener = (MessageListenerSpec) spec;
    listener.setEndpointFactory(endpointFactory);

    try {
      listener.start();
    } catch (ResourceException e) {
      throw new NotSupportedException(e);
    }
  }
  
  /**
   * Called during deactivation of a message endpoint.
   */
  public void endpointDeactivation(MessageEndpointFactory endpointFactory,
                                   ActivationSpec spec)
  {
  }
  
  /**
   * Called during crash recovery.
   */
  public XAResource []getXAResources(ActivationSpec []specs)
    throws ResourceException
  {
    return null;
  }
}

