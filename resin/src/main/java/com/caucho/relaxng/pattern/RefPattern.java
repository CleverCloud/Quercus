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

/**
 * Relax reference pattern
 */
public class RefPattern extends Pattern {
  private GrammarPattern _grammar;
  
  private String _refName;

  /**
   * Creates a new element pattern.
   */
  public RefPattern(GrammarPattern grammar, String refName)
  {
    _grammar = grammar;
    
    _refName = refName;
  }

  /**
   * Returns the Relax schema name.
   */
  public String getTagName()
  {
    return "ref";
  }

  /**
   * Returns the definition name.
   */
  public String getRefName()
  {
    return _refName;
  }

  /**
   * Creates the item.
   */
  public Item createItem(GrammarPattern grammar)
    throws RelaxException
  {
    Pattern pattern = grammar.getDefinition(_refName);

    if (pattern == null) {
      // XXX: line #
      throw error(L.l("<ref name=\"{0}\"/> is an unknown reference.",
                      _refName));
    }

    for (Pattern ptr = this;
         ptr != null && ! (ptr instanceof ElementPattern);
         ptr = ptr.getParent()) {
      if (ptr == pattern) {
        throw error(L.l("<define name=\"{0}\"/> calls itself recursively in a <ref/>.",
                        _refName));
      }
    }

    return pattern.createItem(grammar);
  }

  /**
   * Returns a string for the production.
   */
  public String toProduction()
  {
    Pattern pattern = _grammar.getDefinition(_refName);

    return pattern.toProduction();
  }

  /**
   * Returns true if the pattern equals.
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;

    if (! (o instanceof Pattern))
      return false;

    return o.equals(_grammar.getDefinition(_refName));
  }

  /**
   * Debugging.
   */
  public String toString()
  {
    return "RefPattern[" + _refName + "]";
  }
}

