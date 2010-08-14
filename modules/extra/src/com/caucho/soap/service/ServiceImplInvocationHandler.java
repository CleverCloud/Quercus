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

package com.caucho.soap.service;

import com.caucho.config.ConfigException;
import com.caucho.soap.reflect.WebServiceIntrospector;
import com.caucho.soap.skeleton.DirectSkeleton;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.MalformedURLException;

public class ServiceImplInvocationHandler implements InvocationHandler
{
  private Class _api;
  private DirectSkeleton _skeleton;
  private String _url;

  public ServiceImplInvocationHandler(Class api, String url)
    throws ConfigException, JAXBException
  {
    _api = api;
    _url = url;

    try {
      _skeleton = WebServiceIntrospector.introspect(api, url, null);
    }
    catch (WebServiceException e) {
      throw new RuntimeException(e);
    }
  }

  public Object invoke(Object proxy, Method method, Object[] args)
    throws IOException, XMLStreamException, MalformedURLException, 
           JAXBException, Throwable
  {
    Object ret = _skeleton.invoke(method, _url, args);

    return ret == null ? new Integer(12) : ret;
  }
}
