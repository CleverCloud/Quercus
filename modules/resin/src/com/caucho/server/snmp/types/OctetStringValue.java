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
 * SNMP OctetString (range 0x00-0xFF) type.
 */
public class OctetStringValue extends SnmpValue
{
  public static final OctetStringValue PUBLIC
    = new OctetStringValue("public");
  
  String _value;
  
  public OctetStringValue(String value)
  {
    _value = value;
  }

  @Override
  public int getType()
  {
    return SnmpValue.OCTET_STRING;
  }
  
  @Override
  public void toAsn1(StringBuilder sb)
  {
    header(sb, _value.length());

    sb.append(_value);
  }

  public String getString()
  {
    return _value;
  }
  
  public String toString()
  {
    return "OctetString['" + _value + "']";
  }
  
  public boolean equals(Object obj)
  {
    if (obj instanceof OctetStringValue)
      return _value.equals(((OctetStringValue) obj)._value);
    else
      return false;
  }
}
