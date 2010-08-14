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
import com.caucho.relaxng.program.AnyNameItem;
import com.caucho.relaxng.program.NameClassItem;

/**
 * Relax name pattern
 */
public class AnyNamePattern extends NameClassPattern {
  private NameClassPattern _except;

  private AnyNameItem _item;
  
  /**
   * Creates a new element pattern.
   */
  public AnyNamePattern()
  {
  }

  /**
   * Returns the Relax schema name.
   */
  public String getTagName()
  {
    return "anyName";
  }

  /**
   * Sets the exclude name pattern.
   */
  public void setExcept(NameClassPattern pattern)
  {
    _except = pattern;
  }

  /**
   * Creates the program.
   */
  public NameClassItem createNameItem()
    throws RelaxException
  {
    if (_item == null) {
      AnyNameItem item = new AnyNameItem();

      if (_except != null)
        item.setExcept(_except.createNameItem());

      _item = item;
    }
    
    return _item;
  }

  /**
   * Returns a string for the production.
   */
  public String toProduction()
  {
    return "*";
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;

    if (! (o instanceof AnyNamePattern))
      return false;

    AnyNamePattern pattern = (AnyNamePattern) o;

    if (_except == null)
      return pattern._except == null;
    else
      return _except.equals(pattern._except);
  }

  /**
   * Debugging.
   */
  public String toString()
  {
    return "AnyNamePattern[]";
  }
}

