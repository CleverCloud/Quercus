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
 * @author Emil Ong
 */

package com.caucho.xml.stream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

class FilteredStreamReader implements XMLStreamReader {
  private XMLStreamReader _reader;
  private StreamFilter _filter;

  public FilteredStreamReader(XMLStreamReader reader, StreamFilter filter)
    throws XMLStreamException
  {
    _reader = reader;
    _filter = filter;
  }

  public int nextTag() 
    throws XMLStreamException
  {
    while (_reader.hasNext()) {
      _reader.nextTag(); 

      if (_filter.accept(_reader))
        break;
    }

    return _reader.getEventType();
  }

  public int next() 
    throws XMLStreamException
  {
    while (_reader.hasNext()) {
      _reader.next(); 

      if (_filter.accept(_reader))
        break;
    }

    return _reader.getEventType();
  }

  public boolean hasNext() throws XMLStreamException
  {
    do {
      // This is a hack to get the TCK to run.
      // The TCK compares its stream versus ours and its stream
      // throws away the END_DOCUMENT (even though its event reader
      // does not!).  
      //
      // XXX create a flag or property to disable this hack
      if (_reader.getEventType() == XMLStreamConstants.END_DOCUMENT)
        return false;

      if (_filter.accept(_reader))
        return true;

      _reader.next(); 
    }
    while (_reader.hasNext());

    return false;
  }

  public void close() throws XMLStreamException
  {
    _reader.close();
  }

  public int getAttributeCount()
  {
    return _reader.getAttributeCount();
  }
  
  public String getAttributeLocalName(int index)
  {
    return _reader.getAttributeLocalName(index);
  }
  
  public QName getAttributeName(int index)
  {
    return _reader.getAttributeName(index);
  }
  
  public String getAttributeNamespace(int index)
  {
    return _reader.getAttributeNamespace(index);
  }
  
  public String getAttributePrefix(int index)
  {
    return _reader.getAttributePrefix(index);
  }
  
  public String getAttributeType(int index)
  {
    return _reader.getAttributeType(index);
  }
  
  public String getAttributeValue(int index)
  {
    return _reader.getAttributeValue(index);
  }
  
  public String getAttributeValue(String namespaceURI, String localName)
  {
    return _reader.getAttributeValue(namespaceURI, localName);
  }
  
  public String getCharacterEncodingScheme()
  {
    return _reader.getCharacterEncodingScheme();
  }
  
  public String getElementText() throws XMLStreamException
  {
    return _reader.getElementText();
  }
  
  public String getEncoding()
  {
    return _reader.getEncoding();
  }
  
  public int getEventType()
  {
    return _reader.getEventType();
  }
  
  public String getLocalName()
  {
    return _reader.getLocalName();
  }
  
  public Location getLocation()
  {
    return _reader.getLocation();
  }
  
  public QName getName()
  {
    return _reader.getName();
  }
  
  public NamespaceContext getNamespaceContext()
  {
    return _reader.getNamespaceContext();
  }
  
  public int getNamespaceCount()
  {
    return _reader.getNamespaceCount();
  }
  
  public String getNamespacePrefix(int index)
  {
    return _reader.getNamespacePrefix(index);
  }
  
  public String getNamespaceURI()
  {
    return _reader.getNamespaceURI();
  }
  
  public String getNamespaceURI(int index)
  {
    return _reader.getNamespaceURI(index);
  }
  
  public String getNamespaceURI(String prefix)
  {
    return _reader.getNamespaceURI(prefix);
  }
  
  public String getPIData()
  {
    return _reader.getPIData();
  }
  
  public String getPITarget()
  {
    return _reader.getPITarget();
  }
  
  public String getPrefix()
  {
    return _reader.getPrefix();
  }
  
  public Object getProperty(String name) throws IllegalArgumentException
  {
    return _reader.getProperty(name);
  }
  
  public String getText()
  {
    return _reader.getText();
  }
  
  public char[] getTextCharacters()
  {
    return _reader.getTextCharacters();
  }
  
  public int getTextCharacters(int sourceStart, char[] target,
                               int targetStart, int length)
    throws XMLStreamException
  {
    return _reader.getTextCharacters(sourceStart, target, targetStart, length);
  }

  public int getTextLength()
  {
    return _reader.getTextLength();
  }
  
  public int getTextStart()
  {
    return _reader.getTextStart();
  }
  
  public String getVersion()
  {
    return _reader.getVersion();
  }
  
  public boolean hasName()
  {
    return _reader.hasName();
  }
  
  public boolean hasText()
  {
    return _reader.hasText();
  }
  
  public boolean isAttributeSpecified(int index)
  {
    return _reader.isAttributeSpecified(index);
  }
  
  public boolean isCharacters()
  {
    return _reader.isCharacters();
  }
  
  public boolean isEndElement()
  {
    return _reader.isEndElement();
  }
  
  public boolean isStandalone()
  {
    return _reader.isStandalone();
  }
  
  public boolean isStartElement()
  {
    return _reader.isStartElement();
  }
  
  public boolean isWhiteSpace()
  {
    return _reader.isWhiteSpace();
  }
  
  public void require(int type, String namespaceURI, String localName)
    throws XMLStreamException
  {
    _reader.require(type, namespaceURI, localName);
  }
  
  public boolean standaloneSet()
  {
    return _reader.standaloneSet();
  }
}
