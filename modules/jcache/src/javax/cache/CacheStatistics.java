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

package javax.cache;

/**
 * Provides the means to observe the behavior of the Cache.
 */
public interface CacheStatistics
{
  /**
   * No statistics will be colleced.
   */
  public static int STATISTICS_ACCURACY_BEST_EFFORT = 1;

  /**
   * The  counters are thread safe.
   */
  public static int STATISTICS_ACCURACY_GUARANTEED = 2;

  /**
   *
   */
  public static int STATISTICS_ACCURACY_NONE = 0;

  /**
   * resets the counters for the hits and misses.
   */
  public void clearStatistics();

  /**
   * Returns the number of hits, i.e., the number of times
   * requested values were found in the cache.
   */
  public int getCacheHits();

  /**
   * Returns the number of times requested values were not found
   * in the local cache.  Note that this statistic does not
   *
   */
  public int getCacheMisses();

  /**
   * Returns the current number of items in the local cache.
   */
  public int getObjectCount();

  /**
   * Returns the setting for cache statistics..
   * @return
   */
  public int getStatisticsAccuracy();  
}
