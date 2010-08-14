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

package com.caucho.jaxb;

import com.caucho.jaxb.skeleton.ClassSkeleton;

import com.caucho.util.L10N;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.bind.Binder;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.MarshalException;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.helpers.AbstractMarshallerImpl;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.validation.Schema;

import java.io.IOException;

import java.util.HashMap;

/**
 * A very basic implementation of Binder.
 **/
public class BinderImpl extends Binder<Node> {
  private static final L10N L = new L10N(BinderImpl.class);

  private final JAXBContextImpl _context;
  /*
  private Marshaller _marshaller;
  private Unmarshaller _unmarshaller;*/
  private Schema _schema;
  private ValidationEventHandler _validationEventHandler;

  private final HashMap<Object,Node> _jaxbToXmlMap 
    = new HashMap<Object,Node>();
  private final HashMap<Node,Object> _xmlToJaxbMap 
    = new HashMap<Node,Object>();
  
  BinderImpl(JAXBContextImpl context)
  {
    _context = context;
    _validationEventHandler = _context.DEFAULT_VALIDATION_EVENT_HANDLER;
  }

  /*
  public Marshaller getMarshaller()
    throws JAXBException
  {
    if (_marshaller == null)
      _marshaller = _context.createMarshaller();

    return _marshaller;
  }

  public Unmarshaller getUnmarshaller()
    throws JAXBException
  {
    if (_unmarshaller == null)
      _unmarshaller = _context.createUnmarshaller();

    return _unmarshaller;
  }*/

  public ValidationEventHandler getEventHandler() 
    throws JAXBException
  {
    return _validationEventHandler;
  }

  public void setEventHandler(ValidationEventHandler handler)
    throws JAXBException
  {
    if (handler == null) 
      _validationEventHandler = _context.DEFAULT_VALIDATION_EVENT_HANDLER;
    else
      _validationEventHandler = handler;
  }

  public Object getProperty(String name) 
    throws PropertyException
  {
    if (name == null)
      throw new IllegalArgumentException(name);

    if ("jaxb.encoding".equals(name)) {
      // XXX
      return null;
    }
    else if ("jaxb.formatted.output".equals(name)) {
      // XXX
      return null;
    }
    else if ("jaxb.schemaLocation".equals(name)) {
      // XXX
      return null;
    }
    else if ("jaxb.noNamespaceSchemaLocation".equals(name)) {
      // XXX
      return null;
    }
    else if ("jaxb.fragment".equals(name)) { 
      // XXX
      return null;
    }
    else
      throw new PropertyException(name);
  }

  public void setProperty(String name, Object value)
    throws PropertyException
  {
    if (name == null)
      throw new IllegalArgumentException(name);

    if ("jaxb.encoding".equals(name)) { /* XXX */ }
    else if ("jaxb.formatted.output".equals(name)) { /* XXX */ }
    else if ("jaxb.schemaLocation".equals(name)) { /* XXX */ }
    else if ("jaxb.noNamespaceSchemaLocation".equals(name)) { /* XXX */ }
    else if ("jaxb.fragment".equals(name)) { /* XXX */ }
    else 
      throw new PropertyException(name);
  }

  public Schema getSchema()
  {
    return _schema;
  }

  public void setSchema(Schema schema)
  {
    _schema = schema;
  }

  public Object getJAXBNode(Node node)
  {
    if (node == null)
      throw new IllegalArgumentException(L.l("Node may not be null"));

    return _xmlToJaxbMap.get(node);
  }

  public Node getXMLNode(Object jaxbObject)
  {
    if (jaxbObject == null)
      throw new IllegalArgumentException(L.l("JAXB object may not be null"));

    if (jaxbObject instanceof JAXBElement) 
      return _jaxbToXmlMap.get(((JAXBElement) jaxbObject).getValue());
    else
      return _jaxbToXmlMap.get(jaxbObject);
  }

  public void marshal(Object jaxbObject, Node xmlNode)
    throws JAXBException
  {
    // XXX Schema

    if (jaxbObject == null)
      throw new IllegalArgumentException(L.l("JAXB object may not be null"));

    if (xmlNode == null)
      throw new IllegalArgumentException(L.l("Node may not be null"));

    ClassSkeleton skeleton = _context.findSkeletonForObject(jaxbObject);

    if (skeleton == null)
      throw new MarshalException(L.l("Unable to marshal {0}: its type unknown to this JAXBContext", jaxbObject));

    Document doc = xmlNode.getOwnerDocument();

    if (xmlNode.getNodeType() == Node.DOCUMENT_NODE)
      doc = (Document) xmlNode;

    Node child = doc.createElement("root");

    try {
      xmlNode.appendChild(skeleton.bindTo(this, child, jaxbObject, null, null));
    }
    catch (IOException e) {
      throw new JAXBException(e);
    }
  }

  public Object unmarshal(Node xmlNode)
    throws JAXBException
  {
    // XXX Schema
    
    if (xmlNode == null)
      throw new IllegalArgumentException(L.l("Node may not be null"));

    Node root = xmlNode;

    if (xmlNode.getNodeType() == Node.DOCUMENT_NODE) {
      Document doc = (Document) xmlNode;
      root = doc.getDocumentElement();
    }

    QName name = JAXBUtil.qnameFromNode(root);

    ClassSkeleton skeleton = _context.getRootElement(name);

    if (skeleton == null)
      throw new UnmarshalException(L.l("Root element {0} is unknown to this context",
                                       name));

    try {
      return skeleton.bindFrom(this, null, new NodeIterator(root));
    }
    catch (IOException e) {
      throw new JAXBException(e);
    }
  }

  public <T> JAXBElement<T> unmarshal(Node xmlNode, Class<T> declaredType)
    throws JAXBException
  {
    if (xmlNode == null)
      throw new IllegalArgumentException(L.l("Node may not be null"));

    Node root = xmlNode;

    if (xmlNode.getNodeType() == Node.DOCUMENT_NODE) {
      Document doc = (Document) xmlNode;
      root = doc.getDocumentElement();
    }

    ClassSkeleton skeleton = _context.findSkeletonForClass(declaredType);

    if (skeleton == null)
      throw new UnmarshalException(L.l("Type {0} is unknown to this context",
                                       declaredType));

    try {
      T value = (T) skeleton.bindFrom(this, null, new NodeIterator(root));

      QName qname = JAXBUtil.qnameFromNode(root);

      return new JAXBElement<T>(qname, declaredType, value);
    }
    catch (IOException e) {
      throw new JAXBException(e);
    }
 }

  public Object updateJAXB(Node xmlNode) 
    throws JAXBException
  {
    Node root = xmlNode;

    if (xmlNode.getNodeType() == Node.DOCUMENT_NODE) {
      Document doc = (Document) xmlNode;
      root = doc.getDocumentElement();
    }

    Object jaxbObject = getJAXBNode(root);

    if (jaxbObject == null)
      throw new JAXBException(L.l("Unknown xmlNode"));

    ClassSkeleton skeleton = _context.findSkeletonForObject(jaxbObject);

    if (skeleton == null) {
      // we strip the JAXBElement when we bind objects, so rewrap
      // the object and try again
      QName qname = JAXBUtil.qnameFromNode(root);

      jaxbObject = new JAXBElement(qname, jaxbObject.getClass(), jaxbObject);

      skeleton = _context.findSkeletonForObject(jaxbObject);

      if (skeleton == null)
        throw new UnmarshalException(L.l("Type {0} is unknown to this context",
                                         jaxbObject.getClass()));
    }

    try {
      return skeleton.bindFrom(this, jaxbObject, new NodeIterator(root));
    }
    catch (IOException e) {
      throw new JAXBException(e);
    }
  }

  public Node updateXML(Object jaxbObject) 
    throws JAXBException
  {
    return updateXML(jaxbObject, getXMLNode(jaxbObject));
  }

  public Node updateXML(Object jaxbObject, Node xmlNode)
    throws JAXBException
  {
    ClassSkeleton skeleton = _context.findSkeletonForObject(jaxbObject);

    if (skeleton == null)
      throw new MarshalException(L.l("Unable to update {0}: its type unknown to this JAXBContext", jaxbObject));

    try {
      return skeleton.bindTo(this, xmlNode, jaxbObject, null, null);
    }
    catch (IOException e) {
      throw new JAXBException(e);
    }
  }

  public void bind(Object jaxbObject, Node xmlNode)
  {
    if (jaxbObject instanceof JAXBElement)
      jaxbObject = ((JAXBElement) jaxbObject).getValue();

    // ensure that old bindings one way or the other are cleaned up
    Node oldNode = _jaxbToXmlMap.get(jaxbObject);

    if (xmlNode != oldNode)
      _xmlToJaxbMap.remove(oldNode);
    else {
      Object oldObject = _xmlToJaxbMap.get(xmlNode);

      if (oldObject != jaxbObject)
        _jaxbToXmlMap.remove(oldObject);
    }

    // insert the new binding
    _jaxbToXmlMap.put(jaxbObject, xmlNode);
    _xmlToJaxbMap.put(xmlNode, jaxbObject);
  }

  public void invalidate(Node root)
  {
    if (root == null)
      return;

    remove(root);

    for (Node node = root.getFirstChild(); 
         node != null; 
         node = node.getNextSibling())
      invalidate(root);
  }

  private void remove(Node root) 
  {
    if (_xmlToJaxbMap.containsKey(root)) {
      Object obj = _xmlToJaxbMap.remove(root);

      if (_jaxbToXmlMap.containsKey(obj))
        _jaxbToXmlMap.remove(obj);
    }
  }
}
