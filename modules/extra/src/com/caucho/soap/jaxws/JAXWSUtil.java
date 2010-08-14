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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jws.HandlerChain;
import javax.jws.WebService;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.handler.LogicalHandler;

import com.caucho.util.L10N;

import com.caucho.soap.jaxws.handlerchain.HandlerChains;

import com.caucho.xml.stream.StaxUtil;
import com.caucho.xml.stream.XMLStreamReaderImpl;
import com.caucho.xml.stream.XMLStreamWriterImpl;

public class JAXWSUtil {
  private static final Logger log =
    Logger.getLogger(JAXWSUtil.class.getName());
  private final static L10N L = new L10N(JAXWSUtil.class);
  private static Unmarshaller _handlerChainUnmarshaller = null;

  public static void writeStartSOAPEnvelope(Writer out, String namespace)
    throws IOException
  {
    out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    out.write("<soapenv:Envelope xmlns:soapenv=\"" + namespace + "\">");
    out.write("<soapenv:Body>");

    out.flush();
  }

  public static void writeEndSOAPEnvelope(Writer out)
    throws IOException
  {
    out.write("</soapenv:Body>");
    out.write("</soapenv:Envelope>");

    out.flush();
  }

  public static void extractSOAPBody(InputStream in, OutputStream out)
    throws WebServiceException
  {
    boolean foundBody = false;

    try {
      XMLStreamReaderImpl reader = new XMLStreamReaderImpl(in);

      // skip the Envelope
      reader.nextTag();

      if (reader.getEventType() != reader.START_ELEMENT ||
          ! "Envelope".equals(reader.getLocalName())) {
        throw new WebServiceException(L.l("Invalid response from server: No Envelope found"));
      }

      // find the body
      while (reader.hasNext()) {
        reader.next();

        if (reader.getEventType() == reader.START_ELEMENT &&
            "Body".equals(reader.getLocalName())) {

          // Copy the body contents to a StreamDataHandler
          reader.nextTag();

          XMLStreamWriterImpl xmlWriter = new XMLStreamWriterImpl(out, false);

          StaxUtil.copyReaderToWriter(reader, xmlWriter);

          xmlWriter.flush();

          foundBody = true;

          break;
        }
      }
    }
    catch (XMLStreamException e) {
      throw new WebServiceException(e);
    }

    if (! foundBody)
      throw new WebServiceException(L.l("Invalid response from server"));
  }

  public static HandlerResolver createHandlerResolver(Class cl, 
                                                      HandlerChain handlerChain)
    throws WebServiceException
  {
    try {
      if (_handlerChainUnmarshaller == null) {
        JAXBContext context = 
          JAXBContext.newInstance("com.caucho.soap.jaxws.handlerchain");
        _handlerChainUnmarshaller = context.createUnmarshaller();
      }

      if (log.isLoggable(Level.FINER)) {
        log.finer("Creating handler chain for " + cl +
                  " from file " + handlerChain.file());
      }

      InputStream is = cl.getResourceAsStream(handlerChain.file());

      HandlerChains handlerChains = 
        (HandlerChains) _handlerChainUnmarshaller.unmarshal(is);

      return handlerChains;
    }
    catch (Exception e) {
      throw new WebServiceException(e);
    }
  }

  public static List<Handler> sortHandlerChain(List<Handler> handlerChain)
  {
    // According to the JAX-WS documentation, handler chains must be sorted
    // so that all LogicalHandlers appear before protocol Handlers 
    // (protocol Handlers are just Handlers that are not LogicalHandlers)
    //
    // XXX do this by bubbling up instead of creating a new list
    List list = new ArrayList<Handler>();

    for (int i = 0; i < handlerChain.size(); i++) {
      Handler handler = handlerChain.get(i);

      if (handler instanceof LogicalHandler)
        list.add(handler);
    }

    for (int i = 0; i < handlerChain.size(); i++) {
      Handler handler = handlerChain.get(i);

      if (! (handler instanceof LogicalHandler))
        list.add(handler);
    }

    return list;
  }

  public static Class getEndpointInterface(Class type)
  {
    WebService webService = (WebService) type.getAnnotation(WebService.class);

    if (webService != null && ! "".equals(webService.endpointInterface())) {
      try {
        ClassLoader loader = type.getClassLoader();
        return loader.loadClass(webService.endpointInterface());
      }
      catch (ClassNotFoundException e) {
        throw new WebServiceException(e);
      }
    }

    return type;
  }

  public static String getTargetNamespace(Class type, Class api)
  {
    WebService webService = (WebService) type.getAnnotation(WebService.class);

    // try to get the namespace from the annotation first...
    if (webService != null) {
      if (! "".equals(webService.targetNamespace()))
        return webService.targetNamespace();

      else if (! api.equals(type)) {
        webService = (WebService) api.getAnnotation(WebService.class);

        if (! "".equals(webService.targetNamespace()))
          return webService.targetNamespace();
      }
    }

    // get the namespace from the package name
    String namespace = null;
    String packageName = type.getPackage().getName();
    StringTokenizer st = new StringTokenizer(packageName, ".");

    while (st.hasMoreTokens()) { 
      if (namespace == null) 
        namespace = st.nextToken();
      else
        namespace = st.nextToken() + "." + namespace;
    }

    return "http://"+namespace+"/";
  }
}
