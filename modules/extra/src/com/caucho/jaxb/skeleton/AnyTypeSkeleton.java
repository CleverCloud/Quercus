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
import com.caucho.jaxb.mapping.XmlInstanceWrapper;
import com.caucho.jaxb.mapping.Namer;
import com.caucho.jaxb.mapping.XmlMapping;
import com.caucho.jaxb.property.Property;
import com.caucho.util.L10N;
import com.caucho.xml.stream.StaxUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import static javax.xml.XMLConstants.*;
import javax.xml.bind.annotation.*;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import java.io.IOException;

import java.lang.annotation.*;
import java.lang.reflect.*;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 *
 **/
public class AnyTypeSkeleton extends ClassSkeleton<Object> {
  private static final Logger log 
    = Logger.getLogger(AnyTypeSkeleton.class.getName());
  private static final L10N L = new L10N(AnyTypeSkeleton.class);

  public AnyTypeSkeleton(JAXBContextImpl context)
    throws JAXBException
  {
    super(context);
  }

  public Object read(Unmarshaller u, XMLStreamReader in)
    throws IOException, XMLStreamException, JAXBException
  {
    String type = in.getAttributeValue(W3C_XML_SCHEMA_INSTANCE_NS_URI, "type");

    if (type != null) {
      QName typeQName = StaxUtil.resolveStringToQName(type, in);
      Class cl = JAXBUtil.getClassForDatatype(typeQName);
      
      Property property = _context.createProperty(cl);

      return property.read(u, in, null);
    }
    else {
      DOMResult result = new DOMResult();
      XMLOutputFactory factory = _context.getXMLOutputFactory();
      XMLStreamWriter out = factory.createXMLStreamWriter(result);

      StaxUtil.copyReaderToWriter(in, out);

      Node node = result.getNode();

      if (node.getNodeType() == Node.DOCUMENT_NODE)
        node = ((Document) node).getDocumentElement();

      return node;
    }
  }

  public Object bindFrom(BinderImpl binder, Object existing, NodeIterator node)
    throws JAXBException
  {
    // skipping a subtree is much easier in DOM =D
    return null;
  }

  public void write(Marshaller m, XMLStreamWriter out,
                    Object obj, Namer namer, 
                    ArrayList<XmlMapping> attributes)
    throws IOException, XMLStreamException, JAXBException
  {
    if (obj != null) {
      ClassSkeleton skeleton = _context.findSkeletonForObject(obj);

      if (skeleton != null) {
        // XXX if (skeleton.getTypeName() == null) {}

        /*
        XmlInstanceWrapper instanceWrapper 
          = new XmlInstanceWrapper(skeleton.getTypeName());

        attributes = instanceWrapper.getExtendedIterator(attributes);*/

        skeleton.write(m, out, obj, null, attributes);
        return;
      }

      Property property = _context.getSimpleTypeProperty(obj.getClass());

      if (property != null) {
        XmlInstanceWrapper instanceWrapper = 
          XmlInstanceWrapper.getInstance(property.getSchemaType());

        if (attributes == null) 
          attributes = new ArrayList<XmlMapping>(1);

        attributes.add(instanceWrapper);

        // XXX the second obj here is a hack: we just need it not to be null.
        // Figure out if the api is wrong or there is a reasonable value for
        // the fifth argument instead of this second obj.
        property.write(m, out, obj, namer, obj, attributes);
        return;
      }

      if (obj instanceof Node) {
        XMLInputFactory factory = _context.getXMLInputFactory();
        DOMSource source = new DOMSource((Node) obj);
        XMLStreamReader in = factory.createXMLStreamReader(source);

        StaxUtil.copyReaderToWriter(in, out);
        
        return;
      }

      QName fieldName = namer.getQName(obj);

      if (fieldName.getNamespaceURI() == null)
        out.writeEmptyElement(fieldName.getLocalPart());
      else if (fieldName.getPrefix() == null)
        out.writeEmptyElement(fieldName.getNamespaceURI(), 
                              fieldName.getLocalPart());
      else
        out.writeEmptyElement(fieldName.getPrefix(), 
                              fieldName.getLocalPart(), 
                              fieldName.getNamespaceURI()); 
    }
  }

  public Node bindTo(BinderImpl binder, Node node, 
                     Object obj, Namer namer)
    throws JAXBException
  {
    return node;
  }

  public String toString()
  {
    return "AnyTypeSkeleton[]";
  }
}
