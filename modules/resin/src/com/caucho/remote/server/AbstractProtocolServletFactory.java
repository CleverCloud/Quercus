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

package com.caucho.remote.server;

import com.caucho.config.*;
import com.caucho.remote.*;

import java.lang.annotation.Annotation;

import javax.ejb.*;
import javax.jws.*;
import javax.servlet.*;

import javax.enterprise.inject.spi.Annotated;
import javax.servlet.*;

/**
 * Abstract factory for creating @WebService and @Remote servlets. 
 */
abstract public class AbstractProtocolServletFactory
  implements ProtocolServletFactory
{
  /**
   * Sets the ServiceType annotation
   */
  public void setServiceType(Annotation ann)
  {
  }
  
  /**
   * Sets the ServiceType annotated
   */
  public void setAnnotated(Annotated annotated)
  {
  }
  
  /**
   * Creates a new servlet skeleton based on an API and an object
   *
   * @param serviceApi the remoteApi exposed to the server
   * @param service the managed service object
   */
  abstract public Servlet createServlet(Class serviceApi, Object service)
    throws ServiceException;

  /**
   * Returns the remote interface to expose as a service.
   */
  protected Class getRemoteAPI(Class serviceClass)
  {
    Remote remote = (Remote) serviceClass.getAnnotation(Remote.class);

    if (remote != null)
      return remote.value()[0];

    for (Class ifc : serviceClass.getInterfaces()) {
      if (ifc.isAnnotationPresent(Remote.class))
        return ifc;
    }
    
    WebService webService
      = (WebService) serviceClass.getAnnotation(WebService.class);

    if (webService != null && ! "".equals(webService.endpointInterface())) {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      try {
        Class api = Class.forName(webService.endpointInterface(),
                                  false, loader);

        return api;
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }

    Class remoteAPI = null;
    
    for (Class ifc : serviceClass.getInterfaces()) {
      if (ifc.getName().startsWith("java.io"))
        continue;
      else if (ifc.getName().startsWith("javax.ejb"))
        continue;

      if (remoteAPI != null) {
        // XXX: possible warning
        return serviceClass;
      }
      else
        remoteAPI = ifc;
    }

    if (remoteAPI != null)
      return remoteAPI;
    else
      return serviceClass;
  }
}
