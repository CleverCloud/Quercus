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
public class NsNameItem extends NameClassItem {
  private String _ns;

  private NameClassItem _except;

  public NsNameItem(String ns)
  {
    _ns = ns;
  }

  public void setExcept(NameClassItem except)
  {
    _except = except;
  }
    
  /**
   * Adds to the first set, the set of element names possible.
   */
  public void firstSet(HashSet<QName> set)
  {
    set.add(new QName("*", _ns));
  }

  /**
   * Returns true if the name matches.
   */
  public boolean matches(QName name)
  {
    if (! _ns.equals(name.getNamespaceURI()))
      return false;
    else if (_except != null && _except.matches(name))
      return false;
    else
      return true;
  }

  /**
   * Returns the pretty printed syntax.
   */
  public String toSyntaxDescription(String prefix)
  {
    if (_except != null) {
      if (prefix.equals(""))
        return "<{" + _ns + "}:* -" + _except.toSyntaxDescription(" ") + ">";
      else
        return prefix + "(" + "{" + _ns + "}:* -" + _except.toSyntaxDescription(" ") + ")";
    }
    else if (prefix.equals(""))
      return "<{" + _ns + "}:*>";
    else
      return prefix + "{" + _ns + "}:*";
  }

  public int hashCode()
  {
    return _ns.hashCode();
  }
  
  public boolean equals(Object o)
  {
    if (this == o)
      return true;

    if (! (o instanceof NsNameItem))
      return false;

    NsNameItem name = (NsNameItem) o;

    if (! _ns.equals(name._ns))
      return false;

    if (_except == null)
      return name._except == null;
    else
      return _except.equals(name._except);
  }
}

