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

package com.caucho.management.server;

import com.caucho.jmx.Description;
import com.caucho.jmx.Units;

import java.util.Date;

/**
 * MBean API for a JMS Queue.
 *
 * <pre>
 * resin:type=Queue,name=jms/myqueue,...
 * </pre>
 */
@Description("Manages a JMS queue")
public interface JmsQueueMXBean extends ManagedObjectMXBean {
  /**
   * Returns a URL describing the topic configuration.
   */
  @Description("A descriptive URL for the topic")
  public String getUrl();

  //
  // statistics
  //

  /**
   * Returns the number of consumers for the queue.
   */
  @Description("The number of queue consumers")
  public int getConsumerCount();

  /**
   * Returns the number of consumers for the queue.
   */
  @Description("The number of queue Receivers")
  public int getReceiverCount();
  
  /**
   * Returns the number of listener failures
   */
  @Description("The number of listener failures")
  public long getListenerFailCountTotal();

  /**
   * Returns the last time of listener failures
   */
  @Description("The last time for a listener failures")
  public Date getListenerFailLastTime();

  /**
   * Returns the current size of the queue.
   */
  @Description("The queue size")
  public int getQueueSize();
}
