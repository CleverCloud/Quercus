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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;

import com.caucho.soap.jaxws.HandlerChainInvoker;

/**
 * XXX Necessary?
 * Invokes a request on a Java POJO
 */
abstract public class Skeleton {
  public final static String XMLNS =
    "http://www.w3.org/2000/xmlns/";
  public final static String SOAP_ENVELOPE =
    "http://schemas.xmlsoap.org/soap/envelope/";
  // Older namespace URI: "http://www.w3.org/2003/05/soap-envelope";
  public final static String SOAP_ENCODING =
    "http://schemas.xmlsoap.org/soap/encoding/";
  public final static String SOAP_RESULT =
    "http://www.w3.org/2003/05/soap-rpc";
  public final static String XMLNS_XSD =
    "http://www.w3.org/2001/XMLSchema";
  public final static String XMLNS_XSI =
    "http://www.w3.org/2001/XMLSchema-instance";

  public final static String SOAP_ENVELOPE_PREFIX = "soapenv";

  /**
   * Invokes the request.
   */
  abstract public void invoke(Object service,
                              HttpServletRequest request,
                              HttpServletResponse response)
    throws IOException, XMLStreamException, Throwable;

  public Object invoke(Method method, String url, Object[] args)
    throws IOException, XMLStreamException, MalformedURLException, 
           JAXBException, Throwable
  {
    return invoke(method, url, args, null);
  }

  abstract public Object invoke(Method method, String url, Object[] args,
                                HandlerChainInvoker handlerChain)
    throws IOException, XMLStreamException, MalformedURLException, 
           JAXBException, Throwable;

  abstract public String getPortName();
  abstract public String getNamespace();
}


