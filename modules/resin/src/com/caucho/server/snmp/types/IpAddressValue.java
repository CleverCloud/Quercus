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
 * SNMP IpAddress type.
 */
public class IpAddressValue extends OctetStringValue
{
  /*
   * @param value may be of the form "128.255.255.0" or "\xF0\xFF\xFF\x00"
   */
  public IpAddressValue(String value)
  {
    super(value);
  }
  
  @Override
  public int getType()
  {
    return SnmpValue.IP_ADDRESS;
  }
  
  @Override
  public void toAsn1(StringBuilder sb)
  {
    header(sb, 4);

    int ch;
    int i = 0;

    while (i < _value.length()) {
      int val = 0;

      while (i < _value.length()
             && '0' <= (ch = _value.charAt(i++)) && ch <= '9') {
        val = val * 10 + ch - '0';
      }
      
      sb.append((char) val);
    }
  }
  
  public String toString()
  {
    return "IpAddress[" + _value + "]";
  }
}
