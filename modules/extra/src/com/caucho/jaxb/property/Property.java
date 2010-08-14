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
 * @author Scott Ferguson
 */

package com.caucho.jaxb.property;

import java.io.IOException;

import java.util.ArrayList;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import javax.xml.namespace.QName;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Node;

import com.caucho.jaxb.BinderImpl;
import com.caucho.jaxb.NodeIterator;
import com.caucho.jaxb.mapping.Namer;
import com.caucho.jaxb.mapping.XmlMapping;
import com.caucho.jaxb.skeleton.ClassSkeleton;

import com.caucho.util.L10N;

/**
 */
public abstract class Property {
  public static final L10N L = new L10N(Property.class);

  //
  // Schema generation methods
  // 
  public boolean isXmlPrimitiveType()
  {
    return true;
  }

  public String getMinOccurs()
  {
    return "0";
  }

  public String getMaxOccurs()
  {
    return null;
  }

  public boolean isNullable()
  {
    return false;
  }

  public Object getNilValue()
  {
    return null;
  }

  public abstract QName getSchemaType();

  // XXX: This is hideous -- there is way too much overloading going on here
  // R/W methods
  //

  public Object readAttribute(XMLStreamReader in, int i)
    throws IOException, JAXBException
  {
    throw new JAXBException(L.l("Internal error: Property does not support attributes {0}", this));
  }

  public abstract Object read(Unmarshaller u, 
                              XMLStreamReader in, 
                              Object previous)
    throws IOException, XMLStreamException, JAXBException;
  
  public Object read(Unmarshaller u, XMLStreamReader in, 
                     Object previous, ClassSkeleton attributed, Object parent)
    throws IOException, XMLStreamException, JAXBException
  {
    return read(u, in, previous);
  }
  
  public abstract Object bindFrom(BinderImpl binder, 
                                  NodeIterator node, 
                                  Object previous)
    throws IOException,JAXBException;

  public abstract void write(Marshaller m, XMLStreamWriter out, 
                             Object value, Namer namer)
    throws IOException, XMLStreamException, JAXBException;

  public abstract Node bindTo(BinderImpl binder, Node node, 
                              Object value, Namer namer)
    throws IOException,JAXBException;

  public void write(Marshaller m, XMLStreamWriter out, 
                    Object value, Namer namer, Object obj)
    throws IOException, XMLStreamException, JAXBException
  {
    write(m, out, value, namer);
  }

  public void write(Marshaller m, XMLStreamWriter out, 
                    Object value, Namer namer, Object obj,
                    ArrayList<XmlMapping> attributes)
    throws IOException, XMLStreamException, JAXBException
  {
    write(m, out, value, namer, obj);
  }

  public Node bindTo(BinderImpl binder, Node node, 
                     Object value, Namer namer,
                     ArrayList<XmlMapping> attributes)
    throws IOException, JAXBException
  {
    return bindTo(binder, node, value, namer);
  }
}
