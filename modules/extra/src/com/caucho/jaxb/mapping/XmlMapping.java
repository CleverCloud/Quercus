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
import com.caucho.jaxb.NodeIterator;
import com.caucho.jaxb.accessor.Accessor;
import com.caucho.jaxb.property.Property;
import com.caucho.jaxb.skeleton.ClassSkeleton;
import com.caucho.util.L10N;
import com.caucho.xml.stream.StaxUtil;

import org.w3c.dom.Node;

import static javax.xml.XMLConstants.*;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshalException;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSchemaTypes;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import javax.xml.namespace.QName;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

public abstract class XmlMapping implements Namer {
  private static final L10N L = new L10N(XmlMapping.class);

  public static final String XML_SCHEMA_PREFIX = "xsd";
  public static final String XML_INSTANCE_PREFIX = "xsi";
  public static final String XML_MIME_NS = "http://www.w3.org/2005/05/xmlmime";

  protected static boolean _generateRICompatibleSchema = true;

  protected Accessor _accessor;
  protected JAXBContextImpl _context;
  protected Property _property;
  protected boolean _nillable;

  protected XmlMapping()
  {
  }

  protected XmlMapping(JAXBContextImpl context, Accessor accessor)
  {
    _context = context;
    _accessor = accessor;
  }

  public static final Comparator<XmlMapping> nameComparator 
    = new Comparator<XmlMapping>() {
      public int compare(XmlMapping a1, XmlMapping a2) 
      {
        return a1._accessor.getName().compareTo(a2._accessor.getName());
      }

      public boolean equals(Object o)
      {
        return this == o;
      }
    };

  public static void setGenerateRICompatibleSchema(boolean compatible)
  {
    _generateRICompatibleSchema = compatible;
  }

  public static XmlMapping newInstance(JAXBContextImpl context, 
                                       Accessor accessor)
    throws JAXBException
  {
    XmlAnyAttribute xmlAnyAttribute = 
      accessor.getAnnotation(XmlAnyAttribute.class);
    XmlAnyElement xmlAnyElement = 
      accessor.getAnnotation(XmlAnyElement.class);
    XmlAttribute xmlAttribute = 
      accessor.getAnnotation(XmlAttribute.class);
    XmlElement xmlElement = 
      accessor.getAnnotation(XmlElement.class);
    XmlElementRef xmlElementRef = 
      accessor.getAnnotation(XmlElementRef.class);
    XmlElements xmlElements = 
      accessor.getAnnotation(XmlElements.class);
    XmlID xmlID = 
      accessor.getAnnotation(XmlID.class);
    XmlIDREF xmlIDREF = 
      accessor.getAnnotation(XmlIDREF.class);
    XmlValue xmlValue = 
      accessor.getAnnotation(XmlValue.class);

    // jaxb/02d1
    if (xmlID != null && ! String.class.equals(accessor.getType()))
      throw new JAXBException(L.l("Fields or properties annotated with XmlID must have type String: {0}", accessor));

    // check for conflicting annotations
    Annotation[] annotations = new Annotation[] {
      xmlAnyAttribute, xmlAnyElement, xmlAttribute, xmlElement, 
      xmlElementRef, xmlElements, xmlIDREF, xmlValue
    };

    int count = 0;
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < annotations.length; i++) {
      if (annotations[i] != null) {
        count++;
        sb.append("@" + annotations[i].getClass().getName());
        sb.append(", ");
      }
    }

    if (count > 1) {
      throw new JAXBException(L.l("Annotations {0} cannot be used together on a single field or property", sb.toString()));
    }

    if (xmlValue != null)
      return new XmlValueMapping(context, accessor);

    else if (xmlAnyAttribute != null)
      return new AnyAttributeMapping(context, accessor);

    else if (xmlAttribute != null)
      return new AttributeMapping(context, accessor);

    else if (xmlElementRef != null)
      return new ElementRefMapping(context, accessor);

    else if (xmlElements != null)
      return new ElementsMapping(context, accessor);

    else if (xmlAnyElement != null)
      return new AnyElementMapping(context, accessor);

    else
      return new ElementMapping(context, accessor);
  }

  // accessor methods

  public Accessor getAccessor()
  {
    return _accessor;
  }

  public QName getSchemaType()
  {
    return _property.getSchemaType();
  }

  // utility methods

  public void putQNames(Map<QName,XmlMapping> map)
    throws JAXBException
  {
  }

  protected QName qnameFromXmlElement(XmlElement element)
  {
    String name = _accessor.getName();
    String namespace = null;

    XmlSchema xmlSchema = _accessor.getPackageAnnotation(XmlSchema.class);

    if (xmlSchema != null &&
        xmlSchema.elementFormDefault() == XmlNsForm.QUALIFIED)
      namespace = xmlSchema.namespace();

    if (! element.name().equals("##default"))
      name = element.name();

    if (! element.namespace().equals("##default"))
      namespace = element.namespace();

    if (namespace == null)
      return new QName(name);
    else
      return new QName(namespace, name);
  }

  // output methods

  public void write(Marshaller m, XMLStreamWriter out, Object obj)
    throws IOException, JAXBException, XMLStreamException
  {
    Object value = _accessor.get(obj);

    ArrayList<XmlMapping> attributes = null;

    if (value == null && _nillable) {
      attributes = new ArrayList<XmlMapping>();
      attributes.add(NilWrapper.INSTANCE);
    }

    // XXX: interface/enum
    if (_property != null)
      _property.write(m, out, value, this, obj, attributes);
  }

  public void write(Marshaller m, XMLStreamWriter out,
                    Object obj, ArrayList<XmlMapping> attributes)
    throws IOException, JAXBException, XMLStreamException
  {
    Object value = _accessor.get(obj);

    if (value == null && _nillable) {
      if (attributes == null)
        attributes = new ArrayList<XmlMapping>();

      attributes.add(NilWrapper.INSTANCE);
    }

    _property.write(m, out, value, this, obj, attributes);
  }

  public Node bindTo(BinderImpl binder, Node node, Object obj)
    throws IOException, JAXBException
  {
    return _property.bindTo(binder, node, _accessor.get(obj), this);
  }

  // input methods
 
  public void readAttribute(XMLStreamReader in, int i, Object parent)
    throws IOException, XMLStreamException, JAXBException
  {
    throw new UnsupportedOperationException(L.l("Internal JAXB error: this field or property does not map to an attribute {0}", _accessor));
  }

  public void read(Unmarshaller u, XMLStreamReader in, 
                     Object parent, ClassSkeleton attributed)
    throws IOException, XMLStreamException, JAXBException
  {
    Object value = 
      _property.read(u, in, _accessor.get(parent), attributed, parent);

    _accessor.set(parent, value);
  }

  public void read(Unmarshaller u, XMLStreamReader in, Object parent)
    throws IOException, XMLStreamException, JAXBException
  {
    Object value = _property.read(u, in, _accessor.get(parent));
    _accessor.set(parent, value);
  }

  public void bindFrom(BinderImpl binder, NodeIterator node, Object obj)
    throws IOException, JAXBException
  {
    Object value = _property.bindFrom(binder, node, _accessor.get(obj));
    _accessor.set(obj, value);
  }

  // abstract methods

  public abstract QName getQName(Object obj)
    throws JAXBException;

  public abstract void generateSchema(XMLStreamWriter out)
    throws JAXBException, XMLStreamException;

}
