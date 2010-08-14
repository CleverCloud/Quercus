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

package com.caucho.jms.jdbc;

import com.caucho.config.ConfigException;
import com.caucho.jms.JmsExceptionWrapper;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * A jdbc topic.
 */
//public class JdbcTopic extends JdbcDestination implements Topic {
public class JdbcTopic {
  static final Logger log = Logger.getLogger(JdbcTopic.class.getName());
  static final L10N L = new L10N(JdbcTopic.class);

  private int _id;

  public JdbcTopic()
  {
  }

  /**
   * Returns the topic's name.
   */
  public String getTopicName()
  {
    return getName();
  }

  public String getName()
  {
    return "ook";
  }

  /**
   * Sets the topic's name.
   */
  public void setTopicName(String name)
  {
    //setName(name);
  }

  /**
   * Returns true for a topic.
   */
  public boolean isTopic()
  {
    return true;
  }

  /**
   * Returns the JDBC id for the topic.
   */
  public int getId()
  {
    return _id;
  }

  /**
   * Initializes the JdbcTopic
   */
  @PostConstruct
  public void init()
    throws ConfigException, SQLException
  {
    /*
    if (_jdbcManager.getDataSource() == null)
      throw new ConfigException(L.l("JdbcTopic requires a <data-source> element."));
    
    if (getName() == null)
      throw new ConfigException(L.l("JdbcTopic requires a <topic-name> element."));
    
    _jdbcManager.init();

    _id = createDestination(getName(), true);

    super.init();
    */
  }

  /**
   * Creates a consumer.
   */
    /*
  public MessageConsumerImpl createConsumer(SessionImpl session,
                                            String selector,
                                            boolean noLocal)
    throws JMSException
  {
    return new JdbcTopicConsumer(session, selector,
                                 _jdbcManager, this, noLocal);
  }
    */

  /**
   * Creates a durable subscriber.
   */
    /*
  public TopicSubscriber createDurableSubscriber(SessionImpl session,
                                                 String selector,
                                                 boolean noLocal,
                                                 String name)
    throws JMSException
  {
    return new JdbcTopicConsumer(session, selector,
                                 _jdbcManager, this, noLocal, name);
  }
    */

  /**
   * Sends the message to the queue.
   */
  public void send(Message message)
    throws JMSException
  {
    /*
    long expireTime = message.getJMSExpiration();
    if (expireTime <= 0)
      expireTime = Long.MAX_VALUE / 2;

    purgeExpiredMessages();
    
    try {
      _jdbcManager.getJdbcMessage().send(message, _id, expireTime);
    } catch (Exception e) {
      throw new JMSExceptionWrapper(e);
    }

    messageAvailable();
    */
  }

  /**
   * Returns a printable view of the queue.
   */
  public String toString()
  {
    //return "JdbcTopic[" + getName() + "]";
    
    return "JdbcTopic[]";
  }
}

