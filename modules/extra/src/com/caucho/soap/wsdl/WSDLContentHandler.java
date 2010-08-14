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

package com.caucho.soap.wsdl;

import com.caucho.log.Log;
import com.caucho.util.IntMap;
import com.caucho.util.L10N;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.namespace.QName;
import java.util.logging.Logger;

/**
 * WSDL Content handler
 */
public class WSDLContentHandler extends DefaultHandler {
  private final static Logger log = Log.open(WSDLContentHandler.class);
  private final static L10N L = new L10N(WSDLContentHandler.class);

  private final static String WSDL = "http://schemas.xmlsoap.org/wsdl/";

  private final static int TOP = 0;
  private final static int WSDL_DEFINITIONS = 1;
  private final static int WSDL_IMPORT = 2;
  private final static int WSDL_TYPES = 3;
  private final static int WSDL_MESSAGE = 4;
  private final static int WSDL_PORT_TYPE = 5;
  private final static int WSDL_BINDING = 6;
  private final static int WSDL_SERVICE = 7;
  private final static int WSDL_PART = 8;
  private final static int WSDL_OPERATION = 9;
  private final static int WSDL_INPUT = 10;
  private final static int WSDL_OUTPUT = 11;
  private final static int WSDL_FAULT = 12;
  private final static int WSDL_PORT = 13;
  private final static int WSDL_BINDING_OPERATION = 14;

  private final static IntMap _keywords = new IntMap();

  private int _state = TOP;

  /**
   * Starts a WSDL element.
   */
  public void startElement (String uri, String localName,
                            String qName, Attributes attributes)
    throws SAXException
  {
    QName qname = new QName(uri, localName);

    switch (_state) {
    case TOP:
      switch (getKeyword(qname)) {
      case WSDL_DEFINITIONS:
        _state = WSDL_DEFINITIONS;
        break;

      default:
        throw error(L.l("Expected <wsdl:descriptions> at <{0}>.", qName));
      }
      break;
      
    case WSDL_DEFINITIONS:
      switch (getKeyword(qname)) {
      case WSDL_IMPORT:
        _state = WSDL_IMPORT;
        break;

      case WSDL_TYPES:
        _state = WSDL_TYPES;
        break;

      case WSDL_MESSAGE:
        _state = WSDL_MESSAGE;
        break;

      case WSDL_PORT_TYPE:
        _state = WSDL_PORT_TYPE;
        break;

      case WSDL_BINDING:
        _state = WSDL_BINDING;
        break;

      case WSDL_SERVICE:
        _state = WSDL_SERVICE;
        break;

      default:
        throw error(L.l("<{0}> is an unexpected tag.", qName));
      }
      break;
      
    case WSDL_MESSAGE:
      switch (getKeyword(qname)) {
      case WSDL_PART:
        _state = WSDL_PART;
        break;
        
      default:
        throw error(L.l("<{0}> is an unexpected tag.", qName));
      }
      break;
      
    case WSDL_PORT_TYPE:
      switch (getKeyword(qname)) {
      case WSDL_OPERATION:
        _state = WSDL_OPERATION;
        break;
        
      default:
        throw error(L.l("<{0}> is an unexpected tag.", qName));
      }
      break;
      
    case WSDL_BINDING:
      switch (getKeyword(qname)) {
      case WSDL_OPERATION:
        _state = WSDL_BINDING_OPERATION;
        break;
        
      default:
        throw error(L.l("<{0}> is an unexpected tag.", qName));
      }
      break;
      
    case WSDL_SERVICE:
      switch (getKeyword(qname)) {
      case WSDL_PORT:
        _state = WSDL_PORT;
        break;
        
      default:
        throw error(L.l("<{0}> is an unexpected tag.", qName));
      }
      break;

    default:
      throw error(L.l("<{0}> is an unexpected tag.", qName));
    }
  }
  
  /**
   * Ends a WSDL element.
   */
  public void endElement (String uri, String localName, String qName)
    throws SAXException
  {
    QName qname = new QName(uri, localName);

    switch (_state) {
    case WSDL_DEFINITIONS:
      if (getKeyword(qname) == WSDL_DEFINITIONS)
        _state = TOP;
      else
        throw error(L.l("</{0}> is an unexpected end tag.", qName));
      break;
      
    case WSDL_IMPORT:
      if (getKeyword(qname) == WSDL_IMPORT)
        _state = WSDL_DEFINITIONS;
      else
        throw error(L.l("</{0}> is an unexpected end tag.", qName));
      break;
      
    case WSDL_TYPES:
      if (getKeyword(qname) == WSDL_TYPES)
        _state = WSDL_DEFINITIONS;
      else
        throw error(L.l("</{0}> is an unexpected end tag.", qName));
      break;
      
    case WSDL_MESSAGE:
      if (getKeyword(qname) == WSDL_MESSAGE)
        _state = WSDL_DEFINITIONS;
      else
        throw error(L.l("</{0}> is an unexpected end tag.", qName));
      break;
      
    case WSDL_PORT_TYPE:
      if (getKeyword(qname) == WSDL_PORT_TYPE)
        _state = WSDL_DEFINITIONS;
      else
        throw error(L.l("</{0}> is an unexpected end tag.", qName));
      break;
      
    case WSDL_BINDING:
      if (getKeyword(qname) == WSDL_BINDING)
        _state = WSDL_DEFINITIONS;
      else
        throw error(L.l("</{0}> is an unexpected end tag.", qName));
      break;
      
    case WSDL_SERVICE:
      if (getKeyword(qname) == WSDL_SERVICE)
        _state = WSDL_DEFINITIONS;
      else
        throw error(L.l("</{0}> is an unexpected end tag.", qName));
      break;
      
    case WSDL_PART:
      if (getKeyword(qname) == WSDL_PART)
        _state = WSDL_MESSAGE;
      else
        throw error(L.l("</{0}> is an unexpected end tag.", qName));
      break;
      
    case WSDL_OPERATION:
      if (getKeyword(qname) == WSDL_OPERATION)
        _state = WSDL_PORT_TYPE;
      else
        throw error(L.l("</{0}> is an unexpected end tag.", qName));
      break;
      
    case WSDL_INPUT:
      if (getKeyword(qname) == WSDL_INPUT)
        _state = WSDL_OPERATION;
      else
        throw error(L.l("</{0}> is an unexpected end tag.", qName));
      break;
      
    case WSDL_OUTPUT:
      if (getKeyword(qname) == WSDL_OUTPUT)
        _state = WSDL_OPERATION;
      else
        throw error(L.l("</{0}> is an unexpected end tag.", qName));
      break;
      
    case WSDL_FAULT:
      if (getKeyword(qname) == WSDL_FAULT)
        _state = WSDL_OPERATION;
      else
        throw error(L.l("</{0}> is an unexpected end tag.", qName));
      break;
      
    case WSDL_PORT:
      if (getKeyword(qname) == WSDL_PORT)
        _state = WSDL_SERVICE;
      else
        throw error(L.l("</{0}> is an unexpected end tag.", qName));
      break;
      
    case WSDL_BINDING_OPERATION:
      if (getKeyword(qname) == WSDL_OPERATION)
        _state = WSDL_BINDING;
      else
        throw error(L.l("</{0}> is an unexpected end tag.", qName));
      break;

    default:
      throw error(L.l("</{0}> is an unexpected end tag.", qName));
    }
  }

  /**
   * Returns the keyword for the name.
   */
  private int getKeyword(QName qname)
  {
    return _keywords.get(qname);
  }

  /**
   * Throws an error.
   */
  private SAXException error(String msg)
  {
    return new SAXException(msg);
  }

  static {
    _keywords.put(new QName(WSDL, "descriptions"), WSDL_DEFINITIONS);
    _keywords.put(new QName(WSDL, "import"), WSDL_IMPORT);
    _keywords.put(new QName(WSDL, "types"), WSDL_TYPES);
    _keywords.put(new QName(WSDL, "message"), WSDL_MESSAGE);
    _keywords.put(new QName(WSDL, "portType"), WSDL_PORT_TYPE);
    _keywords.put(new QName(WSDL, "binding"), WSDL_BINDING);
    _keywords.put(new QName(WSDL, "service"), WSDL_SERVICE);
    _keywords.put(new QName(WSDL, "part"), WSDL_PART);
    _keywords.put(new QName(WSDL, "operation"), WSDL_OPERATION);
    _keywords.put(new QName(WSDL, "input"), WSDL_INPUT);
    _keywords.put(new QName(WSDL, "output"), WSDL_OUTPUT);
    _keywords.put(new QName(WSDL, "fault"), WSDL_FAULT);
    _keywords.put(new QName(WSDL, "port"), WSDL_PORT);
  }
}
