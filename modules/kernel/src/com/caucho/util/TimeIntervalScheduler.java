/**
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
 * @author Fred Zappert (fred@caucho.com)
 */

package com.caucho.util;

/**
 * Defines an interface for scheduling "something" on an entry that
 * a provider of that service would implement.
 *
 */
public interface TimeIntervalScheduler<E extends TimeIntervalEntry> {


  /**
   * Schedules an entryy using the time interval of the entry.
   * @param entry
   * @return
   */
  public void schedule(E entry);

  public void schedule(E entry, long time);

  /**
   * Schedules entry, based on the time interval provided.
   * @param entry
   * @param interval
   * @return
   */
  public void schedule(E entry, TimeInterval interval);

  public TimeInterval getSchedulerInterval(long time);

  public  TimeInterval getSchedulerInterval(long time, long intervalDuration);

  public enum ScheduleType
  {
    // FIRM,
    SOFT,
    WEAK;
  }

}
