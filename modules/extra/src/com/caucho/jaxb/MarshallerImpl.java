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

package com.caucho.jaxb;

import com.caucho.jaxb.adapters.BeanAdapter;
import com.caucho.jaxb.skeleton.ClassSkeleton;
import com.caucho.xml.stream.StaxUtil;
import com.caucho.util.L10N;

import java.io.File;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.MarshalException;
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

public class MarshallerImpl extends AbstractMarshallerImpl {
  public static final L10N L = new L10N(MarshallerImpl.class);

  private JAXBContextImpl _context;
  private Listener _listener;
  private XMLOutputFactory _xmlOutputFactory;
  

  MarshallerImpl(JAXBContextImpl context)
    throws JAXBException
  {
    _context = context;
    _xmlOutputFactory = XMLOutputFactory.newInstance();
    _xmlOutputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                                  Boolean.TRUE);
  }

  /**
   * Marshal the content tree rooted at jaxbElement into a .
   */
  public void marshal(Object jaxbElement, XMLStreamWriter writer)
    throws JAXBException
  {
    ClassSkeleton skeleton = _context.findSkeletonForObject(jaxbElement);

    if (skeleton == null)
      throw new MarshalException(L.l("Unable to marshal {0}: its type unknown to this JAXBContext", jaxbElement));

    Class c = skeleton.getType();
    writer = StaxUtil.toRepairingXMLStreamWriter(writer);

    // tck/JAXBMarshall
    if (! _context.createJAXBIntrospector().isElement(jaxbElement) &&
        ! c.isAnnotationPresent(XmlRootElement.class))
      throw new MarshalException("JAXBIntrospector.isElement()==false");

    /*
    String name = null;
    String namespace = null;

    /// XXX: put into skeleton?
    XmlType xmlTypeAnnotation = (XmlType) c.getAnnotation(XmlType.class);

    if (name == null) {
      name = 
        (xmlTypeAnnotation == null ? c.getName() : xmlTypeAnnotation.name());
    }

    XmlRootElement xre = (XmlRootElement) c.getAnnotation(XmlRootElement.class);

    if (xre != null)
      name = xre.name();*/

    String encoding = getEncoding();

    if (encoding == null)
      encoding = "utf-8";

    try {
      if (!isFragment())
        writer.writeStartDocument(encoding, "1.0");

      // XXX this needs to happen after the startElement is written
      // jaxb/5003
      /*
      if (getNoNSSchemaLocation() != null)
        writer.writeAttribute("xsi",
                              "http://www.w3.org/2001/XMLSchema-instance",
                              "noNamespaceSchemaLocation",
                              getNoNSSchemaLocation());
      */

      skeleton.write(this, writer, jaxbElement, null, null);

      writer.writeEndDocument();
    }
    catch (JAXBException e) {
      throw e;
    }
    catch (Exception e) {
      throw new JAXBException(e);
    }
  }

  public Listener getListener()
  {
    return _listener;
  }

  public void setListener(Marshaller.Listener listener)
  {
    _listener = listener;
  }

  public void marshal(Object obj, XMLEventWriter writer) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public void marshal(Object obj, File file) throws JAXBException
  {
    throw new UnsupportedOperationException();
  }

  public void marshal(Object obj, Result result) throws JAXBException
  {
    if (! _context.createJAXBIntrospector().isElement(obj))
      throw new MarshalException(L.l("Object is not a JAXB element: {0}", obj));

    try {
      XMLStreamWriter out = _xmlOutputFactory.createXMLStreamWriter(result);

      marshal(obj, out);

      out.flush();
    }
    catch (XMLStreamException e) {
      throw new JAXBException(e);
    }
  }

  public <A extends XmlAdapter> A getAdapter(Class<A> type)
  {
    A a = super.getAdapter(type);

    if (a == null)
      return (A)new BeanAdapter();

    return a;
  }
}

