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

package com.caucho.jms.queue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import com.caucho.util.Alarm;
import com.caucho.util.L10N;

/**
 * Implements an abstract queue.
 */
abstract public class AbstractQueue<E> extends AbstractDestination<E>
  implements javax.jms.Queue, MessageQueue<E>, BlockingQueue<E>
{
  private static final L10N L = new L10N(AbstractQueue.class);
  private static final Logger log
    = Logger.getLogger(AbstractQueue.class.getName());

  private QueueAdmin _admin;

  // stats
  private long _listenerFailCount;
  private long _listenerFailLastTime;

  protected AbstractQueue()
  {
  }

  public void setQueueName(String name)
  {
    setName(name);
  }

  protected void init()
  {
  }

  @PostConstruct
  public void postConstruct()
  {
    try {
      init();
    } catch (Exception e) {
      // XXX: issue with examples: iterating with closed table
      log.log(Level.WARNING, e.toString(), e);
    }

    _admin = new QueueAdmin(this);
    _admin.register();
  }

  /**
   * Sends a message to the queue
   */
  @Override
  public void send(String msgId,
                   E msg,
                   int priority,
                   long expireTime)
    throws MessageException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void send(String msgId,
                   E msg,
                   int priority,
                   long expireTime,
                   Object publisher)
    throws MessageException
  {
    send(msgId, msg, priority, expireTime);
  }

  /**
   * Primary message receiving, registers a callback for any new
   * message.
   */
  @Override
  public QueueEntry<E> receiveEntry(long expireTime, boolean isAutoAck)
  {
    return null;//receiveEntry(timeout, isAutoAck, null);
  }
  
  public QueueEntry<E> receiveEntry(long expireTime, boolean isAutoAck, 
                                    QueueEntrySelector selector)
    throws MessageException
  {
    return receiveEntry(expireTime, isAutoAck);
  }
  
  /**
   * Adds the callback to the listening list.
   */
  @Override
  public EntryCallback<E> addMessageCallback(MessageCallback<E> callback,
                                             boolean isAutoAck)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Removes the callback from the listening list.
   */
  @Override
  public void removeMessageCallback(EntryCallback<E> entryCallback)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Acknowledge receipt of the message.
   *
   * @param msgId message to acknowledge
   */
  @Override
  public void acknowledge(String msgId)
  {
  }

  /**
   * Rollback the message read.
   */
  @Override
  public void rollback(String msgId)
  {
  }

  //
  // convenience methods
  //

  /**
   * Receives a message, blocking until expireTime if no message is
   * available.
   */
  public E receive()
    throws MessageException
  {
    long expireTime;
    
    if (Alarm.isTest())
      expireTime = Alarm.getCurrentTimeActual() + 120000L;
    else
      expireTime = Long.MAX_VALUE / 2;
    
    return receive(expireTime, true);
  }

  /**
   * Receives a message, blocking until expireTime if no message is
   * available.
   */
  public E receive(long expireTime)
    throws MessageException
  {
    return receive(expireTime, true);
  }

  /**
   * Receives a message, blocking until expireTime if no message is
   * available.
   */
  public E receive(long expireTime,
                   boolean isAutoAcknowledge)
    throws MessageException
  {
    QueueEntry<E> entry = receiveEntry(expireTime, isAutoAcknowledge);

    if (entry != null)
      return entry.getPayload();
    else
      return null;
  }

  public ArrayList<? extends QueueEntry<E>> getBrowserList()
  {
    return new ArrayList<QueueEntry<E>>();
  }

  //
  // BlockingQueue api
  //

  @Override
  public int size()
  {
    return 0;
  }

  public Iterator<E> iterator()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Adds the item to the queue, waiting if necessary
   */
  public boolean offer(E message, long timeout, TimeUnit unit)
  {
    int priority = 0;

    timeout = unit.toMillis(timeout);

    long expires = Alarm.getCurrentTimeActual() + timeout;

    send(generateMessageID(), message, priority, expires);

    return true;
  }

  @Override
  public boolean offer(E message)
  {
    return offer(message, 0, TimeUnit.SECONDS);
  }

  @Override
  public void put(E value)
  {
    offer(value, Integer.MAX_VALUE, TimeUnit.SECONDS);
  }

  @Override
  public E poll(long timeout, TimeUnit unit)
  {
    long msTimeout = unit.toMillis(timeout);
    
    long expireTime = msTimeout + Alarm.getCurrentTimeActual();
    
    E payload = receive(expireTime);

    try {
      if (payload == null)
        return null;
      else if (payload instanceof ObjectMessage)
        return (E) ((ObjectMessage) payload).getObject();
      else if (payload instanceof TextMessage)
        return (E) ((TextMessage) payload).getText();
      else
        return payload;
      /*
      else
        throw new MessageException(L.l("'{0}' is an unsupported message for the BlockingQueue API.",
                                       payload));
                                       */
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new MessageException(e);
    }
  }

  @Override
  public int remainingCapacity()
  {
    return Integer.MAX_VALUE;
  }

  @Override
  public E peek()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public E poll()
  {
    return poll(0, TimeUnit.MILLISECONDS);
  }

  @Override
  public E take()
  {
    return poll(Integer.MAX_VALUE, TimeUnit.SECONDS);
  }

  @Override
  public int drainTo(Collection<? super E> c)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public int drainTo(Collection<? super E> c, int max)
  {
    throw new UnsupportedOperationException();
  }

  //
  // JMX statistics
  //

  /**
   * Returns the number of active message consumers
   */
  public int getConsumerCount()
  {
    return 0;
  }

  /**
   * Returns the number of receivers.
   *
   * @return
   */
  public int getReceiverCount()
  {
    return 0;
  }

  /**
   * Returns the queue size
   */
  public int getQueueSize()
  {
    return -1;
  }

  /**
   * Returns the number of listener failures.
   */
  public long getListenerFailCountTotal()
  {
    return _listenerFailCount;
  }

  /**
   * Returns the number of listener failures.
   */
  public long getListenerFailLastTime()
  {
    return _listenerFailLastTime;
  }

  /**
   * Called when a listener throws an excepton
   */
  public void addListenerException(Exception e)
  {
    synchronized (this) {
      _listenerFailCount++;
      _listenerFailLastTime = Alarm.getCurrentTimeActual();
    }
  }

  protected void startPoll()
  {
  }

  protected void stopPoll()
  {
  }

  @PreDestroy
  @Override
  public void close()
  {
    stopPoll();

    super.close();
  }
}

