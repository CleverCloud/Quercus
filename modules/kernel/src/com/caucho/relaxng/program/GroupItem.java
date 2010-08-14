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
import java.util.NoSuchElementException;

/**
 * Generates programs from patterns.
 */
public class GroupItem extends Item {
  protected final static L10N L = new L10N(GroupItem.class);

  private Item _first;
  private Item _second;

  private GroupItem(Item first, Item second)
  {
    _first = first;
    _second = second;
  }

  public static Item create(Item first, Item second)
  {
    if (first == null || second == null)
      return null;
    else if (first instanceof EmptyItem)
      return second;
    else if (second instanceof EmptyItem)
      return first;
    else if (first instanceof GroupItem) {
      GroupItem firstSeq = (GroupItem) first;
      
      return create(firstSeq.getFirst(), create(firstSeq.getSecond(), second));
    }
    else if (first instanceof InElementItem) {
      InElementItem firstElt = (InElementItem) first;

      return InElementItem.create(firstElt.getFirst(),
                                  create(firstElt.getSecond(), second));
    }
    else
      return new GroupItem(first, second);
  }

  Item getFirst()
  {
    return _first;
  }

  Item getSecond()
  {
    return _second;
  }

  /**
   * Returns the first set, the set of element names possible.
   */
  public void firstSet(HashSet<QName> set)
  {
    _first.firstSet(set);
    if (_first.allowEmpty())
      _second.firstSet(set);
  }

  /**
   * Adds to the first set the set of element names required.
   */
  public void requiredFirstSet(HashSet<QName> set)
  {
    if (! _first.allowEmpty())
      _first.requiredFirstSet(set);
    else
      _second.requiredFirstSet(set);
  }
  
  /**
   * Return all possible child items or null
   */
  public Iterator<Item> getItemsIterator()
  {
    if ( _first == null && _second == null )
      return emptyItemIterator();

    return new Iterator<Item>() {
      private int _cnt;

      public boolean hasNext()
      {
        if (_cnt == 0)
          return _first != null || _second != null;
        else if (_cnt == 1)
          return _first != null && _second != null;
        else
          return false;
      }

      public Item next()
      {
        if (!hasNext())
          throw new NoSuchElementException();

        if (_cnt++ == 0)
          return _first != null ? _first : _second;
        else
          return _second;
      }

      public void remove()
      {
        throw new UnsupportedOperationException();
      }
    };
  }


  /**
   * Returns the next item when an element of the given name is returned
   *
   * @param name the name of the element
   * @param contItem the continuation item
   *
   * @return the program for handling the element
   */
  public Item startElement(QName name)
    throws RelaxException
  {
    Item nextHead = _first.startElement(name);

    Item tail = GroupItem.create(nextHead, _second);

    if (_first.allowEmpty())
      return ChoiceItem.create(tail, _second.startElement(name));
    else
      return tail;
  }
  
  /**
   * Returns the next item when some text data is available.
   *
   * @param string the text data
   *
   * @return the program for handling the element
   */
  @Override
  public Item text(CharSequence string)
    throws RelaxException
  {
    Item nextHead = _first.text(string);

    Item tail = GroupItem.create(nextHead, _second);

    if (_first.allowEmpty())
      return ChoiceItem.create(tail, _second.text(string));
    else
      return tail;
  }

  /**
   * Returns the attribute set, the set of attribute names possible.
   */
  public void attributeSet(HashSet<QName> set)
  {
    _first.attributeSet(set);
    _second.attributeSet(set);
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
    return (_first.allowAttribute(name, value) ||
            _second.allowAttribute(name, value));
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
    Item first = _first.setAttribute(name, value);
    Item second = _second.setAttribute(name, value);

    if (first == _first && second == _second)
      return this;
    else if (first == null)
      return second;
    else if (second == null)
      return first;
    else
      return create(first, second);
  }

  /**
   * Returns the next item after the attributes end.
   */
  public Item attributeEnd()
  {
    Item first = _first.attributeEnd();
    Item second = _second.attributeEnd();

    if (first == null || second == null)
      return null;
    else if (first == _first && second == _second)
      return this;
    else
      return create(first, second);
  }
  
  /**
   * Allows empty if both allow empty.
   */
  public boolean allowEmpty()
  {
    return _first.allowEmpty() && _second.allowEmpty();
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
    return _first.allowsElement(name) || _second.allowsElement(name);
  }

  /**
   * Returns the pretty printed syntax.
   */
  public String toSyntaxDescription(int depth)
  {
    if (_second instanceof EmptyItem)
      return _first.toSyntaxDescription(depth);

    ArrayList<Item> items = new ArrayList<Item>();

    Item item = this;
    while (item instanceof GroupItem) {
      GroupItem groupItem = (GroupItem) item;

      items.add(groupItem._first);
      
      item = groupItem._second;
    }

    if (item != null && ! (item instanceof EmptyItem))
      items.add(item);
    
    CharBuffer cb = CharBuffer.allocate();

    cb.append('(');

    boolean isSimple = true;
    for (int i = 0; i < items.size(); i++) {
      item = items.get(i);

      if (i == 0) {
        cb.append(item.toSyntaxDescription(depth + 1));
        isSimple = item.isSimpleSyntax();
      }
      else
        isSimple = addSyntaxItem(cb, item, depth, isSimple);

      if (i + 1 < items.size()) {
        Item next = items.get(i + 1);

        if (next instanceof ZeroOrMoreItem) {
          ZeroOrMoreItem starItem = (ZeroOrMoreItem) next;

          if (starItem.getItem().equals(item)) {
            cb.append("+");
            i++;

            if (i == 1 && i == items.size() - 1) {
              cb.delete(0, 1);

              return cb.close();
            }
          }
        }
      }
    }

    cb.append(')');
    
    return cb.close();
  }

  /**
   * Adds an item to the description.
   */
  private boolean addSyntaxItem(CharBuffer cb, Item item,
                                int depth, boolean isSimple)
  {
    if (! item.isSimpleSyntax())
      isSimple = false;

    if (isSimple) {
      cb.append(", ");
    }
    else {
      cb.append(",");
      addSyntaxNewline(cb, depth + 1);
    }
      
    cb.append(item.toSyntaxDescription(depth + 1));
    
    return isSimple;
  }

  /**
   * Returns true for an element with simple syntax.
   */
  protected boolean isSimpleSyntax()
  {
    return (_second instanceof EmptyItem) && _first.isSimpleSyntax();
  }

  /**
   * Returns the hash code for the empty item.
   */
  public int hashCode()
  {
    return _first.hashCode() * 65521 + _second.hashCode();
  }

  /**
   * Returns true if the object is an empty item.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof GroupItem))
      return false;

    GroupItem seq = (GroupItem) o;

    return _first.equals(seq._first) && _second.equals(seq._second);
  }

  public String toString()
  {
    return "GroupItem[" + _first + ", " + _second + "]";
  }
}

