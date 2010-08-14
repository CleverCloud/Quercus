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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.soap.jaxrpc;

import com.caucho.log.Log;
import com.caucho.soap.wsdl.WSDLOperation;
import com.caucho.soap.wsdl.WSDLPort;
import com.caucho.util.L10N;
import com.caucho.xml.XMLWriter;

import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.rpc.Call;
import javax.xml.rpc.JAXRPCException;
import javax.xml.rpc.ParameterMode;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Service
 */
public class CallImpl implements Call {
  private final static L10N L = new L10N(CallImpl.class);
  private final static Logger log = Log.open(CallImpl.class);

  public final static String XMLNS =
    "http://www.w3.org/2000/xmlns/";

  public final static String SOAP_ENVELOPE =
    "http://www.w3.org/2003/05/soap-envelope";
  public final static String SOAP_ENCODING =
    "http://schemas.xmlsoap.org/soap/encoding/";

  private WSDLPort _port;
  private WSDLOperation _op;

  CallImpl(WSDLPort port)
  {
    _port = port;
  }

  CallImpl(WSDLPort port, WSDLOperation op)
  {
    _port = port;
    _op = op;
  }


  /**
   * Returns true if the parameter and return type should be invoked.
   */
  public boolean isParameterAndReturnSpecRequired(QName operationName)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Adds a parameter type and mode for the operation.
   */
  public void addParameter(String paramName,
                           QName xmlType,
                           ParameterMode parameterMode)
    throws JAXRPCException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Adds a parameter type and mode for the operation.
   */
  public void addParameter(String paramName,
                           QName xmlType,
                           Class javaType,
                           ParameterMode parameterMode)
    throws JAXRPCException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Adds Returns the XML type of a parameter.
   */
  public QName getParameterTypeByName(String paramName)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets the return type.
   */
  public void setReturnType(QName xmlType)
    throws JAXRPCException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets the return type.
   */
  public void setReturnType(QName xmlType,
                            Class javaType)
    throws JAXRPCException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the return type.
   */
  public QName getReturnType()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Removes the parameters.
   */
  public void removeAllParameters()
    throws JAXRPCException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the operation name.
   */
  public QName getOperationName()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the port type.
   */
  public QName getPortTypeName()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets the port type.
   */
  public void setPortTypeName(QName portType)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets the target endpoing.
   */
  public void setTargetEndpointAddress(String address)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the target endpoint.
   */
  public String getTargetEndpointAddress()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets a property.
   */
  public void setProperty(String name, Object value)
    throws JAXRPCException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets a property.
   */
  public Object getProperty(String name)
    throws JAXRPCException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Removes a property.
   */
  public void removeProperty(String name)
    throws JAXRPCException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Iterates over the property names.
   */
  public Iterator getPropertyNames()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Invokes the operation
   */
  public Object invoke(Object []params)
    throws java.rmi.RemoteException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Invokes the operation
   */
  public Object invoke(QName operationName, Object []params)
    throws java.rmi.RemoteException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Invokes the operation in one-way mode.
   */
  public void invokeOneWay(Object []params)
  {
    writeCall(params);
  }

  /**
   * Creates the send message.
   */

  /**
   * Returns the Map of the output parameters.
   */
  public Map getOutputParams()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a list of theoutput parameters.
   */
  public List getOutputValues()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Writes the call.
   */
  private void writeCall(Object []params)
  {
    /*
    if (_op.getInput() == null)
      throw new IllegalStateException(L.l("writing call with no input"));

    OutputStream os = null;

    try {
      os = com.caucho.vfs.Vfs.lookup("file:/tmp/caucho/qa/soap.xml").openWrite();

      XMLWriter writer = new XmlPrinter(os);

      writeCall(writer, params);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
        if (os != null) os.close();
      } catch (Throwable e) {
      }
    }
    */
  }

  /**
   * Writes the call.
   */
  private void writeCall(XMLWriter writer, Object []params)
    throws IOException, SAXException
  {
    writer.startDocument();
    /*

    WSDLOperation op = _op;
    QName opName = op.getName();

    writer.startPrefixMapping("env", SOAP_ENVELOPE);
    writer.startPrefixMapping("m", opName.getNamespaceURI());

    writer.startElement(SOAP_ENVELOPE, "Envelope", "env:Envelope");
    writer.attribute(XMLNS, "env", "xmlns:env", SOAP_ENVELOPE);
    writer.attribute(XMLNS, "m", "xmlns:m", opName.getNamespaceURI());
    writer.attribute(SOAP_ENVELOPE, "encodingStyle", "env:encodingStyle",
                     SOAP_ENCODING);

    writer.startElement(SOAP_ENVELOPE, "Header", "env:Header");
    */
    /*
    writer.attribute(SOAP_ENVELOPE, "encodingStyle", "env:encodingStyle",
                     "ook");
    */

    /*
    writer.endElement(SOAP_ENVELOPE, "Header", "env:Header");

    writer.startElement(SOAP_ENVELOPE, "Body", "env:Body");

    writer.startElement(opName.getNamespaceURI(), opName.getLocalPart(),
                        "m:" + opName.getLocalPart());

    WSDLMessage input = op.getInput();

    ArrayList<WSDLMessage.Part> wsdlParams = input.getParts();

    writeParams(writer, wsdlParams, params);

    writer.endElement(opName.getNamespaceURI(), opName.getLocalPart(),
                      "m:" + opName.getLocalPart());

    writer.endElement(SOAP_ENVELOPE, "Body", "env:Body");

    writer.endElement(SOAP_ENVELOPE, "env", "Envelope");
    writer.endPrefixMapping("env");
    */

    writer.endDocument();
  }

  /*
   * Starts writing an element.
  private void writeParams(XMLWriter writer,
                           ArrayList<WSDLMessage.Part> msgParts,
                           Object []params)
    throws IOException, SAXException
  {
    for (int i = 0; i < params.length; i++) {
      Object param = params[i];
      WSDLMessage.Part part = null;
      String name = null;

      if (i < msgParts.size()) {
        part = msgParts.get(i);
        name = part.getName();
      }
      else
        name = "a" + i;

      writer.startElement("", name, name);
      writer.text(String.valueOf(param));
      writer.endElement("", name, name);
    }
  }
   */

  /**
   * Starts writing an element.
   */
  private void startElement(XMLWriter writer, QName name)
    throws IOException, SAXException
  {
    if (name.getPrefix().equals("")) {
      writer.startElement(name.getNamespaceURI(),
                          name.getLocalPart(),
                          name.getLocalPart());
    }
    else {
      writer.startElement(name.getNamespaceURI(),
                          name.getLocalPart(),
                          name.getPrefix() + ":" + name.getLocalPart());
    }
  }

  /**
   * Ends writing an element.
   */
  private void endElement(XMLWriter writer, QName name)
    throws IOException, SAXException
  {
    if (name.getPrefix().equals("")) {
      writer.endElement(name.getNamespaceURI(),
                          name.getLocalPart(),
                          name.getLocalPart());
    }
    else {
      writer.endElement(name.getNamespaceURI(),
                        name.getLocalPart(),
                        name.getPrefix() + ":" + name.getLocalPart());
    }
  }

  /**
   * Returns the id.
   */
  public String toString()
  {
    if (_op != null)
      return "CallImpl[" + _port.getName() + ",op=" + _op.getName() + "]";
    else
      return "CallImpl[" + _port.getName() + "]";
  }
}


