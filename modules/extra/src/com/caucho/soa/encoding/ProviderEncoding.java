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

package com.caucho.soa.encoding;

import com.caucho.config.ConfigurationException;
import com.caucho.soap.jaxws.HandlerChainInvoker;
import com.caucho.soap.jaxws.JAXWSUtil;
import com.caucho.soap.jaxws.PortInfoImpl;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.jws.HandlerChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingType;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.handler.PortInfo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.logging.Logger;

/**
 * Invokes a service Provider.
 */
public abstract class ProviderEncoding implements ServiceEncoding {
  private static final L10N L = new L10N(ProviderEncoding.class);
  private static final Logger log =
    Logger.getLogger(ProviderEncoding.class.getName());
  private static final TransformerFactory factory 
    = TransformerFactory.newInstance();

  protected final Class _class;
  protected final Provider _provider;
  protected final Transformer _transformer;
  protected final Service.Mode _mode;
  protected HandlerChainInvoker _handlerChain;

  protected ProviderEncoding(Object service)
    throws ConfigurationException
  {
    _provider = (Provider) service;
    _class = service.getClass();

    ServiceMode serviceMode = 
      (ServiceMode) _class.getAnnotation(ServiceMode.class);

    if (serviceMode != null)
      _mode = serviceMode.value();
    else
      _mode = Service.Mode.PAYLOAD;

    try {
      _transformer = factory.newTransformer();

      if (_mode == Service.Mode.PAYLOAD) {
        _transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        _transformer.setOutputProperty(OutputKeys.INDENT, "no");
      }
    }
    catch (TransformerConfigurationException e) {
      throw new ConfigurationException(e);
    }
    
    PortInfo portInfo = null;

    BindingType bindingType 
      = (BindingType) _class.getAnnotation(BindingType.class);

    WebServiceProvider provider = 
      (WebServiceProvider) _class.getAnnotation(WebServiceProvider.class);

    if (provider != null && bindingType != null) {
      QName portName = new QName(provider.targetNamespace(),
                                 provider.portName());
      QName serviceName = new QName(provider.targetNamespace(),
                                    provider.serviceName());

      portInfo = new PortInfoImpl(bindingType.value(), portName, serviceName);
    }

    HandlerChain handlerChain = 
      (HandlerChain) _class.getAnnotation(HandlerChain.class);

    if (handlerChain != null) { 
      if (portInfo != null) {
        HandlerResolver handlerResolver = 
          JAXWSUtil.createHandlerResolver(_class, handlerChain);

        List<Handler> chain = handlerResolver.getHandlerChain(portInfo);

        if (chain != null)
          _handlerChain = new HandlerChainInvoker(chain);
      }
      else {
        log.fine(L.l("@HandlerChain given on Provider {0}, but @WebServiceProvider and/or @BindingType were not fully specified", _class.getName()));
      }
    }
  }

  public static ProviderEncoding createProviderEncoding(Object service)
    throws ConfigurationException
  {
    Class cl = service.getClass();
    Type[] interfaces = cl.getGenericInterfaces();

    for (int i = 0; i < interfaces.length; i++) {
      if (interfaces[i] instanceof ParameterizedType) {
        ParameterizedType pType = (ParameterizedType) interfaces[i];

        if (Provider.class.equals(pType.getRawType())) {
          Type[] args = pType.getActualTypeArguments();

          if (args.length == 1) {
            if (Source.class.equals(args[0]))
              return new SourceProviderEncoding(service);
            else if (SOAPMessage.class.equals(args[0]))
              return new SOAPMessageProviderEncoding(service);
          }
        }
      }
    }

    throw new ConfigurationException(L.l("Class {0} does not implement valid Provider", cl.getName()));
  }

  public void setService(Object service)
  {
  }

  @PostConstruct
  public void init()
  {
  }

  public void invoke(HttpServletRequest request, HttpServletResponse response)
    throws Throwable
  {
    InputStream in = request.getInputStream();
    OutputStream out = response.getOutputStream();

    if (_handlerChain != null) {
      in = _handlerChain.invokeServerInbound(request, out);
      out = new ByteArrayOutputStream();

      if (in == null)
        return;
    }

    invoke(in, out);

    if (_handlerChain != null) {
      byte[] output = ((ByteArrayOutputStream) out).toByteArray();
      Source source = new StreamSource(new ByteArrayInputStream(output));
      DOMResult result = new DOMResult();

      _transformer.transform(source, result);

      out = response.getOutputStream();

      _handlerChain.invokeServerOutbound(new DOMSource(result.getNode()), out);

      out.flush();
    }
  }
}
