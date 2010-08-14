/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.env.meter;

import java.util.concurrent.atomic.AtomicLong;

import com.caucho.util.Alarm;

public final class TimeRangeMeter extends AbstractMeter implements TimeSensor {
  private final double _scale;

  private final AtomicLong _activeCount = new AtomicLong();
  private final AtomicLong _count = new AtomicLong();
  private final AtomicLong _time = new AtomicLong();
  private final AtomicLong _timeMax = new AtomicLong();
  
  private final AtomicLong _lastAvgCount = new AtomicLong();
  private final AtomicLong _lastAvgTime = new AtomicLong();
  private final AtomicLong _lastCount = new AtomicLong();

  public TimeRangeMeter(String name)
  {
    super(name);

    _scale = 1.0;
  }

  public AbstractMeter createCount(String name)
  {
    return new TimeRangeCountProbe(name);
  }

  public AbstractMeter createActiveCount(String name)
  {
    return new TimeRangeActiveCountProbe(name);
  }

  public AbstractMeter createMax(String name)
  {
    return new TimeRangeMaxProbe(name);
  }

  public final long start()
  {
    long startTime = Alarm.getCurrentTime();

    _activeCount.incrementAndGet();

    return startTime;
  }

  public final void add(long startTime)
  {
    _activeCount.decrementAndGet();
    
    long time = Alarm.getCurrentTime() - startTime;
    
    _count.incrementAndGet();
    _time.addAndGet(time);

    long max = _timeMax.get();
    while (max < time) {
      _timeMax.compareAndSet(max, time);
      max = _timeMax.get();
    }
  }
  
  /**
   * Return the probe's next sample.
   */
  public final double sample()
  {
    long count = _count.get();
    long lastCount = _lastAvgCount.getAndSet(count);
    long time = _time.get();
    long lastTime = _lastAvgTime.getAndSet(time);

    if (count == lastCount)
      return 0;
    else
      return _scale * (time - lastTime) / (double) (count - lastCount);
  }
  
  /**
   * Return the probe's next sample.
   */
  public final double sampleCount()
  {
    long count = _count.get();
    long lastCount = _lastCount.getAndSet(count);

    return count - lastCount;
  }
  
  /**
   * Return the probe's next sample.
   */
  public final double sampleMax()
  {
    long max = _timeMax.getAndSet(0);

    return _scale * max;
  }

  class TimeRangeCountProbe extends AbstractMeter {
    TimeRangeCountProbe(String name)
    {
      super(name);
    }

    public double sample()
    {
      return sampleCount();
    }
  }

  class TimeRangeActiveCountProbe extends AbstractMeter {
    TimeRangeActiveCountProbe(String name)
    {
      super(name);
    }

    public double sample()
    {
      return _activeCount.get();
    }
  }

  class TimeRangeMaxProbe extends AbstractMeter {
    TimeRangeMaxProbe(String name)
    {
      super(name);
    }

    public double sample()
    {
      return sampleMax();
    }
  }
}
