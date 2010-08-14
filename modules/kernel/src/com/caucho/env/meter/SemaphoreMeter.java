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


public final class SemaphoreMeter extends AbstractMeter implements SemaphoreSensor {
  // sample data
  private final AtomicLong _acquireCount = new AtomicLong();
  private final AtomicLong _releaseCount = new AtomicLong();
  
  // min/max
  private final AtomicLong _min = new AtomicLong();
  private final AtomicLong _max = new AtomicLong();

  // for sample
  private long _lastAcquireCount;
  
  public SemaphoreMeter(String name)
  {
    super(name);
  }

  public AbstractMeter createCount(String name)
  {
    return new CountProbe(name);
  }

  public AbstractMeter createMin(String name)
  {
    return new MinProbe(name);
  }

  public AbstractMeter createMax(String name)
  {
    return new MaxProbe(name);
  }

  public final void acquire()
  {
    long acquire = _acquireCount.incrementAndGet();
    long release = _releaseCount.get();

    long count = acquire - release;
    long max;

    while ((max = _max.get()) < count) {
      _max.compareAndSet(max, count);
    }
  }

  public final void release()
  {
    long acquire = _acquireCount.get();
    long release = _releaseCount.incrementAndGet();

    long count = acquire - release;
    
    long min;

    while (count < (min = _min.get())) {
      _min.compareAndSet(min, count);
    }
  }
  
  /**
   * Return the probe's next average.
   */
  public final double sample()
  {
    return _acquireCount.get() - _releaseCount.get();
  }
  
  /**
   * Return the probe's next sample.
   */
  public final double sampleCount()
  {
    long lastAcquireCount = _lastAcquireCount;
    _lastAcquireCount = _acquireCount.get();
    
    return _lastAcquireCount - lastAcquireCount;
  }
  
  /**
   * Return the probe's next sample.
   */
  public final double sampleMax()
  {
    long count = _acquireCount.get() - _releaseCount.get();
    long max = _max.getAndSet(count);

    return max;
  }
  
  /**
   * Return the probe's next sample.
   */
  public final double sampleMin()
  {
    long count = _acquireCount.get() - _releaseCount.get();
    long min = _min.getAndSet(count);

    return min;
  }

  class CountProbe extends AbstractMeter {
    CountProbe(String name)
    {
      super(name);
    }

    public double sample()
    {
      return sampleCount();
    }
  }

  class MaxProbe extends AbstractMeter {
    MaxProbe(String name)
    {
      super(name);
    }

    public double sample()
    {
      return sampleMax();
    }
  }

  class MinProbe extends AbstractMeter {
    MinProbe(String name)
    {
      super(name);
    }

    public double sample()
    {
      return sampleMin();
    }
  }
}
