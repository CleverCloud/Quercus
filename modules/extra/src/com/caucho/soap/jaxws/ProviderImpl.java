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

package com.caucho.soap.jaxws;

import com.caucho.util.L10N;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.spi.Provider;
import javax.xml.ws.spi.ServiceDelegate;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Provider
 */
public class ProviderImpl extends Provider {
  private final static Logger log
    = Logger.getLogger(ProviderImpl.class.getName());
  private final static L10N L = new L10N(ProviderImpl.class);

  private ClassLoader _loader = Thread.currentThread().getContextClassLoader();

  public Endpoint createAndPublishEndpoint(String address,
                                           Object implementor)
  {
    throw new UnsupportedOperationException();
  }

  public Endpoint createEndpoint(String bindingId,
                                 Object implementor)
  {
    throw new UnsupportedOperationException();
  }

  public ServiceDelegate createServiceDelegate(URL wsdl,
                                               QName serviceName,
                                               Class serviceClass)
  {
    return new ServiceDelegateImpl(wsdl, serviceName, serviceClass);
  }

  /**
   * Returns the id.
   */
  public String toString()
  {
    return "ProviderImpl[]";
  }
}


