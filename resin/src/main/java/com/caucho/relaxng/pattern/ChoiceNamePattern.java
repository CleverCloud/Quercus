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
import com.caucho.relaxng.program.ChoiceNameItem;
import com.caucho.relaxng.program.NameClassItem;
import com.caucho.util.CharBuffer;

import java.util.ArrayList;

/**
 * Relax element pattern
 */
public class ChoiceNamePattern extends NameClassPattern {
  private ArrayList<NameClassPattern> _patterns
    = new ArrayList<NameClassPattern>();

  private NameClassItem _item;

  /**
   * Creates a new choice pattern.
   */
  public ChoiceNamePattern()
  {
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
  public NameClassPattern getChild(int i)
  {
    return _patterns.get(i);
  }
  
  /**
   * Adds an element.
   */
  public void addNameChild(NameClassPattern child)
    throws RelaxException
  {
    if (child instanceof ChoiceNamePattern) {
      ChoiceNamePattern list = (ChoiceNamePattern) child;

      for (int i = 0; i < list.getSize(); i++)
        addChild(list.getChild(i));
      
      return;
    }

    if (! _patterns.contains(child))
      _patterns.add(child);
  }

  /**
   * Returns the Relax schema name.
   */
  public String getTagName()
  {
    return "choice";
  }

  /**
   * Creates the production item.
   */
  public NameClassItem createNameItem()
    throws RelaxException
  {
    if (_item == null) {
      ChoiceNameItem item = new ChoiceNameItem();

      for (int i = 0; i < _patterns.size(); i++) {
        item.addItem(_patterns.get(i).createNameItem());
      }

      _item = item.getMin();
    }

    return _item;
  }

  /**
   * Returns a string for the production.
   */
  public String toProduction()
  {
    CharBuffer cb = new CharBuffer();

    for (int i = 0; i < _patterns.size(); i++) {
      if (i != 0)
        cb.append(" | ");
      cb.append(_patterns.get(i).toProduction());
    }
    
    return cb.toString();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;

    if (! (o instanceof ChoiceNamePattern))
      return false;

    ChoiceNamePattern choice = (ChoiceNamePattern) o;

    if (_patterns.size() != choice._patterns.size())
      return false;

    return isSubset(choice) && choice.isSubset(this);
  }

  private boolean isSubset(ChoiceNamePattern item)
  {
    if (_patterns.size() != item._patterns.size())
      return false;

    for (int i = 0; i < _patterns.size(); i++) {
      Pattern subPattern = _patterns.get(i);

      if (! item._patterns.contains(subPattern))
        return false;
    }

    return true;
  }

  /**
   * Debugging.
   */
  public String toString()
  {
    return "ChoiceNamePattern" + _patterns;
  }
}

