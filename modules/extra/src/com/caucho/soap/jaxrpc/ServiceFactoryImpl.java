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
import com.caucho.soap.wsdl.WSDLDefinitions;
import com.caucho.soap.wsdl.WSDLParser;
import com.caucho.util.L10N;

import javax.xml.namespace.QName;
import javax.xml.rpc.Service;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.ServiceFactory;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Service factory.
 */
public class ServiceFactoryImpl extends ServiceFactory {
  private final static Logger log = Log.open(ServiceFactoryImpl.class);
  private final static L10N L = new L10N(ServiceFactoryImpl.class);
  
  /**
   * Creates a service given a qname.
   */
  public Service createService(QName serviceName)
  {
    return new ServiceImpl(serviceName);
  }
  
  /**
   * Creates a service given a WSDL location and a service name.
   */
  public Service createService(URL wsdl, QName serviceName)
    throws ServiceException
  {
    /*
    WSDLDefinitions defs = parseWSDL(wsdl);

    WSDLService service = defs.getService(serviceName);

    if (service == null)
      throw new ServiceException(L.l("'{0}' is an unknown service in {1}.",
                                     serviceName, wsdl));

    return new ServiceImpl(service);
    */
    throw new UnsupportedOperationException();
  }
  
  /**
   * Creates a service given an interface.
   */
  public Service loadService(Class serviceInterface)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Creates a service given an interface.
   */
  public Service loadService(URL wsdl,
                             Class serviceInterface,
                             Properties properties)
    throws ServiceException
  {
    parseWSDL(wsdl);

    return new ServiceImpl((QName) null);
  }
  
  /**
   * Creates a service given an interface.
   */
  public Service loadService(URL wsdl,
                             QName serviceName,
                             Properties properties)
    throws ServiceException
  {
    parseWSDL(wsdl);
    
    return new ServiceImpl(serviceName);
  }

  /**
   * Parses the wsdl.
   */
  private WSDLDefinitions parseWSDL(URL wsdl)
    throws ServiceException
  {
    try {
      InputStream is = wsdl.openStream();
      try {
        return WSDLParser.parse(is);
      } finally {
        is.close();
      }
    } catch (Exception e) {
      throw new ServiceException(e);
    }
  }

  /**
   * Returns the id.
   */
  public String toString()
  {
    return "ServiceFactoryImpl[]";
  }
}


