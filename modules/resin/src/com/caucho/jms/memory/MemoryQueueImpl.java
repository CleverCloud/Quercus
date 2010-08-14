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

package com.caucho.jms.memory;

import com.caucho.jms.queue.AbstractMemoryQueue;

/**
 * Implements a memory queue.
 */
public class MemoryQueueImpl<E> extends AbstractMemoryQueue<E,MemoryQueueEntry<E>>
{
  /**
   * Returns the configuration URL.
   */
  @Override
  public String getUrl()
  {
    return "memory:name=" + getName();
  }

  /**
   * Adds the message to the persistent store.  Called if there are no
   * active listeners.
   */
  @Override
  public MemoryQueueEntry<E> writeEntry(String msgId,
                                        E payload,
                                        int priority,
                                        long expireTime)
  {
    int leaseTimeout = -1;
    
    MemoryQueueEntry<E> entry
      = new MemoryQueueEntry<E>(msgId,
                                leaseTimeout, priority, expireTime,
                                payload);

    return entry;
  }

  @Override
  protected void acknowledge(MemoryQueueEntry<E> entry)
  {
  }
}

