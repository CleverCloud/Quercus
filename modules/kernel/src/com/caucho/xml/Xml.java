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

import com.caucho.util.FreeList;

import org.w3c.dom.DOMImplementation;

/**
 * XML parser interface.  The parser can parse directly into the DOM or it
 * can be used as a SAX parser.
 *
 * <p>To parse a file into a DOM Document use
 * <pre><code>
 * Document doc = new Xml().parseDocument("foo.xml");
 * </code></pre>
 *
 * <p>To parse a string into a DOM Document use
 * <pre><code>
 * String xml = "&lt;top>small test&lt;/top>";
 * Document doc = new Xml().parseDocumentString(xml);
 * </code></pre>
 *
 * <p>To parse a file using the SAX API use
 * <pre><code>
 * Xml xml = new Xml();
 * xml.setContentHandler(myContentHandler);
 * xml.parse("foo.xml");
 * </code></pre>
 */
public class Xml extends XmlParser {
  private static FreeList<Xml> _freeList = new FreeList<Xml>(16);
  
  /**
   * Create a new strict XML parser
   */
  public Xml()
  {
    super(new XmlPolicy(), null);

    init();
  }

  /**
   * Initialize the parser.
   */
  public void init()
  {
    super.init();
    
    _strictComments = true;
    _strictAttributes = true;
    _strictCharacters = true;
    _strictXml = true;
    _singleTopElement = true;
    _optionalTags = false;
  }

  /**
   * Creates an Xml parser.
   */
  public static Xml create()
  {
    Xml xml = _freeList.allocate();
    if (xml == null)
      xml = new Xml();
    xml.init();

    return xml;
  }

  /**
   * Frees an Xml parser.
   */
  public void free()
  {
    _owner = null;
    _contentHandler = null;
    _entityResolver = null;
    _dtdHandler = null;
    _errorHandler = null;
    _dtd = null;
  
    _freeList.free(this);
  }

  /**
   * Create a new DOM document
   */
  static public CauchoDocument createDocument()
  {
    return new QDocument();
  }

  /**
   * Create a new DOM implementation
   */
  static public DOMImplementation createDOMImplementation()
  {
    return new QDOMImplementation();
  }
}
