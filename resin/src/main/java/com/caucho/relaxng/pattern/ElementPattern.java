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

package com.caucho.relaxng.pattern;

import com.caucho.relaxng.RelaxException;
import com.caucho.relaxng.program.ElementItem;
import com.caucho.relaxng.program.Item;

/**
 * Relax element pattern
 */
public class ElementPattern extends Pattern {
  private String _defName;
  
  private NameClassPattern _name;
  //private GroupPattern _children = new GroupPattern();
  private Pattern _children;
  private Item _item;

  /**
   * Creates a new element pattern.
   */
  public ElementPattern(String defName)
  {
    _defName = defName;
  }

  /**
   * Returns the Relax schema name.
   */
  public String getTagName()
  {
    return "element";
  }

  /**
   * Returns the definition name.
   */
  public String getDefName()
  {
    return _defName;
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
   * Returns true if it contains an element.
   */
  public boolean hasElement()
  {
    return true;
  }

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
   * Adds an element.
   */
  public NameClassPattern getNameChild()
    throws RelaxException
  {
    return _name;
  }

  /**
   * Adds an element.
   */
  public void addChild(Pattern child)
    throws RelaxException
  {
    if (_name == null)
      throw new RelaxException(L.l("<element> must have <name> definitions before other children."));
    
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
      throw new RelaxException(L.l("<element> must have a <name> definition."));
    
    if (_children == null)
      throw new RelaxException(L.l("<element> tag '{0}' must have a child grammar production.",
                                   _name.toProduction()));
  }

  /**
   * Creates the item, i.e. program
   */
  public Item createItem(GrammarPattern grammar)
    throws RelaxException
  {
    if (_item == null) {
      ElementItem item = new ElementItem(this, _name.createNameItem());
      _item = item;
      item.setChildrenItem(_children.createItem(grammar));
    }
    
    return _item;
  }

  /**
   * Returns a string for the production.
   */
  public String toProduction()
  {
    return _name.toProduction();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;

    if (! (o instanceof ElementPattern))
      return false;

    ElementPattern elt = (ElementPattern) o;

    return _defName.equals(elt._defName);
  }

  /**
   * Debugging.
   */
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }
}

