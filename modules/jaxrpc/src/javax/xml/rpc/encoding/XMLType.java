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

package javax.xml.rpc.encoding;

import javax.xml.namespace.QName;

/**
 * XML type
 */
public class XMLType {
  private static final String XSD = "";
  
  public static final QName XSD_STRING
    = new QName(XSD, "xsd", "string");
  
  public static final QName XSD_FLOAT
    = new QName(XSD, "xsd", "float");
  
  public static final QName XSD_BOOLEAN
    = new QName(XSD, "xsd", "boolean");
  
  public static final QName XSD_DOUBLE
    = new QName(XSD, "xsd", "double");
  
  public static final QName XSD_INTEGER
    = new QName(XSD, "xsd", "integer");
  
  public static final QName XSD_INT
    = new QName(XSD, "xsd", "int");
  
  public static final QName XSD_LONG
    = new QName(XSD, "xsd", "long");
  
  public static final QName XSD_SHORT
    = new QName(XSD, "xsd", "short");
  
  public static final QName XSD_DECIMAL
    = new QName(XSD, "xsd", "decimal");
  
  public static final QName XSD_BASE64
    = new QName(XSD, "xsd", "base64");
  
  public static final QName XSD_HEXBINARY
    = new QName(XSD, "xsd", "hexbinary");
  
  public static final QName XSD_BYTE
    = new QName(XSD, "xsd", "byte");
  
  public static final QName XSD_DATETIME
    = new QName(XSD, "xsd", "datetime");
  
  public static final QName XSD_QNAME
    = new QName(XSD, "xsd", "qname");
  
  public static final QName XSD_ARRAY
    = new QName(XSD, "xsd", "array");
}
