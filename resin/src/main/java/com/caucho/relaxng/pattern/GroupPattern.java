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
import com.caucho.relaxng.program.GroupItem;
import com.caucho.relaxng.program.Item;
import com.caucho.util.CharBuffer;

import java.util.ArrayList;

/**
 * Relax element pattern
 */
public class GroupPattern extends Pattern {
  private ArrayList<Pattern> _patterns = new ArrayList<Pattern>();

  /**
   * Creates a new element pattern.
   */
  public GroupPattern()
  {
  }

  /**
   * Returns the Relax schema name.
   */
  public String getTagName()
  {
    return "group";
  }

  /**
   * Returns the number of children.
   */
  public int getSize()
  {
    return _patterns.size();
  }

  /**
   * Returns the n-th child.
   */
  public Pattern getChild(int i)
  {
    return _patterns.get(i);
  }

  /**
   * Adds an element.
   */
  public void addChild(Pattern child)
    throws RelaxException
  {
    child.setParent(this);
    child.setElementName(getElementName());

    if (child instanceof GroupPattern) {
      GroupPattern list = (GroupPattern) child;

      for (int i = 0; i < list.getSize(); i++)
        addChild(list.getChild(i));
      
      return;
    }

    _patterns.add(child);

    boolean hasData = false;
    boolean hasElement = false;
    for (int i = 0; i < _patterns.size(); i++) {
      if (_patterns.get(i).hasData())
        hasData = true;
      else if (_patterns.get(i).hasElement())
        hasElement = true;
    }

    if (hasData && hasElement)
      throw new RelaxException(L.l("<data> may not be in a <group>.  Use <text> instead."));
  }

  /**
   * Creates the production item.
   */
  public Item createItem(GrammarPattern grammar)
    throws RelaxException
  {
    if (_patterns.size() == 0)
      return null;

    Item tail = _patterns.get(_patterns.size() - 1).createItem(grammar);

    for (int i = _patterns.size() - 2; i >= 0; i--)
      tail = GroupItem.create(_patterns.get(i).createItem(grammar), tail);
    
    return tail;
  }

  /**
   * Returns a string for the production.
   */
  public String toProduction()
  {
    if (_patterns.size() == 0)
      return "notAllowed";

    CharBuffer cb = new CharBuffer();
    
    for (int i = 0; i < _patterns.size(); i++) {
      if (i != 0)
        cb.append(", ");

      Pattern pattern = _patterns.get(i);

      if (pattern instanceof ChoicePattern &&
          ! (((ChoicePattern) pattern).hasEmpty()))
        cb.append("(" + _patterns.get(i).toProduction() + ")");
      else
        cb.append(_patterns.get(i).toProduction());
    }
    
    return cb.toString();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;

    if (! (o instanceof GroupPattern))
      return false;

    GroupPattern group = (GroupPattern) o;

    if (_patterns.size() != group._patterns.size())
      return false;

    for (int i = 0; i < _patterns.size(); i++)
      if (! _patterns.get(i).equals(group._patterns.get(i)))
        return false;

    return true;
  }

  /**
   * Debugging.
   */
  public String toString()
  {
    return "GroupPattern" + _patterns;
  }
}

