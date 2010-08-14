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

package javax.xml.rpc;

import javax.xml.namespace.QName;
import javax.xml.rpc.encoding.TypeMappingRegistry;
import javax.xml.rpc.handler.HandlerRegistry;
import java.net.URL;
import java.rmi.Remote;
import java.util.Iterator;

/**
 * Abstract implementation of a XML-RPC service.
 */
public interface Service {
  /**
   * Returns an implementation of the generated stub.
   */
  public Remote getPort(QName portName,
                        Class serviceEndpoingInterface)
    throws ServiceException;
  
  /**
   * Returns an implementation of the generated stub.
   */
  public Remote getPort(Class serviceEndpoingInterface)
    throws ServiceException;
  
  /**
   * Returns the calls for the port.
   */
  public Call []getCalls(QName portName)
    throws ServiceException;
  
  /**
   * Returns the call for the port.
   */
  public Call createCall(QName portName)
    throws ServiceException;
  
  /**
   * Returns the call for the port.
   */
  public Call createCall(QName portName,
                         QName operationName)
    throws ServiceException;
  
  /**
   * Returns the call for the port.
   */
  public Call createCall(QName portName,
                         String operationName)
    throws ServiceException;
  
  /**
   * Returns the call for the port.
   */
  public Call createCall()
    throws ServiceException;
  
  /**
   * Returns the name of the service.
   */
  public QName getServiceName()
    throws ServiceException;
  
  /**
   * Returns the service endpoing ports.
   */
  public Iterator getPorts()
    throws ServiceException;
  
  /**
   * Returns the location of WSDL document.
   */
  public URL getWSDLDocumentLocation()
    throws ServiceException;
  
  /**
   * Returns the TypeMappingRegistry for the Service.
   */
  public TypeMappingRegistry getTypeMappingRegistry();
  
  /**
   * Returns the HandlerRegistry for the Service.
   */
  public HandlerRegistry getHandlerRegistry();
}
