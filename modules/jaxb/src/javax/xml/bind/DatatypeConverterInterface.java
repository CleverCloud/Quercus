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

public interface DatatypeConverterInterface {

  String parseAnySimpleType(String lexicalXSDAnySimpleType);

  byte[] parseBase64Binary(String lexicalXSDBase64Binary);

  boolean parseBoolean(String lexicalXSDBoolean);

  byte parseByte(String lexicalXSDByte);

  Calendar parseDate(String lexicalXSDDate);

  Calendar parseDateTime(String lexicalXSDDateTime);

  BigDecimal parseDecimal(String lexicalXSDDecimal);

  double parseDouble(String lexicalXSDDouble);

  float parseFloat(String lexicalXSDFloat);

  byte[] parseHexBinary(String lexicalXSDHexBinary);

  int parseInt(String lexicalXSDInt);

  BigInteger parseInteger(String lexicalXSDInteger);

  long parseLong(String lexicalXSDLong);

  QName parseQName(String lexicalXSDQName, NamespaceContext nsc);

  short parseShort(String lexicalXSDShort);

  String parseString(String lexicalXSDString);

  Calendar parseTime(String lexicalXSDTime);

  long parseUnsignedInt(String lexicalXSDUnsignedInt);

  int parseUnsignedShort(String lexicalXSDUnsignedShort);

  String printAnySimpleType(String val);

  String printBase64Binary(byte[] val);

  String printBoolean(boolean val);

  String printByte(byte val);

  String printDate(Calendar val);

  String printDateTime(Calendar val);

  String printDecimal(BigDecimal val);

  String printDouble(double val);

  String printFloat(float val);

  String printHexBinary(byte[] val);

  String printInt(int val);

  String printInteger(BigInteger val);

  String printLong(long val);

  String printQName(QName val, NamespaceContext nsc);

  String printShort(short val);

  String printString(String val);

  String printTime(Calendar val);

  String printUnsignedInt(long val);

  String printUnsignedShort(int val);

}

