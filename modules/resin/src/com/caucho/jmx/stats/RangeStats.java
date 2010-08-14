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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jmx.stats;

import com.caucho.util.Alarm;

/**
 * Statistics for a range of values.
 */
public class RangeStats {
  private String _name;
  private long _startTime;

  private long _current;
  private long _lowWaterMark;
  private long _highWaterMark;

  /**
   * Create a null range stats.
   */
  public RangeStats()
  {
    _startTime = Alarm.getCurrentTime();
  }

  /**
   * Create a null range stats.
   */
  public RangeStats(String name)
  {
    _startTime = Alarm.getCurrentTime();
    
    _name = name;
  }

  /**
   * Returns the current value.
   */
  public long getCurrent()
  {
    return _current;
  }

  /**
   * Returns the low water mark value.
   */
  public long getLowWaterMark()
  {
    return _lowWaterMark;
  }

  /**
   * Returns the high water mark value.
   */
  public long getHighWaterMark()
  {
    return _highWaterMark;
  }

  /**
   * Sets the current value.
   */
  public void setCurrent(long current)
  {
    _current = current;

    if (current < _lowWaterMark)
      _lowWaterMark = current;
    
    if (_highWaterMark < current)
      _highWaterMark = current;
  }

  /**
   * Resets the range.
   */
  public void reset()
  {
    _lowWaterMark = _highWaterMark = _current;
  }
}
