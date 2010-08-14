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

package com.caucho.jaxb.property;

import java.io.IOException;

import static javax.xml.XMLConstants.*;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElementWrapper;

import javax.xml.namespace.QName;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Node;

import com.caucho.jaxb.BinderImpl;
import com.caucho.jaxb.NodeIterator;
import com.caucho.jaxb.mapping.ConstantNamer;
import com.caucho.jaxb.mapping.Namer;
import com.caucho.util.L10N;

/**
 * represents a property in a skeleton
 */
public class WrapperProperty extends Property {
  public static final L10N L = new L10N(WrapperProperty.class);

  private final Namer _wrappedNamer;
  private final String _name;
  private final String _namespace;
  private final QName _wrappedQName;
  private final QName _wrapperQName;
  private final Property _property;
  private final boolean _nillable;

  public WrapperProperty(Property property, 
                         XmlElementWrapper elementWrapper,
                         String wrappedNamespace, String wrappedName)
  {
    _wrappedQName = new QName(wrappedNamespace, wrappedName);

    if ("##default".equals(elementWrapper.name()))
      _name = wrappedName;
    else
      _name = elementWrapper.name();

    if ("##default".equals(elementWrapper.namespace()))
      _namespace = wrappedNamespace;
    else
      _namespace = elementWrapper.namespace();

    _wrapperQName = new QName(_namespace, _name);
    _property = property;
    _nillable = elementWrapper.nillable();

    _wrappedNamer = ConstantNamer.getConstantNamer(_wrappedQName);
  }

  public WrapperProperty(Property property, 
                         QName wrapperQName, QName wrappedQName)
  {
    _name = wrapperQName.getLocalPart();

    if (wrapperQName.getNamespaceURI() != null &&
        ! "".equals(wrapperQName.getNamespaceURI()))
      _namespace = wrapperQName.getNamespaceURI();
    else
      _namespace = null;

    _wrapperQName = wrapperQName;
    _wrappedQName = wrappedQName;
    _nillable = false;
    _property = property;

    _wrappedNamer = ConstantNamer.getConstantNamer(_wrappedQName);
  }

  public QName getWrapperQName()
  {
    return _wrapperQName;
  }

  //
  // Schema generation methods
  // 
  public boolean isXmlPrimitiveType()
  {
    return _property.isXmlPrimitiveType(); // XXX
  }

  public String getMinOccurs()
  {
    return "0";
  }

  public String getMaxOccurs()
  {
    return null;
  }

  public boolean isNullable()
  {
    return false;
  }

  public QName getSchemaType()
  {
    return _property.getSchemaType(); // XXX
  }

  //
  // R/W methods
  //

  public Object read(Unmarshaller u, XMLStreamReader in, Object previous)
    throws IOException, XMLStreamException, JAXBException
  {
    if (_nillable) {
      String nil = in.getAttributeValue(W3C_XML_SCHEMA_INSTANCE_NS_URI, "nil");

      if ("true".equals(nil)) {
        in.nextTag(); // skip wrapper
        // XXX make sure nothing is in between the start and end wrapper
        in.next(); // skip wrapper

        return null;
      }
    }

    in.nextTag(); // skip wrapper

    while (in.getEventType() == in.START_ELEMENT && 
           _wrappedQName.equals(in.getName()))
      previous = _property.read(u, in, previous);

    in.nextTag();

    return previous;
  }
  
  public Object bindFrom(BinderImpl binder, NodeIterator node, Object previous)
    throws IOException,JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public void write(Marshaller m, XMLStreamWriter out, 
                    Object value, Namer namer)
    throws IOException, XMLStreamException, JAXBException
  {
    if (value != null || _nillable) {
      if (_namespace != null && ! "".equals(_namespace))
        out.writeStartElement(_namespace, _name);
      else
        out.writeStartElement(_name);

      if (value == null && _nillable) {
        out.writeNamespace("xsi", W3C_XML_SCHEMA_INSTANCE_NS_URI);
        out.writeAttribute(W3C_XML_SCHEMA_INSTANCE_NS_URI, "nil", "true");
      }
    }

    _property.write(m, out, value, _wrappedNamer);

    if (value != null || _nillable) {
      out.writeEndElement();
    }
  }

  public Node bindTo(BinderImpl binder, Node node,
                     Object value, Namer namer)
    throws IOException,JAXBException
  {
    throw new UnsupportedOperationException();
  }
}
