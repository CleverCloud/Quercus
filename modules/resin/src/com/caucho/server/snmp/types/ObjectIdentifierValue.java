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
 * SNMP ObjectIdentifier type.
 */
public class ObjectIdentifierValue extends SnmpValue
{
  protected String _id;

  /*
   * @param id is in dot notation (i.e. "1.2.3.4.5")
   */
  public ObjectIdentifierValue(String id)
  {
    _id = id;
  }
  
  @Override
  public int getType()
  {
    return SnmpValue.OBJECT_IDENTIFIER;
  }
  
  @Override
  public String getString()
  {
    return _id;
  }
  
  @Override
  public void toAsn1(StringBuilder sb)
  {
    StringBuilder innerSb = new StringBuilder();

    int x = 0;
    
    char ch;
    int i = 0;
    while (i < _id.length() && (ch = _id.charAt(i++)) != '.') {
      x = x * 10 + ch - '0';
    }

    int y = 0;
    
    while (i < _id.length() && (ch = _id.charAt(i++)) != '.') {
      y = y * 10 + ch - '0';
    }

    encodeInteger(innerSb, x * 40 + y);
    
    while (i < _id.length()) {
      int val = 0;
      
      while (i < _id.length() && (ch = _id.charAt(i++)) != '.') {
        val = val * 10 + ch - '0';
      }
      
      encodeInteger(innerSb, val);
    }
    
    header(sb, innerSb.length());
    
    sb.append(innerSb);
  }
  
  private void encodeInteger(StringBuilder sb, int val)
  {
    int len = 1;
    
    int total = val;
    while ((total >>= 7) > 0) {
      len++;
    }
    
    for (int i = 1; i < len; i++) {
      int shift = (len - i) * 7;
      sb.append((char) ((val >> shift) & 0x7F | 0x80));
    }
    
    sb.append((char) (val & 0x7F));
  }
  
  public String toString()
  {
    return "ObjectIdentifier[" + _id + "]";
  }

}
