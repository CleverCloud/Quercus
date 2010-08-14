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

package com.caucho.jms.connection;

import com.caucho.jms.memory.*;
import com.caucho.util.L10N;

import javax.jms.*;
import java.util.logging.Logger;

/**
 * A sample connection factory.
 */
public class XAConnectionFactoryImpl extends ConnectionFactoryImpl
{
  private static final Logger log
    = Logger.getLogger(XAConnectionFactoryImpl.class.getName());
  private static final L10N L = new L10N(XAConnectionFactoryImpl.class);

  public XAConnectionFactoryImpl()
  {
  }

  /**
   * Creates a new queue connection
   */
  @Override
  public Connection createConnection()
    throws JMSException
  {
    return createXAConnection();
  }

  /**
   * Creates a new connection
   *
   * @param username the username to authenticate with the server.
   * @param password the password to authenticate with the server.
   *
   * @return the created connection
   */
  public Connection createConnection(String username, String password)
    throws JMSException
  {
    return createXAConnection(username, password);
  }

  /**
   * Creates queue.
   */
  public Queue createQueue(String name)
    throws JMSException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates topics.
   */
  public Topic createTopic(String name)
    throws JMSException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a new queue connection
   */
  public QueueConnection createQueueConnection()
    throws JMSException
  {
    return createXAQueueConnection();
  }

  /**
   * Creates a new queue connection
   *
   * @param username the username to authenticate with the server.
   * @param password the password to authenticate with the server.
   *
   * @return the created connection
   */
  public QueueConnection createQueueConnection(String username,
                                               String password)
    throws JMSException
  {
    return createXAQueueConnection(username, password);
  }

  /**
   * Creates a new queue connection
   */
  public TopicConnection createTopicConnection()
    throws JMSException
  {
    return createXATopicConnection();
  }

  /**
   * Creates a new queue connection
   *
   * @param username the username to authenticate with the server.
   * @param password the password to authenticate with the server.
   *
   * @return the created connection
   */
  public TopicConnection createTopicConnection(String username,
                                               String password)
    throws JMSException
  {
    return createXATopicConnection(username, password);
  }
}
