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

import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.JMSException;
import javax.jms.TemporaryQueue;

import com.caucho.jms.memory.MemoryQueue;
import com.caucho.util.L10N;

/**
 * A temporary queue
 */
public class TemporaryQueueImpl extends MemoryQueue implements TemporaryQueue
{
  private static final L10N L = new L10N(TemporaryQueueImpl.class);
  
  private static int _idCount;

  private JmsSession _session;
  private boolean _isClosed;
  
  private AtomicInteger _messageConsumerCount;
  
  TemporaryQueueImpl(JmsSession session)
  {
    _session = session;
    _messageConsumerCount = new AtomicInteger();
    setName("TemporaryQueue-" + _idCount++);
  }

  JmsSession getSession()
  {
    return _session;
  }
  
  public void addMessageConsumer() 
  {
    _messageConsumerCount.incrementAndGet();
  }
  
  public void removeMessageConsumer() 
  {
    _messageConsumerCount.decrementAndGet();
  }
  
  public void delete()
    throws JMSException
  {
    if (_messageConsumerCount.get() > 0)
      throw new javax.jms.IllegalStateException(L.l("temporary queue is still active"));
  }
}

