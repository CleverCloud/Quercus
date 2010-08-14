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

import com.caucho.config.ConfigException;
import com.caucho.jms.queue.AbstractDestination;
import com.caucho.jms.JmsConnectionFactory;
import com.caucho.services.message.MessageSender;
import com.caucho.services.message.MessageServiceException;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import javax.annotation.*;
import javax.jms.*;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Configures message senders, avoiding JCA.
 */
public class MessageSenderResource implements MessageSender {
  private static final L10N L = new L10N(MessageSenderResource.class);
  private static final Logger log
    = Logger.getLogger(MessageSenderResource.class.getName());

  private ConnectionFactory _connFactory;
  private Connection _conn;
  private Destination _destination;

  public MessageSenderResource()
  {
  }

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
   * Initialize the sender resource.
   *
   * @throws JMSException
   * @throws ConfigException
   */
  @PostConstruct
  public void init() throws JMSException, ConfigException
  {
    if (_destination == null)
      throw new ConfigException(L.l("'destination' required for message sender."));

    if (_connFactory == null && _destination instanceof AbstractDestination)
      _connFactory = new JmsConnectionFactory();

    if (_connFactory == null)
      throw new ConfigException(L.l("'connection-factory' required for message sender"));

    _conn = _connFactory.createConnection();

    if (_conn == null)
      throw new NullPointerException();
  }

  /**
   * Sends a message to the destination
   *
   * @param header
   * @param value
   * @throws MessageServiceException
   */
  public void send(HashMap header, Object value)
    throws MessageServiceException
  {
    try {
      Session session = getSession();

      try {
        Message message;

        if (value == null) {
            message = session.createMessage();
        }
        else if (value instanceof String) {
          message = session.createTextMessage((String) value);
        }
        else if (value instanceof java.io.Serializable) {
          ObjectMessage objMessage = session.createObjectMessage();
          objMessage.setObject((java.io.Serializable) value);
          message = objMessage;
        }
        else {
           throw new MessageServiceException(L.l("value '{0}' must be serializable",
                                              value));
        }

        MessageProducer producer = session.createProducer(_destination);

        producer.send(message);

        producer.close();
      } finally {
        session.close();
      }
    } catch (MessageServiceException e) {
      throw e;
    } catch (Exception e) {
      throw new MessageServiceException(e);
    }
  }

  /**
   * Returns the JMS session.
   */
  private Session getSession() throws JMSException
  {
    return _conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
  }
}

