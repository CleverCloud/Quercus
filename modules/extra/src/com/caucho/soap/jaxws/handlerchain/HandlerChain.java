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

package com.caucho.soap.jaxws.handlerchain;

import java.util.*;
import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.PortInfo;

@XmlRootElement(
  name="handler-chains", 
  namespace="http://java.sun.com/xml/ns/javaee"
)
public class HandlerChain {
  private QName _portNamePattern;
  private String _bindingId;
  private List<Handler> _handlers;

  @XmlElement(name="handler",
              namespace="http://java.sun.com/xml/ns/javaee")
  public List<Handler> getHandler()
  {
    if (_handlers == null)
      _handlers = new ArrayList<Handler>();

    return _handlers;
  }

  public void setHandler(List<Handler> handlers)
  {
    _handlers = handlers;
  }

  @XmlElement(name="port-name-pattern",
              namespace="http://java.sun.com/xml/ns/javaee")
  public QName getPortNamePattern()
  {
    return _portNamePattern;
  }

  public void setPortNamePattern(QName portNamePattern)
  {
    _portNamePattern = portNamePattern;
  }

  @XmlElement(name="protocol-bindings",
              namespace="http://java.sun.com/xml/ns/javaee")
  public String getBindingID()
  {
    return _bindingId;
  }

  public void setBindingID(String protocolBindings)
  {
    if ("##SOAP11_HTTP".equals(protocolBindings))
      _bindingId = "http://schemas.xmlsoap.org/wsdl/soap/http";

    else if ("##SOAP11_HTTP_MTOM".equals(protocolBindings))
      _bindingId = "http://schemas.xmlsoap.org/wsdl/soap/http?mtom=true";

    else if ("##SOAP12_HTTP".equals(protocolBindings))
      _bindingId = "http://www.w3.org/2003/05/soap/bindings/HTTP/";

    else if ("##SOAP12_HTTP_MTOM".equals(protocolBindings))
      _bindingId = "http://www.w3.org/2003/05/soap/bindings/HTTP/?mtom=true";

    else if ("##XML_HTTP".equals(protocolBindings))
      _bindingId = "http://www.w3.org/2004/08/wsdl/http";

    else
      _bindingId = protocolBindings; // XXX??
  }

  public boolean isDefault()
  {
    return _bindingId == null && _portNamePattern == null;
  }

  public List<javax.xml.ws.handler.Handler> toHandlerList()
  {
    List<javax.xml.ws.handler.Handler> list =
      new ArrayList<javax.xml.ws.handler.Handler>();

    for (Handler handler : getHandler())
      list.add(handler.toHandler());

    return list;
  }
}
