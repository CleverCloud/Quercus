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
import com.caucho.soap.wsdl.WSDLPort;
import com.caucho.soap.wsdl.WSDLService;
import com.caucho.util.L10N;

import javax.xml.namespace.QName;
import javax.xml.rpc.Call;
import javax.xml.rpc.Service;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.encoding.TypeMappingRegistry;
import javax.xml.rpc.handler.HandlerRegistry;
import java.net.URL;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Service
 */
public class ServiceImpl implements Service {
  private final static Logger log = Log.open(ServiceImpl.class);
  private final static L10N L = new L10N(ServiceImpl.class);

  private final QName _serviceName;
  private WSDLService _service;

  ServiceImpl(QName serviceName)
  {
    _serviceName = serviceName;
  }

  ServiceImpl(WSDLService service)
  {
    _serviceName = null;//service.getName();
    _service = service;
  }
  
  /**
   * Creates a call.
   */
  public Call createCall()
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Creates a call.
   */
  public Call createCall(QName portName)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Creates a call.
   */
  public Call createCall(QName portName, QName opName)
    throws ServiceException
  {
    /*
    WSDLPort port = getPort(portName);
    WSDLOperation op = port.getOperation(opName);

    if (op == null)
      throw new ServiceException(L.l("{0} is an unknown operation in {1}",
                                     portName, opName));

    return new CallImpl(port, op);
    */
    throw new UnsupportedOperationException();
  }
  
  /**
   * Creates a call.
   */
  public Call createCall(QName portName, String operationName)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Creates a call.
   */
  public Call []getCalls(QName portName)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a call.
   */
  public HandlerRegistry getHandlerRegistry()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a port.
   */
  public java.rmi.Remote getPort(Class serviceEndpointInterface)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a port.
   */
  public java.rmi.Remote getPort(QName portName,
                                 Class serviceEndpointInterface)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the list of ports.
   */
  public Iterator getPorts()
  {
    throw new UnsupportedOperationException();
    //return _service.getPortNames();
  }

  /**
   * Returns the name of the service.
   */
  public QName getServiceName()
  {
    return _serviceName;
  }

  /**
   * Returns the type mapping registry.
   */
  public TypeMappingRegistry getTypeMappingRegistry()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the location of the WSDL
   */
  public URL getWSDLDocumentLocation()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the port.
   */
  private WSDLPort getPort(QName portName)
    throws ServiceException
  {
    throw new UnsupportedOperationException();
    /*
    WSDLPort port = _service.getPort(portName);

    if (port == null)
      throw new ServiceException(L.l("'{0}' is an unknown port in service '{1}'",
                                     portName, getServiceName()));

    return port;*/
  }

  /**
   * Returns the id.
   */
  public String toString()
  {
    return "ServiceImpl[" + getServiceName() + "]";
  }
}


