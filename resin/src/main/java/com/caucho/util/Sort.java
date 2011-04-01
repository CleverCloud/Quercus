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

import java.util.ArrayList;

abstract public class Sort {
  abstract public boolean lessThan(Object a, Object b);

  private void quicksort(ArrayList list, int head, int tail)
  {
    if (tail - head < 2)
      return;
    else if (tail - head == 2) {
      Object va = list.get(head);
      Object vb = list.get(head + 1);

      if (lessThan(vb, va)) {
        list.set(head, vb);
        list.set(head + 1, va);
      }

      return;
    }

    int center = head + 1;
    Object pivot = list.get(head);
    for (int i = head + 1; i < tail; i++) {
      Object value = list.get(i);
      
      if (lessThan(value, pivot)) {
        Object centerValue = list.get(center);
        list.set(center, value);
        list.set(i, centerValue);
        center++;
      }
    }

    if (center == tail) {
      Object tailValue = list.get(tail - 1);
      list.set(head, tailValue);
      list.set(tail - 1, pivot);

      quicksort(list, head, tail - 1);
    } else {
      quicksort(list, head, center);
      quicksort(list, center, tail);
    }
  }

  public void sort(ArrayList list)
  {
    quicksort(list, 0, list.size());
  }
}


