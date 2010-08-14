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
import org.w3c.dom.*;
import javax.xml.namespace.*;
import java.util.*;

public interface SOAPElement extends Node, Element {
  public SOAPElement addAttribute(Name name, String value) 
    throws SOAPException;

  public SOAPElement addAttribute(QName qname, String value) 
    throws SOAPException;

  public SOAPElement addChildElement(Name name) 
    throws SOAPException;

  public SOAPElement addChildElement(QName qname) 
    throws SOAPException;

  public SOAPElement addChildElement(SOAPElement element) 
    throws SOAPException;

  public SOAPElement addChildElement(String localName) 
    throws SOAPException;

  public SOAPElement addChildElement(String localName, String prefix) 
    throws SOAPException;

  public SOAPElement addChildElement(String localName, 
                                     String prefix, 
                                     String uri) 
    throws SOAPException;

  public SOAPElement addNamespaceDeclaration(String prefix, String uri) 
    throws SOAPException;
  
  public SOAPElement addTextNode(String text) 
    throws SOAPException;

  public QName createQName(String localName, String prefix) 
    throws SOAPException;

  public Iterator getAllAttributes();

  public Iterator getAllAttributesAsQNames();
  
  public String getAttributeValue(Name name);

  public String getAttributeValue(QName qname);

  public Iterator getChildElements();

  public Iterator getChildElements(Name name);

  public Iterator getChildElements(QName qname);

  public Name getElementName();

  public QName getElementQName();
  
  public String getEncodingStyle();

  public Iterator getNamespacePrefixes();

  public String getNamespaceURI(String prefix);

  public Iterator getVisibleNamespacePrefixes();

  public boolean removeAttribute(Name name);

  public boolean removeAttribute(QName qname);

  public void removeContents();

  public boolean removeNamespaceDeclaration(String prefix);

  public SOAPElement setElementQName(QName newName) 
    throws SOAPException;

  public void setEncodingStyle(String encodingStyle)
    throws SOAPException;
}

