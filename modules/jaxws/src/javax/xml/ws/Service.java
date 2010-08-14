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

package javax.xml.ws;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.spi.Provider;
import javax.xml.ws.spi.ServiceDelegate;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.Executor;

public class Service {
  private final transient ServiceDelegate _delegate;

  protected Service(URL wsdl, QName serviceName)
  {
    Provider provider = Provider.provider();

    _delegate = provider.createServiceDelegate(wsdl, serviceName, getClass());
  }

  public void addPort(QName portName, String bindingId, String endpointAddress)
  {
    _delegate.addPort(portName, bindingId, endpointAddress);
  }

  public static Service create(QName serviceName)
  {
    return new Service(null, serviceName);
  }

  public static Service create(URL wsdlDocumentLocation, QName serviceName)
  {
    return new Service(wsdlDocumentLocation, serviceName);
  }

  public <T> Dispatch<T> createDispatch(QName portName, Class<T> type,
                                        Mode mode)
  {
    return _delegate.createDispatch(portName, type, mode);
  }

  public Dispatch<Object> createDispatch(QName portName,
                                         JAXBContext context, Mode mode)
  {
    return _delegate.createDispatch(portName, context, mode);
  }

  public Executor getExecutor()
  {
    return _delegate.getExecutor();
  }

  public HandlerResolver getHandlerResolver()
  {
    return _delegate.getHandlerResolver();
  }

  public <T> T getPort(Class<T> serviceEndpointName)
  {
    return _delegate.getPort(serviceEndpointName);
  }

  public <T> T getPort(QName portName, Class<T> serviceEndpointName)
  {
    return _delegate.getPort(portName, serviceEndpointName);
  }

  public Iterator<QName> getPorts()
  {
    return _delegate.getPorts();
  }

  public QName getServiceName()
  {
    return _delegate.getServiceName();
  }

  public URL getWSDLDocumentLocation()
  {
    return _delegate.getWSDLDocumentLocation();
  }

  public void setExecutor(Executor executor)
  {
    _delegate.setExecutor(executor);
  }

  public void setHandlerResolver(HandlerResolver handlerResolver)
  {
    _delegate.setHandlerResolver(handlerResolver);
  }

  public static enum Mode {
    MESSAGE, PAYLOAD;
  }
}

