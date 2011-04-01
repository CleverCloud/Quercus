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
 * Implements the {@link TimeInterval} interface.
 */
public class TimeIntervalElement
  implements TimeInterval
{
  private final long _starts;
  private final long _stops;
  private final long _duration;
  private final int _hashCode;
  private String _toString;

  public TimeIntervalElement(long starts, long stops)
  {

    _starts = starts;
    _stops = stops;
    _duration = _stops - _starts;
    _hashCode = hashLong((starts + 37 * _stops));
    validate();
  }

  private void validate()
  {
    if (_stops < _starts)
      throw new IllegalStateException(getClass().getName());
  }


  public final long startTime()
  {
    return _starts;
  }

  public final long stopTime()
  {
    return _stops;
  }

  public final long duration()
  {
    return _duration;
  }

  public final boolean current()
  {
    long now = intervalNow();
    return (_starts <= now) && (now < _stops);
  }

  public final boolean current(long time)
  {
    return (_starts <= time) && (time < _stops);
  }

  public final boolean equals(Object other)
  {
    if ((other == null) || !(other instanceof TimeInterval))
      return false;

    TimeInterval otherInterval = (TimeInterval) other;

    return ((_starts == otherInterval.startTime())
      && (_stops == otherInterval.stopTime()));
  }

  /**
   * Returns the hashCode that was calculated when this immutable instance was
   * instantiated.
   */
  @Override
   public final int hashCode()
  {
    return _hashCode;
  }

  public final int compareTo(TimeInterval interval)
  {
    return compareWith(interval).value();
  }

  public final int compare(TimeInterval interval1, TimeInterval interval2)
  {
    return interval1.compareTo(interval2);
  }

  public final Comparison compareWith(TimeInterval other)
  {
    return TimeInterval.Comparison.comparison(this, other);
  }

  private int hashLong(long item)
  {
     return (int) (item ^ (item >>> 32));
  }

  /**
   * Provides the current time for use by the interval.
   */
  protected  long intervalNow()
  {
    return Alarm.getCurrentTime();
  }

  public TimeInterval getTimeInterval()
  {
    return this;
  }

   @Override
   public String toString()
   {
     if (_toString == null)
     {
       StringBuilder sb = new StringBuilder(getClass().getSimpleName());
       sb.append("[starts=").append(_starts);
       sb.append(", stops=").append(_stops);
       sb.append(", duration=").append(_duration).append("]");
       _toString = sb.toString();
     }

     return _toString;
   }
}
