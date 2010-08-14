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

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import java.io.Serializable;

import javax.annotation.*;
import javax.jms.*;

import com.caucho.jms.message.*;
import com.caucho.jms.connection.*;
import com.caucho.util.Alarm;

import com.caucho.util.L10N;

/**
 * Implements an abstract topic.
 */
abstract public class AbstractTopic<E> extends AbstractDestination<E>
  implements javax.jms.Topic
{
  private static final L10N L = new L10N(AbstractTopic.class);

  private TopicAdmin _admin;

  public void setTopicName(String name)
  {
    setName(name);
  }

  protected void init()
  {
  }

  @PostConstruct
  public void postConstruct()
  {
    init();

    _admin = new TopicAdmin(this);
    _admin.register();
  }

  @Override
  public void send(String msgId, E msg, int priority, long expires)
    throws MessageException
  {
    send(msgId, msg, priority, expires, null);
  }

  /**
   * Polls the next message from the store.  If no message is available,
   * wait for the timeout.
   */
  @Override
  public E receive(long timeout)
  {
    throw new java.lang.IllegalStateException(L.l("topic cannot be used directly for receive."));
  }

  public abstract AbstractQueue<E> createSubscriber(Object publisher,
                                                 String name,
                                                 boolean noLocal);

  public abstract void closeSubscriber(AbstractQueue<E> subscriber);

  //
  // BlockingQueue api
  //

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
  public boolean offer(E value, long timeout, TimeUnit unit)
  {
    int priority = 0;

    timeout = unit.toMillis(timeout);

    long expires = Alarm.getCurrentTime() + timeout;

    send(generateMessageID(), value, priority, expires);

    return true;
  }

  public boolean offer(E value)
  {
    return offer(value, 0, TimeUnit.SECONDS);
  }

  public void put(E value)
  {
    offer(value, Integer.MAX_VALUE, TimeUnit.SECONDS);
  }

  public E poll(long timeout, TimeUnit unit)
  {
    long msTimeout = unit.toMillis(timeout);

    E payload = receive(msTimeout);

    try {
      if (payload == null)
        return null;
      else if (payload instanceof ObjectMessage)
        return (E) ((ObjectMessage) payload).getObject();
      else if (payload instanceof TextMessage)
        return (E) ((TextMessage) payload).getText();
      else if (payload instanceof Serializable)
        return payload;
      else
        throw new MessageException(L.l("'{0}' is an unsupported message for the BlockingQueue API.",
                                       payload));
    } catch (JMSException e) {
      throw new MessageException(e);
    }
  }

  public int remainingCapacity()
  {
    return Integer.MAX_VALUE;
  }

  public E peek()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public E poll()
  {
    return poll(0, TimeUnit.MILLISECONDS);
  }

  public E take()
  {
    return poll(Integer.MAX_VALUE, TimeUnit.SECONDS);
  }

  public int drainTo(Collection c)
  {
    throw new UnsupportedOperationException();
  }

  public int drainTo(Collection c, int max)
  {
    throw new UnsupportedOperationException();
  }

}

