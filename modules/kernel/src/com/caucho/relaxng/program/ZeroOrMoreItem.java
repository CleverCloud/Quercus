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

import com.caucho.relaxng.RelaxException;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Generates programs from patterns.
 */
public class ZeroOrMoreItem extends Item {
  protected final static L10N L = new L10N(ZeroOrMoreItem.class);

  private Item _item;

  public ZeroOrMoreItem(Item item)
  {
    _item = item;
  }

  /**
   * Returns the item.
   */
  public Item getItem()
  {
    return _item;
  }

  /**
   * Returns the first set, the set of element names possible.
   */
  public void firstSet(HashSet<QName> set)
  {
    _item.firstSet(set);
  }

  /**
   * Returns the first set, the set of element names possible.
   */
  public void requiredFirstSet(HashSet<QName> set)
  {
  }
  
  /**
   * The element always allows
   */
  public boolean allowEmpty()
  {
    return true;
  }
  
  /**
   * Return all possible child items or null
   */
  public Iterator<Item> getItemsIterator()
  {
    return itemIterator(_item);
  }

  /**
   * Returns the next item on the match.
   *
   * @param name the name of the element
   * @param contItem the continuation item
   */
  public Item startElement(QName name)
    throws RelaxException
  {
    Item next = _item.startElement(name);

    if (next == null)
      return null;
    else
      return next.groupContinuation(this);
  }

  /**
   * Returns the first set, the set of attribute names possible.
   */
  public void attributeSet(HashSet<QName> set)
  {
    _item.attributeSet(set);
  }
  
  /**
   * Returns true if the attribute is allowed.
   *
   * @param name the name of the attribute
   * @param value the value of the attribute
   *
   * @return true if the attribute is allowed
   */
  public boolean allowAttribute(QName name, String value)
    throws RelaxException
  {
    return _item.allowAttribute(name, value);
  }
  
  /**
   * Returns true if the element is allowed somewhere in the item.
   * allowsElement is used for error messages to give more information
   * in cases of order dependency.
   *
   * @param name the name of the element
   *
   * @return true if the element is allowed somewhere
   */
  public boolean allowsElement(QName name)
  {
    return _item.allowsElement(name);
  }

  /**
   * Returns the pretty printed syntax.
   */
  public String toSyntaxDescription(int depth)
  {
    return _item.toSyntaxDescription(depth) + "*";
  }

  /**
   * Returns true for an element with simple syntax.
   */
  protected boolean isSimpleSyntax()
  {
    return _item.isSimpleSyntax();
  }

  /**
   * Returns the hash code for the empty item.
   */
  public int hashCode()
  {
    return 17 + _item.hashCode();
  }

  /**
   * Returns true if the object is an empty item.
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    
    if (! (o instanceof ZeroOrMoreItem))
      return false;

    ZeroOrMoreItem item = (ZeroOrMoreItem) o;

    return _item.equals(item._item);
  }

  public String toString()
  {
    return "ZeroOrMoreItem[" + _item + "]";
  }
}

