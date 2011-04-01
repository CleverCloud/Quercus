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

/**
 * Meters gather information from Sensors in a ResinSystem. Some sampled
 * meters like the CPU and JMX don't have an associated Sensor, but gather
 * the data as they are polled.
 * 
 * Meters typically gather information when polled and then reset any internal
 * accumulated counters. For example, a request counter meter will clear the
 * request count after being sampled.
 * 
 * Meter values can typically be peeked which will return the current value
 * without resetting internal counters.
 */
public interface Meter {
  /**
   * Returns the meter's name.
   */
  public String getName();
  
  /**
   * Gather the meter's next sample.
   */
  public double sample();

  /**
   * Returns the current meter value without updating the sample.
   */
  public double peek();
}
