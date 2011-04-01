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
import com.caucho.util.L10N;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Relax grammar pattern
 */
public class GrammarPattern extends Pattern {
  protected static final L10N L = new L10N(GrammarPattern.class);
  
  private Pattern _start;
  private int _id;

  private HashMap<String,Pattern> _definitions = new HashMap<String,Pattern>();

  /**
   * Creates a new grammar pattern.
   */
  public GrammarPattern()
  {
  }

  /**
   * Returns the Relax schema name.
   */
  public String getTagName()
  {
    return "grammar";
  }

  /**
   * Returns the start pattern.
   */
  public Pattern getStart()
  {
    return _start;
  }

  /**
   * Sets the start element
   */
  public void setStart(Pattern start)
    throws RelaxException
  {
    if (_start != null)
      throw new RelaxException(L.l("Duplicate <start> in <grammar>.  The <grammar> element can only have one <start>."));

    _start = start;
  }

  /**
   * Generates a name.
   */
  public String generateId()
  {
    return "__caucho_" + _id++;
  }

  /**
   * Start definition.
   */
  public void setDefinition(String name, Pattern pattern)
  {
    _definitions.put(name, pattern);
  }

  /**
   * Gets a definition.
   */
  public Pattern getDefinition(String name)
  {
    return _definitions.get(name);
  }

  /**
   * Merges an include.
   */
  public void mergeInclude(GrammarPattern grammar)
  {
    _definitions.putAll(grammar._definitions);
  }

  /**
   * Returns equals.
   */
  public boolean equals(Object o)
  {
    return this == o;
  }

  /**
   * Debugging.
   */
  public String toString()
  {
    return "GrammarPattern[" + _start + "]";
  }
}

