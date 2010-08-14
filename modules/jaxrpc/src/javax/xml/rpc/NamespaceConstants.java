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

package javax.xml.rpc;

/**
 * Holds the common namespace constants.
 */
public class NamespaceConstants {
  public static final String NSPREFIX_SOAP_ENVELOPE =
    "soapenv";
  public static final String NSPREFIX_SOAP_ENCODING =
    "soapenc";
  public static final String NSPREFIX_SCHEMA_XSD =
    "xsd";
  public static final String NSPREFIX_SCHEMA_XSI =
    "xsi";
  public static final String NSURI_SOAP_ENVELOPE =
    "http://schemas.xmlsoap.org/soap/envelope/";
  public static final String NSURI_SOAP_ENCODING =
    "http://schemas.xmlsoap.org/soap/encoding/";
  public static final String NSURI_SOAP_NEXT_ACTOR =
    "http://schemas.xmlsoap.org/soap/actor/next";
  public static final String NSURI_SCHEMA_XSD =
    "http://www.w3.org/2001/XMLSchema";
  public static final String NSURI_SCHEMA_XSI =
    "http://www.w3.org/2001/XMLSchema-instance";
}
