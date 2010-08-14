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
 * @author Sam 
 */

package javax.portlet;

public class PortletMode
{
  public final static PortletMode VIEW = new PortletMode("view");
  public final static PortletMode EDIT = new PortletMode("edit");
  public final static PortletMode HELP = new PortletMode("help");

  private String _name;

  public PortletMode(String name)
  {
    if (name==null) {
      throw new IllegalArgumentException("PortletMode name can not be NULL");
    }
    _name = name.toLowerCase();
  }

  public String toString()
  {
    return _name;
  }

  public int hashCode()
  {
    return _name.hashCode();
  }

  public boolean equals(Object o)
  {
    if ( o instanceof PortletMode )
      return _name.equals(((PortletMode) o)._name);
    else
      return false;
  }
}

