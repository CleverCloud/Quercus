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
import com.caucho.relaxng.program.NameClassItem;
import com.caucho.relaxng.program.NameItem;
import com.caucho.xml.QName;

/**
 * Relax name pattern
 */
public class NamePattern extends NameClassPattern {
  private final QName _name;
  private final NameItem _item;

  /**
   * Creates a new element pattern.
   */
  public NamePattern(QName name)
  {
    _name = name;
    _item = new NameItem(_name);
  }

  /**
   * Returns the Relax schema name.
   */
  public String getTagName()
  {
    return "name";
  }

  /**
   * Creates the program.
   */
  public NameClassItem createNameItem()
    throws RelaxException
  {
    return _item;
  }

  /**
   * Returns a string for the production.
   */
  public String toProduction()
  {
    return _name.getName();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;

    if (! (o instanceof NamePattern))
      return false;

    NamePattern elt = (NamePattern) o;

    return _name.equals(elt._name);
  }

  /**
   * Debugging.
   */
  public String toString()
  {
    return "Name[" + _name.getName() + "]";
  }
}

