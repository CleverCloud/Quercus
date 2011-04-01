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
import com.caucho.relaxng.program.Item;
import com.caucho.relaxng.program.ZeroOrMoreItem;

/**
 * Relax one or more pattern
 */
public class ZeroOrMorePattern extends Pattern {
  private GroupPattern _patterns = new GroupPattern();

  
  /**
   * Creates a new zero-or-more pattern.
   */
  public ZeroOrMorePattern()
  {
  }

  /**
   * Creates a new zero-or-more pattern.
   */
  public ZeroOrMorePattern(Pattern pattern)
    throws RelaxException
  {
    _patterns.addChild(pattern);
  }

  /**
   * Returns the Relax schema name.
   */
  public String getTagName()
  {
    return "zeroOrMore";
  }

  /**
   * Adds an element.
   */
  public void addChild(Pattern child)
    throws RelaxException
  {
    _patterns.addChild(child);
  }

  /**
   * returns the group.
   */
  public Pattern getPatterns()
  {
    return _patterns;
  }

  /**
   * Creates the production item.
   */
  public Item createItem(GrammarPattern grammar)
    throws RelaxException
  {
    return new ZeroOrMoreItem(_patterns.createItem(grammar));
  }

  /**
   * Returns a string for the production.
   */
  public String toProduction()
  {
    return "(" + _patterns.toProduction() + ")*";
  }

  public int hashCode()
  {
    return 91 + _patterns.hashCode();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;

    if (! (o instanceof ZeroOrMorePattern))
      return false;

    ZeroOrMorePattern pattern = (ZeroOrMorePattern) o;

    return _patterns.equals(pattern._patterns);
  }

  /**
   * Debugging.
   */
  public String toString()
  {
    return "ZeroOrMorePattern" + _patterns;
  }
}

