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

package com.caucho.jms.connection;

import javax.jms.*;

import com.caucho.jms.queue.*;

/**
 * A basic topic.
 */
public class TopicPublisherImpl extends MessageProducerImpl
  implements TopicPublisher
{
  public TopicPublisherImpl(JmsSession session, AbstractTopic topic)
  {
    super(session, topic);
  }

  /**
   * Returns the topic
   */
  public Topic getTopic()
    throws JMSException
  {
    if (_session == null)
      throw new javax.jms.IllegalStateException(L.l("getTopic(): TopicPublisher is closed."));
    
    return (Topic) super.getDestination();
  }

  /**
   * Publishes a message to the topic
   *
   * @param message the message to publish
   */
  public void publish(Message message)
    throws JMSException
  {
    send(message);
  }
  
  /**
   * Publishes a message to the topic
   *
   * @param message the message to publish
   * @param deliveryMode the delivery mode
   * @param priority the priority
   * @param timeToLive how long the message should live
   */
  public void publish(Message message,
                      int deliveryMode,
                      int priority,
                      long timeToLive)
    throws JMSException
  {
    send(message, deliveryMode, priority, timeToLive);
  }

  /**
   * Publishes a message to the topic
   *
   * @param topic the topic the message should be publish to
   * @param message the message to publish
   */
  public void publish(Topic topic, Message message)
    throws JMSException
  {
    send(topic, message);
  }
  
  /**
   * Publishes a message to the topic
   *
   * @param topic the topic the message should be publish to
   * @param message the message to publish
   * @param deliveryMode the delivery mode
   * @param priority the priority
   * @param timeToLive how long the message should live
   */
  public void publish(Topic topic,
                      Message message,
                      int deliveryMode,
                      int priority,
                      long timeToLive)
    throws JMSException
  {
    send(topic, message, deliveryMode, priority, timeToLive);
  }
}



