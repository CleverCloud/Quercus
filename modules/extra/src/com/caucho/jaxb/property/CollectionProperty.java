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

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.caucho.jaxb.BinderImpl;
import com.caucho.jaxb.JAXBUtil;
import com.caucho.jaxb.NodeIterator;
import com.caucho.jaxb.mapping.Namer;
import com.caucho.util.L10N;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;

/**
 * a Collection Property
 */
public class CollectionProperty extends IterableProperty {
  public static final L10N L = new L10N(CollectionProperty.class);

  public CollectionProperty(Property componentProperty)
  {
    _componentProperty = componentProperty;
  }

  public Object read(Unmarshaller u, XMLStreamReader in, Object previous)
    throws IOException, XMLStreamException, JAXBException
  {
    Collection collection = (Collection) previous;

    if (collection == null)
      collection = createCollection();

    collection.add(_componentProperty.read(u, in, null));

    return collection;
  }

  public Object bindFrom(BinderImpl binder, NodeIterator node, Object previous)
    throws IOException, JAXBException
  {
    Node child = node.getNode();

    Collection collection = (Collection) previous;

    collection.add(_componentProperty.bindFrom(binder, node, null));

    return collection;
  }

  public void write(Marshaller m, XMLStreamWriter out,
                    Object value, Namer namer)
    throws IOException, XMLStreamException, JAXBException
  {
    if (value != null) {
      validateType(value);

      Collection collection = (Collection) value;

      for (Object o : collection)
        _componentProperty.write(m, out, o, namer);
    }
  }

  public Node bindTo(BinderImpl binder, Node node,
                     Object value, Namer namer)
    throws IOException, JAXBException
  {
    if (value != null) {
      validateType(value);

      Collection collection = (Collection) value;

      Node child = node.getFirstChild();
      for (Object o : collection) {
        if (child != null) {
          // try to reuse as many of the child nodes as possible
          Node newNode = _componentProperty.bindTo(binder, child, o, namer);

          if (newNode != child) {
            node.replaceChild(child, newNode);
            binder.invalidate(child);
          }

          child = child.getNextSibling();
          node = JAXBUtil.skipIgnorableNodes(node);
        }
        else {
          QName qname = namer.getQName(value);
          Node newNode = JAXBUtil.elementFromQName(qname, node);
          newNode = _componentProperty.bindTo(binder, newNode, o, namer);

          node.appendChild(newNode);
        }
      }
    }

    return node;
  }

  public QName getSchemaType()
  {
    return _componentProperty.getSchemaType();
  }

  public boolean isXmlPrimitiveType()
  {
    return getComponentProperty().isXmlPrimitiveType();
  }

  public String getMaxOccurs()
  {
    return "unbounded";
  }

  public boolean isNullable()
  {
    return true;
  }

  protected Collection createCollection()
  {
    return new ArrayList();
  }

  protected void validateType(Object obj)
  {
    if (! (obj instanceof Collection))
      throw new ClassCastException(L.l("Argument is not a Collection: {0}", obj));
  }
}


