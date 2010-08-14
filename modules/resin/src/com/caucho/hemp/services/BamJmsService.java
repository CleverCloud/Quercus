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

package com.caucho.hemp.services;

import com.caucho.bam.SimpleActor;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import java.io.Serializable;

import java.util.*;
import java.util.logging.*;

import javax.jms.ConnectionFactory;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * jms service
 */
public class BamJmsService
  extends SimpleActor
{
  private static final L10N L = new L10N(BamJmsService.class);
  private static final Logger log
    = Logger.getLogger(BamJmsService.class.getName());

  private ConnectionFactory _factory;
  private Destination _queue;

  private Connection _conn;
  private Session _session;
  private MessageProducer _producer;

  public BamJmsService()
  {
  }

  /**
   * Sets the connection factory
   */
  public void setConnectionFactory(ConnectionFactory factory)
  {
    _factory = factory;
  }

  /**
   * Sets the destination
   */
  public void setDestination(Destination queue)
  {
    _queue = queue;
  }

  @PostConstruct
  public void init()
    throws JMSException
  {
    if (_factory == null)
      throw new ConfigException(L.l("{0} requires a JMS ConnectionFactory",
                                    getClass().getSimpleName()));

    if (_queue == null)
      throw new ConfigException(L.l("{0} requires a JMS destination",
                                    getClass().getSimpleName()));

    _conn = _factory.createConnection();
    _session = _conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
    _producer = _session.createProducer(_queue);
  }

  /**
   * Sends to a queue
   */
  @Override
  public void message(String to, String from, Serializable value)
  {
    try {
      ObjectMessage msg = _session.createObjectMessage(value);

      _producer.send(msg);
    } catch (JMSException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  @PreDestroy()
  public void close()
  {
    try {
      _session.close();
      _conn.close();
    } catch (JMSException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
}
