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
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.helpers.ValidationEventImpl;
import javax.xml.bind.helpers.ValidationEventLocatorImpl;

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

import java.util.Collection;
import java.util.Map;

public class XmlValueMapping extends SingleQNameXmlMapping {
  private static final L10N L = new L10N(XmlValueMapping.class);

  // special constructor for JAXBElementMapping
  protected XmlValueMapping(JAXBContextImpl context)
  {
    super(context, null);
  }

  public XmlValueMapping(JAXBContextImpl context, Accessor accessor)
    throws JAXBException
  {
    super(context, accessor);

    // XXX
    _qname = new QName(accessor.getName());

    XmlMimeType xmlMimeType = accessor.getAnnotation(XmlMimeType.class);
    String mimeType = null;
    
    if (xmlMimeType != null)
      mimeType = xmlMimeType.value();

    _property = _context.createProperty(accessor.getGenericType(), false, 
                                        mimeType, true, true);

    if (! _property.isXmlPrimitiveType() && 
        ! Collection.class.isAssignableFrom(accessor.getType()))
      throw new JAXBException(L.l("XmlValue must be either a collection or a simple type"));
  }

  public void setQName(QName qname)
  {
    _qname = qname;
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
}
