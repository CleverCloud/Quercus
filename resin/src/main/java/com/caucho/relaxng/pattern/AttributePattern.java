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

package com.caucho.relaxng.pattern;

import com.caucho.relaxng.RelaxException;
import com.caucho.relaxng.program.AttributeItem;
import com.caucho.relaxng.program.Item;

/**
 * Relax attribute pattern
 */
public class AttributePattern extends Pattern {

  private NameClassPattern _name;
  private Pattern _children;

  private Item _item;

  /**
   * Creates a new attribute pattern.
   */
  public AttributePattern()
  {
  }

  /**
   * Returns the Relax schema name.
   */
  public String getTagName()
  {
    return "attribute";
  }

  /**
   * Returns the children pattern.
   */
  /*
  public GroupPattern getChildren()
  {
    return _children;
  }
  */

  /**
   * Adds an element.
   */
  public void addNameChild(NameClassPattern child)
    throws RelaxException
  {
    _name = child;
    setElementName(_name.toProduction());
  }

  /**
   * get the name child
   */
  public NameClassPattern getNameChild()
    throws RelaxException
  {
    return _name;
  }

  /**
   * Adds an attribute.
   */
  public void addChild(Pattern child)
    throws RelaxException
  {
    if (_name == null)
      throw new RelaxException(L.l("<attribute> must have a <name> definition before any children."));
    
    child.setParent(_children);
    // XXX: (group always null?)
    // child.setElementName(_children.getElementName());

    if (_children == null)
      _children = child;
    else if (_children instanceof GroupPattern) {
      GroupPattern group = (GroupPattern) _children;
      group.addChild(child);
    }
    else {
      GroupPattern group = new GroupPattern();
      group.addChild(_children);
      group.addChild(child);
      _children = group;
    }
  }
  
  /**
   * Ends the element.
   */
  public void endElement()
    throws RelaxException
  {
    if (_name == null)
      throw new RelaxException(L.l("<attribute> must have a <name> definition."));
  }

  /**
   * Creates the program (somewhat bogus)
   */
  public Item createItem(GrammarPattern grammar)
    throws RelaxException
  {
    if (_item == null)
      _item = new AttributeItem(_name.createNameItem());
    
    return _item;
  }

  /**
   * Returns a string for the production.
   */
  public String toProduction()
  {
    return "@" + _name.toProduction();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;

    if (! (o instanceof AttributePattern))
      return false;

    AttributePattern elt = (AttributePattern) o;

    if (! _name.equals(elt._name))
      return false;
    else if (_children == elt._children)
      return true;
    else if (_children == null || elt._children == null)
      return false;
    else
      return _children.equals(elt._children);
  }

  /**
   * Debugging.
   */
  public String toString()
  {
    return "AttributePattern[" + _name.toProduction() + "]";
  }
}

