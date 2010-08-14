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

import com.caucho.jaxb.BinderImpl;
import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.jaxb.JAXBUtil;
import com.caucho.jaxb.NodeIterator;
import com.caucho.jaxb.mapping.Namer;
import com.caucho.jaxb.skeleton.LaxAnyTypeSkeleton;
import com.caucho.util.L10N;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.io.IOException;

import org.w3c.dom.Node;

/**
 * a property referencing some other Skeleton
 */
public class LaxAnyTypeProperty extends Property {
  public static final QName SCHEMA_TYPE = 
    new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "anyType", "xsd");

  private static final L10N L = new L10N(LaxAnyTypeProperty.class);

  private LaxAnyTypeSkeleton _skeleton;

  public LaxAnyTypeProperty(JAXBContextImpl context)
    throws JAXBException
  {
    _skeleton = new LaxAnyTypeSkeleton(context);
  }

  public Object read(Unmarshaller u, XMLStreamReader in, Object previous)
    throws IOException, XMLStreamException, JAXBException
  {
    return _skeleton.read(u, in);
  }
  
  public Object bindFrom(BinderImpl binder, NodeIterator node, Object previous)
    throws IOException, JAXBException
  {
    return _skeleton.bindFrom(binder, null, node);
  }

  public void write(Marshaller m, XMLStreamWriter out, Object obj, Namer namer)
    throws IOException, XMLStreamException, JAXBException
  {
    _skeleton.write(m, out, obj, namer, null);
  }

  public Node bindTo(BinderImpl binder, Node node, Object obj, Namer namer)
    throws IOException, JAXBException
  {
    return _skeleton.bindTo(binder, node, obj, namer, null);
  }

  public QName getSchemaType()
  {
    return SCHEMA_TYPE;
  }

  public boolean isXmlPrimitiveType()
  {
    return false;
  }

  public String toString()
  {
    return "LaxAnyTypeProperty[" + _skeleton + "]";
  }
}


