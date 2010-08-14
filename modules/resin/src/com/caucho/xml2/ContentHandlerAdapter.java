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

package com.caucho.xml2;

import org.xml.sax.AttributeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DocumentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Loose XML parser interface.  The parser can parse directly into
 * the DOM, or it can be used as a SAX parser.
 */
class ContentHandlerAdapter implements ContentHandler {
  private DocumentHandler handler;
  private QAttributeList attributeList;

  ContentHandlerAdapter(DocumentHandler handler)
  {
    this.handler = handler;
    attributeList = new QAttributeList();
  }
  
  public void setDocumentLocator(Locator locator)
  {
    handler.setDocumentLocator(locator);
  }
  
  public void startDocument()
    throws SAXException
  {
    handler.startDocument();
  }
  
  public void endDocument()
    throws SAXException
  {
    handler.endDocument();
  }
  
  public void startPrefixMapping(String prefix, String uri)
    throws SAXException
  {
  }
  
  public void endPrefixMapping(String prefix)
    throws SAXException
  {
  }
  
  public void startElement(String namespaceURI, String localName,
                           String qName, Attributes attrs)
    throws SAXException
  {
    attributeList.init((QAttributes) attrs);
    
    handler.startElement(qName, attributeList);
  }
  
  public void endElement (String namespaceURI, String localName, String qName)
    throws SAXException
  {
    handler.endElement(qName);
  }
  
  public void characters(char ch[], int start, int length)
    throws SAXException
  {
    handler.characters(ch, start, length);
  }
  
  public void ignorableWhitespace(char ch[], int start, int length)
    throws SAXException
  {
    handler.ignorableWhitespace(ch, start, length);
  }
  
  public void processingInstruction (String target, String data)
    throws SAXException
  {
    handler.processingInstruction(target, data);
  }
  
  public void skippedEntity(String name)
    throws SAXException
  {
  }

  static class QAttributeList implements AttributeList {
    private QAttributes attributes;

    void init(QAttributes attributes)
    {
      this.attributes = attributes;
    }

    public int getLength()
    {
      return attributes.getLength();
    }    

    public String getName(int i)
    {
      return attributes.getQName(i);
    }    

    public String getValue(int i)
    {
      return attributes.getValue(i);
    }    

    public String getValue(String qName)
    {
      return attributes.getValue(qName);
    }    

    public String getType(int i)
    {
      return attributes.getType(i);
    }    

    public String getType(String qName)
    {
      return attributes.getType(qName);
    }    
  }
}
