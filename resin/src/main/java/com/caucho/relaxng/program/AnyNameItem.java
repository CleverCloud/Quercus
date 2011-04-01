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

package com.caucho.relaxng.program;

import com.caucho.xml.QName;

import java.util.HashSet;

/**
 * Matches names.
 */
public class AnyNameItem extends NameClassItem {
  private NameClassItem _except;
  
  public AnyNameItem()
  {
  }

  /**
   * Sets the exception pattern.
   */
  public void setExcept(NameClassItem except)
  {
    _except = except;
  }
    
  /**
   * Adds to the first set, the set of element names possible.
   */
  public void firstSet(HashSet<QName> set)
  {
  }

  /**
   * Returns true if the name matches.
   */
  public boolean matches(QName name)
  {
    if (_except == null)
      return true;
    else
      return ! _except.matches(name);
  }

  /**
   * Returns the pretty printed syntax.
   */
  public String toSyntaxDescription(String prefix)
  {
    if (_except != null) {
      if (prefix.equals(""))
        return "<* -" + _except.toSyntaxDescription(" ") + ">";
      else
        return prefix + "(* -" + _except.toSyntaxDescription(" ") + ")";
    }
    else if (prefix.equals(""))
      return "<*>";
    else
      return prefix + "*";
  }

  public int hashCode()
  {
    return 321;
  }
  
  public boolean equals(Object o)
  {
    if (this == o)
      return true;

    if (! (o instanceof AnyNameItem))
      return false;

    return true;
  }
}

