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
 * A sample queue session.  Lets the client create queues, browsers, etc.
 */
public class QueueSessionImpl extends JmsSession
  implements XAQueueSession, QueueSession
{
  /**
   * Creates the session
   */
  public QueueSessionImpl(ConnectionImpl connection,
                          boolean isTransacted, int ackMode,
                          boolean isXA)
    throws JMSException
  {
    super(connection, isTransacted, ackMode, isXA);
  }

  /**
   * Creates a receiver to receive messages.
   *
   * @param queue the queue to receive messages from.
   */
  public QueueReceiver createReceiver(Queue queue)
    throws JMSException
  {
    checkOpen();
    
    return createReceiver(queue, null);
  }

  /**
   * Creates a receiver to receive messages.
   *
   * @param queue the queue to receive messages from.
   * @param messageSelector query to restrict the messages.
   */
  public QueueReceiver createReceiver(Queue queue, String messageSelector)
    throws JMSException
  {
    return (QueueReceiver)createConsumer(queue, messageSelector);
  }

  /**
   * Creates a QueueSender to send messages to a queue.
   *
   * @param queue the queue to send messages to.
   */
  public QueueSender createSender(Queue queue)
    throws JMSException
  {
    checkOpen();

    if (queue == null) {
      return new QueueSenderImpl(this, null);
    }
    
    if (! (queue instanceof AbstractQueue))
      throw new InvalidDestinationException(L.l("'{0}' is an unknown destination.  The destination must be a Resin JMS destination for Session.createProducer.",
                                                queue));

    AbstractQueue dest = (AbstractQueue) queue;

    /*if (dest instanceof TemporaryQueueImpl) {
      TemporaryQueueImpl temp = (TemporaryQueueImpl) dest;

      if (temp.getSession() != this) {
        throw new javax.jms.IllegalStateException(L.l("temporary queue '{0}' does not belong to this session '{1}'",
                                                      queue, this));
      }
    }*/

    return new QueueSenderImpl(this, dest);
  }

  /**
   * Creates a new topic.
   */
  @Override
  public Topic createTopic(String topicName)
    throws JMSException
  {
    throw new javax.jms.IllegalStateException(L.l("QueueSession: createTopic() is invalid."));
  }

  /**
   * Creates a temporary topic.
   */
  @Override
  public TemporaryTopic createTemporaryTopic()
    throws JMSException
  {
    throw new javax.jms.IllegalStateException(L.l("QueueSession: createTemporaryTopic() is invalid."));
  }

  /**
   * Creates a durable subscriber to receive messages.
   *
   * @param topic the topic to receive messages from.
   */
  @Override
  public TopicSubscriber createDurableSubscriber(Topic topic, String name)
    throws JMSException
  {
    throw new javax.jms.IllegalStateException(L.l("QueueSession: createDurableSubscriber() is invalid."));
  }

  /**
   * Creates a subscriber to receive messages.
   *
   * @param topic the topic to receive messages from.
   * @param messageSelector topic to restrict the messages.
   * @param noLocal if true, don't receive messages we've sent
   */
  @Override
  public TopicSubscriber createDurableSubscriber(Topic topic,
                                                 String name,
                                                 String messageSelector,
                                                 boolean noLocal)
    throws JMSException
  {
    throw new javax.jms.IllegalStateException(L.l("QueueSession: createDurableSubscriber() is invalid."));
  }

  /**
   * Unsubscribe from a durable subscription.
   */
  @Override
  public void unsubscribe(String name)
    throws JMSException
  {
    throw new javax.jms.IllegalStateException(L.l("QueueSession: unsubscribe() is invalid."));
  }

  public QueueSession getQueueSession()
  {
    return this;
  }
}
