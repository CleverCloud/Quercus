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

package javax.xml.stream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

public interface XMLStreamReader extends XMLStreamConstants {

  public void close() throws XMLStreamException;

  public int getAttributeCount();
  
  public String getAttributeLocalName(int index);
  
  public QName getAttributeName(int index);
  
  public String getAttributeNamespace(int index);
  
  public String getAttributePrefix(int index);
  
  public String getAttributeType(int index);
  
  public String getAttributeValue(int index);
  
  public String getAttributeValue(String namespaceURI, String localName);
  
  public String getCharacterEncodingScheme();
  
  public String getElementText() throws XMLStreamException;
  
  public String getEncoding();
  
  public int getEventType();
  
  public String getLocalName();
  
  public Location getLocation();
  
  public QName getName();
  
  public NamespaceContext getNamespaceContext();
  
  public int getNamespaceCount();
  
  public String getNamespacePrefix(int index);
  
  public String getNamespaceURI();
  
  public String getNamespaceURI(int index);
  
  public String getNamespaceURI(String prefix);
  
  public String getPIData();
  
  public String getPITarget();
  
  public String getPrefix();
  
  public Object getProperty(String name) throws IllegalArgumentException;
  
  public String getText();
  
  public char[] getTextCharacters();
  
  public int getTextCharacters(int sourceStart, char[] target,
                               int targetStart, int length)
    throws XMLStreamException;

  public int getTextLength();
  
  public int getTextStart();
  
  public String getVersion();
  
  public boolean hasName();
  
  public boolean hasNext() throws XMLStreamException;

  public boolean hasText();
  
  public boolean isAttributeSpecified(int index);
  
  public boolean isCharacters();
  
  public boolean isEndElement();
  
  public boolean isStandalone();
  
  public boolean isStartElement();
  
  public boolean isWhiteSpace();
  
  public int next() throws XMLStreamException;
  
  public int nextTag() throws XMLStreamException;
  
  public void require(int type, String namespaceURI, String localName)
    throws XMLStreamException;
  
  public boolean standaloneSet();

}

