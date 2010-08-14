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
 * a Double Property
 */
public class DoubleProperty extends CDataProperty {
  public static final QName SCHEMA_TYPE = 
    new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "double", "xsd");

  public static final DoubleProperty OBJECT_PROPERTY 
    = new DoubleProperty(true);
  public static final DoubleProperty PRIMITIVE_PROPERTY 
    = new DoubleProperty(false);

  protected DoubleProperty(boolean isNullable)
  {
    _isNullable = isNullable;
  }

  public Object getNilValue()
  {
    return Double.valueOf(0.0);
  }

  public String write(Object in)
  {
    return DatatypeConverter.printDouble(((Number) in).doubleValue());
  }

  protected Object read(String in)
  {
    return DatatypeConverter.parseDouble(in);
  }

  public QName getSchemaType()
  {
    return SCHEMA_TYPE;
  }

  public void write(Marshaller m, XMLStreamWriter out, double d, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    StaxUtil.writeStartElement(out, qname);
    out.writeCharacters(DatatypeConverter.printDouble(d));
    StaxUtil.writeEndElement(out, qname);
  }
}
