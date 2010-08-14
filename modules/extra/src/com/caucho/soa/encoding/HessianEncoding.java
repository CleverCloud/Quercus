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

package com.caucho.soa.encoding;

import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianOutput;
import com.caucho.hessian.io.SerializerFactory;
import com.caucho.hessian.server.HessianSkeleton;
import com.caucho.server.util.CauchoSystem;
import com.caucho.services.server.GenericService;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.jws.WebService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Invokes a service based on a Hessian-encoded request.
 */
public class HessianEncoding implements ServiceEncoding {
  protected static Logger log
    = Logger.getLogger(HessianEncoding.class.getName());
  private static final L10N L = new L10N(HessianEncoding.class);

  private Object _serviceImpl;
  
  private HessianSkeleton _skeleton;

  private SerializerFactory _serializerFactory;

  /**
   * Sets the service class.
   */
  public void setService(Object serviceImpl)
  {
    _serviceImpl = serviceImpl;
  }

  /**
   * Sets the serializer factory.
   */
  public void setSerializerFactory(SerializerFactory factory)
  {
    _serializerFactory = factory;
  }

  /**
   * Gets the serializer factory.
   */
  public SerializerFactory getSerializerFactory()
  {
    if (_serializerFactory == null)
      _serializerFactory = new SerializerFactory();

    return _serializerFactory;
  }

  /**
   * Sets the serializer send collection java type.
   */
  public void setSendCollectionType(boolean sendType)
  {
    getSerializerFactory().setSendCollectionType(sendType);
  }

  @PostConstruct
  public void init()
    throws Exception
  {
    if (_serviceImpl == null)
      _serviceImpl = this;

    Class api = null;

    if (_serviceImpl != null) {
      api = findWebServiceEndpointInterface(_serviceImpl.getClass());

      if (api == null)
        api = findRemoteAPI(_serviceImpl.getClass());

      if (api == null)
        api = _serviceImpl.getClass();
    }

    _skeleton = new HessianSkeleton(_serviceImpl, api);
  }
  
  private Class findWebServiceEndpointInterface(Class implClass)
    throws ClassNotFoundException
  {
    if (implClass.isAnnotationPresent(javax.jws.WebService.class)) {
      WebService webServiceAnnotation = 
        (WebService) implClass.getAnnotation(javax.jws.WebService.class);

      String endpoint = webServiceAnnotation.endpointInterface();
      if (endpoint != null && ! "".equals(endpoint))
        return CauchoSystem.loadClass(endpoint);
    }

    return null;
  }

  private Class findRemoteAPI(Class implClass)
  {
    if (implClass == null || implClass.equals(GenericService.class))
      return null;
    
    Class []interfaces = implClass.getInterfaces();

    if (interfaces.length == 1)
      return interfaces[0];

    return findRemoteAPI(implClass.getSuperclass());
  }

  public void invoke(InputStream is, OutputStream os)
  {
    try {
      Hessian2Input in = new Hessian2Input(is);
      AbstractHessianOutput out;

      SerializerFactory serializerFactory = getSerializerFactory();

      in.setSerializerFactory(serializerFactory);

      int code = in.read();

      if (code != 'c') {
        // XXX: deflate
        throw new IOException(L.l("expected 'c' in hessian input at {0}", 
                                  code));
      }

      int major = in.read();
      int minor = in.read();

      if (major >= 2)
        out = new Hessian2Output(os);
      else
        out = new HessianOutput(os);

      out.setSerializerFactory(serializerFactory);

      if (_skeleton == null)
        throw new Exception("skeleton is null!");

      _skeleton.invoke(in, out);

      out.close();
    } catch (IOException e) {
      log.log(Level.INFO, L.l("Unable to process request: "), e);
    } catch (Throwable e) {
      log.log(Level.INFO, L.l("Unable to process request: "), e);
    }
  }
}
