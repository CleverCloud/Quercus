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

import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.Environment;
import com.caucho.util.L10N;

import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerDelegateMBean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNDI object for the Resin mbean server.
 */
public class EnvironmentMBeanServer extends AbstractMBeanServer {
  private static final L10N L = new L10N(EnvironmentMBeanServer.class);
  private static final Logger log
    = Logger.getLogger(EnvironmentMBeanServer.class.getName());

  private EnvironmentLocal<MBeanContext> _localContext =
    new EnvironmentLocal<MBeanContext>();
  
  private MBeanServerDelegate _globalDelegate;

  private MBeanContext _globalContext;
  
  /**
   * Creates an MBeanServerProxy based on the context class loader.
   */
  public EnvironmentMBeanServer(String domain, MBeanServerDelegate delegate)
  {
    super(domain);

    if (Jmx.getMBeanServer() == null)
      Jmx.setMBeanServer(this);

    ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
    
    MBeanContext context = new MBeanContext(this, systemLoader, delegate,
                                            null);

    _globalContext = context;

    _localContext.setGlobal(context);

    try {
      IntrospectionMBean mbean;
      mbean = new IntrospectionMBean(delegate, MBeanServerDelegateMBean.class);

      MBeanWrapper mbeanWrapper;
      mbeanWrapper = new MBeanWrapper(context, SERVER_DELEGATE_NAME,
                                      delegate, mbean);

      context.registerMBean(mbeanWrapper, SERVER_DELEGATE_NAME);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Returns the local context.
   */
  protected MBeanContext createContext(ClassLoader loader)
  {
    synchronized (_localContext) {
      MBeanContext context = _localContext.getLevel(loader);

      if (context == null) {
        if (loader instanceof DynamicClassLoader
            && ((DynamicClassLoader) loader).isDestroyed())
          throw new IllegalStateException(L.l("JMX context {0} has been closed.",
                                              loader));

        MBeanServerDelegate delegate;
        delegate = new MBeanServerDelegateImpl("Resin-JMX");

        context = new MBeanContext(this, loader, delegate, _globalContext);

        MBeanContext parent = null;

        if (loader != null)
          parent = createContext(loader.getParent());

        if (parent != null)
          context.setProperties(parent.copyProperties());

        _localContext.set(context, loader);

        try {
          IntrospectionMBean mbean;
          mbean = new IntrospectionMBean(delegate, MBeanServerDelegateMBean.class);

          MBeanWrapper mbeanWrapper;
          mbeanWrapper = new MBeanWrapper(context, SERVER_DELEGATE_NAME,
                                          delegate, mbean);

          context.registerMBean(mbeanWrapper, SERVER_DELEGATE_NAME);
        } catch (Exception e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }

      return context;
    }
  }

  /**
   * Returns the local context.
   */
  @Override
  protected MBeanContext getCurrentContext(ClassLoader loader)
  {
    if (loader == null)
      loader = Environment.getEnvironmentClassLoader(loader);
    
    synchronized (_localContext) {
      return _localContext.getLevel(loader);
    }
  }

  /**
   * Sets the local context.
   */
  @Override
  protected void setCurrentContext(MBeanContext context, ClassLoader loader)
  {
    if (loader == null)
      loader = Environment.getEnvironmentClassLoader(loader);
    
    synchronized (_localContext) {
      if (_localContext.getLevel(loader) != null
          && _localContext.getLevel(loader) != context)
        throw new IllegalStateException(L.l("replacing context is forbidden"));
      
      _localContext.set(context, loader);
    }
  }
  
 /**
   * Returns the local context.
   */
  protected MBeanContext getContext(ClassLoader loader)
  {
    return _localContext.get(loader);
  }

  /**
   * Returns the local context.
   */
  protected void removeContext(MBeanContext context, ClassLoader loader)
  {
    if (_localContext.get(loader) == context)
      _localContext.remove(loader);
  }

  /**
   * Returns the string form.
   */
  public String toString()
  {
    return "EnvironmentMBeanServer[]";
  }
}
