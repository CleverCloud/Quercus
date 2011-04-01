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

public class DoubleArray {
  double []data;
  int size;

  public void clear() {
    size = 0;
  }
  
  private void expand(int max) {
    while (max > data.length) {
      double []next = new double[data.length * 2];

      for (int i = 0; i < data.length; i++)
        next[i] = data[i];

      data = next;
    }
  }

  public int size() { return size; }

  public void append(double i) {
    expand(size + 1);

    data[size++] = i;
  }

  public void append(DoubleArray array) {
    expand(size + array.size);

    for (int i = 0; i < array.size; i++)
      data[size++] = array.data[i];
  }

  public void insert(int i, double value) {
    expand(size + 1);

    System.arraycopy(data, i, data, i + 1, size - i);
    data[i] = value;
    size++;
  }

  public double pop() {
    return data[--size];
  }

  public void setLength(int size)
  {
    expand(size);

    for (int i = this.size; i < size; i++)
      data[i] = 0;

    this.size = size;
  }

  public double get(int i) {
    return data[i];
  }

  public double last() {
    return data[size - 1];
  }

  public void set(int i, double value) {
    if (i + 1 > size)
      setLength(i + 1);

    data[i] = value;
  }

  public boolean contains(double test)
  {
    for (int i = 0; i < size; i++)
      if (data[i] == test)
        return true;

    return false;
  }

  public boolean isSubset(DoubleArray subset)
  {
    for (int i = 0; i < subset.size; i++)
      if (! contains(subset.data[i]))
        return false;

    return true;
  }

  public void union(DoubleArray newArray)
  {
    for (int i = 0; i < newArray.size; i++) {
      if (! contains(newArray.data[i]))
        append(newArray.data[i]);
    }
  }

  public DoubleArray() { 
    data = new double[16];
    size = 0;
  }
}
