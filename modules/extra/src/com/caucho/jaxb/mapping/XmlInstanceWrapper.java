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
 * @author Emil Ong
 */

package com.caucho.jaxb.mapping;

import com.caucho.jaxb.BinderImpl;
import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.jaxb.JAXBUtil;
import com.caucho.jaxb.property.QNameProperty;
import com.caucho.util.L10N;
import com.caucho.xml.stream.StaxUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import static javax.xml.XMLConstants.*;
import javax.xml.bind.annotation.*;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.events.*;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;

import java.io.IOException;

import java.lang.annotation.*;
import java.lang.reflect.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 **/
public class XmlInstanceWrapper extends XmlMapping {
  private static final Logger log 
    = Logger.getLogger(XmlInstanceWrapper.class.getName());
  private static final L10N L = new L10N(XmlInstanceWrapper.class);
  private static final QName XSI_TYPE_NAME 
    = new QName(W3C_XML_SCHEMA_INSTANCE_NS_URI, "type", "xsi");

  private static final HashMap<QName,XmlInstanceWrapper> _instances =
    new HashMap<QName,XmlInstanceWrapper>();

  private final QName _type;

  private XmlInstanceWrapper(QName type)
  {
    super();

    _type = type;
  }

  public static XmlInstanceWrapper getInstance(QName type)
  {
    XmlInstanceWrapper instance = _instances.get(type);

    if (instance == null) {
      instance = new XmlInstanceWrapper(type);
      _instances.put(type, instance);
    }

    return instance;
  }

  public void write(Marshaller m, XMLStreamWriter out, Object obj)
    throws IOException, XMLStreamException, JAXBException
  {
    String typeString = StaxUtil.qnameToString(out, _type);
    StaxUtil.writeAttribute(out, XSI_TYPE_NAME, typeString);
  }

  public QName getQName(Object obj)
    throws JAXBException
  {
    return XSI_TYPE_NAME;
  }

  public void putQNames(Map<QName,XmlMapping> map)
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public void generateSchema(XMLStreamWriter out)
    throws JAXBException, XMLStreamException
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return "XmlInstanceWrapper[" + _type + "]";
  }
}
