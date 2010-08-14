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

package javax.jms;

import java.io.Serializable;

/**
 * The main destination.
 */
public interface Session extends Runnable {
  public final static int AUTO_ACKNOWLEDGE = 1;
  public final static int CLIENT_ACKNOWLEDGE = 2;
  public final static int DUPS_OK_ACKNOWLEDGE = 3;
  public final static int SESSION_TRANSACTED = 0;

  public BytesMessage createBytesMessage()
    throws JMSException;

  public MapMessage createMapMessage()
    throws JMSException;

  public Message createMessage()
    throws JMSException;

  public ObjectMessage createObjectMessage()
    throws JMSException;

  public ObjectMessage createObjectMessage(Serializable object)
    throws JMSException;

  public StreamMessage createStreamMessage()
    throws JMSException;

  public TextMessage createTextMessage()
    throws JMSException;

  public TextMessage createTextMessage(String text)
    throws JMSException;

  public boolean getTransacted()
    throws JMSException;

  public int getAcknowledgeMode()
    throws JMSException;

  public void commit()
    throws JMSException;

  public void rollback()
    throws JMSException;

  public void close()
    throws JMSException;

  public void recover()
    throws JMSException;

  public MessageListener getMessageListener()
    throws JMSException;

  public void setMessageListener(MessageListener listener)
    throws JMSException;

  public void run();

  public MessageProducer createProducer(Destination destination)
    throws JMSException;

  public MessageConsumer createConsumer(Destination destination)
    throws JMSException;

  public MessageConsumer createConsumer(Destination destination,
                                        String selector)
    throws JMSException;

  public MessageConsumer createConsumer(Destination destination,
                                        String selector,
                                        boolean noLocal)
    throws JMSException;

  public Queue createQueue(String queueName)
    throws JMSException;

  public Topic createTopic(String queueName)
    throws JMSException;

  public TopicSubscriber createDurableSubscriber(Topic topic,
                                                 String queue)
    throws JMSException;

  public TopicSubscriber createDurableSubscriber(Topic topic,
                                                 String queue,
                                                 String messageSelector,
                                                 boolean noLocal)
    throws JMSException;

  public QueueBrowser createBrowser(Queue queue)
    throws JMSException;

  public QueueBrowser createBrowser(Queue queue,
                                    String messageSelector)
    throws JMSException;

  public TemporaryQueue createTemporaryQueue()
    throws JMSException;

  public TemporaryTopic createTemporaryTopic()
    throws JMSException;

  public void unsubscribe(String name)
    throws JMSException;
}
