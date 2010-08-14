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

package javax.enterprise.deploy.shared;

/**
 * Enumeration for status actions.
 */
public class ModuleType {
  public static final ModuleType EAR = new ModuleType(0);
  public static final ModuleType EJB = new ModuleType(1);
  public static final ModuleType CAR = new ModuleType(2);
  public static final ModuleType RAR = new ModuleType(3);
  public static final ModuleType WAR = new ModuleType(4);

  private static final String[] NAMES = {
    "ear", "ejb", "car", "rar", "war"
  };
  private static final ModuleType[] VALUES = {
    EAR, EJB, CAR, RAR, WAR
  };

  private int _value;

  /**
   * Creates the action type.
   */
  protected ModuleType(int value)
  {
    _value = value;
  }

  /**
   * Returns the type.
   */
  public int getValue()
  {
    return _value;
  }

  /**
   * Returns the string table for action type.
   */
  protected String []getStringTable()
  {
    return NAMES;
  }

  /**
   * Returns the string table for action type.
   */
  protected ModuleType []getEnumValueTable()
  {
    return VALUES;
  }

  /**
   * Returns an action of the type.
   */
  public static ModuleType getModuleType(int value)
  {
    if (value >= 0 && value <= VALUES.length)
      return VALUES[value];
    else
      return new ModuleType(value);
  }

  /**
   * Returns the lowest integer value.
   */
  protected int getOffset()
  {
    return 0;
  }

  /**
   * Returns the string name.
   */
  public String toString()
  {
    if (_value >= 0 && _value <= NAMES.length)
      return NAMES[_value];
    else
      return String.valueOf(_value);
  }
}

