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
import com.caucho.jaxb.NodeIterator;
import com.caucho.util.L10N;
import com.caucho.xml.stream.StaxUtil;

import org.w3c.dom.Node;

import static javax.xml.XMLConstants.*;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlMimeType;

import javax.xml.namespace.QName;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.lang.annotation.Annotation;

import java.io.IOException;

import java.util.Map;

import com.caucho.jaxb.accessor.Accessor;

import com.caucho.jaxb.property.CDataProperty;
import com.caucho.jaxb.property.QNameProperty;

import com.caucho.jaxb.skeleton.ClassSkeleton;

public class AttributeMapping extends SingleQNameXmlMapping {
  private static final L10N L = new L10N(AttributeMapping.class);

  public AttributeMapping(JAXBContextImpl context, Accessor accessor)
    throws JAXBException
  {
    super(context, accessor);

    XmlAttribute attribute = accessor.getAnnotation(XmlAttribute.class);

    String name = accessor.getName();
    String namespace = null;

    if (attribute != null) {
      if (! attribute.name().equals("##default"))
        name = attribute.name();

      if (! attribute.namespace().equals("##default"))
        namespace = attribute.namespace();
    }

    if (namespace == null)
      _qname = new QName(name);
    else
      _qname = new QName(namespace, name);

    XmlMimeType xmlMimeType = accessor.getAnnotation(XmlMimeType.class);
    String mimeType = null;
    
    if (xmlMimeType != null)
      mimeType = xmlMimeType.value();

    // XXX XmlList value?
    _property = 
      _context.createProperty(accessor.getGenericType(), false, mimeType, true);

    if (! (_property instanceof CDataProperty) &&
        ! (_property instanceof QNameProperty))
      throw new JAXBException(L.l("Attributes must be simple XML types: {0}", _property));
  }

  public void write(Marshaller m, XMLStreamWriter out, Object obj)
    throws IOException, XMLStreamException, JAXBException
  {
    Object value = _accessor.get(obj);

    if (value != null) {
      String output = null;
      
      if (_property instanceof CDataProperty)
        output = ((CDataProperty) _property).write(value);

      else if (_property instanceof QNameProperty)
        output = StaxUtil.qnameToString(out, (QName) value);

      else
        throw new JAXBException(L.l("Invalid property for attribute: {0}", 
                                    _property));

      StaxUtil.writeAttribute(out, _qname, output);
    }
  }

  // input methods

  public void readAttribute(XMLStreamReader in, int i, Object parent)
    throws IOException, XMLStreamException, JAXBException
  {
    Object value = _property.readAttribute(in, i);
    _accessor.set(parent, value);
  }

  public void read(Unmarshaller u, XMLStreamReader in, 
                   Object parent, ClassSkeleton attributed)
    throws IOException, XMLStreamException, JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public void read(Unmarshaller u, XMLStreamReader in, Object parent)
    throws IOException, XMLStreamException, JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public void bindFrom(BinderImpl binder, NodeIterator node, Object obj)
    throws IOException, JAXBException
  {
    throw new UnsupportedOperationException();
  }

 public void generateSchema(XMLStreamWriter out)
    throws JAXBException, XMLStreamException
  {
    out.writeEmptyElement(XML_SCHEMA_PREFIX, "attribute", 
        W3C_XML_SCHEMA_NS_URI);

    // See http://forums.java.net/jive/thread.jspa?messageID=167171
    // Primitives are always required

    XmlAttribute attribute = _accessor.getAnnotation(XmlAttribute.class);

    if (attribute.required() || 
        (_generateRICompatibleSchema && _accessor.getType().isPrimitive()))
      out.writeAttribute("use", "required");

    XmlID xmlID = _accessor.getAnnotation(XmlID.class);

    if (xmlID != null)
      out.writeAttribute("type", "xsd:ID"); // jaxb/22d0
    else {
      String type = 
        StaxUtil.qnameToString(out, _property.getSchemaType());

      out.writeAttribute("type", type);
    }

    out.writeAttribute("name", _qname.getLocalPart());
  }
}
