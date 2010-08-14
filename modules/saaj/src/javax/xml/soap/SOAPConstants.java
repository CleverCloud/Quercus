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

package javax.xml.soap;
import javax.xml.namespace.*;

public interface SOAPConstants {
  static final String DEFAULT_SOAP_PROTOCOL = "SOAP 1.1 Protocol";
  static final String DYNAMIC_SOAP_PROTOCOL = "Dynamic Protocol";
  static final String SOAP_1_1_CONTENT_TYPE = "text/xml";
  static final String SOAP_1_1_PROTOCOL = "SOAP 1.1 Protocol";
  static final String SOAP_1_2_CONTENT_TYPE = "application/soap+xml";
  static final String SOAP_1_2_PROTOCOL = "SOAP 1.2 Protocol";
  static final String SOAP_ENV_PREFIX = "env";

  static final String URI_NS_SOAP_1_1_ENVELOPE = 
    "http://schemas.xmlsoap.org/soap/envelope/";

  static final String URI_NS_SOAP_1_2_ENCODING =
    "http://www.w3.org/2003/05/soap-encoding";

  static final String URI_NS_SOAP_1_2_ENVELOPE =
    "http://www.w3.org/2003/05/soap-envelope";

  static final String URI_NS_SOAP_ENCODING =
    "http://schemas.xmlsoap.org/soap/encoding/";

  static final String URI_NS_SOAP_ENVELOPE =
    "http://schemas.xmlsoap.org/soap/envelope/";

  static final String URI_SOAP_1_2_ROLE_NEXT =
    "http://www.w3.org/2003/05/soap-envelope/role/next";

  static final String URI_SOAP_1_2_ROLE_NONE =
    "http://www.w3.org/2003/05/soap-envelope/role/none";

  static final String URI_SOAP_1_2_ROLE_ULTIMATE_RECEIVER =
    "http://www.w3.org/2003/05/soap-envelope/role/ultimateReceiver";

  static final String URI_SOAP_ACTOR_NEXT =
    "http://schemas.xmlsoap.org/soap/actor/next";

  static final QName SOAP_DATAENCODINGUNKNOWN_FAULT 
    = new QName(URI_NS_SOAP_1_2_ENVELOPE, 
                "DataEncodingUnknown", 
                SOAP_ENV_PREFIX);

  static final QName SOAP_MUSTUNDERSTAND_FAULT
    = new QName(URI_NS_SOAP_1_2_ENVELOPE, "MustUnderstand", SOAP_ENV_PREFIX);

  static final QName SOAP_RECEIVER_FAULT
    = new QName(URI_NS_SOAP_1_2_ENVELOPE, "Receiver", SOAP_ENV_PREFIX);

  static final QName SOAP_SENDER_FAULT
    = new QName(URI_NS_SOAP_1_2_ENVELOPE, "Sender", SOAP_ENV_PREFIX);

  static final QName SOAP_VERSIONMISMATCH_FAULT
    = new QName(URI_NS_SOAP_1_2_ENVELOPE, "VersionMismatch", SOAP_ENV_PREFIX);


}
