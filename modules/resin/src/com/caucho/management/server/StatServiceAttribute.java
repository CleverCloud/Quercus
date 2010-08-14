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

package com.caucho.management.server;

import java.io.Serializable;

/**
 * A stat attribute
 *
 * <pre>
 * resin:type=StatService
 * </pre>
 */
public class StatServiceAttribute implements java.io.Serializable
{
  // the object name
  private final String _name;
  // the attribute name
  private final String _attribute;
  // the description
  private final String _description;

  /**
   * null-arg constructor for Hessian.
   */
  private StatServiceAttribute()
  {
    _name = null;
    _attribute = null;
    _description = null;
  }

  public StatServiceAttribute(String name,
                              String attribute,
                              String description)
  {
    _name = name;
    _attribute = attribute;
    _description = description;
  }

  public String getName()
  {
    return _name;
  }

  public String getAttribute()
  {
    return _attribute;
  }

  public String getDescription()
  {
    return _description;
  }

  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _name
            + "," + _attribute
            + "," + _description
            + "]");
  }
}
