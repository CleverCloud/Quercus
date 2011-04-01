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
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Generates programs from patterns.
 */
public class InterleaveItem extends Item {
  protected final static L10N L = new L10N(InterleaveItem.class);

  private boolean _allEmpty = true;
  private ArrayList<Item> _items = new ArrayList<Item>();

  public InterleaveItem()
  {
  }

  public static Item create(Item left, Item right)
  {
    InterleaveItem item = new InterleaveItem();

    item.addItem(left);
    item.addItem(right);
    
    return item.getMin();
  }

  public void addItem(Item item)
  {
    if (item == null) {
      _allEmpty = false;
      return;
    }
    else if (item instanceof EmptyItem) {
      return;
    }
    else if (item instanceof InterleaveItem) {
      InterleaveItem interleave = (InterleaveItem) item;

      for (int i = 0; i < interleave._items.size(); i++)
        addItem(interleave._items.get(i));

      return;
    }

    /* XXX: remove for perf?
    for (int i = 0; i < _items.size(); i++) {
      Item subItem = _items.get(i);

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
      
      if (item instanceof GroupItem &&
          subItem instanceof GroupItem) {
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
    */

    _allEmpty = false;
    _items.add(item);
  }

  public Item getMin()
  {
    if (_items.size() == 0)
      return _allEmpty ? EmptyItem.create() : null;
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
   * Only allow empty if all allow empty.
   */
  public boolean allowEmpty()
  {
    for (int i = 0; i < _items.size(); i++) {
      if (! _items.get(i).allowEmpty())
        return false;
    }
      
    return true;
  }

  /**
   * Interleaves a continuation.
   */
  public Item interleaveContinuation(Item cont)
  {
    InterleaveItem item = new InterleaveItem();

    for (int i = 0; i < _items.size(); i++)
      item.addItem(_items.get(i).interleaveContinuation(cont));

    return item.getMin();
  }

  /**
   * Adds an inElement continuation.
   */
  public Item inElementContinuation(Item cont)
  {
    InterleaveItem item = new InterleaveItem();

    for (int i = 0; i < _items.size(); i++)
      item.addItem(_items.get(i).inElementContinuation(cont));

    return item.getMin();
  }

  /**
   * Adds a group continuation.
   */
  public Item groupContinuation(Item cont)
  {
    InterleaveItem item = new InterleaveItem();

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
      Item item = _items.get(i);

      Item nextItem = item.startElement(name);

      if (nextItem == null)
        continue;

      Item resultItem;

      if (nextItem == item)
        resultItem = this;
      else {
        InterleaveItem rest = new InterleaveItem();
        for (int j = 0; j < _items.size(); j++) {
          if (i != j)
            rest.addItem(_items.get(j));
        }

        resultItem = nextItem.interleaveContinuation(rest);
      }
      
      if (result == null)
        result = resultItem;
      else {
        if (choice == null) {
          choice = new ChoiceItem();
          choice.addItem(result);
        }
        choice.addItem(resultItem);
      }
    }

    if (choice != null)
      return choice.getMin();
    else
      return result;
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
   * Returns the first set, the set of attribute names possible.
   */
  public void attributeSet(HashSet<QName> set)
  {
    for (int i = 0; i < _items.size(); i++)
      _items.get(i).attributeSet(set);
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

    InterleaveItem interleave = new InterleaveItem();

    for (int i = _items.size() - 1; i >= 0; i--) {
      Item next = _items.get(i).setAttribute(name, value);

      if (next != null)
        interleave.addItem(next);
    }

    return interleave.getMin();
  }

  /**
   * Returns true if the item can match empty.
   */
  public Item attributeEnd()
  {
    InterleaveItem interleave = new InterleaveItem();

    for (int i = _items.size() - 1; i >= 0; i--) {
      Item next = _items.get(i).attributeEnd();

      if (next == null)
        return null;

      interleave.addItem(next);
    }

    if (interleave.equals(this))
      return this;
    else
      return interleave.getMin();
  }
    
  /**
   * Returns the next item on some text
   */
  @Override
  public Item text(CharSequence string)
    throws RelaxException
  {
    Item result = null;
    ChoiceItem choice = null;

    for (int i = 0; i < _items.size(); i++) {
      Item item = _items.get(i);

      Item nextItem = item.text(string);

      if (nextItem == null)
        continue;

      Item resultItem;

      if (nextItem == item)
        resultItem = this;
      else {
        InterleaveItem rest = new InterleaveItem();
        for (int j = 0; j < _items.size(); j++) {
          if (i != j)
            rest.addItem(_items.get(j));
        }

        resultItem = nextItem.interleaveContinuation(rest);
      }
      
      if (result == null)
        result = resultItem;
      else {
        if (choice == null) {
          choice = new ChoiceItem();
          choice.addItem(result);
        }
        choice.addItem(resultItem);
      }
    }

    if (choice != null)
      return choice.getMin();
    else
      return result;
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
    if (_items.size() == 1)
      return _items.get(0).toSyntaxDescription(depth);
    
    CharBuffer cb = CharBuffer.allocate();

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
        cb.append(" & ");
      }
      else {
        addSyntaxNewline(cb, depth);
        cb.append("& ");
      }
      
      cb.append(item.toSyntaxDescription(depth + 2));
    }

    cb.append(')');

    return cb.close();
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
    
    if (! (o instanceof InterleaveItem))
      return false;

    InterleaveItem interleave = (InterleaveItem) o;

    return isSubset(interleave) && interleave.isSubset(this);
  }

  private boolean isSubset(InterleaveItem item)
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

  public String toString()
  {
    return "InterleaveItem" + _items;
  }
}

