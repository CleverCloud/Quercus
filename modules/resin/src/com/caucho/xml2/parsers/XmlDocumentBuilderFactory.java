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

import org.w3c.dom.DOMImplementation;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * JAXP document builder factory for strict XML parsing.
 */
public class XmlDocumentBuilderFactory extends DocumentBuilderFactory {
  public XmlDocumentBuilderFactory()
  {
  }
  
  /**
   * Creates a new SAX Parser
   */
  public DocumentBuilder newDocumentBuilder()
  {
    return new XmlDocumentBuilder();
  }

  public Object getAttribute(String key)
  {
    return null;
  }

  public void setAttribute(String key, Object value)
  {
  }

  public boolean getFeature(String key)
  {
    return false;
  }

  public void setFeature(String key, boolean value)
  {
  }

  public DOMImplementation getDOMImplementation()
  {
    return new QDOMImplementation();
  }

  /**
   * Document builder created from the factory.
   */
  class XmlDocumentBuilder extends AbstractDocumentBuilder {
    /**
     * Creates a new document builder.
     */
    XmlDocumentBuilder()
    {
      _parser = new Xml();
      _parser.setConfig(XmlDocumentBuilderFactory.this);
    }
  }
}
