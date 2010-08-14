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

package com.caucho.xml2.parsers;

import com.caucho.xml2.Xml;
import com.caucho.xml2.XmlParser;

import org.xml.sax.Parser;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * JAXP SAX parser factory for strict XML parsing.
 */
public class XmlSAXParserFactory extends SAXParserFactory {
  private int _namespaces = -1;
  private int _namespacePrefixes = -1;
  private int _validation = -1;

  public XmlSAXParserFactory()
  {
  }
  
  /**
   * Creates a new SAX Parser
   */
  public SAXParser newSAXParser()
  {
    return new XmlSAXParser(this);
  }

  public Object getProperty(String key)
  {
    return null;
  }

  public void setProperty(String key, Object value)
  {
  }

  public boolean getFeature(String key)
  {
    if (key.equals("http://xml.org/sax/features/namespaces"))
      return isNamespaceAware();
    else if (key.equals("http://xml.org/sax/features/namespace-prefixes"))
      return isNamespacePrefixes();
    else if (key.equals("http://xml.org/sax/features/validation"))
      return _validation != 0;
    else
      return false;
  }

  public void setFeature(String key, boolean value)
    throws SAXNotRecognizedException
  {
    if (key.equals("http://xml.org/sax/features/namespaces"))
      _namespaces = value ? 1 : 0;
    else if (key.equals("http://xml.org/sax/features/namespace-prefixes"))
      _namespacePrefixes = value ? 1 : 0;
    else if (key.equals("http://xml.org/sax/features/validation"))
      _validation = value ? 1 : 0;
    else
      throw new SAXNotRecognizedException(key);
  }

  public boolean isNamespacePrefixes()
  {
    if (_namespacePrefixes >= 0)
      return _namespacePrefixes == 1;
    else if (_namespaces >= 0)
      return _namespaces == 1;
    else
      return false;
  }

  public boolean isNamespaceAware()
  {
    if (_namespacePrefixes >= 0)
      return true;
    else if (_namespaces >= 0)
      return _namespaces == 1;
    else
      return super.isNamespaceAware();
  }

  public class XmlSAXParser extends SAXParser {
    private XmlSAXParserFactory _factory;
    private XmlParser _parser;

    XmlSAXParser(XmlSAXParserFactory factory)
    {
      _factory = factory;
      _parser = new Xml();
      _parser.setNamespaceAware(_factory.isNamespaceAware());
      _parser.setNamespacePrefixes(_factory.isNamespacePrefixes());
      // _parser.setValidating(_factory.isValidating());
      _parser.setValidating(true);
    }

    public Object getProperty(String key)
    {
      return null;
    }

    public void setProperty(String key, Object value)
    {
    }

    public Parser getParser()
    {
      return _parser;
    }

    public XMLReader getXMLReader()
    {
      return _parser;
    }

    /**
     * Returns true if the factory is namespace aware.
     */
    public boolean isNamespaceAware()
    {
      return _factory.isNamespaceAware();
    }

    /**
     * true if the factory is namespace aware.
     */
    public void setNamespaceAware(boolean isAware)
    {
      _factory.setNamespaceAware(isAware);
    }
    
    /**
     * True if the factory is validating
     */
    public boolean isValidating()
    {
      return _factory.isValidating();
    }
    
    /**
     * True if the factory is validating
     */
    public void setValidating(boolean isValidating)
    {
      _factory.setValidating(isValidating);
    }
  }
}
