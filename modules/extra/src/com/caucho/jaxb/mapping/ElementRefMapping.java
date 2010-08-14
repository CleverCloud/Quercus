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

import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.util.L10N;

import org.w3c.dom.Node;

import static javax.xml.XMLConstants.*;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshalException;

import javax.xml.bind.annotation.XmlElementRef;

import javax.xml.namespace.QName;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.lang.annotation.Annotation;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.io.IOException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.caucho.jaxb.accessor.Accessor;

import com.caucho.jaxb.property.ArrayProperty;
import com.caucho.jaxb.property.ListProperty;
import com.caucho.jaxb.property.MultiProperty;
import com.caucho.jaxb.property.Property;

import com.caucho.jaxb.skeleton.ClassSkeleton;

public class ElementRefMapping extends XmlMapping {
  private static final L10N L = new L10N(ElementRefMapping.class);

  public ElementRefMapping(JAXBContextImpl context, Accessor accessor)
  {
    super(context, accessor);
  }

  // XXX this method probably shouldn't have side effects
  public void putQNames(Map<QName,XmlMapping> map)
    throws JAXBException
  {
    Class cl = _accessor.getType();

    // XXX process this
    XmlElementRef elementRef = _accessor.getAnnotation(XmlElementRef.class);

    if (Collection.class.isAssignableFrom(cl)) {
      if (_accessor.getGenericType() instanceof ParameterizedType) {
        ParameterizedType ptype = 
          (ParameterizedType) _accessor.getGenericType();
        Type[] args = ptype.getActualTypeArguments();

        if (args.length != 1)
          throw new JAXBException(L.l("Collections annotated with @XmlElementRef must be parameterized: {0}", _accessor.getName()));
        else if (args[0] instanceof Class)
          cl = (Class) args[0];
        else
          throw new JAXBException(L.l("Unknown type {0} on field or property {1}", args[0], _accessor.getName()));
      }
      else
        throw new JAXBException(L.l("Collections annotated with @XmlElementRef must be parameterized: {0}", _accessor.getName()));
    }
    else if (cl.isArray())
      cl = cl.getComponentType();

    List<ClassSkeleton> skeletons = _context.getRootElements(cl);

    if (skeletons.size() == 0)
      throw new JAXBException(L.l("The type ({0}) of field {1} is unknown to this context", cl, _accessor.getName()));

    Map<QName,Property> qnameToPropertyMap = new HashMap<QName,Property>();
    Map<Class,Property> classToPropertyMap = new HashMap<Class,Property>();

    for (int i = 0; i < skeletons.size(); i++) {
      ClassSkeleton skeleton = skeletons.get(i);
      map.put(skeleton.getElementName(), this);

      QName qname = skeleton.getElementName();
      Property property = _context.createProperty(skeleton.getType());

      qnameToPropertyMap.put(qname, property);
      classToPropertyMap.put(skeleton.getType(), property);
    }

    _property = new MultiProperty(qnameToPropertyMap, classToPropertyMap);

    if (List.class.isAssignableFrom(_accessor.getType()))
      _property = new ListProperty(_property);

    else if (_accessor.getType().isArray()) {
      Class cType = _accessor.getType().getComponentType();
      _property = ArrayProperty.createArrayProperty(_property, cType);
    }
  }

  public void generateSchema(XMLStreamWriter out)
    throws JAXBException, XMLStreamException
  {
    out.writeEmptyElement(XML_SCHEMA_PREFIX, "element", 
                          W3C_XML_SCHEMA_NS_URI);

    XmlElementRef elementRef = _accessor.getAnnotation(XmlElementRef.class);

    if (_property.getMaxOccurs() != null)
      out.writeAttribute("maxOccurs", _property.getMaxOccurs());

    // XXX
    out.writeAttribute("ref", "XXX");
  }

  public QName getQName(Object obj)
    throws JAXBException
  {
    if (obj instanceof JAXBElement) {
      JAXBElement element = (JAXBElement) obj;

      return element.getName();
    }
    else {
      ClassSkeleton skeleton = _context.findSkeletonForObject(obj);

      if (skeleton == null || skeleton.getElementName() == null)
        throw new JAXBException(L.l("Cannot find root element name for object {0}", obj));

      return skeleton.getElementName();
    }
  }
}
