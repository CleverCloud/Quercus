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


public final class AverageMeter extends TotalMeter implements AverageSensor {
  private final double _scale;

  private final Object _lock = new Object();

  // sample data
  private final AtomicLong _count = new AtomicLong();
  private final AtomicLong _sum = new AtomicLong();
  private final AtomicLong _max = new AtomicLong();
  private double _sumSquare;

  private long _lastCount;

  private long _lastAvgCount;
  private long _lastAvgSum;

  // for 95%
  private long _lastStdCount;
  private double _lastStdSum;

  public AverageMeter(String name)
  {
    super(name);

    _scale = 1.0;
  }

  public TotalMeter createCount(String name)
  {
    return new AverageCountProbe(name);
  }

  public AbstractMeter createMax(String name)
  {
    return new MaxProbe(name);
  }

  public AbstractMeter createSigma(String name, int n)
  {
    return new SigmaProbe(name, n);
  }

  public final void add(long value)
  {
    double sqValue = value * value;

    _count.incrementAndGet();
    _sum.addAndGet(value);
    _sumSquare += sqValue;

    long max;
    while ((max = _max.get()) < value
           && ! _max.compareAndSet(max, value)) {
    }
  }

  /**
   * Return the probe's next average.
   */
  public final double sample()
  {
    synchronized (_lock) {
      long count = _count.get();
      long lastCount = _lastAvgCount;
      _lastAvgCount = count;

      long sum = _sum.get();
      double lastSum = _lastAvgSum;
      _lastAvgSum = sum;

      if (count == lastCount)
        return 0;
      else
        return _scale * (sum - lastSum) / (double) (count - lastCount);
    }
  }

  @Override
  public double getTotal()
  {
    return _sum.get();
  }

  /**
   * Return the probe's next sample.
   */
  public final double sampleCount()
  {
    synchronized (_lock) {
      long count = _count.get();
      long lastCount = _lastCount;
      _lastCount = count;

      return count - lastCount;
    }
  }

  /**
   * Return the probe's next 2-sigma
   */
  public final double sampleSigma(int n)
  {
    synchronized (_lock) {
      long count = _count.get();
      long lastCount = _lastStdCount;
      _lastStdCount = count;

      double sum = _sum.get();
      double lastSum = _lastStdSum;
      _lastStdSum = sum;

      double sumSquare = _sumSquare;
      _sumSquare = 0;

      if (count == lastCount)
        return 0;

      double avg = (sum - lastSum) / (count - lastCount);
      double part = (count - lastCount) * sumSquare - sum * sum;

      if (part < 0)
        part = 0;

      double std = Math.sqrt(part) / (count - lastCount);

      return _scale * (avg + n * std);
    }
  }

  /**
   * Return the probe's next sample.
   */
  public final double sampleMax()
  {
    synchronized (_lock) {
      long max = _max.getAndSet(0);

      return _scale * max;
    }
  }

  class AverageCountProbe extends TotalMeter {
    AverageCountProbe(String name)
    {
      super(name);
    }

    public double sample()
    {
      return sampleCount();
    }

    @Override
    public double getTotal()
    {
      return _count.get();
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

  class SigmaProbe extends AbstractMeter {
    private final int _n;

    SigmaProbe(String name, int n)
    {
      super(name);

      _n = n;
    }

    public double sample()
    {
      return sampleSigma(_n);
    }
  }
}
