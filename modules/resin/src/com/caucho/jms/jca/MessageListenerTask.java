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

import javax.jms.*;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.work.Work;
import java.lang.IllegalStateException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The JMS MessageListener endpoint
 */
public class MessageListenerTask implements Work {
  private static final Logger log
    = Logger.getLogger(MessageListenerTask.class.getName());

  private MessageEndpoint _endpoint;
  private MessageListener _listener;

  private ResourceAdapterImpl _ra;
  
  private QueueConnection _queueConnection;
  private QueueSession _queueSession;
  private QueueReceiver _queueConsumer;
  
  private TopicConnection _topicConnection;
  private TopicSession _topicSession;
  private TopicSubscriber _topicConsumer;
  
  private Connection _connection;
  private Session _session;
  private MessageConsumer _consumer;

  private volatile boolean _isClosed;

  MessageListenerTask(ResourceAdapterImpl ra, MessageEndpoint endpoint)
    throws JMSException
  {
    _endpoint = endpoint;
    _listener = (MessageListener) endpoint;

    _ra = ra;

    init();
  }

  void init()
    throws JMSException
  {
    ConnectionFactory factory = _ra.getConnectionFactory();

    Destination queue = _ra.getDestination();

    if (queue instanceof Queue &&
        factory instanceof QueueConnectionFactory) {
      QueueConnectionFactory queueFactory;
      queueFactory = (QueueConnectionFactory) factory;
      
      _queueConnection = queueFactory.createQueueConnection();
      _queueSession = _queueConnection.createQueueSession(false, 1);
      _queueConsumer = _queueSession.createReceiver((Queue) queue);
      _queueConnection.start();
    }
    else if (queue instanceof Topic &&
             factory instanceof TopicConnectionFactory) {
      TopicConnectionFactory topicFactory;
      topicFactory = (TopicConnectionFactory) factory;
      
      _topicConnection = topicFactory.createTopicConnection();
      _topicSession = _topicConnection.createTopicSession(false, 1);
      _topicConsumer = _topicSession.createSubscriber((Topic) queue);
      _topicConnection.start();
    }
    else {
      _connection = factory.createConnection();
      _session = _connection.createSession(false, 1);
      _consumer = _session.createConsumer(queue);
      _connection.start();
    }
  }

  /**
   * Runs the endpoint.
   */
  public void run()
  {
    while (! _isClosed) {
      try {
        Message msg;

        if (_consumer != null)
          msg = _consumer.receive(60000);
        else if (_queueConsumer != null)
          msg = _queueConsumer.receive(60000);
        else if (_topicConsumer != null)
          msg = _topicConsumer.receive(60000);
        else {
          _isClosed = true;
          throw new IllegalStateException();
        }

        if (msg != null)
          _listener.onMessage(msg);
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  /**
   * Called when the resource adapter doesn't need a proxy endpoint.
   */
  public void release()
  {
    _isClosed = true;
    
    Connection connection = _connection;
    try {
      if (connection != null)
        connection.stop();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
    _connection = null;
    
    Session session = _session;
    _session = null;
    
    MessageConsumer consumer = _consumer;
    _consumer = null;
    
    MessageEndpoint endpoint = _endpoint;
    _endpoint = null;
    
    try {
      if (consumer != null)
        consumer.close();
      if (session != null)
        session.close();
      if (connection != null)
        connection.close();
    } catch (Throwable e) {
    }

    _listener = null;

    if (endpoint != null)
      endpoint.release();
  }
}

