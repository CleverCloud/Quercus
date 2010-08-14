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
 * SNMP type
 */
abstract public class SnmpValue
{
  public static final int EOC = 0x00;
  public static final int INTEGER = 0x02;
  public static final int OCTET_STRING = 0x04;
  public static final int NULL = 0x05;
  public static final int OBJECT_IDENTIFIER = 0x06;
  public static final int SEQUENCE = 0x30;
  public static final int IP_ADDRESS = 0x40;
  public static final int COUNTER = 0x41;
  public static final int GAUGE = 0x42;
  public static final int TIME_TICKS = 0x43;
  public static final int OPAQUE = 0x44;
  public static final int GET_REQUEST_PDU = 0xA0;
  public static final int GET_NEXT_REQUEST_PDU = 0xA1;
  public static final int GET_RESPONSE_PDU = 0xA2;
  public static final int SET_REQUEST_PDU = 0xA3;
  public static final int TRAP_PDU = 0xA4;
  
  abstract public void toAsn1(StringBuilder sb);
  
  abstract public int getType();

  public static SnmpValue create(Object obj)
  {
    if (obj instanceof Number)
      return new IntegerValue(((Number) obj).intValue());
    else if (obj instanceof Boolean)
      return new IntegerValue(((Boolean) obj).booleanValue() ? 1 : 0);
    else
      return new OctetStringValue(obj.toString());
  }
  
  public static SnmpValue create(Object obj, String typeStr)
  {
    if (typeStr == null)
      return create(obj);
    
    int type = SnmpValue.OCTET_STRING;
    
    if (typeStr.equalsIgnoreCase("INTEGER"))
      type = INTEGER;
    else if (typeStr.equalsIgnoreCase("TIME_TICKS"))
      type = TIME_TICKS;
    else if (typeStr.equalsIgnoreCase("GAUGE"))
      type = GAUGE;
    else if (typeStr.equalsIgnoreCase("COUNTER"))
      type = COUNTER;
    else if (typeStr.equalsIgnoreCase("OBJECT_IDENTIFIER"))
      type = OBJECT_IDENTIFIER;
    
    return create(obj, type);
  }

  public static SnmpValue create(Object obj, int type)
  {
    if (obj instanceof Number) {
      switch (type) {
        case INTEGER:
          return new IntegerValue(((Number) obj).intValue());
        case COUNTER:
          return new CounterValue(((Number) obj).longValue());
        case GAUGE:
          return new GaugeValue(((Number) obj).longValue());
        case TIME_TICKS:
          // time ticks are in hundredths of a second
          return new TimeTicksValue(((Number) obj).longValue() / 10);
      }
    }
    
    switch (type) {
      case OBJECT_IDENTIFIER:
        return new ObjectIdentifierValue(obj.toString());
      case IP_ADDRESS:
        return new IpAddressValue(obj.toString());
    }
    
    return new OctetStringValue(obj.toString());
  }
  
  public final String toAsn1()
  {
    StringBuilder sb = new StringBuilder();
    
    toAsn1(sb);
    
    return sb.toString();
  }

  final protected void header(StringBuilder sb, int len)
  {
    sb.append((char) getType());
    
    if (len < 0x7F) {
      sb.append((char) len);
      return;
    }

    int bytes = 1;
    
    if ((len >> 24) > 0)
      bytes = 4;
    else if ((len >> 16) > 0)
      bytes = 3;
    else if ((len >> 8) > 0)
      bytes = 2;
    
    // size of length field
    sb.append((char) (0x80 | bytes));
    
    while (bytes > 0) {
      switch (bytes) {
        case 1:
          sb.append((char) (len & 0xFF));
          break;
        case 2:
          sb.append((char) ((len >> 8) & 0xFF));
          break;
        case 3:
          sb.append((char) ((len >> 16) & 0xFF));
          break;
        case 4:
          sb.append((char) ((len >> 24) & 0xFF));
          break;
      }
      
      bytes--;
    }

  }
  
  public long getLong()
  {
    throw new UnsupportedOperationException();
  }

  public String getString()
  {
    throw new UnsupportedOperationException();
  }
  
  public static String typeName(int identifier)
  {
    switch(identifier) {
      case EOC:
        return "EOC";
      case INTEGER:
        return "INTEGER";
      case OCTET_STRING:
        return "OCTET_STRING";
      case NULL:
        return "NULL";
      case OBJECT_IDENTIFIER:
        return "OBJECT_IDENTIFIER";
      case SEQUENCE:
        return "SEQUENCE";
      case IP_ADDRESS:
        return "IP_ADDRESS";
      case COUNTER:
        return "COUNTER";
      case GAUGE:
        return "GAUGE";
      case TIME_TICKS:
        return "TIME_TICKS";
      case OPAQUE:
        return "OPAQUE";
      case GET_REQUEST_PDU:
        return "GET_REQUEST_PDU";
      case GET_NEXT_REQUEST_PDU:
        return "GET_NEXT_REQUEST_PDU";
      case GET_RESPONSE_PDU:
        return "GET_RESPONSE_PDU";
      case SET_REQUEST_PDU:
        return "SET_REQUEST_PDU";
      case TRAP_PDU:
        return "TRAP_PDU";
      default:
        return "UNKNOWN";
    }
  }
  
}
