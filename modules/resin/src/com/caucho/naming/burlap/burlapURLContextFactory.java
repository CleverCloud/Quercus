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

package com.caucho.naming.burlap;

import com.caucho.burlap.client.BurlapProxyFactory;
import com.caucho.util.L10N;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 * Create a remote object
 */
public class burlapURLContextFactory implements ObjectFactory {
  private static L10N L = new L10N(burlapURLContextFactory.class);
  private static Logger dbg
    = Logger.getLogger(burlapURLContextFactory.class.getName());

  private BurlapProxyFactory _proxyFactory = new BurlapProxyFactory();
  private BurlapModel _model;

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
      BurlapModel model = _model;

      if (model == null) {
        model = new BurlapModel();
        _model = model;
      }

      return new BurlapContextImpl(model, env);
    }

    String url = (String) obj;

    if (url.startsWith("burlap:"))
      url = url.substring("burlap:".length());

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
