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
import java.util.*;

public interface SOAPFault extends SOAPBodyElement {
  abstract Detail addDetail() throws SOAPException;

  abstract void addFaultReasonText(String text, Locale locale) 
    throws SOAPException;

  abstract void appendFaultSubcode(QName subcode) 
    throws SOAPException;

  abstract Detail getDetail();
  abstract String getFaultActor();
  abstract String getFaultCode();
  abstract Name getFaultCodeAsName();
  abstract QName getFaultCodeAsQName();
  abstract String getFaultNode();
  abstract Iterator getFaultReasonLocales() throws SOAPException;
  abstract String getFaultReasonText(Locale locale) throws SOAPException;
  abstract Iterator getFaultReasonTexts() throws SOAPException;
  abstract String getFaultRole();
  abstract String getFaultString();
  abstract Locale getFaultStringLocale();
  abstract Iterator getFaultSubcodes();
  abstract boolean hasDetail();
  abstract void removeAllFaultSubcodes();
  abstract void setFaultActor(String faultActor) throws SOAPException;
  abstract void setFaultCode(Name faultCodeQName) throws SOAPException;
  abstract void setFaultCode(QName faultCodeQName) throws SOAPException;
  abstract void setFaultCode(String faultCode) throws SOAPException;
  abstract void setFaultNode(String uri) throws SOAPException;
  abstract void setFaultRole(String uri) throws SOAPException;
  abstract void setFaultString(String faultString) throws SOAPException;
  abstract void setFaultString(String faultString, Locale locale) 
    throws SOAPException;
}
