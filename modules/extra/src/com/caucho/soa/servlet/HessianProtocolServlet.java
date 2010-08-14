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

package com.caucho.soa.servlet;

import com.caucho.hessian.io.SerializerFactory;
import com.caucho.soa.encoding.HessianEncoding;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Invokes a service based on a Hessian-encoded request.
 */
public class HessianProtocolServlet
  extends GenericServlet
  implements ProtocolServlet
{
  protected static Logger log
    = Logger.getLogger(HessianProtocolServlet.class.getName());
  
  private final HessianEncoding _hessian = new HessianEncoding();

  /**
   * Sets the service class.
   */
  public void setService(Object service)
  {
    _hessian.setService(service);
  }

  /**
   * Sets the serializer factory.
   */
  public void setSerializerFactory(SerializerFactory factory)
  {
    _hessian.setSerializerFactory(factory);
  }

  /**
   * Sets the serializer send collection java type.
   */
  public void setSendCollectionType(boolean sendType)
  {
    _hessian.setSendCollectionType(sendType);
  }

  public void init()
    throws ServletException
  {
    try {
      _hessian.init();
    } catch (ServletException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  public void service(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    _hessian.invoke(request.getInputStream(),
                    response.getOutputStream());
  }
}
