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

import com.caucho.util.FreeList;

import java.util.logging.Logger;

/**
 * Represents a row of a group item.
 */
class GroupItem {
  private static FreeList<GroupItem> _freeList = new FreeList<GroupItem>(256);
  
  private Data []_data;
  
  private boolean []_isGroupByFields;

  /**
   * Creates a group item of a given size.
   */
  private GroupItem(int size)
  {
    _data = new Data[size];
    for (int i = 0; i < size; i++)
      _data[i] = new Data();
  }

  /**
   * Creates a group item of a given size.
   */
  static GroupItem allocate(boolean []isGroupByFields)
  {
    GroupItem item = _freeList.allocate();

    if (item == null)
      item = new GroupItem(isGroupByFields.length);

    item.setSize(isGroupByFields.length);
    item.setGroupByFields(isGroupByFields);

    return item;
  }

  /**
   * Creates a group item of a given size.
   */
  GroupItem allocateCopy()
  {
    GroupItem item = _freeList.allocate();

    if (item == null)
      item = new GroupItem(_data.length);

    item.setSize(_isGroupByFields.length);
    item.setGroupByFields(_isGroupByFields);

    for (int i = 0; i < _isGroupByFields.length; i++) {
      if (_isGroupByFields[i]) {
        getData(i).copyTo(item.getData(i));
      }
    }

    return item;
  }

  /**
   * Sets the size.
   */
  public void init(int size, boolean []isGroupByFields)
  {
    setSize(size);
    setGroupByFields(isGroupByFields);
    clear();
  }

  /**
   * Sets the group item size.
   */
  private void setSize(int size)
  {
    if (_data.length < size) {
      _data = new Data[size];

      for (int i = 0; i < size; i++)
        _data[i] = new Data();
    }
  }

  /**
   * Sets the group-by fields
   */
  private void setGroupByFields(boolean []isGroupByFields)
  {
    _isGroupByFields = isGroupByFields;
  }

  /**
   * Clears the data.
   */
  public void clear()
  {
    int length = _data.length;
    
    for (int i = 0; i < length; i++)
      _data[i].clear();
  }

  /**
   * Return true for null
   */
  public boolean isNull(int index)
  {
    return _data[index].isNull();
  }

  /**
   * Sets the data as a double.
   */
  public void setDouble(int index, double value)
  {
    _data[index].setDouble(value);
  }

  /**
   * Gets the data as a double.
   */
  public double getDouble(int index)
  {
    return _data[index].getDouble();
  }

  /**
   * Sets the data as a long.
   */
  public void setLong(int index, long value)
  {
    _data[index].setLong(value);
  }

  /**
   * Gets the data as a long.
   */
  public long getLong(int index)
  {
    return _data[index].getLong();
  }

  /**
   * Sets the data as a string.
   */
  public void setString(int index, String value)
  {
    _data[index].setString(value);
  }

  /**
   * Gets the data as a string.
   */
  public String getString(int index)
  {
    return _data[index].getString();
  }

  /**
   * Returns the data object a string.
   */
  public Data getData(int index)
  {
    return _data[index];
  }

  /**
   * Returns the hashCode.
   */
  public int hashCode()
  {
    int hash = 37;

    boolean []isGroupByFields = _isGroupByFields;
    int length = isGroupByFields.length;
    if (length == 0)
      return hash;

    Data []data = _data;
    
    for (int i = 0; i < length; i++) {
      if (isGroupByFields[i]) {
        hash = hash * 65521 + data[i].hashCode();
      }
    }

    return hash;
  }

  /**
   * Returns equality based on group by
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    
    else if (! o.getClass().equals(getClass()))
      return false;

    GroupItem item = (GroupItem) o;

    boolean []isGroupByFields = _isGroupByFields;
    if (isGroupByFields != item._isGroupByFields)
      return false;

    for (int i = 0; i < isGroupByFields.length; i++) {
      if (isGroupByFields[i] && ! _data[i].equals(item._data[i]))
        return false;
    }

    return true;
  }
}
