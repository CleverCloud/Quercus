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

package javax.jcr;

import java.util.HashMap;

public final class PropertyType {
  public static final int STRING = 1;
  public static final int BINARY = 2;
  public static final int LONG = 3;
  public static final int DOUBLE = 4;
  public static final int DATE = 5;
  public static final int BOOLEAN = 6;
  public static final int NAME = 7;
  public static final int PATH = 8;
  public static final int REFERENCE = 9;
  public static final int UNDEFINED = 0;
  
  public static final String TYPENAME_STRING = "String";
  public static final String TYPENAME_BINARY = "Binary";
  public static final String TYPENAME_LONG = "Long";
  public static final String TYPENAME_DOUBLE = "Double";
  public static final String TYPENAME_DATE = "Date";
  public static final String TYPENAME_BOOLEAN = "Boolean";
  public static final String TYPENAME_NAME = "Name";
  public static final String TYPENAME_PATH = "Path";
  public static final String TYPENAME_REFERENCE = "Reference";
  public static final String TYPENAME_UNDEFINED = "undefined";

  private static final HashMap _nameMap = new HashMap();
  
  private PropertyType() {
  }
  
  public static String nameFromValue(int type)
  {
    switch (type) {
    case STRING:
      return TYPENAME_STRING;
      
    case BINARY:
      return TYPENAME_BINARY;
      
    case LONG:
      return TYPENAME_LONG;
      
    case DOUBLE:
      return TYPENAME_DOUBLE;
      
    case DATE:
      return TYPENAME_DATE;
      
    case BOOLEAN:
      return TYPENAME_BOOLEAN;
      
    case NAME:
      return TYPENAME_NAME;
      
    case PATH:
      return TYPENAME_PATH;
      
    case REFERENCE:
      return TYPENAME_REFERENCE;
      
    case UNDEFINED:
      return TYPENAME_UNDEFINED;
      
    default:
      throw new IllegalArgumentException(type + " is an unknown PropertyType");
    }
  }

  /**
   * Returns the numeric constant value of the type with the specified name.
   *
   * @param name the name of the property type
   * @return the numeric constant value
   * @throws IllegalArgumentException if <code>name</code>
   *                                  is not a valid property type name.
   */
  public static int valueFromName(String name)
  {
    Integer value = (Integer) _nameMap.get(name);

    if (value != null)
      return value.intValue();
    else
      throw new IllegalArgumentException(name + " is an unknown PropertyType");
  }

  static {
    _nameMap.put(TYPENAME_STRING, new Integer(STRING));
    _nameMap.put(TYPENAME_BINARY, new Integer(BINARY));
    _nameMap.put(TYPENAME_LONG, new Integer(LONG));
    _nameMap.put(TYPENAME_DOUBLE, new Integer(DOUBLE));
    _nameMap.put(TYPENAME_DATE, new Integer(DATE));
    _nameMap.put(TYPENAME_BOOLEAN, new Integer(BOOLEAN));
    _nameMap.put(TYPENAME_NAME, new Integer(NAME));
    _nameMap.put(TYPENAME_PATH, new Integer(PATH));
    _nameMap.put(TYPENAME_REFERENCE, new Integer(REFERENCE));
    _nameMap.put(TYPENAME_UNDEFINED, new Integer(UNDEFINED));
  }
}
