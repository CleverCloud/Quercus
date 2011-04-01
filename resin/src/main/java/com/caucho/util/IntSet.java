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

package com.caucho.util;

public class IntSet {
  int []data;
  int size;

  public void clear() {
    size = 0;
  }
  
  private void expand(int max) {
    while (max > data.length) {
      int []next = new int[data.length * 2];

      for (int i = 0; i < data.length; i++)
        next[i] = data[i];

      data = next;
    }
  }

  public int length() { return size / 2; }
  public int size() { return size / 2; }

  public int getMin(int i) { return data[2 * i]; } 
  public int getMax(int i) { return data[2 * i + 1]; } 

  private void insert(int i, int min, int max) {
    expand(size + 2);

    System.arraycopy(data, i, data, i + 2, size - i);

    data[i] = min;
    data[i + 1] = max;

    size += 2; 
  }

  private void delete(int i) {
    System.arraycopy(data, i + 2, data, i, size - i - 2);

    size -= 2; 
  }

  /**
   * Adds the range [min,max] to the set.
   */
  public void union(int min, int max) {
    for (int i = 1; i < size; i += 2) {
      if (max < data[i - 1] - 1) {
        insert(i - 1, min, max);
        return;
      }

      if (min > data[i] + 1)
        continue;

      if (min < data[i - 1])
        data[i - 1] = min;
      if (max > data[i])
        data[i] = max;

      int j = i + 2;
      while (j < size && max > data[j - 1] + 1) {
        if (max < data[j - 1])
          data[i] = data[j - 1];

        delete(j - 1);
      }
      return;
    }

    insert(size, min, max);
  }

  /**
   * Adds a point to the set
   */
  public void union(int value) {
    union(value, value);
  }

  /**
   * The union of two sets.
   */
  public void union(IntSet set) {
    for (int i = 1; i < set.size; i += 2)
      union(set.data[i - 1], set.data[i]);
  }

  /**
   * The union with the negation of the 2nd set
   */
  public void unionNegate(IntSet set, int min, int max) {
    for (int i = 1; i < set.size; i += 2) {
      union(min, set.data[i - 1] - 1);
      min = set.data[i] + 1;
    }
    union(min, max);
  }

  /**
   * Negate the set within a universe
   */
  public void negate(int minValue, int maxValue) {
    int max = minValue;

    if (size > 0 && data[0] == minValue) {
      max = data[1];
      delete(0);
      if (max == maxValue)
        return;
      else
        max++;
    }

    for (int i = 1; i < size; i += 2) {
      int newMax = data[i];
      data[i] = data[i - 1] - 1;
      data[i - 1] = max;

      if (newMax == maxValue)
        return;
      max = newMax + 1;
    }

    insert(size, max, maxValue);
  }

  /**
   * Negate the set
   */
  public void negate() {
    negate(Integer.MIN_VALUE, Integer.MAX_VALUE);
  }

  /**
   * Calculates the set difference from a and b
   *
   * @return true if the original set is not contained in the 2nd
   */
  public boolean difference(IntSet set)
  {
    int i = 1;
    int j = 1;

    while (i < size && j < set.size) {
      int aMin = data[i - 1];
      int aMax = data[i];

      int bMin = set.data[j - 1];
      int bMax = set.data[j];

      //       aaaa
      //  bbbb
      if (bMax < aMin) {
        j += 2;
      }
      //  aaaa
      //       bbbb
      else if (aMax < bMin) {
        i += 2;
      }
      //   aaaa
      //  bbbbbb
      else if (bMin <= aMin && aMax <= bMax) {
        delete(i - 1);
      }
      //  aaaaaa
      //   bbbb
      else if (aMin < bMin && bMax < aMax) {
        insert(i + 1, bMax + 1, aMax);
        data[i] = bMin - 1;
        i += 2;
        j += 2;
      }
      //  aaaa
      //   bbbb
      else if (aMin < bMin) {
        data[i] = bMin - 1;
        i += 2;
      }
      //  aaaa
      // bbbb
      else if (aMax > bMax) {
        data[i - 1] = bMax + 1;
        j += 2;
      } 
      else {
        throw new RuntimeException("Impossible case");
      }
    }

    return size != 0;
  }

  /**
   * Calculates the set intersection of a and b
   *
   * @return true if not disjoint
   */
  public boolean intersection(IntSet set)
  {
    int i = 1;
    int j = 1;

    while (i < size && j < set.size) {
      int aMin = data[i - 1];
      int aMax = data[i];

      int bMin = set.data[j - 1];
      int bMax = set.data[j];

      //       aaaa
      //  bbbb
      if (bMax < aMin) {
        j += 2;
      }
      //  aaaa
      //       bbbb
      else if (aMax < bMin) {
        delete(i - 1);
      }
      //   aaaa
      //  bbbbbb
      else if (bMin <= aMin && aMax <= bMax) {
        i += 2;
      }
      //  aaaaaa
      //   bbbb
      else if (aMin <= bMin && bMax <= aMax) {
        data[i - 1] = bMin;
        data[i] = bMax;
        if (bMax < aMax)
          insert(i + 1, bMax + 1, aMax);
        i += 2;
        j += 2;
      }
      //  aaaa
      //   bbbb
      else if (aMin <= bMin) {
        data[i - 1] = bMin;
        i += 2;
      }
      //  aaaa
      // bbbb
      else if (bMin < aMin) {
        data[i] = bMax;
        insert(i + 1, bMax + 1, aMax);
        i += 2;
      }
      else {
        throw new RuntimeException("case");
      }
    }

    while (i < size)
      delete(i - 1);

    return size != 0;
  }

  /**
   * True if the set contains the element
   */
  public boolean contains(int test)
  {
    for (int i = 1; i < size; i += 2) {
      if (test < data[i - 1])
        return false;
      if (test <= data[i])
        return true;
    }

    return false;
  }

  /**
   * True if the argument is a subset of the set
   */
  public boolean isSubset(IntSet subset)
  {
    int i = 1;
    int j = 1;

    while (i < size && j < subset.size) {
      if (data[i] < subset.data[j - 1])
        i += 2;
      else if (subset.data[j - 1] < data[i - 1] || subset.data[j] > data[i])
        return false;
      else
        j += 2;
    }

    return true;
  }

  /**
   * True if the two sets are disjoint.
   */
  public boolean isDisjoint(IntSet set)
  {
    int i = 1;
    int j = 1;

    while (i < size && j < set.size) {
      if (data[i] < set.data[j - 1])
        i += 2;
      else if (set.data[j] < data[i - 1])
        j += 2;
      else
        return false;
    }

    return true;
  }

  /**
   * Returns a visible.
   */
  public String toString()
  {
    StringBuffer sbuf = new StringBuffer();
    
    sbuf.append("IntSet[");
    for (int i = 1; i < size; i += 1) {
      if (i != 1)
        sbuf.append(" ");

      sbuf.append(data[i - 1]);
      if (data[i - 1] != data[i]) {
        sbuf.append(",");
        sbuf.append(data[i]);
      }
    }
    sbuf.append("]");

    return sbuf.toString();
  }

  /**
   * Returns a clone of the set.
   */
  public Object clone()
  {
    IntSet set = new IntSet(false);

    set.data = new int[data.length];
    set.size = size;
    
    System.arraycopy(data, 0, set.data, 0, size);

    return set;
  }

  private IntSet(boolean dummy) { 
  }

  /**
   * Creates an empty set.
   */
  public IntSet() { 
    data = new int[16];
    size = 0;
  }
}
