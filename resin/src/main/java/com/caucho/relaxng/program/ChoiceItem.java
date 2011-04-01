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
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Generates programs from patterns.
 */
public class ChoiceItem extends Item {
  protected final static L10N L = new L10N(ChoiceItem.class);

  private ArrayList<Item> _items = new ArrayList<Item>();

  private boolean _allowEmpty = false;

  public ChoiceItem()
  {
  }

  public static Item create(Item left, Item right)
  {
    ChoiceItem choice = new ChoiceItem();
    choice.addItem(left);
    choice.addItem(right);

    return choice.getMin();
  }

  public void addItem(Item item)
  {
    if (item == null)
      return;
    else if (item instanceof EmptyItem) {
      _allowEmpty = true;
      return;
    }
    else if (item instanceof ChoiceItem) {
      ChoiceItem choice = (ChoiceItem) item;

      if (choice._allowEmpty)
        _allowEmpty = true;
      
      for (int i = 0; i < choice._items.size(); i++)
        addItem(choice._items.get(i));

      return;
    }

    for (int i = 0; i < _items.size(); i++) {
      Item subItem = _items.get(i);

      if (item.equals(subItem))
        return;

      if (item instanceof InElementItem &&
          subItem instanceof InElementItem) {
        InElementItem elt1 = (InElementItem) item;
        InElementItem elt2 = (InElementItem) subItem;

        if (elt1.getElementItem().equals(elt2.getElementItem())) {
          subItem = InElementItem.create(elt1.getElementItem(),
                                         create(elt1.getContinuationItem(),
                                                elt2.getContinuationItem()));
          _items.remove(i);
          addItem(subItem);
          return;
        }
      }
      
      if (item instanceof GroupItem
          && subItem instanceof GroupItem) {
        GroupItem group1 = (GroupItem) item;
        GroupItem group2 = (GroupItem) subItem;

        if (group1.getFirst().equals(group2.getFirst())) {
          subItem = GroupItem.create(group1.getFirst(),
                                     create(group1.getSecond(),
                                            group2.getSecond()));
          _items.remove(i);
          addItem(subItem);
          return;
        }
      }
    }

    _items.add(item);
  }

  public Item getMin()
  {
    if (! _allowEmpty && _items.size() == 0)
      return null;
    else if (_allowEmpty && _items.size() == 0)
      return EmptyItem.create();
    else if (_items.size() == 1
             && (! _allowEmpty || _items.get(0).allowEmpty()))
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
   * Returns the first set, the set of element names possible.
   */
  public void requiredFirstSet(HashSet<QName> set)
  {
    if (allowEmpty())
      return;
    
    for (int i = 0; i < _items.size(); i++)
      _items.get(i).requiredFirstSet(set);
  }
  
  /**
   * Allows empty if any item allows empty.
   */
  public boolean allowEmpty()
  {
    if (_allowEmpty)
      return true;
    
    for (int i = 0; i < _items.size(); i++)
      if (_items.get(i).allowEmpty())
        return true;

    return false;
  }

  /**
   * Interleaves a continuation.
   */
  public Item interleaveContinuation(Item cont)
  {
    ChoiceItem item = new ChoiceItem();

    for (int i = 0; i < _items.size(); i++)
      item.addItem(_items.get(i).interleaveContinuation(cont));

    return item.getMin();
  }

  /**
   * Adds an inElement continuation.
   */
  public Item inElementContinuation(Item cont)
  {
    ChoiceItem item = new ChoiceItem();

    for (int i = 0; i < _items.size(); i++)
      item.addItem(_items.get(i).inElementContinuation(cont));

    return item.getMin();
  }

  /**
   * Adds a group continuation.
   */
  public Item groupContinuation(Item cont)
  {
    ChoiceItem item = new ChoiceItem();

    for (int i = 0; i < _items.size(); i++)
      item.addItem(_items.get(i).groupContinuation(cont));

    return item.getMin();
  }
  
  /**
   * Return all possible child items or null
   */
  public Iterator<Item> getItemsIterator()
  {
    if ( _items.size() == 0 )
      return emptyItemIterator();
    else
      return _items.iterator();
  }

  /**
   * Returns the next item on the match.
   */
  public Item startElement(QName name)
    throws RelaxException
  {
    Item result = null;
    ChoiceItem choice = null;

    for (int i = 0; i < _items.size(); i++) {
      Item next = _items.get(i).startElement(name);

      if (next == null) {
      }
      else if (result == null)
        result = next;
      else {
        if (choice == null) {
          choice = new ChoiceItem();
          choice.addItem(result);
        }

        choice.addItem(next);
      }
    }

    if (choice != null)
      return choice.getMin();
    else
      return result;
  }

  /**
   * Returns the first set, the set of attribute names possible.
   */
  public void attributeSet(HashSet<QName> set)
  {
    for (int i = 0; i < _items.size(); i++)
      _items.get(i).attributeSet(set);
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
    for (int i = _items.size() - 1; i >= 0; i--)
      if (_items.get(i).allowAttribute(name, value))
        return true;

    return false;
  }
  
  /**
   * Sets an attribute.
   *
   * @param name the name of the attribute
   * @param value the value of the attribute
   *
   * @return the program for handling the element
   */
  public Item setAttribute(QName name, String value)
    throws RelaxException
  {
    if (! allowAttribute(name, value))
      return this;

    ChoiceItem choice = new ChoiceItem();

    if (_allowEmpty)
      choice.addItem(EmptyItem.create());

    for (int i = _items.size() - 1; i >= 0; i--) {
      Item next = _items.get(i).setAttribute(name, value);

      if (next == null)
        return null;
      
      choice.addItem(next);
    }

    return choice.getMin();
  }

  /**
   * Returns true if the item can match empty.
   */
  public Item attributeEnd()
  {
    ChoiceItem choice = new ChoiceItem();

    if (_allowEmpty)
      choice._allowEmpty = true;

    for (int i = _items.size() - 1; i >= 0; i--) {
      Item next = _items.get(i).attributeEnd();

      if (next == null)
        continue;

      choice.addItem(next);
    }

    if (choice.equals(this))
      return this;
    else
      return choice.getMin();
  }
  
  /**
   * Returns the next item on the match.
   */
  @Override
  public Item text(CharSequence data)
    throws RelaxException
  {
    Item result = null;
    ChoiceItem choice = null;

    for (int i = 0; i < _items.size(); i++) {
      Item next = _items.get(i).text(data);

      if (next == null) {
      }
      else if (result == null)
        result = next;
      else {
        if (choice == null) {
          choice = new ChoiceItem();
          choice.addItem(result);
        }

        choice.addItem(next);
      }
    }

    if (choice != null)
      return choice.getMin();
    else
      return result;
  }
  
  /**
   * Returns the next item when the element closes
   */
  public Item endElement()
    throws RelaxException
  {
    ChoiceItem choice = new ChoiceItem();

    if (_allowEmpty)
      choice._allowEmpty = true;

    for (int i = _items.size() - 1; i >= 0; i--) {
      Item next = _items.get(i).endElement();

      if (next == null)
        continue;

      choice.addItem(next);
    }

    if (choice.equals(this))
      return this;
    else
      return choice.getMin();
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
    
    if (! (o instanceof ChoiceItem))
      return false;

    ChoiceItem choice = (ChoiceItem) o;

    return isSubset(choice) && choice.isSubset(this);
  }

  private boolean isSubset(ChoiceItem item)
  {
    if (_items.size() != item._items.size())
      return false;

    for (int i = 0; i < _items.size(); i++) {
      Item subItem = _items.get(i);

      if (! item._items.contains(subItem))
        return false;
    }

    return true;
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
    for (int i = 0; i < _items.size(); i++) {
      Item subItem = _items.get(i);

      if (subItem.allowsElement(name))
        return true;
    }

    return false;
  }

  /**
   * Returns the pretty printed syntax.
   */
  public String toSyntaxDescription(int depth)
  {
    CharBuffer cb = CharBuffer.allocate();

    if (_items.size() > 1)
      cb.append("(");
    
    boolean isSimple = true;
    for (int i = 0; i < _items.size(); i++) {
      Item item = _items.get(i);
      if (! item.isSimpleSyntax())
        isSimple = false;

      if (i == 0) {
        if (! isSimple)
          cb.append(" ");
      }
      else if (isSimple) {
        cb.append(" | ");
      }
      else {
        addSyntaxNewline(cb, depth);
        cb.append("| ");
      }

      cb.append(item.toSyntaxDescription(depth + 2));
    }

    if (_items.size() > 1)
      cb.append(')');

    if (_allowEmpty)
      cb.append('?');
    
    return cb.close();
  }

  /**
   * Returns true for an element with simple syntax.
   */
  protected boolean isSimpleSyntax()
  {
    return (_items.size() == 1) && _items.get(0).isSimpleSyntax();
  }

  public String toString()
  {
    StringBuffer sb = new StringBuffer();

    sb.append("ChoiceItem[");
    for (int i = 0; i < _items.size(); i++) {
      if (i != 0)
        sb.append(", ");
      sb.append(_items.get(i));
    }

    if (_allowEmpty) {
      sb.append(",empty");
    }

    sb.append("]");

    return sb.toString();
  }
}

