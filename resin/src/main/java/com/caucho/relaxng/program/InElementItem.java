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
 * Represents a match inside an element with a continuation.
 *
 * In the following example, the "c, d" would be eltItem and "e"
 * would be the contItem.
 *
 * <pre>a { b . c, d } e</pre>
 */
public class InElementItem extends Item {
  protected final static L10N L = new L10N(InElementItem.class);

  private final Item _eltItem;
  private final Item _contItem;

  private int _hashCode;

  private InElementItem(Item eltItem, Item contItem)
  {
    _eltItem = eltItem;
    _contItem = contItem;
  }

  public static InElementItem create(Item eltItem, Item contItem)
  {
    if (eltItem == null)
      return null;
    else if (contItem == null)
      return null;
    else
      return new InElementItem(eltItem, contItem);
  }

  public Item getFirst()
  {
    return _eltItem;
  }

  public Item getSecond()
  {
    return _contItem;
  }
  
  public Item getElementItem()
  {
    return _eltItem;
  }

  public Item getContinuationItem()
  {
    return _contItem;
  }

  /**
   * Interleaves a continuation.
   */
  public Item interleaveContinuation(Item cont)
  {
    return create(_eltItem, InterleaveItem.create(cont, _contItem));
  }

  /**
   * Adds a continuation
   */
  public Item inElementContinuation(Item cont)
  {
    return create(_eltItem, create(_contItem, cont));
  }

  /**
   * Adds a continuation
   */
  public Item groupContinuation(Item cont)
  {
    return create(_eltItem, GroupItem.create(_contItem, cont));
  }

  /**
   * Returns the first set, the set of element names possible.
   */
  public void firstSet(HashSet<QName> set)
  {
    _eltItem.firstSet(set);
  }

  /**
   * Returns the first set, the set of element names possible.
   */
  public void requiredFirstSet(HashSet<QName> set)
  {
    _eltItem.requiredFirstSet(set);
  }
  
  /**
   * Allows empty if both allow empty.
   */
  public boolean allowEmpty()
  {
    return _eltItem.allowEmpty();
  }
  

  /**
   * Return all possible child items or null
   */
  public Iterator<Item> getItemsIterator()
  {
    return itemIterator( _eltItem );
  }

  /**
   * Returns the next item when an element of the given name is returned
   *
   * @param name the name of the element
   *
   * @return the program for handling the element
   */
  public Item startElement(QName name)
    throws RelaxException
  {
    Item nextElt = _eltItem.startElement(name);

    if (nextElt == null)
      return null;
    else if (nextElt == _eltItem)
      return this;
    else
      return nextElt.inElementContinuation(_contItem);
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
    Item nextElt = _eltItem.text(string);

    if (nextElt == null)
      return null;
    else if (nextElt == _eltItem)
      return this;
    else
      return create(nextElt, _contItem);
  }

  /**
   * Returns the first set, the set of attribute names possible.
   */
  public void attributeSet(HashSet<QName> set)
  {
    _eltItem.attributeSet(set);
  }
  
  /**
   * Sets an attribute.
   *
   * @param name the name of the attribute
   * @param value the value of the attribute
   *
   * @return the program for handling the element
   */
  public boolean allowAttribute(QName name, String value)
    throws RelaxException
  {
    return _eltItem.allowAttribute(name, value);
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
    Item nextElt = _eltItem.setAttribute(name, value);

    if (nextElt == null)
      return create(EmptyItem.create(), _contItem);
    else if (nextElt == _eltItem)
      return this;
    else
      return create(nextElt, _contItem);
  }

  /**
   * Returns true if the element is allowed to end here.
   */
  public Item attributeEnd()
  {
    Item nextElt = _eltItem.attributeEnd();

    if (nextElt == null) {
      return null;
    }
    else if (nextElt == _eltItem)
      return this;
    else
      return create(nextElt, _contItem);
  }
  
  /**
   * Returns the next item when the element is completes.
   *
   * @return the program for handling the element
   */
  public Item endElement()
    throws RelaxException
  {
    if (_eltItem.allowEmpty())
      return _contItem;
    else
      return null;
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
    return _eltItem.allowsElement(name);
  }

  /**
   * Returns the pretty printed syntax.
   */
  public String toSyntaxDescription(int depth)
  {
    return _eltItem.toSyntaxDescription(depth);
  }

  /**
   * Returns true if the syntax description is simple
   */
  public boolean isSimpleSyntax()
  {
    return _eltItem.isSimpleSyntax();
  }

  /**
   * Returns the hash code for the empty item.
   */
  public int hashCode()
  {
    if (_hashCode == 0)
      _hashCode = calculateHashCode();
    
    return _hashCode;
  }

  /**
   * Returns the hash code for the empty item.
   */
  private int calculateHashCode()
  {
    return _eltItem.hashCode() * 65521 + _contItem.hashCode();
  }

  /**
   * Returns true if the object is an empty item.
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    
    if (! (o instanceof InElementItem))
      return false;

    InElementItem seq = (InElementItem) o;

    return _eltItem.equals(seq._eltItem) && _contItem.equals(seq._contItem);
  }

  public String toString()
  {
    return "InElementItem[" + _eltItem + ",cont=" + _contItem + "]";
  }
}

