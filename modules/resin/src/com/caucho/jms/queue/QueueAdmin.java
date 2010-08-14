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

import com.caucho.management.server.*;
import java.util.Date;

/**
 * Administration for a JMS queue
 */
public class QueueAdmin extends AbstractManagedObject
  implements JmsQueueMXBean
{
  private final AbstractQueue _queue;

  public QueueAdmin(AbstractQueue queue)
  {
    _queue = queue;
  }

  //
  // configuration attributes
  //

  /**
   * Returns the queue's name
   */
  public String getName()
  {
    return _queue.getName();
  }

  /**
   * Returns the queue's url
   */
  public String getUrl()
  {
    return _queue.getUrl();
  }

  //
  // statistics attributes
  //

  /**
   * Returns the number of active message consumers
   */
  public int getConsumerCount()
  {
    return _queue.getConsumerCount();
  }
  
  /**
   * Returns the number of receivers.
   * 
   * @return
   */
  public int getReceiverCount() 
  {
    return _queue.getReceiverCount();    
  }  

  /**
   * Returns the number of active message consumers
   */
  public int getQueueSize()
  {
    return _queue.getQueueSize();
  }

  /**
   * Returns the number of listener failures.
   */
  public long getListenerFailCountTotal()
  {
    return _queue.getListenerFailCountTotal();
  }

  /**
   * Returns the time of the last listener failure
   */
  public Date getListenerFailLastTime()
  {
    long time = _queue.getListenerFailLastTime();

    if (time <= 0)
      return null;
    else
      return new Date(time);
  }

  void register()
  {
    registerSelf();
  }

  void unregister()
  {
    unregisterSelf();
  }
}
