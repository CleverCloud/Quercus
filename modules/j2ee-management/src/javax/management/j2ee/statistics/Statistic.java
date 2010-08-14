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
 * @author Sam
 */

package javax.management.j2ee.statistics;

/**
 * A statistic about a managed object.
 */
public interface Statistic
{
  /**
   * Returns the name of this statistics, corresponding to the name of the {@link Stats}
   * attribute that provides the value.
   */
  public String getName();

  /**
   * Returns the unit of measurement for this statistic.
   *
   * TimeStatistic returns one of "HOUR", "MINUTE",
   * "SECOND", "MILLISECOND", "MICROSECOND", or "NANOSECOND".
   * @return
   */
  public String getUnit();

  /**
   * A description of the statistic.
   */
  public String getDescription();

  /**
   * Returns the time that the first measurement was taken for this statistic,
   * the number of milliseconds sinch 00:00 January 1, 1970.
   */
  public long getStartTime();

  /**
   * Returns the time that the last measurement was taken for this statistic,
   * the number of milliseconds sinch 00:00 January 1, 1970.
   */
  public long getLastSampleTime();
}
