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
import com.caucho.jaxb.accessor.JAXBElementAccessor;
import com.caucho.jaxb.property.Property;
import com.caucho.util.L10N;

import javax.xml.bind.JAXBException;

import javax.xml.namespace.QName;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class JAXBElementMapping extends XmlValueMapping {
  private static final L10N L = new L10N(JAXBElementMapping.class);

  public JAXBElementMapping(JAXBContextImpl context)
    throws JAXBException
  {
    super(context);
    
    _accessor = new JAXBElementAccessor();
  }

  // this method exists for the convenience of (Dynamic)JAXBElementSkeleton
  public void setQName(QName qname)
  {
    _qname = qname;
  }

  // this method exists for the convenience of (Dynamic)JAXBElementSkeleton
  public void setProperty(Property property)
  {
    _property = property;
  }

  public void generateSchema(XMLStreamWriter out)
    throws JAXBException, XMLStreamException
  {
    throw new UnsupportedOperationException();
  }
}
