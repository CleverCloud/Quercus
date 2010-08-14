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

package javax.xml.ws.soap;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPFactory;
import javax.xml.ws.Binding;
import java.util.Set;

/** XXX */
public interface SOAPBinding extends Binding {

  /** XXX */
  static final String SOAP11HTTP_BINDING="http://schemas.xmlsoap.org/wsdl/soap/http";


  /** XXX */
  static final String SOAP11HTTP_MTOM_BINDING="http://schemas.xmlsoap.org/wsdl/soap/http?mtom=true";


  /** XXX */
  static final String SOAP12HTTP_BINDING="http://www.w3.org/2003/05/soap/bindings/HTTP/";


  /** XXX */
  static final String SOAP12HTTP_MTOM_BINDING="http://www.w3.org/2003/05/soap/bindings/HTTP/?mtom=true";


  /** XXX */
  abstract MessageFactory getMessageFactory();


  /** XXX */
  abstract Set<String> getRoles();


  /** XXX */
  abstract SOAPFactory getSOAPFactory();


  /** XXX */
  abstract boolean isMTOMEnabled();


  /** XXX */
  abstract void setMTOMEnabled(boolean flag);


  /** XXX */
  abstract void setRoles(Set<String> roles);

}

