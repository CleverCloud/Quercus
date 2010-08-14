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

import com.caucho.config.program.ConfigProgram;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * The JMS MessageListener endpoing factory
 */
public class ListenerEndpointFactory implements MessageEndpointFactory {
  private static final Logger log
    = Logger.getLogger(ListenerEndpointFactory.class.getName());
  private static final L10N L = new L10N(ListenerEndpointFactory.class);

  private ConfigProgram _program;
  
  private ListenerEndpoint _initialEndpoint;

  public void setListener(ConfigProgram program)
    throws Throwable
  {
    _program = program;

    _initialEndpoint = new ListenerEndpoint();
    program.configure(_initialEndpoint);
  }

  /**
   * Initialization.
   */
  public void init()
    throws ConfigException
  {
    if (_initialEndpoint == null) {
      throw new ConfigException(L.l("ListenerEndpointFactory needs a <listener>"));
    }
  }
  
  /**
   * Creates an endpoint with the associated XA resource.
   */
  public MessageEndpoint createEndpoint(XAResource xaResource)
    throws UnavailableException
  {
    try {
      ListenerEndpoint listener = _initialEndpoint;
      _initialEndpoint = null;

      if (listener == null) {
        listener = new ListenerEndpoint();
        _program.configure(listener);
      }

      return listener;
    } catch (Throwable e) {
      throw new UnavailableException(e);
    }
  }

  /**
   * Returns true to find out whether message deliveries to the
   * message endpoint will be transacted.  This is only a hint.
   */
  public boolean isDeliveryTransacted(Method method)
  {
    return false;
  }
}

