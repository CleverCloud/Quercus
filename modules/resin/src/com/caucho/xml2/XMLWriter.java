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

import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Interface for printing XML documents.
 */
public interface XMLWriter {
  public void setDocumentLocator(Locator locator);
  
  public void startDocument()
    throws IOException, SAXException;

  public void endDocument()
    throws IOException, SAXException;

  public void startPrefixMapping(String prefix, String uri)
    throws IOException, SAXException;

  public void endPrefixMapping(String prefix)
    throws IOException, SAXException;
  
  public void startElement(String namespaceURI, String localName, String qName)
    throws IOException, SAXException;

  public void attribute(String namespaceURI, String localName, String qName,
                        String value)
    throws IOException, SAXException;

  public void endElement(String namespaceURI, String localName, String qName)
    throws IOException, SAXException;

  public void text(String text)
    throws IOException, SAXException;
  
  public void text(char []buffer, int offset, int length)
    throws IOException, SAXException;

  public void cdata(String text)
    throws IOException, SAXException;
  
  public void cdata(char []buffer, int offset, int length)
    throws IOException, SAXException;

  public boolean getEscapeText();
  
  public void setEscapeText(boolean isEscaped);
  
  public void processingInstruction(String name, String value)
    throws IOException, SAXException;
  
  public void comment(String value)
    throws IOException, SAXException;
}
