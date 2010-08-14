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

package com.caucho.jms;

import com.caucho.jms.memory.*;
import com.caucho.jms.message.*;
import com.caucho.jms.connection.*;
import com.caucho.jms.connection.ConnectionFactoryImpl;
import com.caucho.util.L10N;

import javax.jms.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * JMS facade
 */
public class Jms
{
  private static final Logger log
    = Logger.getLogger(Jms.class.getName());
  private static final L10N L = new L10N(Jms.class);

  private ConnectionFactoryImpl _connectionFactory;
  private Connection _conn;

  private MessageFactory _messageFactory = new MessageFactory();

  public Jms()
  {
    try {
      _connectionFactory = new ConnectionFactoryImpl();
      _conn = _connectionFactory.createConnection();
      _conn.start();
    } catch (JMSException e) {
      throw new JmsRuntimeException(e);
    }
  }

  public void send(Destination dest, Message msg)
  {
    Session session = null;
    
    try {
      session = getSession();

      MessageProducer producer = session.createProducer(null);

      producer.send(dest, msg);
    } catch (JMSException e) {
    } finally {
      freeSession(session);
    }
  }

  public void send(Destination dest,
                   Message msg,
                   int deliveryMode,
                   int priority,
                   long ttl)
  {
    Session session = null;
    
    try {
      session = getSession();

      MessageProducer producer = session.createProducer(null);

      producer.send(dest, msg, deliveryMode, priority, ttl);
    } catch (JMSException e) {
    } finally {
      freeSession(session);
    }
  }

  public Message receive(Destination dest)
  {
    Session session = null;
    MessageConsumer consumer = null;
    
    try {
      session = getSession();

      consumer = session.createConsumer(dest);

      return consumer.receive();
    } catch (JMSException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        if (consumer != null)
          consumer.close();
      } catch (JMSException e) {
        log.log(Level.FINE, e.toString(), e);
      }
      
      freeSession(session);
    }
  }

  /**
   * Creates an auto-ack session.
   */
  public Session createSession()
    throws JmsRuntimeException
  {
    try {
      return _conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
    } catch (JMSException e) {
      throw new JmsRuntimeException(e);
    }
  }

  public Connection createConnection()
  {
    try {
      return _connectionFactory.createConnection();
    } catch (JMSException e) {
      throw new JmsRuntimeException(e);
    }
  }
  
  /**
   * Creates a session and listener.
   */
  public Session createListener(Connection conn,
                                Destination queue,
                                MessageListener listener)
    throws JmsRuntimeException
  {
    try {
      Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

      MessageConsumer consumer = session.createConsumer(queue);

      consumer.setMessageListener(listener);

      return session;
    } catch (JMSException e) {
      throw new JmsRuntimeException(e);
    }
  }

  private Session getSession()
    throws JMSException
  {
    Session session = _conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

    return session;
  }

  private void freeSession(Session session)
  {
    try {
      if (session != null)
        session.close();
    } catch (JMSException e) {
      throw new JmsRuntimeException(e);
    }
  }

  public TextMessage createTextMessage(String msg)
  {
    try {
      return _messageFactory.createTextMessage(msg);
    } catch (JMSException e) {
      throw new JmsRuntimeException(e);
    }
  }  
  
}
