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

package javax.xml.ws.spi;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.ws.Dispatch;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.handler.HandlerResolver;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.Executor;

public abstract class ServiceDelegate {
  public abstract void addPort(QName portName, 
                               String bindingId, 
                               String endpointAddress)
    throws WebServiceException;

  public abstract <T> Dispatch<T> createDispatch(QName portName, 
                                                 Class<T> type, 
                                                 Mode mode)
    throws WebServiceException;

  public abstract Dispatch<Object> createDispatch(QName portName, 
                                                  JAXBContext context, 
                                                  Mode mode);
    /// XXX throws ServiceException;

  public abstract <T> T getPort(Class<T> serviceEndpointInterface)
    throws WebServiceException;

  public abstract <T> T getPort(QName portName, 
                                Class<T> serviceEndpointInterface)
    throws WebServiceException;

  public abstract Iterator<QName> getPorts()
    throws WebServiceException;

  public abstract QName getServiceName();

  public abstract URL getWSDLDocumentLocation();

  public abstract Executor getExecutor();
  public abstract void setExecutor(Executor executor);

  public abstract HandlerResolver getHandlerResolver();
  public abstract void setHandlerResolver(HandlerResolver handlerResolver);
}

