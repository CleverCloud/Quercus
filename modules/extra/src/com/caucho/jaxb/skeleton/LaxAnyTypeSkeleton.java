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

package com.caucho.jaxb.skeleton;

import com.caucho.jaxb.BinderImpl;
import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.jaxb.JAXBUtil;
import com.caucho.jaxb.NodeIterator;
import com.caucho.jaxb.mapping.Namer;
import com.caucho.jaxb.mapping.XmlMapping;
import com.caucho.jaxb.property.Property;
import com.caucho.util.L10N;

import org.w3c.dom.Node;

import javax.xml.bind.annotation.*;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.io.IOException;

import java.lang.annotation.*;
import java.lang.reflect.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

/**
 *
 **/
public class LaxAnyTypeSkeleton extends ClassSkeleton<Object> {
  private static final Logger log 
    = Logger.getLogger(LaxAnyTypeSkeleton.class.getName());
  private static final L10N L = new L10N(JAXBElementSkeleton.class);

  public LaxAnyTypeSkeleton(JAXBContextImpl context)
    throws JAXBException
  {
    super(context);
  }

  public Object read(Unmarshaller u, XMLStreamReader in)
    throws IOException, XMLStreamException, JAXBException
  {
    QName name = in.getName();

    ClassSkeleton skeleton = _context.getRootElement(name);

    // Get the AnyTypeSkeleton as a fallback
    if (skeleton == null)
      skeleton = _context.getSkeleton(Object.class);

    return skeleton.read(u, in);
  }

  public Object bindFrom(BinderImpl binder, Object existing, NodeIterator node)
    throws IOException, JAXBException
  {
    QName name = JAXBUtil.qnameFromNode(node.getNode());

    ClassSkeleton skeleton = _context.getRootElement(name);

    if (skeleton == null)
      throw new JAXBException(L.l("No root found for element {0}", name));

    return skeleton.bindFrom(binder, existing, node);
  }

  public void write(Marshaller m, XMLStreamWriter out,
                    Object obj, Namer namer, 
                    ArrayList<XmlMapping> attributes)
    throws IOException, XMLStreamException, JAXBException
  {
    ClassSkeleton skeleton = _context.findSkeletonForObject(obj);

    if (skeleton == null) {
      Property property = _context.getSimpleTypeProperty(obj.getClass());

      if (property == null)
        throw new JAXBException(L.l("Unknown class {0}", obj.getClass()));

      property.write(m, out, obj, namer); // XXX attributes
    }
    else 
      skeleton.write(m, out, obj, null, attributes);
  }

  public Node bindTo(BinderImpl binder, Node node, 
                     Object obj, Namer namer, 
                     ArrayList<XmlMapping> attributes)
    throws IOException, JAXBException
  {
    ClassSkeleton skeleton = _context.findSkeletonForObject(obj);

    if (skeleton == null)
      throw new JAXBException(L.l("Unknown class {0}", obj.getClass()));

    return skeleton.bindTo(binder, node, obj, namer, attributes);
  }

  public String toString()
  {
    return "LaxAnyTypeSkeleton[]";
  }
}
