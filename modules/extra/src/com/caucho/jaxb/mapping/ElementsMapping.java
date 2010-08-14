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
import com.caucho.util.L10N;
import com.caucho.xml.stream.StaxUtil;

import org.w3c.dom.Node;

import static javax.xml.XMLConstants.*;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshalException;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlList;

import javax.xml.namespace.QName;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.lang.annotation.Annotation;

import java.io.IOException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.caucho.jaxb.accessor.Accessor;

import com.caucho.jaxb.property.ArrayProperty;
import com.caucho.jaxb.property.ListProperty;
import com.caucho.jaxb.property.MultiProperty;
import com.caucho.jaxb.property.Property;

import com.caucho.jaxb.skeleton.ClassSkeleton;

public class ElementsMapping extends XmlMapping {
  private static final L10N L = new L10N(ElementsMapping.class);

  private Map<Class,QName> _qnameMap;

  public ElementsMapping(JAXBContextImpl context, Accessor accessor)
    throws JAXBException
  {
    super(context, accessor);
    
    if (accessor.getAnnotation(XmlList.class) != null)
      throw new JAXBException(L.l("@XmlList cannot be used with @XmlElements"));

    XmlElements elements = accessor.getAnnotation(XmlElements.class);

    if (elements.value().length == 0) {
      // XXX special case : equivalent to unannotated
    }

    if (elements.value().length == 1) {
      // XXX special case : equivalent to @XmlElement
    }

    _qnameMap = new LinkedHashMap<Class,QName>();
    Map<QName,Property> qnameToPropertyMap = 
      new LinkedHashMap<QName,Property>();
    Map<Class,Property> classToPropertyMap = 
      new LinkedHashMap<Class,Property>();

    for (XmlElement element : elements.value()) {
      if (XmlElement.DEFAULT.class.equals(element.type()))
        throw new JAXBException(L.l("@XmlElement annotations in @XmlElements must specify a type"));

      QName qname = qnameFromXmlElement(element);
      Property property = _context.createProperty(element.type());

      qnameToPropertyMap.put(qname, property);
      classToPropertyMap.put(element.type(), property);
      _qnameMap.put(element.type(), qname);

      if (! property.isXmlPrimitiveType())
        _context.createSkeleton(element.type());
    }

    _property = new MultiProperty(qnameToPropertyMap, classToPropertyMap);

    if (List.class.isAssignableFrom(accessor.getType()))
      _property = new ListProperty(_property);

    else if (accessor.getType().isArray()) {
      Class cType = accessor.getType().getComponentType();
      _property = ArrayProperty.createArrayProperty(_property, cType);
    }

    // XXX Wrapper
  }

  public void putQNames(Map<QName,XmlMapping> map)
    throws JAXBException
  {
    for (QName qname : _qnameMap.values()) {
      if (map.containsKey(qname))
        throw new JAXBException(L.l("Class contains two elements with the same QName {0}", qname));

      map.put(qname, this);
    }
  }

  public void generateSchema(XMLStreamWriter out)
    throws JAXBException, XMLStreamException
  {
    out.writeStartElement(XML_SCHEMA_PREFIX, "choice", 
                          W3C_XML_SCHEMA_NS_URI);
    out.writeAttribute("minOccurs", "0");

    if (_property.getMaxOccurs() != null)
      out.writeAttribute("maxOccurs", _property.getMaxOccurs());

    MultiProperty multiProperty = null;

    if (_property instanceof ListProperty) {
      ListProperty listProperty = (ListProperty) _property;
      multiProperty = (MultiProperty) listProperty.getComponentProperty();
    }
    else
      multiProperty = (MultiProperty) _property;

    Collection<Property> properties = multiProperty.getProperties();

    XmlElements xmlElements = _accessor.getAnnotation(XmlElements.class);
    XmlElement[] elements = xmlElements.value();

    int i = 0; 

    for (Property property : properties) {
      out.writeEmptyElement(XML_SCHEMA_PREFIX, "element", 
                            W3C_XML_SCHEMA_NS_URI);

      String type = 
        StaxUtil.qnameToString(out, property.getSchemaType());

      out.writeAttribute("type", type);

      if ("##default".equals(elements[i].name()))
        out.writeAttribute("name", _accessor.getName());
      else 
        // XXX namespace
        out.writeAttribute("name", elements[i].name());

      i++;
    }

    out.writeEndElement(); // choice
  }

  public QName getQName(Object obj)
    throws JAXBException
  {
    return _qnameMap.get(obj.getClass());
  }
}
