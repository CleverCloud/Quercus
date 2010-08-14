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
package com.caucho.resources;

import com.caucho.config.types.Trigger;

/**
 * Trigger for delay, period
 */
public class TimerTrigger implements Trigger {
  private long _firstTime;
  private long _period;

  /**
   * Constructs a new timer trigger.
   */
  public TimerTrigger()
  {
  }

  /**
   * Creates a timer trigger.
   * 
   * @param firstTime
   *          The first time the trigger should fire, in milliseconds.
   */
  public TimerTrigger(long firstTime)
  {
    _firstTime = firstTime;
  }

  /**
   * Creates a timer trigger.
   * 
   * @param firstTime
   *          The first time the trigger should fire, in milliseconds.
   * @param period
   *          The interval, in milliseconds, the timer should be triggered.
   */
  public TimerTrigger(long firstTime, long period)
  {
    _firstTime = firstTime;
    _period = period;
  }

  public void setFirstTime(long firstTime)
  {
    _firstTime = firstTime;
  }

  public long getFirstTime()
  {
    return _firstTime;
  }

  public void setPeriod(long period)
  {
    _period = period;
  }

  public long getPeriod()
  {
    return _period;
  }

  /**
   * Returns the time of the next trigger event
   * 
   * @param now
   *          The current time.
   * @return The next trigger time.
   */
  @Override
  public long nextTime(long now)
  {
    if (now < _firstTime)
      return _firstTime;
    else if (_period <= 0)
      return Long.MAX_VALUE / 2;
    else {
      long delta = (now - _firstTime) % _period;

      return now + _period - delta;
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _period + "," + _firstTime + "]";
  }
}
