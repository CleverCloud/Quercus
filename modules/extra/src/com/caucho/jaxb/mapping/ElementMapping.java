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
import com.caucho.xml.stream.StaxUtil;

import org.w3c.dom.Node;

import static javax.xml.XMLConstants.*;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSchemaTypes;

import javax.xml.namespace.QName;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collection;

import com.caucho.jaxb.accessor.Accessor;
import com.caucho.jaxb.property.WrapperProperty;
import com.caucho.jaxb.skeleton.ClassSkeleton;

public class ElementMapping extends SingleQNameXmlMapping {
  private static final L10N L = new L10N(ElementMapping.class);

  public ElementMapping(JAXBContextImpl context, Accessor accessor)
    throws JAXBException
  {
    super(context, accessor);

    // XXX respect the type from the XmlElement annotation

    XmlElement element = accessor.getAnnotation(XmlElement.class);
    boolean xmlList = (accessor.getAnnotation(XmlList.class) != null);

    XmlMimeType xmlMimeType = accessor.getAnnotation(XmlMimeType.class);
    String mimeType = null;
    
    if (xmlMimeType != null)
      mimeType = xmlMimeType.value();

    _property = _context.createProperty(accessor.getGenericType(), false, 
                                        mimeType, xmlList);

    if (element != null) {
      _qname = qnameFromXmlElement(element);
      _nillable = element.nillable();
    }
    else {
      XmlSchema xmlSchema = accessor.getPackageAnnotation(XmlSchema.class);

      if (xmlSchema != null &&
          xmlSchema.elementFormDefault() == XmlNsForm.QUALIFIED)
        _qname = new QName(xmlSchema.namespace(), accessor.getName());
      else
        _qname = new QName(accessor.getName());

      if (! _property.isXmlPrimitiveType())
        _context.createSkeleton(accessor.getType());
    }

    XmlElementWrapper wrapper = 
      accessor.getAnnotation(XmlElementWrapper.class);

    if (wrapper != null) {
      WrapperProperty wrapperProperty = 
        new WrapperProperty(_property, wrapper, 
                            _qname.getNamespaceURI(), 
                            _qname.getLocalPart());

      _property = wrapperProperty;
      _qname = wrapperProperty.getWrapperQName();
    }
  }

  public void generateSchema(XMLStreamWriter out)
    throws JAXBException, XMLStreamException
  {
    XmlElement element = _accessor.getAnnotation(XmlElement.class);
    XmlList xmlList = _accessor.getAnnotation(XmlList.class);

    if (xmlList != null) {
      out.writeStartElement(XML_SCHEMA_PREFIX, "element", 
                            W3C_XML_SCHEMA_NS_URI);
      out.writeAttribute("name", _accessor.getName());

      out.writeStartElement(XML_SCHEMA_PREFIX, "simpleType", 
                            W3C_XML_SCHEMA_NS_URI);

      out.writeEmptyElement(XML_SCHEMA_PREFIX, "list", 
                            W3C_XML_SCHEMA_NS_URI);

      String itemType = 
        StaxUtil.qnameToString(out, _property.getSchemaType());
      
      out.writeAttribute("itemType", itemType);

      out.writeEndElement(); // simpleType

      out.writeEndElement(); // element
    }
    else {
      out.writeEmptyElement(XML_SCHEMA_PREFIX, "element", 
                            W3C_XML_SCHEMA_NS_URI);

      if (! _generateRICompatibleSchema || 
          ! _accessor.getType().isPrimitive()) {

        if (element != null) {
          if (element.required())
            out.writeAttribute("minOccurs", "1");
          else
            out.writeAttribute("minOccurs", "0");

          if (element.nillable())
            out.writeAttribute("nillable", "true");
        }
        else
          out.writeAttribute("minOccurs", "0");
      }

      if (_property.getMaxOccurs() != null)
        out.writeAttribute("maxOccurs", _property.getMaxOccurs());

      if ((element != null && element.nillable()) ||
          Collection.class.isAssignableFrom(_accessor.getType()) ||
          (_accessor.getType().isArray() && 
           ! byte.class.equals(_accessor.getType().getComponentType())))
        out.writeAttribute("nillable", "true");

      String typeName = 
        StaxUtil.qnameToString(out, _property.getSchemaType());

      // jaxb/22d0
      if (_accessor.getAnnotation(XmlID.class) != null)
        typeName = "xsd:ID";

      // look for the XmlSchemaType
      XmlSchemaType xmlSchemaType = 
        _accessor.getAnnotation(XmlSchemaType.class);

      if (xmlSchemaType == null) {
        xmlSchemaType = _accessor.getPackageAnnotation(XmlSchemaType.class);

        if (xmlSchemaType != null) {
          if (XmlSchemaType.DEFAULT.class.equals(xmlSchemaType.type()))
            throw new JAXBException(L.l("@XmlSchemaType with name {0} on package {1} does not specify type", xmlSchemaType.name(), _accessor.getPackage().getName()));

          if (! _accessor.getType().equals(xmlSchemaType.type()))
            xmlSchemaType = null;
        }
      }

      if (xmlSchemaType == null) {
        XmlSchemaTypes xmlSchemaTypes = 
          _accessor.getPackageAnnotation(XmlSchemaTypes.class);

        if (xmlSchemaTypes != null) {
          XmlSchemaType[] array = xmlSchemaTypes.value();

          for (int i = 0; i < array.length; i++) {
            xmlSchemaType = array[i];

            if (XmlSchemaType.DEFAULT.class.equals(xmlSchemaType.type()))
              throw new JAXBException(L.l("@XmlSchemaType with name {0} on package {1} does not specify type", xmlSchemaType.name(), _accessor.getPackage().getName()));

            if (_accessor.getType().equals(xmlSchemaType.type()))
              break;

            xmlSchemaType = null;
          }
        }
      }

      if (xmlSchemaType != null) {
        QName typeQName = new QName(xmlSchemaType.namespace(),
                                    xmlSchemaType.name());
        typeName = StaxUtil.qnameToString(out, typeQName);
      }

      out.writeAttribute("type", typeName);
      out.writeAttribute("name", _qname.getLocalPart());

      XmlMimeType xmlMimeType = _accessor.getAnnotation(XmlMimeType.class);

      if (xmlMimeType != null) {
        out.writeAttribute(XML_MIME_NS, "expectedContentTypes", 
                           xmlMimeType.value());
      }
    }
  }
}
