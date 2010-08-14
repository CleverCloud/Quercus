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

package com.caucho.xpath.expr;

import com.caucho.util.CharBuffer;

/**
 * A variable containing a double
 */
public class NumberVar extends Var {
  private static NumberVar []intVar = new NumberVar[256];
  
  private double value;
  private Double objValue;
  private String strValue;

  /**
   * Creates a new object variable with the object.
   */
  private NumberVar(double value)
  {
    this.value = value;
  }

  /**
   * Creates a new number var with the object.
   */
  public static NumberVar create(double value)
  {
    NumberVar var;

    int index = (int) value;
    
    if (index == value && index > -128 && index < 128) {
      var = intVar[index + 128];
      if (var == null) {
        var = new NumberVar(value);
        intVar[index + 128] = var;
      }

      return var;
    }
    else
      return new NumberVar(value);
  }
  
  /**
   * Returns the value as a double.
   */
  double getDouble()
  {
    return value;
  }
  
  /**
   * Returns the value as an object.
   */
  Object getObject()
  {
    if (objValue == null)
      objValue = new Double(value);
    
    return objValue;
  }
  
  /**
   * Returns the value as a string.
   */
  String getString()
  {
    if (strValue == null) {
      if ((int) value == value) {
        CharBuffer cb = CharBuffer.allocate();
        cb.append((int) value);
        strValue = cb.close();
      }
      else
        strValue =  String.valueOf(value);
    }

    return strValue;
  }
  
  /**
   * Appends the buffer with the string value.
   */
  void getString(CharBuffer cb)
  {
    if ((int) value == value)
      cb.append((int) value);
    else
      cb.append(value);
  }

  /**
   * Clones the variable.
   */
  public Object clone()
  {
    NumberVar var = (NumberVar) NumberVar.create(value);
    var.objValue = objValue;
    var.strValue = strValue;
    
    return var;
  }
  
  public String toString()
  {
    return "[NumberVar " + value + "]";
  }
}
