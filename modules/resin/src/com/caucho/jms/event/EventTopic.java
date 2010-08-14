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

package com.caucho.jms.event;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.*;

import com.caucho.jms.memory.*;
import com.caucho.jms.queue.*;

/**
 * Implements a event topic.
 */
public class EventTopic<E> extends AbstractTopic<E>
{
  private static final Logger log
    = Logger.getLogger(EventTopic.class.getName());

  private ArrayList<AbstractQueue> _subscriptionList
    = new ArrayList<AbstractQueue>();

  private int _id;

  //
  // JMX configuration
  //

  /**
   * Returns the configuration URL.
   */
  public String getUrl()
  {
    return "event:name=" + getName();
  }

  @Override
  public AbstractQueue<E> createSubscriber(Object publisher,
                                        String name,
                                        boolean noLocal)
  {
    AbstractQueue<E> queue;

    if (name != null) {
      queue = new MemorySubscriberQueue<E>(publisher, noLocal);
      queue.setName(getName() + ":sub-" + name);

      _subscriptionList.add(queue);
    }
    else {
      queue = new MemorySubscriberQueue<E>(publisher, noLocal);
      queue.setName(getName() + ":sub-" + _id++);

      _subscriptionList.add(queue);
    }

    if (log.isLoggable(Level.FINE))
      log.fine(this + " create-subscriber(" + queue + ")");

    return queue;
  }

  @Override
  public void closeSubscriber(AbstractQueue queue)
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " close-subscriber(" + queue + ")");

    _subscriptionList.remove(queue);
  }

  @Override
  public void send(String msgId,
                   E payload,
                   int priority,
                   long timeout,
                   Object publisher)
    throws MessageException
  {
    for (int i = 0; i < _subscriptionList.size(); i++) {
      _subscriptionList.get(i).send(msgId, payload, priority, timeout,
                                    publisher);
    }
  }
}

