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

package com.caucho.jmx;

import com.caucho.util.L10N;
import com.caucho.config.inject.SingletonBindingHandle;

import java.util.logging.Logger;
import javax.management.*;

/**
 * JNDI object for the Resin mbean server.
 */
public class GlobalMBeanServer extends AbstractMBeanServer
  implements java.io.Serializable
{
  private static final L10N L = new L10N(GlobalMBeanServer.class);
  private static final Logger log
    = Logger.getLogger(GlobalMBeanServer.class.getName());

  private ClassLoader _loader;
  
  /**
   * Creates an MBeanServerProxy based on the context class loader.
   */
  public GlobalMBeanServer()
  {
    this(Thread.currentThread().getContextClassLoader());
  }
  
  /**
   * Creates an MBeanServerProxy based on the context class loader.
   */
  public GlobalMBeanServer(ClassLoader loader)
  {
    super(Jmx.getMBeanServer().getDefaultDomain());

    _loader = loader;
  }

  /**
   * Returns the local context.
   */
  protected MBeanContext createContext(ClassLoader loader)
  {
    AbstractMBeanServer envServer = Jmx.getMBeanServer();

    // server/2102 vs server/211{1,2}
    return envServer.createContext(loader);
  }

  /**
   * Returns the local context.
   */
  @Override
  protected MBeanContext getCurrentContext(ClassLoader loader)
  {
    AbstractMBeanServer envServer = Jmx.getMBeanServer();

    return envServer.getCurrentContext(loader);
  }

  /**
   * Returns the local context.
   */
  @Override
  protected void setCurrentContext(MBeanContext context, ClassLoader loader)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the local context.
   */
  protected MBeanContext getContext(ClassLoader loader)
  {
    AbstractMBeanServer envServer = Jmx.getMBeanServer();

    return envServer.getContext(loader);
  }

  /**
   * Returns the local view.
   */
  protected MBeanView getView()
  {
    return createContext().getGlobalView();
  }

  /**
   * Serialization.
   */
  private Object writeReplace()
  {
    return new SingletonBindingHandle(MBeanServer.class);
  }

  /**
   * Returns the string form.
   */
  public String toString()
  {
    return getClass().getSimpleName() +  "[]";
  }
}
