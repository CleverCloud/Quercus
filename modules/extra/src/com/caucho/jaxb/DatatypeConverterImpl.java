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
 * @author Emil Ong
 */

package com.caucho.jaxb;

import com.caucho.util.Base64;
import com.caucho.util.L10N;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import javax.xml.bind.DatatypeConverterInterface;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

/**
 */
public class DatatypeConverterImpl implements DatatypeConverterInterface {
  private static final L10N L = new L10N(DatatypeConverterImpl.class);
  private static DatatypeFactory _datatypeFactory = null;

  private final char[] hexDigits = { '0', '1', '2', '3', '4',
                                     '5', '6', '7', '8', '9',
                                     'A', 'B', 'C', 'D', 'E', 'F'};

  private final SimpleDateFormat dateFormat 
    = new SimpleDateFormat("yyyy-MM-dd");
  private final SimpleDateFormat timeFormat 
    = new SimpleDateFormat("HH:mm:ss");
  private final SimpleDateFormat dateTimeFormat
    = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  public DatatypeConverterImpl()
  {
    TimeZone gmt = TimeZone.getTimeZone("GMT");
    dateFormat.getCalendar().setTimeZone(gmt);
    timeFormat.getCalendar().setTimeZone(gmt);
    dateTimeFormat.getCalendar().setTimeZone(gmt);
  }

  //
  // Parsers
  //

  public String parseAnySimpleType(String lexicalXSDAnySimpleType)
  {
    return lexicalXSDAnySimpleType;
  }

  public byte[] parseBase64Binary(String lexicalXSDBase64Binary)
  {
    return Base64.decodeToByteArray(lexicalXSDBase64Binary);
  }

  public boolean parseBoolean(String lexicalXSDBoolean)
  {
    return Boolean.parseBoolean(lexicalXSDBoolean);
  }

  public byte parseByte(String lexicalXSDByte)
  {
    return Byte.parseByte(lexicalXSDByte);
  }

  public Calendar parseDate(String lexicalXSDDate)
    throws IllegalArgumentException
  {
    XMLGregorianCalendar xmlCal = 
      getDatatypeFactory().newXMLGregorianCalendar(lexicalXSDDate);

    return xmlCal.toGregorianCalendar();
  }

  public Calendar parseDateTime(String lexicalXSDDateTime)
    throws IllegalArgumentException
  {
    XMLGregorianCalendar xmlCal = 
      getDatatypeFactory().newXMLGregorianCalendar(lexicalXSDDateTime);

    return xmlCal.toGregorianCalendar();
  }

  public BigDecimal parseDecimal(String lexicalXSDDecimal)
  {
    return new BigDecimal(lexicalXSDDecimal);
  }

  public double parseDouble(String lexicalXSDDouble)
  {
    return Double.parseDouble(lexicalXSDDouble);
  }

  public float parseFloat(String lexicalXSDFloat)
  {
    return Float.parseFloat(lexicalXSDFloat);
  }

  public byte[] parseHexBinary(String lexicalXSDHexBinary)
    throws IllegalArgumentException
  {
    if (lexicalXSDHexBinary.length() % 2 != 0)
      throw new IllegalArgumentException();

    byte[] buffer = new byte[lexicalXSDHexBinary.length() / 2];

    for (int i = 0; i < buffer.length; i++) {
      buffer[i] = 
        (byte) Integer.parseInt(lexicalXSDHexBinary.substring(2 * i, 2 * i + 2),
                                16);
    }

    return buffer;
  }

  public int parseInt(String lexicalXSDInt)
  {
    return Integer.parseInt(lexicalXSDInt);
  }

  public BigInteger parseInteger(String lexicalXSDInteger)
  {
    return new BigInteger(lexicalXSDInteger);
  }

  public long parseLong(String lexicalXSDLong)
  {
    return Long.parseLong(lexicalXSDLong);
  }

  public QName parseQName(String lexicalXSDQName, NamespaceContext nsc)
    throws IllegalArgumentException
  {
    String[] parts = lexicalXSDQName.split(":", 2);

    if (parts.length == 1)
      return new QName(lexicalXSDQName);
    else {
      String namespaceURI = nsc.getNamespaceURI(parts[0]);

      if (namespaceURI == null)
        throw new IllegalArgumentException(L.l("Unknown prefix {0}", parts[0]));

      return new QName(namespaceURI, parts[1], parts[0]);
    }
  }

  public short parseShort(String lexicalXSDShort)
  {
    return Short.parseShort(lexicalXSDShort);
  }

  public String parseString(String lexicalXSDString)
  {
    return lexicalXSDString;
  }

  public Calendar parseTime(String lexicalXSDTime)
    throws IllegalArgumentException
  {
    XMLGregorianCalendar xmlCal = 
      getDatatypeFactory().newXMLGregorianCalendar(lexicalXSDTime);

    return xmlCal.toGregorianCalendar();
  }

  public long parseUnsignedInt(String lexicalXSDUnsignedInt)
  {
    long x = parseLong(lexicalXSDUnsignedInt);

    if (x < 0)
      throw new IllegalArgumentException(L.l("Input not unsigned"));

    return x;
  }

  public int parseUnsignedShort(String lexicalXSDUnsignedShort)
  {
    int x = parseInt(lexicalXSDUnsignedShort);

    if (x < 0)
      throw new IllegalArgumentException(L.l("Input not unsigned"));

    return x;
  }

  //
  // Printers
  //

  public String printAnySimpleType(String val)
  {
    return val;
  }

  public String printBase64Binary(byte[] val)
  {
    return Base64.encodeFromByteArray(val);
  }

  public String printBoolean(boolean val)
  {
    return String.valueOf(val);
  }

  public String printByte(byte val)
  {
    return String.valueOf((int) val);
  }

  public String printDate(Calendar val)
  {
    dateFormat.setCalendar(val);
    return dateFormat.format(val.getTime());
  }

  public String printDateTime(Calendar val)
  {
    // ISO8601 fix
    dateTimeFormat.setCalendar(val);
    String base = dateTimeFormat.format(val.getTime());

    return base.substring(0, base.length() - 2) + ':' + 
           base.substring(base.length() - 2);
  }

  public String printDecimal(BigDecimal val)
  {
    return val.toString();
  }

  public String printDouble(double val)
  {
    return String.valueOf(val);
  }

  public String printFloat(float val)
  {
    return String.valueOf(val);
  }

  public String printHexBinary(byte[] val)
  {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < val.length; i++) {
      sb.append(hexDigits[((int) val[i] & 0xff) >>> 4]);
      sb.append(hexDigits[val[i] & 0x0f]);
    }

    return sb.toString();
  }

  public String printInt(int val)
  {
    return String.valueOf(val);
  }

  public String printInteger(BigInteger val)
  {
    return val.toString();
  }

  public String printLong(long val)
  {
    return String.valueOf(val);
  }

  public String printQName(QName val, NamespaceContext nsc)
  {
    if (val.getPrefix() != null && ! "".equals(val.getPrefix()))
      return val.getPrefix() + ":" + val.getLocalPart();

    if (val.getNamespaceURI() == null || "".equals(val.getNamespaceURI()))
      return val.getLocalPart();

    String prefix = nsc.getPrefix(val.getNamespaceURI());

    if (prefix == null)
      throw new IllegalArgumentException(L.l("No prefix found for namespace {0}", val.getNamespaceURI()));

    return prefix + ":" + val.getLocalPart();
  }

  public String printShort(short val)
  {
    return String.valueOf(val);
  }

  public String printString(String val)
  {
    return val;
  }

  public String printTime(Calendar val)
  {
    timeFormat.setCalendar(val);
    return timeFormat.format(val.getTime());
  }

  public String printUnsignedInt(long val)
  {
    return String.valueOf(val);
  }

  public String printUnsignedShort(int val)
  {
    return String.valueOf(val);
  }

  private static DatatypeFactory getDatatypeFactory()
  {
    if (_datatypeFactory == null) {
      try {
        _datatypeFactory = DatatypeFactory.newInstance();
      }
      catch (DatatypeConfigurationException e) {
        throw new RuntimeException(e);
      }
    }

    return _datatypeFactory;
  }
}
