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

package com.caucho.servlets.naming;

import java.util.*;

import javax.naming.*;
import javax.naming.spi.*;

import com.caucho.naming.*;
import com.caucho.hessian.client.*;

public class HessianContextFactory implements InitialContextFactory {
  /**
   * Returns the initial context
   */
  public Context getInitialContext(Hashtable<?,?> env)
  {
    try {
      String url = (String) env.get(Context.PROVIDER_URL);

      HessianProxyFactory factory = new HessianProxyFactory();
      NamingProxy proxy = (NamingProxy) factory.create(NamingProxy.class, url);

      HessianModel model = new HessianModel(proxy);
    
      return new ContextImpl(model, env);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
