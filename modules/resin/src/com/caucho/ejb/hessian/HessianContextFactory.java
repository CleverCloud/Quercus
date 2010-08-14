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

package com.caucho.ejb.hessian;

import com.caucho.config.ConfigException;
import com.caucho.naming.AbstractModel;
import com.caucho.naming.ContextImpl;
import com.caucho.util.Base64;
import com.caucho.util.L10N;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import java.util.Hashtable;

/**
 * The root context factory.
 */
public class HessianContextFactory implements InitialContextFactory {
  static L10N L = new L10N(HessianContextFactory.class);

  private AbstractModel _model;
  
  /**
   * Returns the initial context for the current thread.
   */
  public Context getInitialContext(Hashtable<?,?> environment)
    throws NamingException
  {
    String prefix = (String) environment.get(Context.PROVIDER_URL);

    String user = (String) environment.get(Context.SECURITY_PRINCIPAL);
    String pw = (String) environment.get(Context.SECURITY_CREDENTIALS);

    if (! prefix.endsWith("/"))
      prefix = prefix + '/';

    if (user != null) {
      String auth = Base64.encode(user + ':' + pw);
      
      HessianModel model = new HessianModel(prefix);
      /* XXX: needs replacement
      HessianClientContainer client;
      try {
        client = new HessianClientContainer(model.getURLPrefix());
        client.setBasicAuthentication(auth);
      } catch (ConfigException e) {
        throw new NamingException(e.toString());
      }

      model.setClientContainer(client);
      */
      return new ContextImpl(model, environment);
    }

    if (_model == null)
      _model = new HessianModel(prefix);

    return new ContextImpl(_model, environment);
  }
}
