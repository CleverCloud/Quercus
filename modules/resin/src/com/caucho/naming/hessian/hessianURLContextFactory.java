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

package com.caucho.naming.hessian;

import com.caucho.hessian.client.HessianProxyFactory;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

/**
 * Create a remote object
 */
public class hessianURLContextFactory implements ObjectFactory {
  private HessianProxyFactory _proxyFactory = new HessianProxyFactory();
  private HessianModel _model;

  /**
   * Returns the object instance for the given name.
   */
  public Object getObjectInstance(Object obj,
                                  Name name,
                                  Context parentContext,
                                  Hashtable<?,?> env)
    throws NamingException
  {
    if (obj == null) {
      HessianModel model = _model;

      if (model == null) {
        model = new HessianModel();
        _model = model;
      }

      return new HessianContextImpl(model, env);
    }

    String url = (String) obj;

    if (url.startsWith("hessian:"))
      url = url.substring("hessian:".length());

    if (url.startsWith("//"))
      url = "http:" + url;

    try {
      return _proxyFactory.create(url);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      NamingException exn = new NamingException(e.toString());
      exn.initCause(e);
      throw exn;
    }
  }
}
