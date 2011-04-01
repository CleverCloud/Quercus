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

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.ref.SoftReference;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * Provides an implementation of the {@linkTimeIntervalMap}
 * for managing schedules based on soft or weak references to the
 * entries in the schedule.
 *
 * 
 */
public class AbstractSoftScheduler<E extends TimeIntervalEntry>
 implements TimeIntervalScheduler<E>
{


  private  long _clockTic;

  private final Map<TimeInterval, ConcurrentLinkedQueue<Reference<E>>> _map
    = new ConcurrentHashMap<TimeInterval, ConcurrentLinkedQueue<Reference<E>>>();

  private final Random _speader =  new Random(37);

  private ScheduleType _scheduleType = ScheduleType.WEAK;



  /**
   * Creates a mamp of TimeIntervals of the requested size.
   * @param clockTic
   */
  public AbstractSoftScheduler(long clockTic)
  {
    _clockTic = clockTic;
  }

  /**
   * Creates a mamp of TimeIntervals of the requested size.
   * @param clockTic
   */
  public AbstractSoftScheduler(long clockTic, ScheduleType scheduleType)
  {
    _clockTic = clockTic;
    _scheduleType = scheduleType;
  }

  private AbstractSoftScheduler()
  {

  }

  /**
   * Schedules the entry to the current interval of the map.
   */
 public final void schedule(E entry)
  {
     schedule(entry, Alarm.getCurrentTime());
  }

  /**
   * Schedules an entry to the map's interval for time time
   * @param entry
   * @param time
   */
  public final void schedule(E entry, long time)
  {
    TimeInterval schedule = getSchedulerInterval(time);
    ConcurrentLinkedQueue<Reference<E>> entries = _map.get(schedule);
    if (entries == null) {
      entries = new ConcurrentLinkedQueue<Reference<E>>();
    }
    entries.add(createReference(entry));
  }


  /**
   * The event will be scheduled to ocuur anytime within the designated
   * time interval.
   *
   * @param entry to add to the schedule
   * @param timeInterval during which the entry may be scheduled
   */
  public final void schedule(E entry, TimeInterval timeInterval)
  {
    long scheduleFor = timeInterval.startTime()
        + _speader.nextInt((int) timeInterval.duration());
     schedule(entry, scheduleFor);
  }

  /**
   * Returns the schedule interval for the designated time.
   * @param time within the interval
   * @return an interval that contains the moment.
   */
  public final TimeInterval getSchedulerInterval(long time)
  {
    long now = Alarm.getCurrentTime();
    long rNow = now % _clockTic;
    long starts = now - rNow;
    long stops = now + _clockTic;
    return new TimeIntervalElement(starts, stops);
  }

  /**
   * Returns the TimeInterval containing the moment, based on a schedule
   * of 
   * @param time
   * @param interavalDuration
   * @return
   */
  public final TimeInterval getSchedulerInterval(long time, long interavalDuration)
  {
    long now = Alarm.getCurrentTime();
    long rNow = now % interavalDuration;
    long starts = now - rNow;
    long stops = now + interavalDuration;
    return new TimeIntervalElement(starts, stops);
  }

  /**
   * Returns the number of distinct time intervals held by the schedule.
   * @return
   */
  public int size()
  {
    return _map.size();
  }

  /**
   * Returns the size of the queue for the specified time.
   */
  public int scheduleSize(long time)
  {
    TimeInterval schedule = getSchedulerInterval(time);
    ConcurrentLinkedQueue<Reference<E>> queue = _map.get(schedule);
    return (queue != null) ? queue.size() : 0;
  }

  private Reference<E> createReference(E entry)
   {
     switch(_scheduleType)
     {
       case SOFT:
         return new SoftReference<E>(entry);

       default:
       case WEAK:
         return new WeakReference<E>(entry);
     }

   }




  @Override
  public String toString()
  {
    return getClass().getSimpleName();
  }
}
