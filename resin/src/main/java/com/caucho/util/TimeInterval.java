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

import java.util.Comparator;

/**
 * Defines a TimeInterval, the period of time something is current.
 */
public interface TimeInterval
  extends ComparableElement<TimeInterval>, Comparator<TimeInterval>, TimeIntervalEntry

{

  /**
   * Returns the time that the interval starts in milliseconds.
   */
  public long startTime();

  /**
   * Returns the time that the interval ends in milliseconds.
   *
   * @return
   */
  public long stopTime();

  /**
   * Returns the duration of the interval in milliseconds.
   * <p/>
   * Note that an internval with a duration of 0 is valid, and can
   * be used to represent a moment in time.
   */
  public long duration();

  /**
   * Returns true if the interval is current.
   *
   * @note An interval is current if the interval has started, but not
   * yet ended.
   */
  public boolean current();

  /**
   * Returns true if the interval is current at the time specified.
   */
  public boolean current(long time);


  /**
   * Defines the set of relationships between two time intervals.
   */
  public enum Comparison implements ComparableElement.Comparison<TimeInterval> {

    PRECEDES(IntervalDistance.DISJOINT, Relation.BEFORE),

    PRECEDES_IMMEDIATELY(IntervalDistance.CONTIGUOUS, Relation.BEFORE),

    ENDS_DURING(IntervalDistance.OVERLAPPING, Relation.BEFORE),

    CONTAINS(IntervalDistance.NESTED, Relation.CHILD),

    IS_CONTAINED(IntervalDistance.NESTED, Relation.PARENT),

    EQUAL(IntervalDistance.EQUAL, Relation.EQUAL),

    STARTS_DURING(IntervalDistance.OVERLAPPING, Relation.AFTER),

    FOLLOWS_IMMEDIATELY(IntervalDistance.CONTIGUOUS, Relation.AFTER),

    FOLLOWS(IntervalDistance.DISJOINT, Relation.AFTER);

    private int _distance;

    private Comparison(IntervalDistance intervalDistance, Relation relation)
    {
      _distance = intervalDistance.value() * Integer.signum(relation.value());
    }

    /**
     * Implements the required method.
     */
    public int value()
    {
      return _distance;
    }

    /**
     * Provides a comparsion of two time intervals.
     */
    public static Comparison comparison(TimeInterval a, TimeInterval b)
    {
      long aStarts = a.startTime();
      long aStops = a.stopTime();
      long bStarts = b.startTime();
      long bStops = b.stopTime();

      switch (Relation.comparison(aStarts, bStarts)) {

        case BEFORE:

          switch (Relation.comparison(aStarts, bStops)) {

            case BEFORE:
              return PRECEDES;

            case EQUAL:
              return PRECEDES_IMMEDIATELY;

            case AFTER:
              return (bStops > aStops) ? CONTAINS : ENDS_DURING;
          }

        case EQUAL:

          switch (Relation.comparison(aStops, bStops)) {
            case BEFORE:
              return CONTAINS;

            case EQUAL:
              return EQUAL;

            case AFTER:
              return IS_CONTAINED;
          }

        case AFTER:

          switch (Relation.comparison(aStops, bStarts)) {
            case BEFORE:
               return (aStops > bStops) ? IS_CONTAINED : STARTS_DURING;

            case EQUAL:
              return FOLLOWS_IMMEDIATELY;

            case AFTER:

              return FOLLOWS;
          }
      }

      return null;
    }

    private enum Relation {
      CHILD(-2),
      BEFORE(-1),
      EQUAL(0),
      AFTER(1),
      PARENT(2);

      private int _relation;

      private Relation(int direction)
      {
        _relation = direction;
      }

      public int value()
      {
        return _relation;
      }

      static Relation comparison(long time1, long time2)
      {
        return (time1 == time2) ? EQUAL : (time1 < time2) ? AFTER : BEFORE;
      }

    }

    private enum IntervalDistance {

      /** The intervals are equal */
      EQUAL(0),

      /** One interval immediately follows the other with no time in between */
      CONTIGUOUS(1),

      /** One interval starts and stops within the other interval */
      NESTED(2),

      /** One interval starts within an interval, and continues beyond it */
      OVERLAPPING(3),

      /** There is time between the two intervals */
      DISJOINT(4);

      private int _distance;

      private IntervalDistance(int distance)
      {
        _distance = (distance >= 0) ? distance : -distance;
      }

      public int value()
      {
        return _distance;
      }
    }
  }


}
