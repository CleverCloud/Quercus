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

package com.caucho.jms.resource;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.program.ContainerProgram;
import com.caucho.jms.queue.AbstractDestination;
import com.caucho.jms.JmsConnectionFactory;
import com.caucho.util.L10N;

import javax.annotation.*;
import javax.jms.*;
import java.util.logging.Logger;

/**
 * Configures application listeners, avoiding JCA.
 */
public class ListenerResource {
  private static L10N L = new L10N(ListenerResource.class);
  protected static Logger log
    = Logger.getLogger(ListenerResource.class.getName());

  private ConnectionFactory _connFactory;
  private Connection _conn;

  private Destination _destination;

  private int _listenerMax = 5;
  private ListenerConfig _listenerConfig;

  /**
   * Sets the JMS connection factory.
   *
   * @param factory
   */
  public void setConnectionFactory(ConnectionFactory factory)
  {
    _connFactory = factory;
  }

  /**
   * Sets the JMS Destination (Queue or Topic)
   *
   * @param destination
   */
  public void setDestination(Destination destination)
  {
    _destination = destination;
  }

  /**
   * Sets the listener constructor.
   */
  public void setListener(ListenerConfig config)
  {
    _listenerConfig = config;
  }

  /**
   * Sets the listener-max
   */
  public void setListenerMax(int listenerMax)
  {
    _listenerMax = listenerMax;
  }

  @PostConstruct
  public void init() throws ConfigException, JMSException
  {
    if (_destination == null)
      throw new ConfigException(L.l("'destination' is required for ListenerResource."));

    if (_listenerConfig == null)
      throw new ConfigException(L.l("'listener' is required for ListenerResource."));

    if (_connFactory == null && _destination instanceof AbstractDestination)
      _connFactory = new JmsConnectionFactory();

    if (_connFactory == null)
      throw new ConfigException(L.l("connection-factory is required for ListenerResource."));

    _conn = _connFactory.createConnection();

    if (_destination instanceof Topic)
      _listenerMax = 1;
  }

  public void start() throws Throwable
  {
    for (int i = 0; i < _listenerMax; i++) {
      MessageListener listener = _listenerConfig.newInstance();

      Session session = _conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

      MessageConsumer consumer = session.createConsumer(_destination);

      consumer.setMessageListener(listener);      
    }

    _conn.start();

    log.fine("ListenerResource[" + _destination + "] started");
  }

  @PreDestroy
  public void stop() throws JMSException
  {
    _conn.stop();

    log.fine("ListenerResource[" + _destination + "] stopped");
  }

  public static class ListenerConfig {
    private Class _type;
    private ContainerProgram _init;

    /**
     * Sets the listener's type.
     *
     * @param type implementation class of MessageListener
     *
     * @throws ConfigException
     */
    public void setType(Class type) throws ConfigException
    {
      Config.validate(type, MessageListener.class);

      _type = type;
    }

    /**
     * Sets the init program.
     */
    public void setInit(ContainerProgram init)
    {
      _init = init;
    }

    @PostConstruct
    public void init() throws ConfigException
    {
      if (_type == null)
        throw new ConfigException(L.l("'type' is required for listener."));
    }

    /**
     * Creates a new MessageListener
     *
     * @return the listener
     * @throws Throwable
     * @throws InstantiationException
     */
    public MessageListener newInstance() throws Throwable, InstantiationException
    {
      MessageListener listener = (MessageListener) _type.newInstance();

      if (_init != null)
        _init.configure(listener);

      return listener;
    }
  }
}

