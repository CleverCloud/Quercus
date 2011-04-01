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

package com.caucho.naming;

import com.caucho.util.L10N;

import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Represents a parsed JNDI name.
 */
public class QName implements Name {
  private static L10N L = new L10N(QName.class);

   // The owning root context.
  protected Context _context;

  // The name items
  private ArrayList<String> _items = new ArrayList<String>();

  /**
   * Creates a root name based on a context.
   *
   * @param context the root context
   */
  public QName(Context context)
  {
    _context = context;
  }

  /**
   * Creates a new name with a single component.
   *
   * @param context the root context
   * @param first the first name component
   */
  public QName(Context context, String first)
  {
    _context = context;
    
    if (first != null)
      _items.add(first);
  }


  /**
   * Creates a new name with two components
   *
   * @param context the root context
   * @param first the first name component
   * @param tail the tail name component
   */
  public QName(Context context, String first, String rest)
  {
    _context = context;
    
    if (first != null)
      _items.add(first);
    if (rest != null)
      _items.add(rest);
  }

  /**
   * Clones the name.
   */
  public Object clone()
  {
    QName name = new QName(_context);

    for (int i = 0; i < _items.size(); i++)
      name._items.add(_items.get(i));

    return name;
  }

  public int size()
  {
    return _items.size();
  }

  public boolean isEmpty()
  {
    return _items.size() == 0;
  }

  public Enumeration getAll()
  {
    return Collections.enumeration(_items);
  }

  public String get(int pos)
  {
    if (pos < _items.size())
      return (String) _items.get(pos);
    else
      return null;
  }

  public Name getPrefix(int posn)
  {
    QName name = new QName(_context);

    for (int i = 0; i < posn; i++)
      name._items.add(_items.get(i));

    return name;
  }
  
  public Name getSuffix(int posn)
  {
    Context subcontext = _context;

    for (int i = 0; i < posn; i++) {
      String item = (String) _items.get(i);
      try {
        Object obj = subcontext.lookup(item);
        if (obj instanceof Context)
          subcontext = (Context) obj;
        else
          break;
      } catch (NamingException e) {
        break;
      }
    }

    QName name = new QName(subcontext);
    for (int i = posn; i < _items.size(); i++) {
      String item = (String) _items.get(i);
      
      name._items.add(_items.get(i));
    }

    return name;
  }

  /**
   * Returns true if the argument is a prefix of the name.
   *
   * @param name the Name to start as a prefix.
   */
  public boolean startsWith(Name name)
  {
    if (name == null)
      return false;
    
    if (size() < name.size())
      return false;

    for (int i = 0; i < name.size(); i++) {
      if (! get(i).equals(name.get(i)))
        return false;
    }

    return true;
  }
  
  public boolean endsWith(Name name)
  {
    if (name == null)
      return false;
    
    int nameSize = name.size();
    if (size() < nameSize)
      return false;

    int offset = size() - nameSize;
    for (int i = 0; i < nameSize; i++)
      if (! get(i + offset).equals(name.get(i)))
        return false;

    return true;
  }

  /**
   * Append a name to the current name.
   *
   * @param suffix the name to add as a suffix
   *
   * @return the modified name
   */
  public Name addAll(Name suffix)
    throws InvalidNameException
  {
    for (int i = 0; i < suffix.size(); i++)
      _items.add(suffix.get(i));
    
    return this;
  }


  /**
   * Insert a name to the current name.
   *
   * @param suffix the name to add as a suffix
   *
   * @return the modified name
   */
  public Name addAll(int posn, Name suffix)
    throws InvalidNameException
  {
    for (int i = 0; i < suffix.size(); i++)
      _items.add(posn, suffix.get(i));

    return this;
  }

  /**
   * Add a component to the tail of the name, returning the name.
   *
   * @param comp the new component to add.
   *
   * @return the modified name
   */
  public Name add(String comp)
    throws InvalidNameException
  {
    _items.add(comp);

    return this;
  }

  /**
   * Add a component at a specific position, returning the name.
   *
   * @return the modified name
   */
  public Name add(int posn, String comp)
    throws InvalidNameException
  {
    _items.add(posn, comp);

    return this;
  }

  public Object remove(int posn)
    throws InvalidNameException
  {
    _items.remove(posn);
    
    return this;
  }

  /**
   * Returns the name's hash code.
   */
  public int hashCode()
  {
    int hashCode = 337;

    for (int i = size() - 1; i >= 0; i--)
      hashCode = 65521 * hashCode + get(i).hashCode();

    return hashCode;
  }

  /**
   * Returns true if the object is an equivalent name.
   *
   * @param obj the object to test for equality.
   */
  public boolean equals(Object obj)
  {
    if (! (obj instanceof Name))
      return false;

    Name name = (Name) obj;

    if (size() != name.size())
      return false;

    for (int i = size() - 1; i >= 0; i--) {
      if (! get(i).equals(name.get(i)))
        return false;
    }

    return true;
  }

  /**
   * Compares the name to another name.
   *
   * @return -1 if less than b, 0 if equal, or 1 if greater chan
   */
  public int compareTo(Object rawB)
  {
    if (! (rawB instanceof Name))
      return -1;

    Name b = (Name) rawB;

    for (int i = 0; i < size(); i++) {
      if (i >= b.size())
        return 1;
      
      String sa = (String) get(i);
      String sb = (String) b.get(i);

      int cmp = sa.compareTo(sb);
      if (cmp != 0)
        return cmp;
    }

    if (size() == b.size())
      return 0;
    else
      return -1;
  }

  /**
   * Converts the name to a string.
   */
  public String toString()
  {
    String name = null;

    for (int i = 0; i < size(); i++) {
      String str = (String) get(i);
      
      if (name != null) {
        try {
          name = _context.composeName(str, name);
        } catch (NamingException e) {
          name = name + "/" + str;
        }
      }
      else
        name = str;
    }

    return name == null ? "" : name;
  }
}
