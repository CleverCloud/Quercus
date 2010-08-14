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
 * @author Nam Nguyen
 */

package com.caucho.server.snmp.types;

/*
 * SNMP Integer (signed) type.
 */
public class IntegerValue extends SnmpValue
{
  public static final IntegerValue ZERO = new IntegerValue(0);
  
  int _value;

  public IntegerValue(int value)
  {
    _value = (int) value;
  }
  
  public long getLong()
  {
    return _value;
  }
  
  @Override
  public int getType()
  {
    return SnmpValue.INTEGER;
  }
  
  @Override
  public void toAsn1(StringBuilder sb)
  {
    int len = length();
    
    header(sb, len);
    assert len < 5;

    int value = _value;
    
    for (int i = 0; i < len; i++) {
      value = _value >> ((len - i - 1) * 8);
    
      sb.append((char) (value & 0xFF));
    }
  }
  
  private int length()
  {
    int length = 1;
    int value = _value;
    
    if (value == 0) {
    }
    else {
      while ((value = value / 256) != 0)
        length++;
    }
    
    return length;
  }
  
  public boolean equals(Object obj)
  {
    if (obj instanceof IntegerValue && _value == ((IntegerValue) obj)._value)
      return true;
    else
      return false;
  }
  
  public String toString()
  {
    return String.valueOf(_value);
  }
}
