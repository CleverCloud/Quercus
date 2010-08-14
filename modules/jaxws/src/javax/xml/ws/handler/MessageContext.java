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

package javax.xml.ws.handler;
import java.util.Map;

public interface MessageContext extends Map<String,Object> {
  static final String HTTP_REQUEST_HEADERS="javax.xml.ws.http.request.headers";
  static final String HTTP_REQUEST_METHOD="javax.xml.ws.http.request.method";
  static final String HTTP_RESPONSE_CODE="javax.xml.ws.http.response.code";
  static final String HTTP_RESPONSE_HEADERS="javax.xml.ws.http.response.headers";

  static final String MESSAGE_OUTBOUND_PROPERTY="javax.xml.ws.handler.message.outbound";
  static final String INBOUND_MESSAGE_ATTACHMENTS="javax.xml.ws.binding.attachments.inbound";
  static final String OUTBOUND_MESSAGE_ATTACHMENTS="javax.xml.ws.binding.attachments.outbound";

  static final String PATH_INFO="javax.xml.ws.http.request.pathinfo";
  static final String QUERY_STRING="javax.xml.ws.http.request.querystring";

  static final String SERVLET_CONTEXT="javax.xml.ws.servlet.context";
  static final String SERVLET_REQUEST="javax.xml.ws.servlet.request";
  static final String SERVLET_RESPONSE="javax.xml.ws.servlet.response";

  static final String WSDL_DESCRIPTION="javax.xml.ws.wsdl.description";
  static final String WSDL_INTERFACE="javax.xml.ws.wsdl.interface";
  static final String WSDL_OPERATION="javax.xml.ws.wsdl.operation";
  static final String WSDL_PORT="javax.xml.ws.wsdl.port";
  static final String WSDL_SERVICE="javax.xml.ws.wsdl.service";

  abstract Scope getScope(String name);
  abstract void setScope(String name, Scope scope);

  public static enum Scope {
    APPLICATION, HANDLER;
  }
}

