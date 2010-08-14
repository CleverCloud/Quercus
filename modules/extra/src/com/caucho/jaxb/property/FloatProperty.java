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

import javax.xml.XMLConstants;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.io.IOException;

import com.caucho.xml.stream.StaxUtil;

/**
 * a Float Property
 */
public class FloatProperty extends CDataProperty {
  public static final QName SCHEMA_TYPE = 
    new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "float", "xsd");

  public static final FloatProperty OBJECT_PROPERTY 
    = new FloatProperty(true);
  public static final FloatProperty PRIMITIVE_PROPERTY 
    = new FloatProperty(false);

  protected FloatProperty(boolean isNullable)
  {
    _isNullable = isNullable;
  }

  public Object getNilValue()
  {
    return Float.valueOf(0.0F);
  }

  public String write(Object in)
  {
    return DatatypeConverter.printFloat(((Number) in).floatValue());
  }

  protected Object read(String in)
  {
    return DatatypeConverter.parseFloat(in);
  }

  public QName getSchemaType()
  {
    return SCHEMA_TYPE;
  }

  public void write(Marshaller m, XMLStreamWriter out, float f, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    StaxUtil.writeStartElement(out, qname);
    out.writeCharacters(DatatypeConverter.printFloat(f));
    StaxUtil.writeEndElement(out, qname);
  }
}
