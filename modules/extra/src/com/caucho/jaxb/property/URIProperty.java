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

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * URI property
 */
public class URIProperty extends CDataProperty {
  public static final QName SCHEMA_TYPE = 
    new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "anyURI", "xsd");

  public static final URIProperty PROPERTY = new URIProperty();

  public String write(Object in)
  {
    URI uri = (URI) in;

    return uri.toString();
  }

  protected Object read(String in)
    throws JAXBException
  {
    try {
      return new URI(in);
    }
    catch (URISyntaxException e) {
      throw new UnmarshalException(e);
    }
  }

  public QName getSchemaType()
  {
    return SCHEMA_TYPE;
  }
}
