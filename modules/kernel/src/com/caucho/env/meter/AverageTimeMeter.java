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
import java.util.concurrent.atomic.AtomicInteger;


public final class AverageTimeMeter extends AbstractMeter {
  private final AtomicLong _sum = new AtomicLong();
  private final AtomicInteger _count = new AtomicInteger();

  public AverageTimeMeter(String name)
  {
    super(name);
  }

  public final void addData(long time)
  {
    long oldValue;

    do {
      oldValue = _sum.get();
    } while (! _sum.compareAndSet(oldValue, oldValue + time));

    _count.incrementAndGet();
  }
  
  /**
   * Return the probe's next sample.
   */
  public final double sample()
  {
    long sum = _sum.getAndSet(0);
    int count = _count.getAndSet(0);

    if (count != 0)
      return sum / (double) count;
    else
      return 0;
  }
}
