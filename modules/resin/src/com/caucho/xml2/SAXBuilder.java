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

import com.caucho.util.CharBuffer;

import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.namespace.QName;
import java.io.IOException;

/**
 * XMLWriter to create a SAX events.
 */
public class SAXBuilder implements XMLWriter, Locator {
  private XMLReader _saxReader;
  private ContentHandler _contentHandler;

  private QAttributes _attributes;
  private CharBuffer _text;
  private boolean _escapeText;

  private boolean _hasElement;

  private String _elementUri;
  private String _elementLocalName;
  private String _elementQName;

  private String _filename;
  private int _line;

  private String _elementSystemId;
  private int _elementLine;

  private Locator _locator;

  public SAXBuilder()
  {
    _text = new CharBuffer();
    _attributes = new QAttributes();
  }

  public SAXBuilder(XMLReader saxReader)
  {
    this();

    init(saxReader);
  }

  public void init(XMLReader saxReader)
  {
    _saxReader = saxReader;

    _contentHandler = saxReader.getContentHandler();

    init();
  }

  public void init()
  {
    _text.clear();
    _attributes.clear();
    _hasElement = false;

    _filename = null;
    _line = 0;
  }

  /**
   * Sets the SAX content handler.
   */
  public void setContentHandler(ContentHandler handler)
  {
    _contentHandler = handler;
  }

  public void setDocumentLocator(Locator locator)
  {
    _locator = locator;
  }

  public void startDocument()
    throws IOException, SAXException
  {
    _contentHandler.setDocumentLocator(this);
    _contentHandler.startDocument();
  }

  public void endDocument()
    throws IOException, SAXException
  {
    pop();

    _contentHandler.endDocument();
  }
  
  public void setLocation(String filename, int line, int column)
  {
    _filename = filename;
    _line = line;
  }

  /**
   * Returns the current filename.
   */
  public String getSystemId()
  {
    if (_elementSystemId != null)
      return _elementSystemId;
    else if (_locator != null)
      return _locator.getSystemId();
    else
      return _filename;
  }

  /**
   * Don't really have a public id (?).
   */
  public String getPublicId()
  {
    if (_locator != null)
      return _locator.getPublicId();
    else
      return null;
  }
  
  /**
   * Returns the current line.
   */
  public int getLineNumber()
  {
    if (_elementSystemId != null)
      return _elementLine;
    else if (_locator != null)
      return _locator.getLineNumber();
    else
      return 0;
  }

  /**
   * The column number is always 0.
   */
  public int getColumnNumber()
  {
    if (_locator != null)
      return _locator.getColumnNumber();
    else
      return 0;
  }

  /**
   * Starts the building of an element.
   *
   * @param uri the element's namespace URI
   * @param localName the element's local name
   * @param qName the element's fully qualified name
   */
  public void startElement(String uri, String localName, String qName)
    throws IOException, SAXException
  {
    pop();

    _elementUri = uri;
    _elementLocalName = localName;
    _elementQName = qName;
    _hasElement = true;

    if (_locator != null) {
      _elementSystemId = _locator.getSystemId();
      _elementLine = _locator.getLineNumber();
    }
  }

  public void startPrefixMapping(String prefix, String uri)
    throws IOException, SAXException
  {
    _contentHandler.startPrefixMapping(prefix, uri);
  }

  public void endPrefixMapping(String prefix)
    throws IOException, SAXException
  {
    _contentHandler.endPrefixMapping(prefix);
  }

  public void attribute(String uri, String localName, String qName,
                        String value)
    throws IOException, SAXException
  {
    QName name = new QName(qName, uri);
    
    _attributes.add(name, value);
  }

  public void endElement(String uri, String localName, String qName)
    throws IOException, SAXException
  {
    pop();
    
    if (uri != null)
      _contentHandler.endElement(uri, localName, qName);
    else
      _contentHandler.endElement("", null, qName);
  }

  public void processingInstruction(String name, String data)
    throws IOException, SAXException
  {
    pop();

    _contentHandler.processingInstruction(name, data);
  }
  
  public void comment(String data)
    throws IOException, SAXException
  {
    pop();
  }

  public boolean getEscapeText()
  {
    return _escapeText;
  }

  public void setEscapeText(boolean isEscaped)
  {
    _escapeText = isEscaped;
  }

  public void text(String text)
    throws IOException, SAXException
  {
    popElement();
    
    _text.append(text);
  }

  public void text(char []buffer, int offset, int length)
    throws IOException, SAXException
  {
    popElement();
    
    _text.append(buffer, offset, length);
  }

  public void cdata(String text)
    throws IOException, SAXException
  {
    popElement();
    
    text(text);
  }

  public void cdata(char []buffer, int offset, int length)
    throws IOException, SAXException
  {
    popElement();
    
    text(buffer, offset, length);
  }

  private void pop()
    throws IOException, SAXException
  {
    popElement();
    
    if (_text.length() == 0)
      return;

    _contentHandler.characters(_text.getBuffer(), 0, _text.getLength());
    
    _text.clear();
  }

  /**
   * Completes the open element processing.
   */
  private void popElement()
    throws IOException, SAXException
  {
    if (_hasElement) {
      if (_elementUri == null)
        _contentHandler.startElement("", null, _elementQName, _attributes);
      else
        _contentHandler.startElement(_elementUri, _elementLocalName,
                                     _elementQName, _attributes);

      _hasElement = false;
      _elementSystemId = null;
      _attributes.clear();
    }
  }
}
