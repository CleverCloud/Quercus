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

import com.caucho.util.L10N;

import javax.jms.JMSException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The JMS MessageListener configuration specification.
 */
public class MessageListenerSpec implements ActivationSpec {
  private static final L10N L = new L10N(MessageListenerSpec.class);
  private static final Logger log
    = Logger.getLogger(MessageListenerSpec.class.getName());

  private ResourceAdapterImpl _ra;
  
  private MessageEndpointFactory _factory;

  private ArrayList<MessageListenerTask> _endpoints =
    new ArrayList<MessageListenerTask>();
  
  /**
   * Returns the associated ResourceAdapter
   */
  public ResourceAdapter getResourceAdapter()
  {
    return _ra;
  }
  
  /**
   * Associate this JavaBean with a resource adapter java bean.
   *
   * This method is called exactly once.
   */
  public void setResourceAdapter(ResourceAdapter ra)
    throws ResourceException
  {
    if (! (ra instanceof ResourceAdapterImpl))
      throw new ResourceException(L.l("'{0}' is not a valid resource adapter for the JMS MessageListenerSpec",
                                      ra.getClass().getName()));

    _ra = (ResourceAdapterImpl) ra;
  }

  /**
   * Sets the endpoint factory.
   */
  void setEndpointFactory(MessageEndpointFactory factory)
  {
    _factory = factory;
  }

  /**
   * Validates the configuration.
   */
  public void validate()
    throws InvalidPropertyException
  {
  }

  /**
   * Starts the listener.
   */
  void start()
    throws ResourceException
  {
    try {
      ListenerEndpoint endpoint;
      endpoint = (ListenerEndpoint) _factory.createEndpoint(null);

      MessageListenerTask task = new MessageListenerTask(_ra, endpoint);

      _endpoints.add(task);

      _ra.getWorkManager().startWork(task);
    } catch (JMSException e) {
      throw new ResourceException(e);
    }
  }

  /**
   * Stops the listener.
   */
  void stop()
    throws JMSException
  {
    for (int i = 0; i < _endpoints.size(); i++) {
      MessageListenerTask task = _endpoints.get(i);

      try {
        task.release();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    _endpoints.clear();
  }
}

