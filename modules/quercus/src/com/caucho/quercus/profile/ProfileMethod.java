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

package com.caucho.quercus.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Report for a method (function)
 */
public class ProfileMethod
{
  private final int _id;
  private final String _name;
  
  private long _count;
  
  private long _selfMicros;
  private long _totalMicros;

  private ArrayList<ProfileItem> _parentList = new ArrayList<ProfileItem>();
  private ArrayList<ProfileItem> _childList = new ArrayList<ProfileItem>();

  public ProfileMethod(int id, String name)
  {
    _id = id;
    _name = name;
  }

  /**
   * Returns the method's id.
   */
  public int getId()
  {
    return _id;
  }

  /**
   * Returns the item's function name
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the function's call count
   */
  public long getCount()
  {
    return _count;
  }

  /**
   * Returns the function's total time in micros
   */
  public long getTotalMicros()
  {
    return _totalMicros;
  }

  /**
   * Returns the function's self time in micros
   */
  public long getSelfMicros()
  {
    return _selfMicros;
  }

  /**
   * Returns the parent items.
   */
  public ArrayList<ProfileItem> getParentItems()
  {
    return _parentList;
  }

  /**
   * Returns the parent items, sorted by micros.
   */
  public ArrayList<ProfileItem> getParentItemsByMicros()
  {
    ArrayList<ProfileItem> parentList
      = new ArrayList<ProfileItem>(_parentList);

    Collections.sort(parentList, new ItemMicrosComparator());
    
    return parentList;
  }

  /**
   * Returns the child items.
   */
  public ArrayList<ProfileItem> getChildItems()
  {
    return _childList;
  }

  /**
   * Returns the child items, sorted by micros.
   */
  public ArrayList<ProfileItem> getChildItemsByMicros()
  {
    ArrayList<ProfileItem> childList
      = new ArrayList<ProfileItem>(_childList);

    Collections.sort(childList, new ItemMicrosComparator());
    
    return childList;
  }

  /**
   * Adds a parent item
   */
  public void addParent(ProfileItem item)
  {
    _parentList.add(item);

    _count += item.getCount();
    _totalMicros += item.getMicros();
    _selfMicros += item.getMicros();
  }

  /**
   * Adds a child item
   */
  public void addChild(ProfileItem item)
  {
    _childList.add(item);

    _selfMicros -= item.getMicros();
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _name
            + ",count=" + _count
            + ",self-micros=" + _selfMicros
            + ",total-micros=" + _totalMicros
            + "]");
  }

  static class ItemMicrosComparator implements Comparator<ProfileItem> {
    public int compare(ProfileItem a, ProfileItem b)
    {
      long delta = b.getMicros() - a.getMicros();

      if (delta == 0)
        return 0;
      else if (delta < 0)
        return -1;
      else
        return 1;
    }
  }
}

