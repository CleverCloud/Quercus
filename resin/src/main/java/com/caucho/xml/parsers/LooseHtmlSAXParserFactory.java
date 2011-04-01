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

package com.caucho.xml.parsers;

import com.caucho.xml.LooseHtml;
import com.caucho.xml.XmlParser;

import org.xml.sax.Parser;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * JAXP SAX parser factory for loose HTML parsing.
 */
public class LooseHtmlSAXParserFactory extends SAXParserFactory {
  /**
   * Creates a new SAX Parser
   */
  public SAXParser newSAXParser()
  {
    return new LooseHtmlSAXParser();
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
    return false;
  }

  public void setFeature(String key, boolean value)
  {
  }

  class LooseHtmlSAXParser extends SAXParser {
    private XmlParser parser;

    LooseHtmlSAXParser()
    {
      parser = new LooseHtml();
    }
  
    public Parser getParser()
    {
      return parser;
    }

    public XMLReader getXMLReader()
    {
      return parser;
    }

    public Object getProperty(String key)
    {
      return null;
    }

    public void setProperty(String key, Object value)
    {
    }
    
    public boolean isNamespaceAware()
    {
      return true;
    }
    
    public boolean isValidating()
    {
      return false;
    }
  }
}
