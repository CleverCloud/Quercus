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
import com.caucho.relaxng.program.InterleaveItem;
import com.caucho.relaxng.program.Item;
import com.caucho.util.CharBuffer;

import java.util.ArrayList;

/**
 * Relax element pattern
 */
public class InterleavePattern extends Pattern {
  private ArrayList<Pattern> _patterns = new ArrayList<Pattern>();

  private Item _item;

  /**
   * Creates a new interleave pattern.
   */
  public InterleavePattern()
  {
  }

  /**
   * Returns the Relax schema name.
   */
  public String getTagName()
  {
    return "interleave";
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
   * Returns true if it contains a data element.
   */
  public boolean hasData()
  {
    for (int i = 0; i < _patterns.size(); i++) {
      if (_patterns.get(i).hasData())
        return true;
    }

    return false;
  }

  /**
   * Returns true if it contains a data element.
   */
  public boolean hasElement()
  {
    for (int i = 0; i < _patterns.size(); i++) {
      if (_patterns.get(i).hasElement())
        return true;
    }

    return false;
  }

  /**
   * Adds an element.
   */
  public void addChild(Pattern child)
    throws RelaxException
  {
    if (child instanceof DataPattern)
      throw new RelaxException(L.l("<data> or <string> may not be used with interleave.  Use <text> instead."));

    if (child instanceof EmptyPattern)
      throw new RelaxException(L.l("<empty> is not allowed as a child of <interleave>"));
    
    child.setParent(this);
    child.setElementName(getElementName());

    if (child instanceof InterleavePattern) {
      InterleavePattern interleave = (InterleavePattern) child;

      for (int i = 0; i < interleave.getSize(); i++)
        addChild(interleave.getChild(i));
      
      return;
    }

    if (_patterns.contains(child))
      return;
    
    _patterns.add(child);
  }

  /**
   * Creates the production item.
   */
  public Item createItem(GrammarPattern grammar)
    throws RelaxException
  {
    if (_item == null) {
      InterleaveItem item = new InterleaveItem();

      for (int i = 0; i < _patterns.size(); i++) {
        item.addItem(_patterns.get(i).createItem(grammar));
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
        cb.append(" & ");
      cb.append(_patterns.get(i).toProduction());
    }
    
    return cb.toString();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;

    if (! (o instanceof InterleavePattern))
      return false;

    InterleavePattern interleave = (InterleavePattern) o;

    if (_patterns.size() != interleave._patterns.size())
      return false;

    return isSubset(interleave) && interleave.isSubset(this);
  }

  private boolean isSubset(InterleavePattern item)
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
    return "InterleavePattern" + _patterns;
  }
}

