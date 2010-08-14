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

/**
 * Management interface for the thread pool.
 *
 * <pre>
 * resin:type=ThreadPool
 * </pre>
 */
@Description("ThreadPool manages all threads used by the Resin server")
public interface ThreadPoolMXBean extends ManagedObjectMXBean {
  //
  // configuration
  //
  
  /**
   * Returns the maximum number of threads.
   */
  @Description("The configured maximum number of threads")
  public int getThreadMax();
  
  /**
   * Returns the maximum number of executor threads.
   */
  @Description("The configured maximum number of executor threads")
  public int getThreadExecutorMax();
  
  /**
   * Returns the priority thread gap
   */
  @Description("The priority thread gap")
  public int getThreadPriorityMin();
  
  /**
   * Returns the minimum number of idle threads.
   */
  @Description("The configured minimum number of idle threads")
  public int getThreadIdleMin();
  
  /**
   * Returns the maximum number of idle threads.
   */
  @Description("The configured maximum number of idle threads")
  public int getThreadIdleMax();

  //
  // Statistics
  //
  
  /**
   * Returns the current number of threads.
   */
  @Description("The current number of managed threads")
  public int getThreadCount();
  
  /**
   * Returns the current number of active threads.
   */
  @Description("The current number of active threads")
  public int getThreadActiveCount();
  
  /**
   * Returns the current number of starting threads.
   */
  @Description("The current number of starting threads")
  public int getThreadStartingCount();
  
  /**
   * Returns the current number of idle threads.
   */
  @Description("The current number of idle threads")
  public int getThreadIdleCount();
  
  /**
   * Returns the current number of waiting schedule threads.
   */
  @Description("The current number of wait threads")
  public int getThreadWaitCount();
  
  /**
   * Returns the total number of started threads.
   */
  @Description("The total number of created threads")
  public long getThreadCreateCountTotal();
  
  /**
   * Returns the total number of overflow threads.
   */
  @Description("The total number of overflow threads")
  public long getThreadOverflowCountTotal();
  
  /**
   * Returns the thread priority queue size
   */
  @Description("The priority queue size")
  public int getThreadPriorityQueueSize();
  
  /**
   * Returns the thread task queue size
   */
  @Description("The task queue size")
  public int getThreadTaskQueueSize();
}
