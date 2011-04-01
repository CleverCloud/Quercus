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
import com.caucho.util.L10N;
import com.caucho.xml.QName;

import java.util.HashSet;

/**
 * Generates programs from patterns.
 */
public class AttributeItem extends Item {
  protected final static L10N L = new L10N(AttributeItem.class);

  private final NameClassItem _name;

  public AttributeItem(NameClassItem name)
  {
    _name = name;
  }

  public NameClassItem getNameClassItem()
  {
    return _name;
  }

  /**
   * Returns the first set, the set of element names possible.
   */
  public void firstSet(HashSet<QName> set)
  {
  }
  
  /**
   * The attribute does not allow the empty match.
   */
  public boolean allowEmpty()
  {
    return false;
  }

  /**
   * Returns the attribute set, the set of attribute names possible.
   */
  public void attributeSet(HashSet<QName> set)
  {
    _name.firstSet(set);
  }
  
  /**
   * Returns true if the attribute is allowed.
   *
   * @param name the name of the attribute
   */
  public boolean allowAttribute(QName name, String value)
    throws RelaxException
  {
    return _name.matches(name);
  }
  
  /**
   * Returns the next item on the match.
   *
   * @param name the name of the attribute
   * @param value the value of the attribute
   */
  public Item setAttribute(QName name, String value)
    throws RelaxException
  {
    if (_name.matches(name))
      return null;
    else
      return this;
  }

  /**
   * Returns the item after the attribute ends.  In this case,
   * return null since this attribute is still required.
   */
  public Item attributeEnd()
  {
    return null;
  }

  /**
   * Returns the pretty printed syntax.
   */
  public String toSyntaxDescription(int depth)
  {
    return _name.toSyntaxDescription("@");
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
    return 27 + _name.hashCode();
  }

  /**
   * Returns true if the object is an empty item.
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    
    if (! (o instanceof AttributeItem))
      return false;

    AttributeItem attr = (AttributeItem) o;

    return _name.equals(attr._name);
  }

  public String toString()
  {
    return "AttributeItem[" + _name + "]";
  }
}

