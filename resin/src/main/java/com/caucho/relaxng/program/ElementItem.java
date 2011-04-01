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

package com.caucho.relaxng.program;

import com.caucho.relaxng.RelaxException;
import com.caucho.relaxng.pattern.ElementPattern;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Generates programs from patterns.
 */
public class ElementItem extends Item {
  protected final static L10N L = new L10N(ElementItem.class);

  private ElementPattern _element;

  private NameClassItem _nameItem;
  private Item _item;
  private Item _childrenItem;

  public ElementItem(ElementPattern element, NameClassItem nameItem)
  {
    _element = element;
    _nameItem = nameItem;
  }

  public NameClassItem getNameClassItem()
  {
    return _nameItem;
  }

  public void setChildrenItem(Item item)
  {
    _childrenItem = item;
  }

  /**
   * Returns the first set, the set of element names possible.
   */
  public void firstSet(HashSet<QName> set)
  {
    _nameItem.firstSet(set);
  }

  /**
   * Returns the first set, the set of element names possible.
   */
  public void requiredFirstSet(HashSet<QName> set)
  {
    _nameItem.firstSet(set);
  }
  
  /**
   * The element does not allow the empty match.
   */
  public boolean allowEmpty()
  {
    return false;
  }
  
  /**
   * Return all possible child items or null
   */
  public Iterator<Item> getItemsIterator()
  {
    if (_item == null) {
      _item = InElementItem.create(_childrenItem,
                                   EmptyItem.create());
    }

    return itemIterator( _item );
  }


  /**
   * Returns the next item on the match.
   *
   * @param name the name of the element
   */
  public Item startElement(QName name)
    throws RelaxException
  {
    if (! _nameItem.matches(name))
      return null;

    if (_item == null) {
      _item = InElementItem.create(_childrenItem,
                                   EmptyItem.create());
    }

    return _item;
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
    return _nameItem.matches(name);
  }

  /**
   * Returns the pretty printed syntax.
   */
  public String toSyntaxDescription(int depth)
  {
    return _nameItem.toSyntaxDescription("");
  }

  /**
   * Returns true for an element with simple syntax.
   */
  protected boolean isSimpleSyntax()
  {
    return true;
  }

  /**
   * Returns the hash code for the empty item.
   */
  public int hashCode()
  {
    return 87 + _element.getDefName().hashCode();
  }

  /**
   * Returns true if the object is an empty item.
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    
    if (! (o instanceof ElementItem))
      return false;

    ElementItem elt = (ElementItem) o;

    return _element.getDefName().equals(elt._element.getDefName());
  }

  public String toString()
  {
    return "ElementItem[" + _nameItem + "]";
  }
}

