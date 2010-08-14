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

package com.caucho.xml2.parsers;

import com.caucho.xml.QDOMImplementation;
import com.caucho.xml.Xml;
import com.caucho.xml.XmlParser;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import java.io.IOException;
import java.io.InputStream;

/**
 * JAXP document builder factory for strict XML parsing.
 */
public class AbstractDocumentBuilder extends DocumentBuilder {
  // The parser implementation.
  protected XmlParser _parser;

  public DOMImplementation getDOMImplementation()
  {
    return new QDOMImplementation();
  }

  /**
   * Parses the document based on an input source.
   *
   * @param is the SAX input source
   *
   * @return the parsed document
   */
  public Document parse(InputSource is)
    throws IOException, SAXException
  {
    return _parser.parseDocument(is);
  }

  /**
   * Parses the document based on an input stream.
   *
   * @param is the input stream.
   *
   * @return the parsed document
   */
  public Document parse(InputStream is)
    throws IOException, SAXException
  {
    return _parser.parseDocument(is);
  }

  /**
   * Parses the document based on an input stream.
   *
   * @param is the input stream.
   * @param systemId the stream's URL.
   *
   * @return the parsed document
   */
  public Document parse(InputStream is, String systemId)
    throws IOException, SAXException
  {
    return _parser.parseDocument(is, systemId);
  }

  public boolean isNamespaceAware()
  {
    return _parser.isNamespaceAware();
  }
    
  public boolean isValidating()
  {
    return false;
  }

  /**
   * Sets the callback to lookup included files.
   *
   * @param er the callback object to find included files.
   */
  public void setEntityResolver(EntityResolver er)
  {
    _parser.setEntityResolver(er);
  }
    
  public void setErrorHandler(ErrorHandler eh)
  {
    _parser.setErrorHandler(eh);
  }
    
  public Document newDocument()
  {
    return Xml.createDocument();
  }
}
