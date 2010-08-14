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

package com.caucho.soap.skeleton;

import com.caucho.jaxb.mapping.ConstantNamer;
import com.caucho.jaxb.mapping.Namer;
import com.caucho.jaxb.property.Property;

import com.caucho.util.Attachment;

import com.caucho.xml.stream.StaxUtil;

import javax.jws.WebParam;
import static javax.xml.XMLConstants.*;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import java.util.UUID;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public abstract class ParameterMarshal {
  protected static final String TARGET_NAMESPACE_PREFIX = "m";
  protected static final String XML_SCHEMA_PREFIX = "xsd";

  protected QName _name;
  protected Namer _namer;
  protected Property _property;
  protected final int _arg;
  protected final Marshaller _marshaller;
  protected final Unmarshaller _unmarshaller;

  protected ParameterMarshal(int arg, Property property, QName name, 
                             Marshaller marshaller, Unmarshaller unmarshaller)
  {
    _arg = arg;
    _property = property;
    _name = name;
    _marshaller = marshaller;
    _unmarshaller = unmarshaller;
    _namer = ConstantNamer.getConstantNamer(_name);
  }

  static ParameterMarshal create(int arg, 
                                 Property property, 
                                 QName name,
                                 WebParam.Mode mode, 
                                 Marshaller marshaller, 
                                 Unmarshaller unmarshaller)
  {
    switch (mode) {
      case IN:
        return new InParameterMarshal(arg, property, name, 
                                      marshaller, unmarshaller);
      case OUT:
        return new OutParameterMarshal(arg, property, name, 
                                       marshaller, unmarshaller);
      case INOUT:
        return new InOutParameterMarshal(arg, property, name, 
                                         marshaller, unmarshaller);
      default:
        throw new UnsupportedOperationException();
    }
  }

  public int getArg()
  {
    return _arg;
  }

  public QName getName()
  {
    return _name;
  }

  public void setName(QName name)
  {
    _name = name;
    _namer = ConstantNamer.getConstantNamer(_name);
  }

  public Property getProperty()
  {
    return _property;
  }

  public void setProperty(Property property)
  {
    _property = property;
  }

  //
  // client
  //

  public void serializeCall(XMLStreamWriter out, Object []args)
    throws IOException, XMLStreamException, JAXBException
  {
  }

  public void serializeCall(PrintWriter writer, OutputStream out, 
                            UUID uuid, Object []args)
    throws IOException
  {
  }

  public Object deserializeReply(XMLStreamReader in, Object previous)
    throws IOException, XMLStreamException, JAXBException
  {
    return null;
  }
  
  public void deserializeReply(XMLStreamReader in, Object[] args)
    throws IOException, XMLStreamException, JAXBException
  {
  }

  //
  // server
  //

  public void prepareArgument(Object []args)
  {
  }

  public void deserializeCall(Attachment attachment, Object []args)
    throws IOException, XMLStreamException, JAXBException
  {
  }

  public void deserializeCall(XMLStreamReader in, Object []args)
    throws IOException, XMLStreamException, JAXBException
  {
  }



  public void serializeReply(XMLStreamWriter out, Object ret)
    throws IOException, XMLStreamException, JAXBException
  {
  }

  public void serializeReply(XMLStreamWriter out, Object []args)
    throws IOException, XMLStreamException, JAXBException
  {
  }

  public void writeElement(XMLStreamWriter out)
    throws XMLStreamException
  {
    out.writeEmptyElement(XML_SCHEMA_PREFIX, "element", W3C_XML_SCHEMA_NS_URI);
    out.writeAttribute("name", _name.getLocalPart());

    /* XXX I'm pretty sure this will now be taken care of by the
     *  default targetNamespace in JAXBContextImpl
     *
    int colon = _property.getSchemaType().indexOf(':');

    if (colon < 0)
      out.writeAttribute("type", TARGET_NAMESPACE_PREFIX + ':' + 
                                 _property.getSchemaType());
    else 
      out.writeAttribute("type", _property.getSchemaType());*/

    String type = StaxUtil.qnameToString(out, _property.getSchemaType());
    out.writeAttribute("type", type);

    // XXX list?
    if (_property.getMaxOccurs() != null) {
      out.writeAttribute("minOccurs", "0");
      out.writeAttribute("maxOccurs", _property.getMaxOccurs());
    }
    else if (_property.getMinOccurs() != null)
      out.writeAttribute("minOccurs", _property.getMinOccurs());
  }

  public String toString()
  {
    return "ParameterMarshal[arg=" + _arg + 
                           ",property=" + _property + 
                           ",name=" + _name + 
                           ",namer=" + _namer + "]";
  }
}
