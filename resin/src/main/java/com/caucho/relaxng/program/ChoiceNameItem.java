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

package com.caucho.relaxng.program;

import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Generates programs from patterns.
 */
public class ChoiceNameItem extends NameClassItem {
  protected final static L10N L = new L10N(ChoiceNameItem.class);

  private ArrayList<NameClassItem> _items = new ArrayList<NameClassItem>();

  public ChoiceNameItem()
  {
  }

  public static NameClassItem create(NameClassItem left, NameClassItem right)
  {
    ChoiceNameItem choice = new ChoiceNameItem();
    choice.addItem(left);
    choice.addItem(right);

    return choice.getMin();
  }

  public void addItem(NameClassItem item)
  {
    if (item == null)
      return;
    else if (item instanceof ChoiceNameItem) {
      ChoiceNameItem choice = (ChoiceNameItem) item;

      for (int i = 0; i < choice._items.size(); i++)
        addItem(choice._items.get(i));

      return;
    }

    for (int i = 0; i < _items.size(); i++) {
      NameClassItem subItem = _items.get(i);

      if (item.equals(subItem))
        return;
    }

    _items.add(item);
  }

  public NameClassItem getMin()
  {
    if (_items.size() == 0)
      return null;
    else if (_items.size() == 1)
      return _items.get(0);
    else
      return this;
  }

  /**
   * Returns the first set, the set of element names possible.
   */
  public void firstSet(HashSet<QName> set)
  {
    for (int i = 0; i < _items.size(); i++)
      _items.get(i).firstSet(set);
  }
  
  /**
   * Allows empty if both allow empty.
   */
  public boolean matches(QName name)
  {
    for (int i = 0; i < _items.size(); i++)
      if (_items.get(i).matches(name))
        return true;

    return false;
  }

  /**
   * Returns the pretty printed syntax.
   */
  public String toSyntaxDescription(String prefix)
  {
    CharBuffer cb = new CharBuffer();

    cb.append("(");
    
    for (int i = 0; i < _items.size(); i++) {
      if (i != 0)
        cb.append(" | ");
      
      cb.append(_items.get(i).toSyntaxDescription(prefix));
    }

    cb.append(")");

    return cb.toString();
  }

  /**
   * Returns the hash code for the empty item.
   */
  public int hashCode()
  {
    int hash = 37;

    for (int i = 0; i < _items.size(); i++)
      hash += _items.get(i).hashCode();

    return hash;
  }

  /**
   * Returns true if the object is an empty item.
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    
    if (! (o instanceof ChoiceNameItem))
      return false;

    ChoiceNameItem choice = (ChoiceNameItem) o;

    return isSubset(choice) && choice.isSubset(this);
  }

  private boolean isSubset(ChoiceNameItem item)
  {
    if (_items.size() != item._items.size())
      return false;

    for (int i = 0; i < _items.size(); i++) {
      NameClassItem subItem = _items.get(i);

      if (! item._items.contains(subItem))
        return false;
    }

    return true;
  }

  public String toString()
  {
    StringBuffer sb = new StringBuffer();

    sb.append("ChoiceNameItem[");
    for (int i = 0; i < _items.size(); i++) {
      if (i != 0)
        sb.append(", ");
      sb.append(_items.get(i));
    }

    sb.append("]");

    return sb.toString();
  }
}

