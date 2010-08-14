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
import com.caucho.relaxng.program.DataItem;
import com.caucho.relaxng.program.Item;

/**
 * Relax empty pattern
 */
public class DataPattern extends Pattern {
  String _type;
  /**
   * Creates a new empty pattern.
   */
  public DataPattern(String type)
  {
    _type = type;
  }

  /**
   * Creates the program (somewhat bogus)
   */
  public Item createItem(GrammarPattern grammar)
    throws RelaxException
  {
    return new DataItem(_type);
  }

  /**
   * Returns a string for the production.
   */
  public String toProduction()
  {
    return "data";
  }

  /**
   * Returns true if it contains a data element.
   */
  public boolean hasData()
  {
    return true;
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;

    if (! (o instanceof DataPattern))
      return false;

    DataPattern pattern = (DataPattern) o;

    return _type.equals(pattern._type);
  }

  /**
   * Debugging.
   */
  public String toString()
  {
    return "DataPattern[" + _type + "]";
  }
}

