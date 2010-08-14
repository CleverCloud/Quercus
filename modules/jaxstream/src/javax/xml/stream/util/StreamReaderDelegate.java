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

package javax.xml.stream.util;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *  Wrapper around an XMLStreamReader
 */
public class StreamReaderDelegate implements XMLStreamReader {

  private XMLStreamReader _parent;

  public StreamReaderDelegate()
  {
    this(null);
  }

  public StreamReaderDelegate(XMLStreamReader reader)
  {
    _parent = reader;
  }

  public void close() throws XMLStreamException
  {
    _parent.close();
  }

  public int getAttributeCount()
  {
    return _parent.getAttributeCount();
  }

  public String getAttributeLocalName(int index)
  {
    return _parent.getAttributeLocalName(index);
  }

  public QName getAttributeName(int index)
  {
    return _parent.getAttributeName(index);
  }

  public String getAttributeNamespace(int index)
  {
    return _parent.getAttributeNamespace(index);
  }

  public String getAttributePrefix(int index)
  {
    return _parent.getAttributePrefix(index);
  }

  public String getAttributeType(int index)
  {
    return _parent.getAttributeType(index);
  }

  public String getAttributeValue(int index)
  {
    return _parent.getAttributeValue(index);
  }

  public String getAttributeValue(String namespaceUri, String localName)
  {
    return _parent.getAttributeValue(namespaceUri, localName);
  }

  public String getCharacterEncodingScheme()
  {
    return _parent.getCharacterEncodingScheme();
  }

  public String getElementText() throws XMLStreamException
  {
    return _parent.getElementText();
  }

  public String getEncoding()
  {
    return _parent.getEncoding();
  }

  public int getEventType()
  {
    return _parent.getEventType();
  }

  public String getLocalName()
  {
    return _parent.getLocalName();
  }

  public Location getLocation()
  {
    return _parent.getLocation();
  }

  public QName getName()
  {
    return _parent.getName();
  }

  public NamespaceContext getNamespaceContext()
  {
    return _parent.getNamespaceContext();
  }

  public int getNamespaceCount()
  {
    return _parent.getNamespaceCount();
  }

  public String getNamespacePrefix(int index)
  {
    return _parent.getNamespacePrefix(index);
  }

  public String getNamespaceURI()
  {
    return _parent.getNamespaceURI();
  }

  public String getNamespaceURI(int index)
  {
    return _parent.getNamespaceURI(index);
  }

  public String getNamespaceURI(String prefix)
  {
    return _parent.getNamespaceURI(prefix);
  }

  public XMLStreamReader getParent()
  {
    return _parent;
  }

  public String getPIData()
  {
    return _parent.getPIData();
  }

  public String getPITarget()
  {
    return _parent.getPITarget();
  }

  public String getPrefix()
  {
    return _parent.getPrefix();
  }

  public Object getProperty(String name)
  {
    return _parent.getProperty(name);
  }

  public String getText()
  {
    return _parent.getText();
  }

  public char[] getTextCharacters()
  {
    return _parent.getTextCharacters();
  }

  public int getTextCharacters(int sourceStart, char[] target,
                               int targetStart, int length)
    throws XMLStreamException
  {
    return _parent.getTextCharacters(sourceStart, target, targetStart, length);
  }

  public int getTextLength()
  {
    return _parent.getTextLength();
  }


  public int getTextStart()
  {
    return _parent.getTextStart();
  }


  public String getVersion()
  {
    return _parent.getVersion();
  }


  public boolean hasName()
  {
    return _parent.hasName();
  }


  public boolean hasNext() throws XMLStreamException
  {
    return _parent.hasNext();
  }


  public boolean hasText()
  {
    return _parent.hasText();
  }


  public boolean isAttributeSpecified(int index)
  {
    return _parent.isAttributeSpecified(index);
  }


  public boolean isCharacters()
  {
    return _parent.isCharacters();
  }


  public boolean isEndElement()
  {
    return _parent.isEndElement();
  }


  public boolean isStandalone()
  {
    return _parent.isStandalone();
  }


  public boolean isStartElement()
  {
    return _parent.isStartElement();
  }


  public boolean isWhiteSpace()
  {
    return _parent.isWhiteSpace();
  }


  public int next() throws XMLStreamException
  {
    return _parent.next();
  }


  public int nextTag() throws XMLStreamException
  {
    return _parent.nextTag();
  }


  public void require(int type, String namespaceURI, String localName)
    throws XMLStreamException
  {
    _parent.require(type, namespaceURI, localName);
  }


  public void setParent(XMLStreamReader reader)
  {
    _parent = reader;
  }


  public boolean standaloneSet()
  {
    return _parent.standaloneSet();
  }

}

