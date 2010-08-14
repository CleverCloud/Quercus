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

package com.caucho.soap.skeleton;

import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.jaxb.JAXBUtil;
import static com.caucho.soap.wsdl.WSDLConstants.*;
import com.caucho.soap.wsdl.WSDLDefinitions;

import com.caucho.util.L10N;

import com.caucho.xml.stream.StaxUtil;

import static javax.xml.XMLConstants.*;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Document wrapped action
 */
public class DocumentWrappedAction extends AbstractAction {
  private final static Logger log = 
    Logger.getLogger(DocumentWrappedAction.class.getName());
  public static final L10N L = new L10N(DocumentWrappedAction.class);

  /*
   * Document wrapped arguments are in an xsd:sequence -- in other words, 
   * there is a prescribed order in which the arguments are expected to be
   * given.  (Any arguments from the header are excepted; headers are 
   * key/value pairs.)  
   *
   */

  public DocumentWrappedAction(Method method, Method eiMethod,
                               JAXBContextImpl jaxbContext, 
                               String targetNamespace,
                               WSDLDefinitions wsdl,
                               Marshaller marshaller,
                               Unmarshaller unmarshaller)
    throws JAXBException, WebServiceException
  {
    super(method, eiMethod, 
          jaxbContext, targetNamespace, wsdl,
          marshaller, unmarshaller);
  }

  protected void writeMethodInvocation(XMLStreamWriter out, Object []args)
    throws IOException, XMLStreamException, JAXBException
  {
    out.writeStartElement(TARGET_NAMESPACE_PREFIX, 
                          _operationName, 
                          _targetNamespace);
    out.writeNamespace(TARGET_NAMESPACE_PREFIX, _targetNamespace);

    for (int i = 0; i < _bodyArgs.length; i++)
      _bodyArgs[i].serializeCall(out, args);

    out.writeEndElement(); // name
  }

  protected Object[] readMethodInvocation(XMLStreamReader header, 
                                          XMLStreamReader in)
    throws IOException, XMLStreamException, JAXBException
  {
    Object[] args = new Object[_arity];

    readHeaders(header, args);

    // skip the method name
    in.nextTag();

    // document wrapped => everything must be in order
    for (int i = 0; i < _bodyArgs.length; i++) {
      _bodyArgs[i].prepareArgument(args);

      // services/1234: 
      // don't loop when an OutParameter is incorrectly specified
      if (! (_bodyArgs[i] instanceof OutParameterMarshal)) {
        // while loop for arrays/lists
        while (in.getEventType() == in.START_ELEMENT &&
               _bodyArgs[i].getName().equals(in.getName()))
          _bodyArgs[i].deserializeCall(in, args);
      }
    }

    // skip the method name close tag
    in.nextTag();

    return args;
  }

  protected void writeResponse(XMLStreamWriter out, Object value, Object[] args)
    throws IOException, XMLStreamException, JAXBException
  {
    out.writeStartElement(TARGET_NAMESPACE_PREFIX, 
                          _responseName, 
                          _targetNamespace);
    out.writeNamespace(TARGET_NAMESPACE_PREFIX, _targetNamespace);

    if (_returnMarshal != null && ! _headerReturn)
      _returnMarshal.serializeReply(out, value);

    for (int i = 0; i < _bodyArgs.length; i++)
      _bodyArgs[i].serializeReply(out, args);

    out.writeEndElement(); // response name
  }

  protected Object readResponse(XMLStreamReader in, Object []args)
    throws IOException, XMLStreamException, JAXBException, Throwable
  {
    Object ret = null;

    in.nextTag();
    in.require(XMLStreamReader.START_ELEMENT, null, "Envelope");

    in.nextTag();
    in.require(XMLStreamReader.START_ELEMENT, null, null);

    if ("Header".equals(in.getLocalName())) {
      in.nextTag();

      while (in.getEventType() == XMLStreamReader.START_ELEMENT) {
        String tagName = in.getLocalName();

        ParameterMarshal marshal = _headerArguments.get(tagName);

        if (marshal != null)
          marshal.deserializeReply(in, args);
        else {
          int depth = 1;

          while (depth > 0) {
            switch (in.nextTag()) {
              case XMLStreamReader.START_ELEMENT:
                depth++;
                break;
              case XMLStreamReader.END_ELEMENT:
                depth--;
                break;
              default:
                throw new IOException("expected </Header>");
            }
          }
        }
      }

      in.require(XMLStreamReader.END_ELEMENT, null, "Header");

      in.nextTag();
    }

    in.require(XMLStreamReader.START_ELEMENT, null, "Body");

    in.nextTag();

    if (in.getEventType() == XMLStreamReader.START_ELEMENT &&
        "Fault".equals(in.getLocalName())) {
      Throwable fault = readFault(in);

      if (fault == null)
        throw new WebServiceException(); // XXX

      throw fault;
    }

    in.require(XMLStreamReader.START_ELEMENT, null, _responseName);

    in.nextTag();

    if (_returnMarshal != null) {
      // while loop for arrays/lists
      while (in.getEventType() == in.START_ELEMENT &&
             _returnMarshal.getName().equals(in.getName())) {
        ret = _returnMarshal.deserializeReply(in, ret);
      }
    }

    // document wrapped => everything must be in order
    for (int i = 0; i < _bodyArgs.length; i++) {
      // while loop for arrays/lists
      while (in.getEventType() == in.START_ELEMENT &&
             _bodyArgs[i].getName().equals(in.getName()))
        _bodyArgs[i].deserializeReply(in, args);
    }

    in.require(XMLStreamReader.END_ELEMENT, null, _responseName);

    in.nextTag();
    in.require(XMLStreamReader.END_ELEMENT, null, "Body");

    in.nextTag();
    in.require(XMLStreamReader.END_ELEMENT, null, "Envelope");

    return ret;
  }

  public void writeWSDLMessages(XMLStreamWriter out, String soapNamespaceURI)
    throws XMLStreamException
  {
    out.writeStartElement(WSDL_NAMESPACE, "message");
    out.writeAttribute("name", _operationName);

    out.writeEmptyElement(WSDL_NAMESPACE, "part");
    out.writeAttribute("name", "parameters"); // XXX partName?
    out.writeAttribute("element", 
                       TARGET_NAMESPACE_PREFIX + ':' + _operationName);

    out.writeEndElement(); // message

    if (! _isOneway) {
      out.writeStartElement(WSDL_NAMESPACE, "message");
      out.writeAttribute("name", _responseName);

      out.writeEmptyElement(WSDL_NAMESPACE, "part");
      out.writeAttribute("name", "parameters"); // XXX partName?
      out.writeAttribute("element", 
                         TARGET_NAMESPACE_PREFIX + ':' + _responseName);

      out.writeEndElement(); // message
    }
  }

  public void writeWSDLBindingOperation(XMLStreamWriter out, 
                                        String soapNamespaceURI)
    throws XMLStreamException
  {
    out.writeStartElement(WSDL_NAMESPACE, "operation");
    out.writeAttribute("name", _operationName);
    // XXX out.writeAttribute("parameterOrder", "");

    out.writeEmptyElement(soapNamespaceURI, "operation");
    out.writeAttribute("soapAction", _soapAction);

    out.writeStartElement(WSDL_NAMESPACE, "input");
    // XXX
    out.writeEmptyElement(soapNamespaceURI, "body");
    out.writeAttribute("use", "literal");

    out.writeEndElement(); // input

    if (! _isOneway) {
      out.writeStartElement(WSDL_NAMESPACE, "output");
      // XXX
      out.writeEmptyElement(soapNamespaceURI, "body");
      out.writeAttribute("use", "literal");

      out.writeEndElement(); // output
    }

    out.writeEndElement(); // operation
  }

  public void writeSchema(XMLStreamWriter out, 
                          String namespace,
                          JAXBContextImpl context)
    throws XMLStreamException, WebServiceException
  {
    QName qname = new QName(namespace, _operationName);

    if (context.hasRootElement(qname))
      throw new WebServiceException(L.l("Duplicate element {0} (This method's name conflicts with an XML root element name)", qname));

    if (context.hasXmlType(qname))
      throw new WebServiceException(L.l("Duplicate type {0} (This method's name conflicts with the XML type name of a class)", qname));

    // XXX header arguments
    
    out.writeEmptyElement(XML_SCHEMA_PREFIX, "element", W3C_XML_SCHEMA_NS_URI);
    out.writeAttribute("name", _operationName);
    out.writeAttribute("type", TARGET_NAMESPACE_PREFIX + ':' + _operationName);

    if (_bodyInputs + _headerInputs == 0) {
      out.writeEmptyElement(XML_SCHEMA_PREFIX, 
                            "complexType", 
                            W3C_XML_SCHEMA_NS_URI);
      out.writeAttribute("name", _operationName);
    }
    else {
      out.writeStartElement(XML_SCHEMA_PREFIX, 
                            "complexType", 
                            W3C_XML_SCHEMA_NS_URI);
      out.writeAttribute("name", _operationName);

      out.writeStartElement(XML_SCHEMA_PREFIX, 
                            "sequence", 
                            W3C_XML_SCHEMA_NS_URI);
      
      for (ParameterMarshal param : _bodyArguments.values()) {
        if (! (param instanceof OutParameterMarshal))
          param.writeElement(out);
      }

      out.writeEndElement(); // sequence

      out.writeEndElement(); // complexType
    }

    if (! _isOneway) {
      QName responseQName = new QName(namespace, _operationName);

      if (context.hasRootElement(responseQName))
        throw new WebServiceException(L.l("Duplicate element {0} (This method's name conflicts with an XML root element name)", responseQName));

      if (context.hasXmlType(responseQName))
        throw new WebServiceException(L.l("Duplicate type {0} (This method's name conflicts with the XML type name of a class)", responseQName));

      out.writeEmptyElement(XML_SCHEMA_PREFIX, 
                            "element", 
                            W3C_XML_SCHEMA_NS_URI);
      out.writeAttribute("name", _responseName);
      out.writeAttribute("type", TARGET_NAMESPACE_PREFIX + ':' + _responseName);

      if (_bodyOutputs + _headerOutputs == 0) {
        out.writeEmptyElement(XML_SCHEMA_PREFIX, 
                              "complexType", 
                              W3C_XML_SCHEMA_NS_URI);
        out.writeAttribute("name", _responseName);
      }
      else {
        out.writeStartElement(XML_SCHEMA_PREFIX, 
                              "complexType", 
                              W3C_XML_SCHEMA_NS_URI);
        out.writeAttribute("name", _responseName);

        out.writeStartElement(XML_SCHEMA_PREFIX, 
                              "sequence", 
                              W3C_XML_SCHEMA_NS_URI);
        
        if (_returnMarshal != null)
          _returnMarshal.writeElement(out);

        for (ParameterMarshal param : _bodyArguments.values()) {
          if (! (param instanceof InParameterMarshal))
            param.writeElement(out);
        }

        out.writeEndElement(); // sequence

        out.writeEndElement(); // complexType
      }
    }
  }

  public void writeWSDLOperation(XMLStreamWriter out, String soapNamespaceURI)
    throws XMLStreamException
  {
    out.writeStartElement(WSDL_NAMESPACE, "operation");
    out.writeAttribute("name", _operationName);
    // XXX out.writeAttribute("parameterOrder", "");

    out.writeEmptyElement(WSDL_NAMESPACE, "input");
    out.writeAttribute("message", 
                       TARGET_NAMESPACE_PREFIX + ':' + _operationName);

    if (! _isOneway) {
      out.writeEmptyElement(WSDL_NAMESPACE, "output");
      out.writeAttribute("message", 
                         TARGET_NAMESPACE_PREFIX + ':' + _responseName);
    }

    out.writeEndElement(); // operation
  }

  public String toString()
  {
    return "DocumentWrappedAction[" + _method.getName() + "]";
  }
}
