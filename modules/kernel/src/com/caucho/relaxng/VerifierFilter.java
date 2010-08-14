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

package com.caucho.relaxng;

import com.caucho.xml.Xml;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;

/**
 * JARV verifier implementation
 */
public class VerifierFilter extends DefaultHandler {
  private Xml _xml;
  
  private Verifier _verifier;
  private VerifierHandler _verifierHandler;
  
  private ContentHandler _contentHandler;
  private ErrorHandler _errorHandler;

  public VerifierFilter(Verifier verifier)
  {
    _verifier = verifier;
    _verifierHandler = verifier.getVerifierHandler();
  }

  public void setParent(Xml xml)
  {
    _xml = xml;
  }

  public void setContentHandler(ContentHandler handler)
  {
    _contentHandler = handler;
  }

  public void setErrorHandler(ErrorHandler handler)
  {
    _errorHandler = handler;

    _verifier.setErrorHandler(handler);
  }


  public void parse(InputSource in)
    throws IOException, SAXException
  {
    _xml.setContentHandler(this);
    _xml.setErrorHandler(this);
    _xml.parse(in);
  }

  public void setDocumentLocator(Locator locator)
  {
    _verifierHandler.setDocumentLocator(locator);

    if (_contentHandler != null)
      _contentHandler.setDocumentLocator(locator);
  }

  public void startDocument()
    throws SAXException
  {
    _verifierHandler.startDocument();

    if (_contentHandler != null)
      _contentHandler.startDocument();
  }

  public void endDocument()
    throws SAXException
  {
    _verifierHandler.endDocument();

    if (_contentHandler != null)
      _contentHandler.endDocument();
  }

  public void startPrefixMapping(String prefix, String uri)
    throws SAXException
  {
    _verifierHandler.startPrefixMapping(prefix, uri);

    if (_contentHandler != null)
      _contentHandler.startPrefixMapping(prefix, uri);
  }

  public void endPrefixMapping(String prefix)
    throws SAXException
  {
    _verifierHandler.endPrefixMapping(prefix);

    if (_contentHandler != null)
      _contentHandler.endPrefixMapping(prefix);
  }

  public void startElement(String uri, String localName, String qName,
                           Attributes atts)
    throws SAXException
  {
    _verifierHandler.startElement(uri, localName, qName, atts);

    if (_contentHandler != null)
      _contentHandler.startElement(uri, localName, qName, atts);
  }

  public void endElement(String uri, String localName, String qName)
    throws SAXException
  {
    _verifierHandler.endElement(uri, localName, qName);

    if (_contentHandler != null)
      _contentHandler.endElement(uri, localName, qName);
  }

  public void characters(char []ch, int start, int length)
    throws SAXException
  {
    _verifierHandler.characters(ch, start, length);

    if (_contentHandler != null)
      _contentHandler.characters(ch, start, length);
  }

  public void ignorableWhitespace(char []ch, int start, int length)
    throws SAXException
  {
    _verifierHandler.ignorableWhitespace(ch, start, length);

    if (_contentHandler != null)
      _contentHandler.ignorableWhitespace(ch, start, length);
  }

  public void processingInstruction(String target, String data)
    throws SAXException
  {
    _verifierHandler.processingInstruction(target, data);

    if (_contentHandler != null)
      _contentHandler.processingInstruction(target, data);
  }

  public void skippedEntity(String name)
    throws SAXException
  {
    _verifierHandler.skippedEntity(name);

    if (_contentHandler != null)
      _contentHandler.skippedEntity(name);
  }

  public void error(SAXParseException e)
    throws SAXException
  {
    if (_errorHandler != null)
      _errorHandler.error(e);
    else
      _verifierHandler.error(e);
  }

  public void fatalError(SAXParseException e)
    throws SAXException
  {
    if (_errorHandler != null)
      _errorHandler.fatalError(e);
    else
      _verifierHandler.fatalError(e);
  }

  public void warning(SAXParseException e)
    throws SAXException
  {
    if (_errorHandler != null)
      _errorHandler.warning(e);
    else
      _verifierHandler.warning(e);
  }
}
