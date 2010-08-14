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

package javax.resource.spi.work;

/**
 * Allow submission of work instances.
 */
public interface WorkManager {
  public static final long IMMEDIATE = 0;
  public static final long INDEFINITE = Long.MAX_VALUE;
  public static final long UNKNOWN = -1;

  /**
   * Accepts a work instance for processing.  The call blocks until
   * the work instance completes.
   */
  public void doWork(Work work)
    throws WorkException;

  /**
   * Accepts a work instance for processing.  The call blocks until
   * the work instance completes.
   */
  public void doWork(Work work,
                     long startTimeout,
                     ExecutionContext context,
                     WorkListener workListener)
    throws WorkException;

  /**
   * Accepts a work instance for processing.  The call blocks until
   * the work instance starts, but does not wait not until the completion.
   */
  public long startWork(Work work)
    throws WorkException;

  /**
   * Accepts a work instance for processing.  The call blocks until
   * the work instance starts, but does not wait not until the completion.
   */
  public long startWork(Work work,
                        long startTimeout,
                        ExecutionContext context,
                        WorkListener listener)
    throws WorkException;

  /**
   * Schedules a work instance.
   */
  public void scheduleWork(Work work)
    throws WorkException;

  /**
   * Schedules a work instance.
   */
  public void scheduleWork(Work work,
                           long startTimeout,
                           ExecutionContext context,
                           WorkListener listener)
    throws WorkException;
}
