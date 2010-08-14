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

import com.caucho.soap.reflect.WebServiceIntrospector;
import com.caucho.soap.skeleton.Skeleton;
import com.caucho.util.L10N;
import com.caucho.util.ThreadPool;
import com.caucho.xml.stream.StaxUtil;
import com.caucho.xml.stream.XMLStreamReaderImpl;
import com.caucho.xml.stream.XMLStreamWriterImpl;

import javax.activation.DataHandler;
import javax.activation.DataSource;

import javax.mail.util.ByteArrayDataSource;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;

import static javax.xml.soap.SOAPConstants.*;
import javax.xml.soap.*;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Response;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.spi.ServiceDelegate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.lang.reflect.Proxy;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Dispatch
 */
public class SOAPMessageDispatch extends AbstractDispatch<SOAPMessage> {
  private final static Logger log
    = Logger.getLogger(SOAPMessageDispatch.class.getName());
  private final static L10N L = new L10N(SOAPMessageDispatch.class);

  private final MessageFactory _factory;

  public SOAPMessageDispatch(String bindingId, Binding binding,
                             Service.Mode mode, Executor executor)
    throws WebServiceException
  {
    super(bindingId, binding, mode, executor);

    try {
      if (SOAPBinding.SOAP11HTTP_BINDING.equals(bindingId) ||
          SOAPBinding.SOAP11HTTP_MTOM_BINDING.equals(bindingId))
        _factory = MessageFactory.newInstance(SOAP_1_1_PROTOCOL);
      else if (SOAPBinding.SOAP12HTTP_BINDING.equals(bindingId) ||
               SOAPBinding.SOAP12HTTP_MTOM_BINDING.equals(bindingId))
        _factory = MessageFactory.newInstance(SOAP_1_2_PROTOCOL);
      else
        throw new WebServiceException(L.l("Unsupported binding id: {0}", 
                                          bindingId));
    }
    catch (SOAPException e) {
      throw new WebServiceException(e);
    }

    if (mode != Service.Mode.MESSAGE)
      throw new WebServiceException(L.l("Instances of Dispatch<SOAPMessage> can only have mode MESSAGE"));
  }

  protected void writeRequest(SOAPMessage msg, OutputStream out)
  {
    try {
      msg.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");
      msg.writeTo(out);
    }
    catch (SOAPException e) {
      throw new WebServiceException(e);
    }
    catch (IOException e) {
      throw new WebServiceException(e);
    }
  }

  protected SOAPMessage formatResponse(byte[] response)
  {
    try {
      InputStream is = new ByteArrayInputStream(response);
      return _factory.createMessage(new MimeHeaders(), is);
    }
    catch (SOAPException e) {
      throw new WebServiceException(e);
    }
    catch (IOException e) {
      throw new WebServiceException(e);
    }
  }
}
