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

package com.caucho.soap.jaxws;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;

import javax.servlet.http.HttpServletRequest;

import javax.xml.namespace.QName;

import javax.xml.stream.XMLStreamException;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.ws.*;
import javax.xml.ws.handler.*;
import static javax.xml.ws.handler.MessageContext.*;
import javax.xml.ws.handler.soap.*;
import javax.xml.ws.soap.SOAPFaultException;

import com.caucho.util.L10N;

import com.caucho.xml.stream.StaxUtil;
import com.caucho.xml.stream.XMLStreamReaderImpl;
import com.caucho.xml.stream.XMLStreamWriterImpl;

/**
 * Responsible for invoking a handler chain.
 *
 * Expected execution order:
 *  - prepare()
 *  - invoke(In|Out)bound()
 *  - [close()]
 *  - finish()
 **/
public class HandlerChainInvoker {
  private final static Logger log = 
    Logger.getLogger(HandlerChainInvoker.class.getName());
  private final static L10N L = new L10N(HandlerChainInvoker.class);

  private final List<Handler> _chain;
  private final BindingProvider _bindingProvider;

  // maintains state over a request-response pattern
  private final boolean[] _invoked;
  private Source _source;
  private LogicalMessageContextImpl _logicalContext;
  private SOAPMessageContextImpl _soapContext;
  private RuntimeException _runtimeException = null;
  private ProtocolException _protocolException = null;
  private boolean _request;
  private boolean _outbound;
  private int _i;

  private static Transformer _transformer;

  public HandlerChainInvoker(List<Handler> chain)
  {
    _chain = JAXWSUtil.sortHandlerChain(chain);
    _invoked = new boolean[_chain.size()];
    _bindingProvider = null;
  }

  public HandlerChainInvoker(List<Handler> chain, 
                             BindingProvider bindingProvider)
  {
    _chain = JAXWSUtil.sortHandlerChain(chain);
    _invoked = new boolean[_chain.size()];
    _bindingProvider = bindingProvider;
  }

  public InputStream invokeServerInbound(HttpServletRequest request,
                                         OutputStream os)
    throws IOException
  {
    Map<String,DataHandler> attachments = new HashMap<String,DataHandler>();

    Map<String,Object> httpProperties = new HashMap<String,Object>();
    httpProperties.put(HTTP_REQUEST_METHOD, request.getMethod());

    Map<String,List<String>> headers = new HashMap<String,List<String>>();

    Enumeration headerNames = request.getHeaderNames();

    while (headerNames.hasMoreElements()) { 
      String name = (String) headerNames.nextElement();
      List<String> values = new ArrayList<String>();

      Enumeration headerValues = request.getHeaders(name);

      while (headerValues.hasMoreElements()) {
        String value = (String) headerValues.nextElement();
        values.add(value);
      }

      headers.put(name, values);
    }

    httpProperties.put(HTTP_REQUEST_HEADERS, headers);

    prepare(httpProperties, /*request=*/true);

    if (! invokeInbound(request.getInputStream(), attachments)) {
      if (getProtocolException() != null) {
        reverseDirection();
        invokeInboundFaultHandlers();
      }
      else if (getRuntimeException() == null)
        uninvokeInbound();

      closeServer();
      finish(os);

      return null;
    }

    return finish();
  }

  public void invokeServerOutbound(Source source, OutputStream os)
  {
    Map<String,DataHandler> attachments = new HashMap<String,DataHandler>();

    Map<String,Object> httpProperties = new HashMap<String,Object>();
    httpProperties.put(HTTP_RESPONSE_CODE, Integer.valueOf(200));
    httpProperties.put(HTTP_RESPONSE_HEADERS, 
                       new HashMap<String,List<String>>());

    prepare(httpProperties, /*request=*/false);

    if (! invokeOutbound(source, attachments)) {
      if (getProtocolException() != null) {
        reverseDirection();
        invokeOutboundFaultHandlers();
      }

      /*
      else if (getRuntimeException() != null) {
        closeServer();
        finish(response.getOutputStream());

        throw getRuntimeException();
      }*/
    }

    // XXX
    closeServer();
    finish(os);
  }

  public boolean invokeClientOutbound(Source source, OutputStream out)
    throws ProtocolException
  {
    // XXX fill this in...
    Map<String,DataHandler> attachments = new HashMap<String,DataHandler>();

    Map<String,Object> httpProperties = new HashMap<String,Object>();
    httpProperties.put(HTTP_REQUEST_METHOD, "POST");
    httpProperties.put(HTTP_REQUEST_HEADERS, 
                       new HashMap<String,List<String>>());

    prepare(httpProperties, /*request=*/true);

    if (! invokeOutbound(source, attachments)) {
      // XXX handle Oneway
      if (getProtocolException() != null) {
        reverseDirection();
        invokeOutboundFaultHandlers();
        closeClient();

        if (getRuntimeException() != null)
          throw getRuntimeException();

        if (getProtocolException() != null)
          throw getProtocolException();

        return false;
      }
      else if (getRuntimeException() != null) {
        closeClient();
        throw getRuntimeException();
      }
      else {
        uninvokeOutbound();
        closeClient();

        if (getRuntimeException() != null)
          throw getRuntimeException();

        if (getProtocolException() != null)
          throw getProtocolException();

        return false;
      }
    }

    finish(out);

    return true;
  }

  public InputStream invokeClientInbound(HttpURLConnection httpConnection)
    throws IOException
  {
    // XXX fill this in...
    Map<String,DataHandler> attachments = new HashMap<String,DataHandler>();

    Map<String,Object> httpProperties = new HashMap<String,Object>();
    httpProperties.put(HTTP_RESPONSE_CODE, 
                       Integer.valueOf(httpConnection.getResponseCode()));
    httpProperties.put(HTTP_RESPONSE_HEADERS,
                       httpConnection.getHeaderFields());

    prepare(httpProperties, /*request=*/false);

    if (! invokeInbound(httpConnection.getInputStream(), attachments)) {
      if (getProtocolException() != null) {
        reverseDirection();
        invokeInboundFaultHandlers();

        if (getRuntimeException() != null)
          throw getRuntimeException();
      }

      else if (getRuntimeException() != null) {
        closeClient();
        throw getRuntimeException();
      }
    }

    // XXX
    closeClient();
    return finish();
  }

  public void prepare(Map<String,Object> httpProperties, boolean request)
  {
    _logicalContext = new LogicalMessageContextImpl();
    _soapContext = new SOAPMessageContextImpl();
    _runtimeException = null;
    _protocolException = null;
    _request = request;

    _logicalContext.putAll(httpProperties);
    _soapContext.putAll(httpProperties);

    importAppProperties(request);
  }

  public void closeServer()
  {
    for (int i = 0; i < _chain.size(); i++)
      close(i);
  }

  public void closeClient()
  {
    for (int i = _chain.size() - 1; i >= 0; i--)
      close(i);
  }

  public void finish(OutputStream out)
  {
    exportAppProperties(_request);

    /*
    if (_runtimeException != null)
      return;*/

    try {
      getTransformer().transform(_source, new StreamResult(out));
    }
    catch (TransformerException e) {
      throw new WebServiceException(e);
    }
  }

  public InputStream finish()
  {
    exportAppProperties(_request);

    if (_runtimeException != null)
      return null;

    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      getTransformer().transform(_source, new StreamResult(baos));

      return new ByteArrayInputStream(baos.toByteArray());
    }
    catch (TransformerException e) {
      throw new WebServiceException(e);
    }
  }

  /**
   * Invoke the handler chain for an outbound message.
   **/
  public boolean invokeOutbound(Source source, 
                                Map<String,DataHandler> attachments)
    throws WebServiceException
  {
    _source = source;

    // Set the mandatory properties
    _logicalContext.put(MESSAGE_OUTBOUND_PROPERTY, Boolean.TRUE);
    _soapContext.put(MESSAGE_OUTBOUND_PROPERTY, Boolean.TRUE);

    _logicalContext.put(OUTBOUND_MESSAGE_ATTACHMENTS, attachments);
    _soapContext.put(OUTBOUND_MESSAGE_ATTACHMENTS, attachments);

    for (_i = 0; _i < _chain.size(); _i++) {
      boolean success = handleMessage(_i);

      if (_protocolException != null)
        return false;

      if (_runtimeException != null)
        return false;

      if (! success)
        return false;
    }

    return true;
  }

  /**
   * When a message direction is reversed within the chain, this method
   * runs the message backwards through the previous handlers.  This
   * method should only be invoked when a handler returns false, but
   * does not throw any kind of exception.
   **/
  public boolean uninvokeOutbound()
    throws WebServiceException
  {
    // Set the mandatory properties
    _logicalContext.put(MESSAGE_OUTBOUND_PROPERTY, Boolean.FALSE);
    _soapContext.put(MESSAGE_OUTBOUND_PROPERTY, Boolean.FALSE);

    // NOTE: the order is reversed for inbound messages
    for (_i--; _i >= 0; _i--) {
      boolean success = handleMessage(_i);

      if (_protocolException != null)
        return false;

      if (_runtimeException != null)
        return false;

      if (! success)
        return false;
    }

    return true;
  }

  public boolean invokeOutboundFaultHandlers()
  {
    for (_i--; _i >= 0; _i--) {
      if (! handleFault(_i))
        return false;

      if (_runtimeException != null)
        return false;
    }

    return true;
  }

  /**
   * Invoke the handler chain for an inbound message.
   **/
  public boolean invokeInbound(InputStream in, 
                               Map<String,DataHandler> attachments)
    throws WebServiceException
  {
    _outbound = false;
    _source = null;

    try {
      DOMResult dom = new DOMResult();
      getTransformer().transform(new StreamSource(in), dom);

      // XXX The TCK seems to assume a source that will stand up to repeated
      // reads... meaning that StreamSource and SAXSource are out, so DOM
      // must be what they want.
      _source = new DOMSource(dom.getNode());
    }
    catch (Exception e) {
      throw new WebServiceException(e);
    }

    // Set the mandatory properties
    _logicalContext.put(MESSAGE_OUTBOUND_PROPERTY, Boolean.FALSE);
    _soapContext.put(MESSAGE_OUTBOUND_PROPERTY, Boolean.FALSE);

    _logicalContext.put(INBOUND_MESSAGE_ATTACHMENTS, attachments);
    _soapContext.put(INBOUND_MESSAGE_ATTACHMENTS, attachments);

    // NOTE: the order is reversed for inbound messages
    for (_i = _chain.size() - 1; _i >= 0; _i--) {
      boolean success = handleMessage(_i);

      if (_protocolException != null)
        return false;

      if (_runtimeException != null)
        return false;

      if (! success)
        return false;
    }

    return true;
  }

  /**
   * When a message direction is reversed within the chain, this method
   * runs the message backwards through the previous handlers.  This
   * method should only be invoked when a handler returns false, but
   * does not throw any kind of exception.
   **/
  public boolean uninvokeInbound()
    throws WebServiceException
  {
    // Set the mandatory properties
    _logicalContext.put(MESSAGE_OUTBOUND_PROPERTY, Boolean.TRUE);
    _soapContext.put(MESSAGE_OUTBOUND_PROPERTY, Boolean.TRUE);

    for (_i++; _i < _chain.size(); _i++) {
      boolean success = handleMessage(_i);

      if (_protocolException != null)
        return false;

      if (_runtimeException != null)
        return false;

      if (! success)
        return false;
    }

    return true;
  }

  public boolean invokeInboundFaultHandlers()
  {
    for (_i++; _i < _chain.size(); _i++) {
      if (! handleFault(_i))
        return false;

      if (_runtimeException != null)
        return false;
    }

    return true;
  }

  public void reverseDirection()
  {
    Boolean direction = 
      (Boolean) _logicalContext.get(MESSAGE_OUTBOUND_PROPERTY);

    if (direction != null) {
      if (Boolean.TRUE.equals(direction)) {
        _logicalContext.put(MESSAGE_OUTBOUND_PROPERTY, Boolean.FALSE);
        _soapContext.put(MESSAGE_OUTBOUND_PROPERTY, Boolean.FALSE);
      }
      else {
        _logicalContext.put(MESSAGE_OUTBOUND_PROPERTY, Boolean.TRUE);
        _soapContext.put(MESSAGE_OUTBOUND_PROPERTY, Boolean.TRUE);
      }
    }
  }

  public ProtocolException getProtocolException()
  {
    return _protocolException;
  }

  public RuntimeException getRuntimeException()
  {
    return _runtimeException;
  }

  public Source getSource()
  {
    return _source;
  }

  private boolean handleMessage(int i)
  {
    Handler handler = _chain.get(i);

    boolean success = false;
    _invoked[i] = true;

    try {
      if (handler instanceof LogicalHandler) {
        _logicalContext.getMessage().setPayload(_source);
        success = handler.handleMessage(_logicalContext);
        _source = _logicalContext.getMessage().getPayload();
      }
      else if (handler instanceof SOAPHandler) {
        try {
          _soapContext.setMessage(_source);
          success = handler.handleMessage(_soapContext);
          _source = _soapContext.getMessage().getSOAPPart().getContent();
        }
        catch (SOAPException e) {
          throw new WebServiceException(e);
        }
      }
      else {
        throw new WebServiceException(L.l("Unsupported Handler type: {0}",
                                          handler.getClass().getName()));
      }
    }
    catch (ProtocolException e) {
      _protocolException = e;
      serializeProtocolException();
    }
    catch (RuntimeException e) {
      _runtimeException = e;
      serializeRuntimeException();
    }

    return success;
  }

  private boolean handleFault(int i)
  {
    Handler handler = _chain.get(i);

    boolean success = false;
    _invoked[i] = true;

    try {
      if (handler instanceof LogicalHandler) {
        _logicalContext.getMessage().setPayload(_source);
        success = handler.handleFault(_logicalContext);
        _source = _logicalContext.getMessage().getPayload();
      }
      else if (handler instanceof SOAPHandler) {
        try {
          _soapContext.setMessage(_source);
          success = handler.handleFault(_soapContext);
          _source = _soapContext.getMessage().getSOAPPart().getContent();
        }
        catch (SOAPException e) {
          throw new WebServiceException(e);
        }
      }
      else {
        throw new WebServiceException(L.l("Unsupported Handler type: {0}",
                                          handler.getClass().getName()));
      }

      _protocolException = null;
    }
    catch (ProtocolException e) {
      _protocolException = e;
      serializeProtocolException();
    }
    catch (RuntimeException e) {
      _runtimeException = e;
      serializeRuntimeException();
    }

    return success;
  }

  private void close(int i)
  {
    Handler handler = _chain.get(i);

    if (! _invoked[i])
      return;

    _invoked[i] = false;

    if (handler instanceof LogicalHandler) {
      _logicalContext.getMessage().setPayload(_source);
      handler.close(_logicalContext);
      _source = _logicalContext.getMessage().getPayload();
    }
    else if (handler instanceof SOAPHandler) {
      try {
        _soapContext.setMessage(_source);
        handler.close(_soapContext);
        _source = _soapContext.getMessage().getSOAPPart().getContent();
      }
      catch (SOAPException e) {
        throw new WebServiceException(e);
      }
    }
  }

  private void importAppProperties(boolean request)
  {
    if (_bindingProvider == null)
      return;

    if (request) {
      _logicalContext.putAll(_bindingProvider.getRequestContext(), 
                             Scope.APPLICATION);
      _soapContext.putAll(_bindingProvider.getRequestContext(),  
                          Scope.APPLICATION);
    }
    else {
      _logicalContext.putAll(_bindingProvider.getResponseContext(), 
                             Scope.APPLICATION);
      _soapContext.putAll(_bindingProvider.getResponseContext(),  
                          Scope.APPLICATION);
    }
  }

  private void exportAppProperties(boolean request)
  {
    if (_bindingProvider == null)
      return;

    if (request) { 
      Map<String,Object> map = null;
      
      map = _logicalContext.getScopedSubMap(Scope.APPLICATION);
      _bindingProvider.getRequestContext().putAll(map);

      map = _soapContext.getScopedSubMap(Scope.APPLICATION);
      _bindingProvider.getRequestContext().putAll(map);
    }
    else {
      Map<String,Object> map = null;
      
      map = _logicalContext.getScopedSubMap(Scope.APPLICATION);
      _bindingProvider.getResponseContext().putAll(map);

      map = _soapContext.getScopedSubMap(Scope.APPLICATION);
      _bindingProvider.getResponseContext().putAll(map);
    }
  }

  private void serializeProtocolException()
    throws WebServiceException
  {
    if (_protocolException instanceof SOAPFaultException) {
      SOAPFaultException sfe = (SOAPFaultException) _protocolException;
      SOAPFault fault = sfe.getFault();

      try {
        MessageFactory factory = _soapContext.getMessageFactory();
        SOAPMessage message = factory.createMessage();
        message.getSOAPBody().addChildElement(fault);
        _soapContext.setMessage(message);
        _source = _soapContext.getMessage().getSOAPPart().getContent();
      }
      catch (SOAPException se) {
        throw new WebServiceException(se);
      }
    }
    else {
      throw new WebServiceException(L.l("Unsupported ProtocolException: {0}",
                                        _protocolException));
    }
  }

  private void serializeRuntimeException()
    throws WebServiceException
  {
    try {
      MessageFactory factory = _soapContext.getMessageFactory();
      SOAPMessage message = factory.createMessage();

      QName faultcode = new QName(message.getSOAPBody().getNamespaceURI(), 
                                  "Server",
                                  message.getSOAPBody().getPrefix());

      message.getSOAPBody().addFault(faultcode, _runtimeException.getMessage());

      _soapContext.setMessage(message);
      _source = _soapContext.getMessage().getSOAPPart().getContent();
    }
    catch (SOAPException se) {
      throw new WebServiceException(se);
    }
  }

  private static Transformer getTransformer()
    throws TransformerException
  {
    if (_transformer == null) {
      TransformerFactory factory = TransformerFactory.newInstance();
      _transformer = factory.newTransformer();
    }

    return _transformer;
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder("HandlerChainInvoker[\n");

    for (Handler handler : _chain) {
      sb.append(handler.toString());
      sb.append('\n');
    }

    sb.append(']');

    return sb.toString();
  }

  public void printSource()
  {
    try {
      StreamResult result = new StreamResult(System.out);
      getTransformer().transform(_source, result);
    }
    catch (Exception e) {
    }
  }

  public String getDirection()
  {
    return _outbound ? "Outbound" : "Inbound";
  }
}
