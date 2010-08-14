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
import com.caucho.util.LruCache;
import com.caucho.xml.QName;

import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Generates programs from patterns.
 */
public class MemoItem extends Item {
  protected final static L10N L = new L10N(MemoItem.class);
  protected final static Logger log
    = Logger.getLogger(MemoItem.class.getName());

  private LruCache<Object,Item> _memoMap;
  private Item _item;

  public MemoItem(Item item, LruCache<Object,Item> memoMap)
  {
    _item = item;
    _memoMap = memoMap;
  }

  public MemoItem(Item item)
  {
    _item = item;
    _memoMap = new LruCache<Object,Item>(256);
  }
  
  /**
   * Adds to the first set, the set of element names possible.
   */
  public void firstSet(HashSet<QName> set)
  {
    _item.firstSet(set);
  }
  
  /**
   * Adds to the first set, the set of element names possible.
   */
  public void requiredFirstSet(HashSet<QName> set)
  {
    _item.requiredFirstSet(set);
  }

  /**
   * Returns true if the item can match empty.
   */
  public boolean allowEmpty()
  {
    return _item.allowEmpty();
  }
  
  /**
   * Return all possible child items or null
   */
  public Iterator<Item> getItemsIterator()
  {
    return itemIterator( _item );
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
    Item item = _item.startElement(name);

    if (item == null)
      return null;
    else if (item == this)
      return this;
    else
      return new MemoItem(item, _memoMap);
  }

  /**
   * Adds to the first set, the set of attribute names possible.
   */
  public void attributeSet(HashSet<QName> set)
  {
    _item.attributeSet(set);
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
    return _item.allowAttribute(name, value);
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
    Item item = _item.setAttribute(name, value);

    if (item == null)
      return null;
    else if (item == this)
      return this;
    else
      return new MemoItem(item, _memoMap);
  }

  /**
   * Returns true if the item can match empty.
   */
  public Item attributeEnd()
  {
    Item item = _item.attributeEnd();
    
    if (item == null)
      return null;
    else if (item == this)
      return this;
    else
      return new MemoItem(item, _memoMap);
  }

  /**
   * Adds text.
   */
  @Override
  public Item text(CharSequence text)
    throws RelaxException
  {
    Item item = _item.text(text);
    
    if (item == null)
      return null;
    else if (item == this)
      return this;
    else
      return new MemoItem(item, _memoMap);
  }
  
  /**
   * Returns the next item when the element closes
   */
  public Item endElement()
    throws RelaxException
  {
    Item item = _item.endElement();
    
    if (item == null)
      return null;
    else if (item == this)
      return this;
    else
      return new MemoItem(item, _memoMap);
  }
}

