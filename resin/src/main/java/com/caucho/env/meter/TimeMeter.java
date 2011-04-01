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


public final class TimeMeter extends AbstractMeter implements TimeSensor {
  private final AtomicLong _count = new AtomicLong();
  private final AtomicLong _time = new AtomicLong();

  public TimeMeter(String name)
  {
    super(name);
  }

  public final void add(long time)
  {
    _count.incrementAndGet();
    _time.addAndGet(time);
  }
  
  /**
   * Return the probe's next sample.
   */
  public final double sample()
  {
    long count = _count.getAndSet(0);
    long time = _time.getAndSet(0);

    if (count == 0)
      return 0;
    else
      return time / (double) count;
  }
}
