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
 * a Boolean Property
 */
public class BooleanProperty extends CDataProperty {
  public static final QName SCHEMA_TYPE = 
    new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "boolean", "xsd");

  public static final BooleanProperty OBJECT_PROPERTY 
    = new BooleanProperty(true);
  public static final BooleanProperty PRIMITIVE_PROPERTY 
    = new BooleanProperty(false);

  public Object getNilValue()
  {
    return Boolean.FALSE;
  }

  protected BooleanProperty(boolean isNullable)
  {
    _isNullable = isNullable;
  }

  public String write(Object in)
  {
    return DatatypeConverter.printBoolean(((Boolean)in).booleanValue());
  }

  protected Object read(String in)
  {
    return Boolean.valueOf(DatatypeConverter.parseBoolean(in));
  }

  public QName getSchemaType()
  {
    return SCHEMA_TYPE;
  }

  public void write(Marshaller m, XMLStreamWriter out, boolean b, QName qname)
    throws IOException, XMLStreamException, JAXBException
  {
    StaxUtil.writeStartElement(out, qname);
    out.writeCharacters(DatatypeConverter.printBoolean(b));
    StaxUtil.writeEndElement(out, qname);
  }
}
