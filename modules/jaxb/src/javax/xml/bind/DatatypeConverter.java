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

package javax.xml.bind;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;

public final class DatatypeConverter {
  private static DatatypeConverterInterface _converter = null;

  private DatatypeConverter()
  {
  }

  public static String parseAnySimpleType(String lexicalXSDAnySimpleType)
  {
    return _converter.parseAnySimpleType(lexicalXSDAnySimpleType);
  }

  public static byte[] parseBase64Binary(String lexicalXSDBase64Binary)
  {
    return _converter.parseBase64Binary(lexicalXSDBase64Binary);
  }

  public static boolean parseBoolean(String lexicalXSDBoolean)
  {
    return _converter.parseBoolean(lexicalXSDBoolean);
  }

  public static byte parseByte(String lexicalXSDByte)
  {
    return _converter.parseByte(lexicalXSDByte);
  }

  public static Calendar parseDate(String lexicalXSDDate)
  {
    return _converter.parseDate(lexicalXSDDate);
  }

  public static Calendar parseDateTime(String lexicalXSDDateTime)
  {
    return _converter.parseDateTime(lexicalXSDDateTime);
  }

  public static BigDecimal parseDecimal(String lexicalXSDDecimal)
  {
    return _converter.parseDecimal(lexicalXSDDecimal);
  }

  public static double parseDouble(String lexicalXSDDouble)
  {
    return _converter.parseDouble(lexicalXSDDouble);
  }

  public static float parseFloat(String lexicalXSDFloat)
  {
    return _converter.parseFloat(lexicalXSDFloat);
  }

  public static byte[] parseHexBinary(String lexicalXSDHexBinary)
  {
    return _converter.parseHexBinary(lexicalXSDHexBinary);
  }

  public static int parseInt(String lexicalXSDInt)
  {
    return _converter.parseInt(lexicalXSDInt);
  }

  public static BigInteger parseInteger(String lexicalXSDInteger)
  {
    return _converter.parseInteger(lexicalXSDInteger);
  }

  public static long parseLong(String lexicalXSDLong)
  {
    return _converter.parseLong(lexicalXSDLong);
  }

  public static QName parseQName(String lexicalXSDQName, NamespaceContext nsc)
  {
    return _converter.parseQName(lexicalXSDQName, nsc);
  }

  public static short parseShort(String lexicalXSDShort)
  {
    return _converter.parseShort(lexicalXSDShort);
  }

  public static String parseString(String lexicalXSDString)
  {
    return _converter.parseString(lexicalXSDString);
  }

  public static Calendar parseTime(String lexicalXSDTime)
  {
    return _converter.parseTime(lexicalXSDTime);
  }

  public static long parseUnsignedInt(String lexicalXSDUnsignedInt)
  {
    return _converter.parseUnsignedInt(lexicalXSDUnsignedInt);
  }

  public static int parseUnsignedShort(String lexicalXSDUnsignedShort)
  {
    return _converter.parseUnsignedShort(lexicalXSDUnsignedShort);
  }

  public static String printAnySimpleType(String val)
  {
    return _converter.printAnySimpleType(val);
  }

  public static String printBase64Binary(byte[] val)
  {
    return _converter.printBase64Binary(val);
  }

  public static String printBoolean(boolean val)
  {
    return _converter.printBoolean(val);
  }

  public static String printByte(byte val)
  {
    return _converter.printByte(val);
  }

  public static String printDate(Calendar val)
  {
    return _converter.printDate(val);
  }

  public static String printDateTime(Calendar val)
  {
    return _converter.printDateTime(val);
  }

  public static String printDecimal(BigDecimal val)
  {
    return _converter.printDecimal(val);
  }

  public static String printDouble(double val)
  {
    return _converter.printDouble(val);
  }

  public static String printFloat(float val)
  {
    return _converter.printFloat(val);
  }

  public static String printHexBinary(byte[] val)
  {
    return _converter.printHexBinary(val);
  }

  public static String printInt(int val)
  {
    return _converter.printInt(val);
  }

  public static String printInteger(BigInteger val)
  {
    return _converter.printInteger(val);
  }

  public static String printLong(long val)
  {
    return _converter.printLong(val);
  }

  public static String printQName(QName val, NamespaceContext nsc)
  {
    return _converter.printQName(val, nsc);
  }

  public static String printShort(short val)
  {
    return _converter.printShort(val);
  }

  public static String printString(String val)
  {
    return _converter.printString(val);
  }

  public static String printTime(Calendar val)
  {
    return _converter.printTime(val);
  }

  public static String printUnsignedInt(long val)
  {
    return _converter.printUnsignedInt(val);
  }

  public static String printUnsignedShort(int val)
  {
    return _converter.printUnsignedShort(val);
  }

  public static void setDatatypeConverter(DatatypeConverterInterface converter)
    throws IllegalArgumentException
  {
    if (converter == null)
      throw new IllegalArgumentException("Datatype converter cannot be null");

    // Can only be set once
    if (_converter == null)
      _converter = converter;
  }

}

