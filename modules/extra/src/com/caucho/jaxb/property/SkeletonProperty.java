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
 * @author Emil Ong, Adam Megacz
 */

package com.caucho.jaxb.property;

import com.caucho.jaxb.BinderImpl;
import com.caucho.jaxb.JAXBUtil;
import com.caucho.jaxb.NodeIterator;
import com.caucho.jaxb.mapping.Namer;
import com.caucho.jaxb.skeleton.ClassSkeleton;
import com.caucho.util.L10N;

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
public class SkeletonProperty extends Property {
  private static final L10N L = new L10N(SkeletonProperty.class);

  private ClassSkeleton _skeleton;

  public SkeletonProperty(ClassSkeleton skeleton)
  {
    _skeleton = skeleton;

    if (_skeleton == null)
      throw new NullPointerException();
  }

  public Object read(Unmarshaller u, XMLStreamReader in, Object previous)
    throws IOException, XMLStreamException, JAXBException
  {
    return _skeleton.read(u, in);
  }

  public Object bindFrom(BinderImpl binder, NodeIterator node, Object previous)
    throws IOException, JAXBException
  {
    Node root = node.getNode();

    Object old = binder.getJAXBNode(root);

    Object ret = _skeleton.bindFrom(binder, old, node);

    binder.bind(ret, root);

    return ret;
  }

  public void write(Marshaller m, XMLStreamWriter out, 
                    Object obj, Namer namer)
    throws IOException, XMLStreamException, JAXBException
  {
    _skeleton.write(m, out, obj, namer, null);
  }

  public Node bindTo(BinderImpl binder, Node node,
                     Object obj, Namer namer)
    throws IOException, JAXBException
  {
    return _skeleton.bindTo(binder, node, obj, namer, null);
  }

  public QName getSchemaType()
  {
    return _skeleton.getTypeName();
  }

  public boolean isXmlPrimitiveType()
  {
    return false;
  }

  public String toString()
  {
    return "SkeletonProperty[" + _skeleton + "]";
  }
}


