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
 * @author Adam Megacz
 */

package com.caucho.jaxb.property;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.caucho.jaxb.BinderImpl;
import com.caucho.jaxb.JAXBUtil;
import com.caucho.jaxb.NodeIterator;
import com.caucho.jaxb.mapping.Namer;
import com.caucho.xml.stream.StaxUtil;


/**
 * a Map property
 */
public class MapProperty extends Property {
  private static final QName _keyName = new QName("key");
  private static final QName _valueName = new QName("value");

  private static final Namer _keyNamer = new Namer() {
    public QName getQName(Object obj)
    {
      return _keyName;
    }
  };

  private static final Namer _valueNamer = new Namer() {
    public QName getQName(Object obj)
    {
      return _valueName;
    }
  };

  private Class _mapType;
  private Property _keyProperty; 
  private Property _valueProperty;

  public MapProperty(Class mapType, 
                     Property keyProperty, 
                     Property valueProperty)
  {
    _mapType = mapType;
    _keyProperty = keyProperty;
    _valueProperty = valueProperty;
  }

  public Object read(Unmarshaller u, XMLStreamReader in, Object previous)
    throws IOException, XMLStreamException, JAXBException
  {
    in.nextTag();

    Object obj = previous;
     
    if (obj == null) {
      try {
        obj = _mapType.newInstance();
      }
      catch (IllegalAccessException e) {
        throw new JAXBException(e);
      }
      catch (InstantiationException e) {
        throw new JAXBException(e);
      }
    }

    Map<Object,Object> map = (Map<Object,Object>) obj;

    while (in.getEventType() == in.START_ELEMENT && 
           "key".equals(in.getLocalName())) {
      Object key = _keyProperty.read(u, in, null);

      if (in.getEventType() != in.START_ELEMENT ||
          ! "value".equals(in.getLocalName()))
        throw new JAXBException("Key without value while reading map");

      Object value = _valueProperty.read(u, in, null);

      map.put(key, value);
    }

    in.nextTag();

    return map;
  }

  public Object bindFrom(BinderImpl binder, NodeIterator node, Object previous)
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public void write(Marshaller m, XMLStreamWriter out,
                    Object obj, Namer namer)
    throws IOException, XMLStreamException, JAXBException
  {
    if (obj != null) {
      Map<Object,Object> map = (Map<Object,Object>) obj;
      QName qname = namer.getQName(obj);

      StaxUtil.writeStartElement(out, qname);

      for (Map.Entry<Object,Object> entry : map.entrySet()) {
        _keyProperty.write(m, out, entry.getKey(), _keyNamer);
        _valueProperty.write(m, out, entry.getValue(), _valueNamer);
      }

      StaxUtil.writeEndElement(out, qname);
    }
  }

  public Node bindTo(BinderImpl binder, Node node,
                     Object obj, Namer namer)
    throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public void generateSchema(XMLStreamWriter out)
    throws XMLStreamException
  {
    throw new UnsupportedOperationException();
  }

  public QName getSchemaType()
  {
    throw new UnsupportedOperationException();
  }

  public boolean isXmlPrimitiveType()
  {
    return false;
  }
}
