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

package com.caucho.jsp;

import com.caucho.vfs.Vfs;
import com.caucho.xml.QName;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Generates the nodes for JSP code.
 */
public class JspContentHandler extends DefaultHandler {
  private JspBuilder _builder;
  private Locator _locator;

  public JspContentHandler(JspBuilder builder)
  {
    _builder = builder;
  }

  /**
   * Sets the document locator
   */
  public void setDocumentLocator(Locator locator)
  {
    _locator = locator;
  }

  /**
   * Starts the document.
   */
  public void startDocument()
    throws SAXException
  {
    try {
      setLocation();
      _builder.startDocument();
    } catch (JspParseException e) {
      throw new SAXException(e);
    }
  }

  /**
   * Ends the document.
   */
  public void endDocument()
    throws SAXException
  {
    try {
      setLocation();
      _builder.endDocument();
    } catch (JspParseException e) {
      throw new SAXException(e);
    }
  }

  /**
   * Adds characters.
   */
  public void characters(char []buf, int offset, int length)
    throws SAXException
  {
    try {
      setLocation();

      if (_builder.getGenerator().isELIgnore()
          || _builder.isTagDependent()) {
        String s = new String(buf, offset, length);
      
        _builder.text(s);
      }
      else
        addText(buf, offset, length);
    } catch (JspParseException e) {
      throw new SAXException(e);
    }
  }

  /**
   * Adds text, checking for JSP-EL
   */
  private void addText(char []buf, int offset, int length)
    throws JspParseException
  {
    int end = offset + length;
    int begin = offset;

    while (offset < end) {
      if (buf[offset] != '$')
        offset++;
      else if (end <= offset + 1)
        offset++;
      else if (buf[offset + 1] != '{')
        offset++;
      else {
        if (begin < offset)
          _builder.text(new String(buf, begin, offset - begin));

        begin = offset;
        offset += 2;
        while (offset < end && buf[offset] != '}') {
          if (buf[offset] == '\'') {
            for (offset++; offset < end && buf[offset] != '\''; offset++) {
            }

            if (offset < end)
              offset++;
          }
          else if (buf[offset] == '"') {
            for (offset++; offset < end && buf[offset] != '"'; offset++) {
            }

            if (offset < end)
              offset++;
          }
          else
            offset++;
        }

        if (offset < end)
          offset++;

        String value = new String(buf, begin, offset - begin);

        QName qname = new QName("resin-c", "out", JspParser.JSTL_CORE_URI);

        _builder.startElement(qname);
        _builder.attribute(new QName("value"), value);
        _builder.attribute(new QName("escapeXml"), "false");
        _builder.endAttributes();
        _builder.endElement("resin-c:out");

        begin = offset;
      }
    }

    if (begin < offset)
      _builder.text(new String(buf, begin, offset - begin));
  }

  /**
   * Starts a prefix mapping.
   */
  public void startPrefixMapping(String prefix, String uri)
    throws SAXException
  {
    try {
      _builder.startPrefixMapping(prefix, uri);
    } catch (JspParseException e) {
      throw new SAXException(e);
    }
    
    /*
    _builder.getParseState().pushNamespace(prefix, uri);
    ParseTagManager manager = _builder.getTagManager();
    */

    /*
    try {
      manager.addTaglib(prefix, uri);
    } catch (JspParseException e) {
      throw new SAXException(e);
    }
    */
  }

  /**
   * Ends a prefix mapping.
   */
  public void endPrefixMapping(String prefix)
    throws SAXException
  {
    _builder.getParseState().popNamespace(prefix);
    ParseTagManager manager = _builder.getTagManager();

    /*
    try {
      manager.addTaglib(prefix, uri);
    } catch (JspParseException e) {
      throw new SAXException(e);
    }
    */
  }

  /**
   * Starts an element.
   */
  public void startElement(String uri, String localName,
                           String qName, Attributes atts)
    throws SAXException
  {
    try {
      setLocation();
      _builder.startElement(new QName(qName, uri));

      for (int i = 0; i < atts.getLength(); i++) {
        setLocation();
        _builder.attribute(new QName(atts.getQName(i), atts.getURI(i)),
                           atts.getValue(i));
      }
      
      setLocation();
      _builder.endAttributes();
    } catch (JspParseException e) {
      throw new SAXException(e);
    }
  }

  /**
   * Ends an element.
   */
  public void endElement (String uri, String localName, String qName)
    throws SAXException
  {
    try {
      setLocation();
      _builder.endElement(qName);
    } catch (JspParseException e) {
      throw new SAXException(e);
    }
  }

  /**
   * Adds a processing instruction
   */
  public void processingInstruction(String key, String value)
    throws SAXException
  {
    try {
      _builder.text("<?" + key + " " + value + "?>");
    } catch (JspParseException e) {
      throw new SAXException(e);
    }
  }

  /**
   * Sets the location.
   */
  private void setLocation()
  {
    if (_locator != null) {
      _builder.setLocation(Vfs.lookup(_locator.getSystemId()),
                           _locator.getSystemId(),
                           _locator.getLineNumber());
    }
  }
}
