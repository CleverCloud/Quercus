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
 * @author Emil Ong, Scott Ferguson
 */

package com.caucho.soap.skeleton;

import com.caucho.jaxb.JAXBContextImpl;
import com.caucho.jaxb.JAXBUtil;
import com.caucho.jaxb.property.Property;
import com.caucho.jaxb.property.AttachmentProperty;

import com.caucho.soap.jaxws.HandlerChainInvoker;

import static com.caucho.soap.wsdl.WSDLConstants.*;
import com.caucho.soap.wsdl.WSDLBinding;
import com.caucho.soap.wsdl.WSDLBindingOperation;
import com.caucho.soap.wsdl.WSDLDefinitions;
import com.caucho.soap.wsdl.WSDLParser;
import com.caucho.soap.wsdl.WSDLPortType;

import com.caucho.util.Attachment;
import com.caucho.util.AttachmentReader;
import com.caucho.util.L10N;

import javax.activation.DataHandler;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import static javax.xml.XMLConstants.*;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;
import static javax.xml.ws.handler.MessageContext.*;
import javax.xml.ws.soap.SOAPFaultException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Invokes a SOAP request on a Java POJO method
 */
public abstract class AbstractAction {
  private final static Logger log = 
    Logger.getLogger(AbstractAction.class.getName());
  private static final L10N L = new L10N(AbstractAction.class);

  private static final HashMap<Method,String> _methodNames 
    = new HashMap<Method,String>();

  protected static final String XML_SCHEMA_PREFIX = "xsd";
  protected static final String TARGET_NAMESPACE_PREFIX = "m";
  protected static final String SOAP_ENCODING_STYLE 
    = "http://schemas.xmlsoap.org/soap/encoding/";
  public final static String SOAP_ENVELOPE_PREFIX = "soapenv";
  public final static String SOAP_ENVELOPE =
    "http://schemas.xmlsoap.org/soap/envelope/";

  protected static XMLOutputFactory _xmlOutputFactory;
  protected static XMLInputFactory _xmlInputFactory 
    = XMLInputFactory.newInstance();

  protected static MessageFactory _messageFactory;
  protected static SOAPMessage _soapMessage;

  protected final Method _method;
  protected final int _arity;
  protected boolean _isOneway;

  protected String _responseName;
  protected String _operationName;
  protected String _portName;
  protected String _inputName;
  protected QName _requestName;
  protected QName _resultName;

  protected final HashMap<String,ParameterMarshal> _bodyArguments
    = new HashMap<String,ParameterMarshal>();
  protected final ParameterMarshal[] _bodyArgs;

  protected final HashMap<String,ParameterMarshal> _headerArguments
    = new HashMap<String,ParameterMarshal>();
  protected final ParameterMarshal[] _headerArgs;

  protected final HashMap<String,ParameterMarshal> _attachmentArguments
    = new HashMap<String,ParameterMarshal>();
  protected final ParameterMarshal[] _attachmentArgs;

  protected final ParameterMarshal _returnMarshal;
  protected final boolean _headerReturn;

  protected final HashMap<Class,ParameterMarshal> _faults
    = new HashMap<Class,ParameterMarshal>();

  protected final HashMap<QName,ParameterMarshal> _faultNames
    = new HashMap<QName,ParameterMarshal>();

  protected int _attachmentInputs;
  protected int _headerInputs;
  protected int _bodyInputs;

  protected int _attachmentOutputs;
  protected int _headerOutputs;
  protected int _bodyOutputs;

  protected final JAXBContextImpl _jaxbContext;
  protected final String _targetNamespace;
  protected final String _soapAction;

  protected WSDLDefinitions _wsdl;
  protected WSDLBindingOperation _bindingOperation;

  protected static XMLOutputFactory getXMLOutputFactory()
  {
    if (_xmlOutputFactory == null) {
      _xmlOutputFactory = XMLOutputFactory.newInstance();
      _xmlOutputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                                    Boolean.TRUE);
    }

    return _xmlOutputFactory;
  }

  protected static SOAPFault createSOAPFault()
    throws SOAPException
  {
    if (_messageFactory == null)
      _messageFactory = MessageFactory.newInstance(); // XXX protocol

    if (_soapMessage == null)
      _soapMessage = _messageFactory.createMessage();

    _soapMessage.getSOAPBody().removeContents();

    return _soapMessage.getSOAPBody().addFault();
  }

  protected AbstractAction(Method method, Method eiMethod,
                           JAXBContextImpl jaxbContext, 
                           String targetNamespace,
                           WSDLDefinitions wsdl,
                           Marshaller marshaller,
                           Unmarshaller unmarshaller)
    throws JAXBException, WebServiceException
  {
    _method = method;
    _arity = _method.getParameterTypes().length;
    _jaxbContext = jaxbContext;
    _targetNamespace = targetNamespace;  // XXX introspect this from the method
    _isOneway = (method.getAnnotation(Oneway.class) != null);

    // set the names for the input/output messages, portType/operation, and
    // binding/operation.
    _operationName = getWebMethodName(method, eiMethod);
    _portName = getPortName(method, eiMethod);
    _inputName = _operationName;
    _responseName = _operationName + "Response";
    _soapAction = getSOAPAction(method, eiMethod);

    //
    // Arguments
    //
    
    _wsdl = wsdl;

    if (_wsdl != null) {
      for (WSDLBinding binding : _wsdl.getBindings()) {
        WSDLPortType portType = binding.getPortType();

        if (portType != null && portType.getName().equals(_portName)) {
          for (WSDLBindingOperation operation : binding.getOperations()) {
            if (operation.getName().equals(_operationName)) {
              _bindingOperation = operation;
              break;
            }
          }
        }
      }
    }

    Class[] params = method.getParameterTypes();
    Type[] genericParams = method.getGenericParameterTypes();
    Annotation[][] paramAnn = method.getParameterAnnotations();

    Annotation[][] eiParamAnn = null;
    
    if (eiMethod != null)
      eiParamAnn = eiMethod.getParameterAnnotations();

    ArrayList<ParameterMarshal> attachmentList = 
      new ArrayList<ParameterMarshal>();
    ArrayList<ParameterMarshal> headerList = 
      new ArrayList<ParameterMarshal>();
    ArrayList<ParameterMarshal> bodyList = 
      new ArrayList<ParameterMarshal>();
    
    for (int i = 0; i < params.length; i++) {
      boolean isHeader = false;
      boolean isAttachment = false;

      String localName = "arg" + i; // As per JAX-WS spec

      QName name = null;
      WebParam.Mode mode = WebParam.Mode.IN;
      WebParam webParam = null;

      for (Annotation ann : paramAnn[i]) {
        if (ann instanceof WebParam) {
          webParam = (WebParam) ann;
          break;
        }
      }

      if (webParam == null && eiParamAnn != null) {
        for (Annotation ann : eiParamAnn[i]) {
          if (ann instanceof WebParam) {
            webParam = (WebParam) ann;
            break;
          }
        }
      }

      if (webParam != null) {
        if (! "".equals(webParam.name()))
          localName = webParam.name();

        if ("".equals(webParam.targetNamespace()))
          name = new QName(localName);
        else 
          name = new QName(webParam.targetNamespace(), localName);

        if (params[i].equals(Holder.class)) {
          mode = webParam.mode();

          if (_isOneway) {
            throw new WebServiceException(L.l("Method {0} annotated with @Oneway, but contains output argument", method.getName()));
          }
        }

        isHeader = webParam.header();

        if (! isHeader)
          isAttachment = isAttachment(webParam);
      }
      else if (params[i].equals(Holder.class)) {
        mode = WebParam.Mode.INOUT;
      }

      if (name == null) 
        name = new QName(localName);

      Type type = JAXBUtil.getActualParameterType(genericParams[i]);
      Property property = _jaxbContext.createProperty(type, true);

      ParameterMarshal pMarshal
        = ParameterMarshal.create(i, property, name, mode,
                                  marshaller, unmarshaller);

      if (isHeader) {
        if (pMarshal instanceof InParameterMarshal)
          _headerInputs++;
        else if (pMarshal instanceof OutParameterMarshal)
          _headerOutputs++;
        else {
          _headerInputs++;
          _headerOutputs++;
        }

        headerList.add(pMarshal);
        _headerArguments.put(localName, pMarshal);
      }
      else if (isAttachment) {
        if (! (property instanceof AttachmentProperty))
          throw new WebServiceException(L.l("Argument {0} of method {1} is of type {2}: Attachment argument types must map to base64Binary", i, method.getName(), params[i]));

        if (pMarshal instanceof InParameterMarshal)
          _attachmentInputs++;
        else if (pMarshal instanceof OutParameterMarshal)
          _attachmentOutputs++;
        else {
          _attachmentInputs++;
          _attachmentOutputs++;
        }

        attachmentList.add(pMarshal);
        _attachmentArguments.put(localName, pMarshal);
      }
      else {
        if (pMarshal instanceof InParameterMarshal)
          _bodyInputs++;
        else if (pMarshal instanceof OutParameterMarshal)
          _bodyOutputs++;
        else {
          _bodyInputs++;
          _bodyOutputs++;
        }

        bodyList.add(pMarshal);
        _bodyArguments.put(localName, pMarshal);
      }
    }

    _attachmentArgs = new ParameterMarshal[attachmentList.size()];
    attachmentList.toArray(_attachmentArgs);

    _headerArgs = new ParameterMarshal[headerList.size()];
    headerList.toArray(_headerArgs);

    _bodyArgs = new ParameterMarshal[bodyList.size()];
    bodyList.toArray(_bodyArgs);

    // 
    // Return type
    //

    if (! Void.TYPE.equals(method.getReturnType())) {
      if (_isOneway)
        throw new WebServiceException(L.l("Method {0} annotated with @Oneway, but has non-void return", method.getName()));

      Property property = 
        _jaxbContext.createProperty(method.getGenericReturnType());

      WebResult webResult = method.getAnnotation(WebResult.class);

      if (webResult == null && eiMethod != null)
        webResult = eiMethod.getAnnotation(WebResult.class);

      if (webResult != null) {
        _headerReturn = webResult.header();

        String localName = webResult.name();

        if ("".equals(localName))
          localName = "return";

        if ("".equals(webResult.targetNamespace()))
          _resultName = new QName(localName);
        else
          _resultName = new QName(webResult.targetNamespace(), localName);
      }
      else {
        _headerReturn = false;
        _resultName = new QName("return"); // XXX namespace?
      }

      _returnMarshal = ParameterMarshal.create(0, property, _resultName, 
                                               WebParam.Mode.OUT,
                                               marshaller, unmarshaller);

      _bodyOutputs++;

      if (_headerReturn)
        _headerOutputs++;
    }
    else {
      _headerReturn = false;
      _returnMarshal = null;
    }

    //
    // Exceptions
    //

    Class[] exceptions = method.getExceptionTypes();

    for (Class exception : exceptions) {
      QName faultName = new QName(targetNamespace, 
                                  exception.getSimpleName(),
                                  TARGET_NAMESPACE_PREFIX);
      // XXX check for generated exception classes versus raw exceptions
      // i.e. things like getFaultInfo()
      Property property = jaxbContext.createProperty(exception);
      ParameterMarshal marshal = 
        ParameterMarshal.create(0, property, faultName, 
                                WebParam.Mode.OUT,
                                marshaller, unmarshaller);

      _faults.put(exception, marshal);
      _faultNames.put(faultName, marshal);
    }
  }

  public static AbstractAction createAction(Method method, 
                                            JAXBContextImpl jaxbContext, 
                                            String targetNamespace,
                                            WSDLDefinitions wsdl,
                                            Marshaller marshaller,
                                            Unmarshaller unmarshaller)
    throws JAXBException, WebServiceException
  {
    // There are three valid modes in JAX-WS:
    //
    //  1. Document wrapped -- all the parameters and return values 
    //  are encapsulated in a single encoded object (i.e. the document).  
    //  This is selected by
    //    SOAPBinding.style() == DOCUMENT
    //    SOAPBinding.use() == LITERAL
    //    SOAPBinding.parameterStyle() == WRAPPED
    //
    //  2. Document bare -- the method must have at most one input and
    //  one output parameter.  No wrapper objects are created.
    //  This is selected by
    //    SOAPBinding.style() == DOCUMENT
    //    SOAPBinding.use() == LITERAL
    //    SOAPBinding.parameterStyle() == BARE
    //
    //  3. RPC style -- parameters and return values are mapped to
    //  wsdl:parts.  This is selected by:
    //    SOAPBinding.style() == RPC
    //    SOAPBinding.use() == LITERAL
    //    SOAPBinding.parameterStyle() == WRAPPED
    //
    // It seems that "use" is never ENCODED in JAX-WS and is not allowed
    // by WS-I, so we don't allow it either.
    //

    // Check for the SOAPBinding annotation...
    
    // look at the declaring class and method first
    Class cl = method.getDeclaringClass();
    Method eiMethod = null;
    SOAPBinding soapBinding = (SOAPBinding) cl.getAnnotation(SOAPBinding.class);
    
    if (method.isAnnotationPresent(SOAPBinding.class))
      soapBinding = method.getAnnotation(SOAPBinding.class);

    if (soapBinding == null) {
      // Then look at the endpoint interface, if available
      WebService webService = (WebService) cl.getAnnotation(WebService.class);

      if (webService != null) {
        if (! "".equals(webService.endpointInterface())) {
          try {
            ClassLoader loader = cl.getClassLoader();

            Class endpointInterface = 
              loader.loadClass(webService.endpointInterface());

            soapBinding = 
              (SOAPBinding) endpointInterface.getAnnotation(SOAPBinding.class);

            eiMethod = endpointInterface.getMethod(method.getName(), 
                                                   method.getParameterTypes());

            if (eiMethod.isAnnotationPresent(SOAPBinding.class))
              soapBinding = eiMethod.getAnnotation(SOAPBinding.class);
          }
          catch (ClassNotFoundException e) {
            throw new WebServiceException(L.l("Endpoint interface {0} not found", webService.endpointInterface()), e);
          }
          catch (NoSuchMethodException e) {
            // We don't care if the method isn't defined in the interface
          }
        }
      }
    }

    // Document wrapped is the default for methods w/o a @SOAPBinding
    if (soapBinding == null)
      return new DocumentWrappedAction(method, eiMethod, 
                                       jaxbContext, targetNamespace, wsdl, 
                                       marshaller, unmarshaller);

    if (soapBinding.use() == SOAPBinding.Use.ENCODED)
      throw new UnsupportedOperationException(L.l("SOAP encoded style is not supported by JAX-WS"));

    if (soapBinding.style() == SOAPBinding.Style.DOCUMENT) {
      if (soapBinding.parameterStyle() == SOAPBinding.ParameterStyle.WRAPPED)
        return new DocumentWrappedAction(method, eiMethod, 
                                         jaxbContext, targetNamespace, wsdl,
                                         marshaller, unmarshaller);
      else {
        return new DocumentBareAction(method, eiMethod, 
                                      jaxbContext, targetNamespace, wsdl,
                                      marshaller, unmarshaller);
      }
    }
    else {
      if (soapBinding.parameterStyle() != SOAPBinding.ParameterStyle.WRAPPED)
        throw new UnsupportedOperationException(L.l("SOAP RPC bare style not supported"));

      return new RpcAction(method, eiMethod, jaxbContext, targetNamespace, wsdl,
                           marshaller, unmarshaller);
    }
  }

  protected boolean isAttachment(WebParam webParam)
  {
    return webParam.name().startsWith("attach");
  }

  /**
   * Client-side invocation.
   */
  public Object invoke(String url, Object[] args, 
                       HandlerChainInvoker handlerChain)
    throws IOException, XMLStreamException, MalformedURLException, 
           JAXBException, Throwable
  {
    XMLStreamReader in = null;
    URL urlObject = new URL(url);
    URLConnection connection = urlObject.openConnection();

    // XXX HTTPS
    if (! (connection instanceof HttpURLConnection))
      return null;

    HttpURLConnection httpConnection = (HttpURLConnection) connection;

    try {
      //
      // Send the request
      //

      httpConnection.setRequestMethod("POST");
      httpConnection.setDoInput(true);
      httpConnection.setDoOutput(true);
      // XXX: Does this change for multipart/attachments?
      httpConnection.setRequestProperty("Content-type", "text/xml");

      OutputStream httpOut = null;
      XMLStreamWriter out = null;
      DOMResult dom = null;

      UUID uuid = UUID.randomUUID();

      if (_attachmentInputs > 0) {
        // note that we have to add the request property (header) before
        // we get the output stream
        httpConnection.addRequestProperty("Content-Type", 
                                          "multipart/related; " + 
                                          "type=\"text/xml\"; " + 
                                          "boundary=\"uuid:" + uuid + "\"");

        httpOut = httpConnection.getOutputStream();

        PrintWriter writer = new PrintWriter(httpOut);
        writer.print("--uuid:" + uuid + "\r\n");
        writer.print("Content-Type: text/xml\r\n");
        writer.print("\r\n");
        writer.flush();
      }
      else
        httpOut = httpConnection.getOutputStream();

      if (handlerChain != null) {
        dom = new DOMResult();
        out = getXMLOutputFactory().createXMLStreamWriter(dom);
      }
      else {
        out = getXMLOutputFactory().createXMLStreamWriter(httpOut);
      }

      writeRequest(out, args);
      out.flush();

      if (_attachmentInputs > 0) {
        httpOut.write("\r\n".getBytes());
        writeAttachments(httpOut, uuid, args);
      }

      if (handlerChain != null) {
        Source source = new DOMSource(dom.getNode());

        if (! handlerChain.invokeClientOutbound(source, httpOut)) {
          source = handlerChain.getSource();
          in = _xmlInputFactory.createXMLStreamReader(source);

          return readResponse(in, args);
        }
      }

      //
      // Parse the response
      // 

      httpConnection.getResponseCode();
      InputStream is = httpConnection.getInputStream();

      if (handlerChain != null)
        is = handlerChain.invokeClientInbound(httpConnection);

      String contentType = httpConnection.getHeaderField("Content-Type");

      if (contentType != null && contentType.startsWith("multipart/related")) {
        String[] tokens = contentType.split(";");

        String boundary = null;

        for (int i = 0; i < tokens.length; i++) {
          int start = tokens[i].indexOf("boundary=");
          
          if (start >= 0) {
            boundary = tokens[i].substring(start + "boundary=".length() + 1,
                                           tokens[i].lastIndexOf('"'));
            break;
          }
        }

        if (boundary == null)
          return null; // XXX throw something about malformed response
      }
 
      in = _xmlInputFactory.createXMLStreamReader(is);

      if (httpConnection.getResponseCode() != 200)
        return null; // XXX more meaningful error

      if (_isOneway)
        return null;

      return readResponse(in, args);
    } 
    finally {
      if (httpConnection != null)
        httpConnection.disconnect();
    }
  }

  protected void writeRequest(XMLStreamWriter out, Object []args)
    throws IOException, XMLStreamException, JAXBException
  {
    out.writeStartDocument("UTF-8", "1.0");
    out.writeStartElement(Skeleton.SOAP_ENVELOPE_PREFIX, 
                          "Envelope", 
                          Skeleton.SOAP_ENVELOPE);
    out.writeNamespace(Skeleton.SOAP_ENVELOPE_PREFIX, Skeleton.SOAP_ENVELOPE);

    out.writeStartElement(Skeleton.SOAP_ENVELOPE_PREFIX, 
                          "Header", 
                          Skeleton.SOAP_ENVELOPE);

    for (ParameterMarshal marshal : _headerArguments.values())
      marshal.serializeCall(out, args);

    out.writeEndElement(); // Header

    out.writeStartElement(Skeleton.SOAP_ENVELOPE_PREFIX, 
                          "Body", 
                          Skeleton.SOAP_ENVELOPE);

    writeMethodInvocation(out, args);

    out.writeEndElement(); // Body
    out.writeEndElement(); // Envelope
  }

  protected void writeAttachments(OutputStream out, UUID uuid, Object[] args)
    throws IOException
  {
    PrintWriter writer = new PrintWriter(out);

    for (int i = 0; i < _attachmentArgs.length; i++)
      _attachmentArgs[i].serializeCall(writer, out, uuid, args);
  }

  abstract protected void writeMethodInvocation(XMLStreamWriter out, 
                                                Object []args)
    throws IOException, XMLStreamException, JAXBException;

  abstract protected Object readResponse(XMLStreamReader in, Object []args)
    throws IOException, XMLStreamException, JAXBException, Throwable;

  /**
   * Invokes the request for a call.
   */
  public int invoke(Object service, XMLStreamReader header,
                    XMLStreamReader in, XMLStreamWriter out,
                    List<Attachment> attachments)
    throws IOException, XMLStreamException, Throwable
  {
    // We're starting out at the point in the input stream where the 
    // method name is listed (with the arguments as children) and the 
    // point in the output stream where the results are to be written.
    
    Object[] args = readMethodInvocation(header, in);
    readAttachments(attachments, args);

    Object value = null;

    try {
      value = _method.invoke(service, args);
    } 
    catch (IllegalAccessException e) {
      throw new Throwable(e);
    } 
    catch (IllegalArgumentException e) {
      throw new Throwable(e);
    }
    catch (InvocationTargetException e) {
      writeFault(out, e.getCause());
      return 500;
    }

    if (! _isOneway) {
      if (_headerOutputs > 0) {
        out.writeStartElement(SOAP_ENVELOPE_PREFIX, "Header", SOAP_ENVELOPE);

        if (_returnMarshal != null && _headerReturn)
          _returnMarshal.serializeReply(out, value);

        for (int i = 0; i < _headerArgs.length; i++)
          _headerArgs[i].serializeReply(out, args);

        out.writeEndElement(); // Header
      }
    }

    // services/1318: We always need a body even if it is oneway
    out.writeStartElement(SOAP_ENVELOPE_PREFIX, "Body", SOAP_ENVELOPE);

    if (! _isOneway)
      writeResponse(out, value, args);

    out.writeEndElement(); // Body

    return 200;
  }

  protected void readHeaders(XMLStreamReader header, Object[] args)
    throws IOException, XMLStreamException, JAXBException
  {
    for (int i = 0; i < _headerArgs.length; i++)
      _headerArgs[i].prepareArgument(args);

    if (header != null) {
      header.nextTag();

      while (header.getEventType() == header.START_ELEMENT) {
        ParameterMarshal arg = _headerArguments.get(header.getLocalName());

        if (arg == null) {
          if (log.isLoggable(Level.FINER))
            log.finer(L.l("Unknown header argument: {0}", header.getName()));

          // skip this subtree
          int depth = 1;

          while (depth > 0) {
            switch (header.nextTag()) {
              case XMLStreamReader.START_ELEMENT:
                depth++;
                break;
              case XMLStreamReader.END_ELEMENT:
                depth--;
                break;
            }
          }
        }
        else {
          arg.deserializeCall(header, args);
        }
      }
    }
  }

  protected void readAttachments(List<Attachment> attachments, Object[] args)
    throws IOException, XMLStreamException, JAXBException
  {
    for (int i = 0; i < _attachmentArgs.length; i++) {
      if (_attachmentArgs[i] instanceof OutParameterMarshal)
        continue;

      _attachmentArgs[i].prepareArgument(args);

      if (attachments != null) {
        if (i < attachments.size())
          _attachmentArgs[i].deserializeCall(attachments.get(i), args);
        
        else
          log.fine(L.l("Received unexpected attachment"));
      }
    }
  }

  // reads the method invocation and returns the arguments
  abstract protected Object[] readMethodInvocation(XMLStreamReader header,
                                                   XMLStreamReader in)
    throws IOException, XMLStreamException, JAXBException;

  abstract protected void writeResponse(XMLStreamWriter out, 
                                        Object value, Object[] args)
    throws IOException, XMLStreamException, JAXBException;

  protected void writeFault(XMLStreamWriter out, Throwable fault)
    throws IOException, XMLStreamException, JAXBException
  {
    out.writeStartElement(SOAP_ENVELOPE_PREFIX, "Body", SOAP_ENVELOPE);
    out.writeStartElement(Skeleton.SOAP_ENVELOPE_PREFIX, 
                          "Fault", 
                          Skeleton.SOAP_ENVELOPE);

    out.writeStartElement("faultcode");
    out.writeCharacters(Skeleton.SOAP_ENVELOPE_PREFIX + ":Server");
    out.writeEndElement(); // faultcode

    //
    // Marshal this exception as a fault.
    // 
    // faults must have exactly the same class as declared on the method,
    // otherwise we emit an internal server error.
    // XXX This may not be behavior required by the standard and we may 
    // be able to improve here by casting as a superclass.
    ParameterMarshal faultMarshal = _faults.get(fault.getClass());

    if (faultMarshal == null) {
      out.writeStartElement("faultstring");
      out.writeCharacters(L.l("Internal server error"));
      out.writeEndElement(); // faultstring
    }
    else {
      out.writeStartElement("faultstring");
      out.writeCharacters(fault.getMessage());
      out.writeEndElement(); // faultstring

      out.writeStartElement("detail");
      faultMarshal.serializeReply(out, fault);
      out.writeEndElement(); // detail 
    }

    out.writeEndElement(); // Fault
    out.writeEndElement(); // Body
  }

  protected Throwable readFault(XMLStreamReader in)
    throws IOException, XMLStreamException, JAXBException, SOAPException
  {
    Throwable fault = null;
    String message = null;
    String actor = null;
    SOAPFault soapFault = createSOAPFault();

    while (in.nextTag() == XMLStreamReader.START_ELEMENT) {
      if ("faultcode".equals(in.getLocalName())) {
        if (in.next() == XMLStreamReader.CHARACTERS) {
          String code = in.getText();
          int colon = code.indexOf(':');

          if (colon >= 0)
            code = code.substring(colon + 1);

          if ("Server".equalsIgnoreCase(code)) {
            // XXX Do anything with this?
          }
          else if ("Client".equalsIgnoreCase(code)) {
            // XXX Do anything with this?
          }
          else if ("VersionMismatch".equalsIgnoreCase(code)) {
            // XXX Do anything with this?
          }
          else if ("MustUnderstand".equalsIgnoreCase(code)) {
            // XXX Do anything with this?
          }

          soapFault.setFaultCode(code);
        }

        while (in.nextTag() != XMLStreamReader.END_ELEMENT) {}
      }
      else if ("faultstring".equals(in.getLocalName())) {
        if (in.next() == XMLStreamReader.CHARACTERS)
          message = in.getText();

        soapFault.setFaultString(message);

        while (in.nextTag() != XMLStreamReader.END_ELEMENT) {}
      }
      else if ("faultactor".equals(in.getLocalName())) {
        if (in.next() == XMLStreamReader.CHARACTERS)
          actor = in.getText();

        soapFault.setFaultActor(actor);

        while (in.nextTag() != XMLStreamReader.END_ELEMENT) {}
      }
      else if ("detail".equals(in.getLocalName())) {
        if (in.nextTag() == XMLStreamReader.START_ELEMENT) {
          ParameterMarshal faultMarshal = _faultNames.get(in.getName());

          if (faultMarshal != null)
            fault = (Exception) faultMarshal.deserializeReply(in, fault);
        }
      }
    }

    if (fault == null)
      fault = new SOAPFaultException(soapFault);

    return fault;
  }

  public boolean hasHeaderInput()
  {
    return false;
  }

  public int getArity()
  {
    return _arity;
  }

  public String getInputName()
  {
    return _inputName;
  }

  public static String getWebMethodName(Method method)
  {
    String methodName = _methodNames.get(method);

    if (methodName == null) {
      Method eiMethod = getEIMethod(method);
      methodName = getWebMethodName(method, eiMethod);

      _methodNames.put(method, methodName);
    }

    return methodName;
  }

  public static String getWebMethodName(Method method, Method eiMethod)
  {
    String name = method.getName();

    WebMethod webMethod = method.getAnnotation(WebMethod.class);

    if (webMethod == null && eiMethod != null)
      webMethod = eiMethod.getAnnotation(WebMethod.class);

    if (webMethod != null && ! "".equals(webMethod.operationName()))
      name = webMethod.operationName();
    
    return name;
  }

  public static String getPortName(Method method, Method eiMethod)
  {
    Class cl = method.getDeclaringClass();
    WebService webService = (WebService) cl.getAnnotation(WebService.class);

    if (webService == null && eiMethod != null) {
      cl = eiMethod.getDeclaringClass();
      webService = (WebService) cl.getAnnotation(WebService.class);
    }

    if (webService != null && ! "".equals(webService.portName()))
      return webService.portName();
    
    return null;
  }

  public static String getSOAPAction(Method method, Method eiMethod)
  {
    String action = "";

    WebMethod webMethod = method.getAnnotation(WebMethod.class);

    if (webMethod == null && eiMethod != null)
      webMethod = eiMethod.getAnnotation(WebMethod.class);

    if (webMethod != null)
      action = webMethod.action();
    
    return action;
  }

  public static Method getEIMethod(Method method)
  {
    try {
      Class cl = method.getDeclaringClass();
      WebService webService = (WebService) cl.getAnnotation(WebService.class);

      if (webService != null) {
        if (! "".equals(webService.endpointInterface())) {
          ClassLoader loader = cl.getClassLoader();

          Class endpointInterface = 
            loader.loadClass(webService.endpointInterface());

          return endpointInterface.getMethod(method.getName(), 
                                             method.getParameterTypes());
        }
      }
    }
    catch (ClassNotFoundException e) {
    }
    catch (NoSuchMethodException e) {
    }

    return null;
  }

  public abstract void writeWSDLMessages(XMLStreamWriter out, 
                                         String soapNamespaceURI)
    throws XMLStreamException;

  public abstract void writeSchema(XMLStreamWriter out, 
                                   String namespace,
                                   JAXBContextImpl context)
    throws XMLStreamException, WebServiceException;
  
  public abstract void writeWSDLBindingOperation(XMLStreamWriter out, 
                                                 String soapNamespaceURI)
    throws XMLStreamException;

  public abstract void writeWSDLOperation(XMLStreamWriter out, 
                                          String soapNamespaceURI)
    throws XMLStreamException;
}
