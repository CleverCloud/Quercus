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

package com.caucho.xml;

import org.xml.sax.AttributeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DocumentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Dual content handler adapter.
 */
class TeeContentHandler implements ContentHandler {
  private ContentHandler _handler1;
  private ContentHandler _handler2;

  TeeContentHandler(ContentHandler handler1, ContentHandler handler2)
  {
    _handler1 = handler1;
    _handler2 = handler2;
  }
  
  public void setDocumentLocator(Locator locator)
  {
    _handler1.setDocumentLocator(locator);
    _handler2.setDocumentLocator(locator);
  }
  
  public void startDocument()
    throws SAXException
  {
    _handler1.startDocument();
    _handler2.startDocument();
  }
  
  public void endDocument()
    throws SAXException
  {
    _handler1.endDocument();
    _handler2.endDocument();
  }
  
  public void startPrefixMapping(String prefix, String uri)
    throws SAXException
  {
    _handler1.startPrefixMapping(prefix, uri);
    _handler2.startPrefixMapping(prefix, uri);
  }
  
  public void endPrefixMapping(String prefix)
    throws SAXException
  {
    _handler1.endPrefixMapping(prefix);
    _handler2.endPrefixMapping(prefix);
  }
  
  public void startElement(String namespaceURI, String localName,
                           String qName, Attributes attrs)
    throws SAXException
  {
    _handler1.startElement(namespaceURI, localName, qName, attrs);
    _handler2.startElement(namespaceURI, localName, qName, attrs);
  }
  
  public void endElement (String namespaceURI, String localName, String qName)
    throws SAXException
  {
    _handler1.endElement(namespaceURI, localName, qName);
    _handler2.endElement(namespaceURI, localName, qName);
  }
  
  public void characters(char ch[], int start, int length)
    throws SAXException
  {
    _handler1.characters(ch, start, length);
    _handler2.characters(ch, start, length);
  }
  
  public void ignorableWhitespace(char ch[], int start, int length)
    throws SAXException
  {
    _handler1.ignorableWhitespace(ch, start, length);
    _handler2.ignorableWhitespace(ch, start, length);
  }
  
  public void processingInstruction (String target, String data)
    throws SAXException
  {
    _handler1.processingInstruction(target, data);
    _handler2.processingInstruction(target, data);
  }
  
  public void skippedEntity(String name)
    throws SAXException
  {
    _handler1.skippedEntity(name);
    _handler2.skippedEntity(name);
  }
}
