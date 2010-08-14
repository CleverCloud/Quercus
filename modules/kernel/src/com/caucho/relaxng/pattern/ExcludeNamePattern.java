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

/**
 * Relax element pattern
 */
public class ExcludeNamePattern extends NameClassPattern {
  private NameClassPattern _pattern;

  /**
   * Creates a new choice pattern.
   */
  public ExcludeNamePattern()
  {
  }

  /**
   * Returns the child.
   */
  public NameClassPattern getNameChild()
  {
    return _pattern;
  }
  
  /**
   * Adds an element.
   */
  public void addNameChild(NameClassPattern child)
    throws RelaxException
  {
    if (_pattern != null)
      throw new RelaxException(L.l("<exclude> must have a single child."));

    _pattern = child;
  }

  /**
   * Returns the Relax schema name.
   */
  public String getTagName()
  {
    return "exclude";
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;

    if (! (o instanceof ExcludeNamePattern))
      return false;

    ExcludeNamePattern exclude = (ExcludeNamePattern) o;

    return _pattern.equals(exclude._pattern);
  }

  /**
   * Debugging.
   */
  public String toString()
  {
    return "ExcludeNamePattern" + _pattern;
  }
}

