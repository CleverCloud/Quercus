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

package com.caucho.xfire;

import java.io.IOException;

import java.util.ArrayList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.Endpoint;

import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.service.binding.ObjectServiceFactory;
import org.codehaus.xfire.service.invoker.BeanInvoker;
import org.codehaus.xfire.transport.http.XFireServlet;

public class ResinXFireServlet extends XFireServlet
{
  private final Class _serviceClass;
  private final Object _instance;

  public ResinXFireServlet(Class serviceClass, Object instance)
  {
    _serviceClass = serviceClass;
    _instance = instance;
  }

  public void init(ServletConfig servletConfig) 
    throws ServletException 
  {
    super.init(servletConfig);
    ObjectServiceFactory factory = 
      new ObjectServiceFactory(getXFire().getTransportManager(), null);
    factory.addIgnoredMethods("java.lang.Comparable");
    Service service = factory.create(_serviceClass);
    service.setInvoker(new BeanInvoker(_instance));
    getController().getServiceRegistry().register(service);
  }
}
