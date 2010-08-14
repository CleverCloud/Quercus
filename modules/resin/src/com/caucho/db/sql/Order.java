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

package com.caucho.db.sql;

import com.caucho.util.IntArray;

import java.sql.SQLException;
import java.util.logging.Logger;

abstract class Order {
  private boolean _isAscending = true;

  protected Order _next;

  /**
   * Set true for ascending.
   */
  public boolean isAscending()
  {
    return _isAscending;
  }

  /**
   * Set true for ascending.
   */
  public void setAscending(boolean isAscending)
  {
    _isAscending = isAscending;
  }

  /**
   * Append the next value.
   */
  static Order append(Order order, Order next)
  {
    if (order == null)
      return next;
    else {
      order._next = next;
      return order;
    }
  }

  abstract public int compare(SelectResult result, int indexA, int indexB)
    throws SQLException;

  /**
   * Sorts the database result.
   */
  public void sort(SelectResult result, IntArray rowArray)
    throws SQLException
  {
    int []rows = rowArray.getArray();
    int size = rowArray.size();

    sort(result, rows, 0, size);
  }

  /**
   * Sorts a subset of the database result.
   */
  private void sort(SelectResult result, int []rows, int offset, int length)
    throws SQLException
  {
    if (length > 3) {
      int head = offset;
      int right = offset + length - 1;
      int tail = right;
      int pivot = offset + length / 2;
      int pivotIndex = rows[pivot];

      // swap(pivot, right)
      int temp = rows[right];
      rows[right] = pivotIndex;
      rows[pivot] = temp;

      --tail;

      for (;;) {

        while (head <= tail && compare(result, rows[head], pivotIndex) < 0) {
          ++head;
        }

        while (head <= tail && compare(result, rows[tail], pivotIndex) > 0) {
          --tail;
        }

        if (head > tail) {
          break;
        }

        // swap(head++, tail--)
        temp = rows[head];
        rows[head++] = rows[tail];
        rows[tail--] = temp;
      }

      if (compare(result, rows[head], pivotIndex) > 0) {
        // swap(head, right)
        temp = rows[head];
        rows[head] = rows[right];
        rows[right] = temp;
      }

      if (offset < head) {
        sort(result, rows, offset, head - offset);
      }

      if (right > head) {
        sort(result, rows, head+1, right - head);
      }
    }
    else if (length == 3) {
      int indexA = rows[offset + 0];
      int indexB = rows[offset + 1];
      int indexC = rows[offset + 2];

      if (compare(result, indexB, indexA) < 0) {
        int temp = indexA;
        indexA = indexB;
        indexB = temp;
      }

      // A <= B <= C
      if (compare(result, indexB, indexC) <= 0) {
      }
      // C < A <= B
      else if (compare(result, indexC, indexA) < 0) {
        int temp = indexC;
        indexC = indexB;
        indexB = indexA;
        indexA = temp;
      }
      // A <= C < B
      else {
        int temp = indexC;
        indexC = indexB;
        indexB = temp;
      }

      rows[offset + 0] = indexA;
      rows[offset + 1] = indexB;
      rows[offset + 2] = indexC;
    }
    else if (length == 2) {
      int indexA = rows[offset];
      int indexB = rows[offset + 1];

      if (compare(result, indexB, indexA) < 0) {
        int temp = indexB;
        indexB = indexA;
        indexA = temp;
      }

      rows[offset + 0] = indexA;
      rows[offset + 1] = indexB;
    }
    else {
      // nothing to do
    }
  }
}
