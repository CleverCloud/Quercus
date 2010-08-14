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

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;

import com.caucho.jms.queue.AbstractDestination;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;

/**
 * A basic message producer.
 */
public class MessageProducerImpl implements MessageProducer {
  static final L10N L = new L10N(MessageProducer.class);
  
  public static final long DEFAULT_TIME_TO_LIVE = 30 * 24 * 3600 * 1000L;

  private int _deliveryMode = DeliveryMode.PERSISTENT;
  private boolean _disableMessageId = true;
  private boolean _disableMessageTimestamp = true;
  private int _priority = 4;
  private long _timeToLive = DEFAULT_TIME_TO_LIVE;

  protected JmsSession _session;
  protected AbstractDestination _queue;

  public MessageProducerImpl(JmsSession session, AbstractDestination queue)
  {
    _session = session;
    _queue = queue;
  }

  /**
   * Returns the producer's destination.
   */
  public Destination getDestination()
    throws JMSException
  {
    if (_session == null || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("getDestination(): message producer is closed."));
    
    return _queue;
  }

  /**
   * Returns the default delivery mode.
   */
  public int getDeliveryMode()
    throws JMSException
  {
    if (_session == null || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("getDeliveryMode(): message producer is closed."));
    
    return _deliveryMode;
  }

  /**
   * Sets the default delivery mode.
   */
  public void setDeliveryMode(int deliveryMode)
    throws JMSException
  {
    if (_session == null || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("setDeliveryMode(): message producer is closed."));
    
    _deliveryMode = deliveryMode;
  }

  /**
   * Returns true if message ids are disabled by default.
   */
  public boolean getDisableMessageID()
    throws JMSException
  {
    if (_session == null || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("getDisableMessageID(): message producer is closed."));

    return _disableMessageId;
  }

  /**
   * Sets true if message ids should be disabled by default.
   */
  public void setDisableMessageID(boolean disable)
    throws JMSException
  {
    if (_session == null || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("setDisableMessageID(): message producer is closed."));
    
    _disableMessageId = disable;
  }

  /**
   * Returns true if message timestamps are disabled by default.
   */
  public boolean getDisableMessageTimestamp()
    throws JMSException
  {
    if (_session == null || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("getDisableMessageTimestam(): message producer is closed."));

    return _disableMessageTimestamp;
  }

  /**
   * Sets true if message timestamps should be disabled by default.
   */
  public void setDisableMessageTimestamp(boolean disable)
    throws JMSException
  {
    if (_session == null || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("setDeliveryMode(): message producer is closed."));

    _disableMessageTimestamp = disable;
  }

  /**
   * Returns the default priority
   */
  public int getPriority()
    throws JMSException
  {
    if (_session == null || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("getPriority(): message producer is closed."));
    
    return _priority;
  }

  /**
   * Sets the default priority.
   */
  public void setPriority(int priority)
    throws JMSException
  {
    if (_session == null || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("setDeliveryMode(): message producer is closed."));

    _priority = priority;
  }

  /**
   * Returns the default time to live
   */
  public long getTimeToLive()
    throws JMSException
  {
    if (_session == null || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("getTimeToLive(): message producer is closed."));
    
    return _timeToLive;
  }

  /**
   * Sets the default time to live.
   */
  public void setTimeToLive(long timeToLive)
    throws JMSException
  {
    if (_session == null || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("setTimeToLive(): message producer is closed."));

    _timeToLive = timeToLive;
  }

  /**
   * Sends a message to the destination
   *
   * @param message the message to send
   */
  public void send(Message message)
    throws JMSException
  {
    if (message == null)
      throw new NullPointerException(L.l("jms message cannot be null for send()")); 

    send(_queue, message, _deliveryMode, _priority, _timeToLive);
  }
  
  /**
   * Sends a message to the destination
   *
   * @param message the message to send
   * @param deliveryMode the delivery mode
   * @param priority the priority
   * @param timeToLive how long the message should live
   */
  public void send(Message message,
                   int deliveryMode,
                   int priority,
                   long timeToLive)
    throws JMSException
  {
    if (message == null)
      throw new NullPointerException(L.l("jms message cannot be null for send()")); 

    send(_queue, message, deliveryMode, priority, timeToLive);
  }

  /**
   * Sends a message to the destination
   *
   * @param destination the destination the message should be send to
   * @param message the message to send
   */
  public void send(Destination destination, Message message)
    throws JMSException
  {
    send(destination, message, _deliveryMode, _priority, _timeToLive);
  }
  
  /**
   * Sends a message to the destination
   *
   * @param destination the destination the message should be send to
   * @param message the message to send
   * @param deliveryMode the delivery mode
   * @param priority the priority
   * @param timeToLive how long the message should live
   */
  public void send(Destination destination,
                   Message message,
                   int deliveryMode,
                   int priority,
                   long timeToLive)
    throws JMSException
  {
    if (destination == null)
      destination = _queue;
    else if (_queue != null && destination != _queue)
      throw new UnsupportedOperationException(L.l("MessageProducer: '{0}' does not match the queue '{1}'",
                                                  destination, _queue));

    if (destination == null)
      throw new UnsupportedOperationException(L.l("MessageProducer: null destination is not supported."));

    if (_session == null || _session.isClosed())
      throw new javax.jms.IllegalStateException(L.l("getDeliveryMode(): message producer is closed."));
    
    if (destination instanceof TemporaryTopicImpl) {
      
      // Message can not be sent on Temporary Queue if Session is not active.      
      if (((TemporaryTopicImpl)destination).isClosed()) {
        throw new javax.jms.IllegalStateException(L.l("temporary queue '{0}' session is not active",
            destination));
      }     
    } 

    _session.send((AbstractDestination) destination,
                  message,
                  deliveryMode, priority,
                  timeToLive);
    // _session.checkThread();
  }

  /**
   * Calculates the expires time.
   */
  protected long calculateExpiration(long timeToLive)
  {
    if (timeToLive <= 0)
      return timeToLive;
    else
      return timeToLive + Alarm.getCurrentTime();
  }

  /**
   * Closes the producer.
   */
  public void close()
    throws JMSException
  {
    _session = null;
  }

  public String toString()
  {
    return getClass().getName() + "[" + _queue + "]";
  }
}

