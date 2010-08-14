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

package com.caucho.xml2;

import com.caucho.xml.QName;

import org.xml.sax.*;

import java.io.IOException;
import java.util.Locale;

/**
 * A fast XML parser.
 */
public class XMLParserImpl implements Parser {
  // Xerces uses the following
  public static final String XMLNS = "http://www.w3.org/2000/xmlns/";

  static final QName DOC_NAME = new QName(null, "#document", null);
  static final QName TEXT_NAME = new QName(null, "#text", null);
  static final QName JSP_NAME = new QName(null, "#jsp", null);
  static final QName WHITESPACE_NAME = new QName(null, "#whitespace", null);
  static final QName JSP_ATTRIBUTE_NAME = new QName("xtp", "jsp-attribute", null);

  private EntityResolver _entityResolver;
  private ErrorHandler _errorHandler;
  private DocumentHandler _documentHandler;
  private DTDHandler _dtdHandler;
  private Locale _locale;

  /**
   * Sets the SAX errorHandler.
   *
   * @param handler the error handler
   */
  public void setErrorHandler(ErrorHandler handler)
  {
    _errorHandler = handler;
  }

  /**
   * Sets the SAX entityResolver.
   *
   * @param resolver the entity resolver
   */
  public void setEntityResolver(EntityResolver resolver)
  {
    _entityResolver = resolver;
  }

  /**
   * Sets the SAX document handler
   *
   * @param handler the document handler
   */
  public void setDocumentHandler(DocumentHandler handler)
  {
    _documentHandler = handler;
  }

  /**
   * Sets the SAX DTD handler
   *
   * @param handler the dtd handler
   */
  public void setDTDHandler(DTDHandler handler)
  {
    _dtdHandler = handler;
  }

  /**
   * Sets the SAX locale
   *
   * @param locale locale
   */
  public void setLocale(Locale locale)
  {
    _locale = locale;
  }
  
  /**
   * parses the input source.
   *
   * @param source the source to parse from
   */
  public void parse(InputSource source)
    throws IOException, SAXException
  {
    
  }
  
  /**
   * parses the file at the given string
   *
   * @param url the source url to parse from
   */
  public void parse(String source)
    throws IOException, SAXException
  {
    
  }
}
