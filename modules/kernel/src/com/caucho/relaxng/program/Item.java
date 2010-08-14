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

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

/**
 * Generates programs from patterns.
 */
abstract public class Item {
  protected final static L10N L = new L10N(Item.class);
  protected final static Logger log
    = Logger.getLogger(Item.class.getName());

  private static final Iterator<Item> EMPTY_ITEM_ITERATOR;
  
  /**
   * Adds to the first set the set of element names possible.
   */
  public void firstSet(HashSet<QName> set)
  {
  }

  /**
   * Adds to the first set the set of element names required.
   */
  public void requiredFirstSet(HashSet<QName> set)
  {
    if (! allowEmpty())
      firstSet(set);
  }

  /**
   * Returns true if the item can match empty.
   */
  public boolean allowEmpty()
  {
    return false;
  }
  
  /**
   * Return all possible child items
   */
  public Iterator<Item> getItemsIterator()
  {
    return emptyItemIterator();
  }

  protected Iterator<Item> emptyItemIterator()
  {
    return EMPTY_ITEM_ITERATOR;
  }

  protected Iterator<Item> itemIterator( final Item item )
  {
    if (item == null)
      return emptyItemIterator();

    return new Iterator<Item>() {
      private boolean _done;

      public boolean hasNext()
      {
        return !_done;
      }

      public Item next()
      {
        if ( ! hasNext() )
          throw new NoSuchElementException();

        _done = true;

        return item;
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
   *
   * @return the program for handling the element
   */
  public Item startElement(QName name)
    throws RelaxException
  {
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
    return false;
  }

  /**
   * Adds to the first set, the set of attribute names possible.
   */
  public void attributeSet(HashSet<QName> set)
  {
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
    return this;
  }

  /**
   * Returns true if the item can match empty.
   */
  public Item attributeEnd()
  {
    return this;
  }

  /**
   * Adds text.
   */
  public Item text(CharSequence text)
    throws RelaxException
  {
    return null;
  }
  
  /**
   * Returns the next item when the element closes
   */
  public Item endElement()
    throws RelaxException
  {
    if (allowEmpty())
      return EmptyItem.create();
    else
      return null;
  }

  /**
   * Interleaves a continuation.
   */
  public Item interleaveContinuation(Item cont)
  {
    throw new IllegalStateException(String.valueOf(getClass().getName()));
  }

  /**
   * Interleaves a continuation.
   */
  public Item inElementContinuation(Item cont)
  {
    throw new IllegalStateException(String.valueOf(getClass().getName()));
  }

  /**
   * Appends a group to a continuation.
   */
  public Item groupContinuation(Item cont)
  {
    throw new IllegalStateException(String.valueOf(getClass().getName()));
  }

  /**
   * Returns the pretty printed syntax.
   */
  public String toSyntaxDescription(int depth)
  {
    return toString();
  }

  /**
   * Returns true for an element with simple syntax.
   */
  protected boolean isSimpleSyntax()
  {
    return false;
  }

  /**
   * Adds a syntax newline.
   */
  protected void addSyntaxNewline(CharBuffer cb, int depth)
  {
    cb.append('\n');
    for (int i = 0; i < depth; i++)
      cb.append(' ');
  }

  /**
   * Throws an error.
   */
  protected RelaxException error(String msg)
  {
    return new RelaxException(msg);
  }

  static {
    EMPTY_ITEM_ITERATOR = 
      new Iterator<Item>() {
        public boolean hasNext()
        {
          return false;
        }

        public Item next()
        {
          throw new NoSuchElementException();
        }

        public void remove()
        {
          throw new UnsupportedOperationException();
        }
      };
  }
}

