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

package com.caucho.jms.file;

import java.util.ArrayList;
import java.util.HashMap;

import com.caucho.jms.queue.AbstractQueue;
import com.caucho.jms.queue.AbstractTopic;
import com.caucho.jms.queue.MessageException;
import com.caucho.vfs.Path;

/**
 * Implements a file topic.
 */
@SuppressWarnings("serial")
public class FileTopicImpl<E> extends AbstractTopic<E>
{
  private HashMap<String,AbstractQueue<E>> _durableSubscriptionMap
    = new HashMap<String,AbstractQueue<E>>();

  private ArrayList<AbstractQueue<E>> _subscriptionList
    = new ArrayList<AbstractQueue<E>>();

  private int _id;

  public FileTopicImpl()
  {
    FileQueueStore.create();
  }

  protected FileTopicImpl(Path path, String name, String serverId)
  {
    try {
      path.mkdirs();
    } catch (Exception e) {
    }

    if (serverId == null)
      serverId = "anon";

    new FileQueueStore(path, serverId);

    setName(name);

    init();
  }

  //
  // Configuration
  //

  /**
   * Sets the path to the backing database
   */
  public void setPath(Path path)
  {
  }

  //
  // JMX configuration attributes
  //

  /**
   * Returns the JMS configuration url.
   */
  public String getUrl()
  {
    return "file:name=" + getName();
  }

  public void init()
  {
  }

  @Override
  public AbstractQueue<E> createSubscriber(Object publisher,
                                        String name,
                                        boolean noLocal)
  {
    AbstractQueue<E> queue;

    if (name != null) {
      queue = _durableSubscriptionMap.get(name);

      if (queue == null) {
        queue = new FileSubscriberQueue<E>(this, publisher, noLocal);
        queue.setName(getName() + ":sub-" + name);

        _subscriptionList.add(queue);
        _durableSubscriptionMap.put(name, queue);
      }

      return queue;
    }
    else {
      queue = new FileSubscriberQueue<E>(this, publisher, noLocal);
      queue.setName(getName() + ":sub-" + _id++);

      _subscriptionList.add(queue);
    }

    return queue;
  }

  @Override
  public void closeSubscriber(AbstractQueue<E> queue)
  {
    if (! _durableSubscriptionMap.values().contains(queue))
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

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getTopicName() + "]";
  }
}

