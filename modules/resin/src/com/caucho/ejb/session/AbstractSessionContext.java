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

package com.caucho.ejb.session;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.SessionContext;
import javax.xml.rpc.handler.MessageContext;

import com.caucho.config.gen.CandiEnhancedBean;
import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.inject.InjectManager;
import com.caucho.ejb.server.AbstractContext;
import com.caucho.util.L10N;

/**
 * Abstract base class for an session context
 */
abstract public class AbstractSessionContext<X,T> extends AbstractContext<X>
  implements SessionContext
{
  private static final Logger log
    = Logger.getLogger(AbstractSessionContext.class.getName());
  
  private static final L10N L = new L10N(AbstractSessionContext.class);

  private transient AbstractSessionManager<X> _manager;
  private transient InjectManager _injectManager;
  private transient ClassLoader _classLoader;
  private Class<T> _api;
  private SessionProxyFactory<T> _proxyFactory;
  
  private HashMap<String,Object> _contextData = new HashMap<String,Object>();

  protected AbstractSessionContext(AbstractSessionManager<X> manager,
                                   Class<T> api)
  {
    assert(manager != null);

    _classLoader = Thread.currentThread().getContextClassLoader();
    
    _manager = manager;
    _api = api;
    
    _injectManager = InjectManager.create();
  }

  @Override
  public AbstractSessionManager<X> getServer()
  {
    return _manager;
  }
  
  public InjectManager getInjectManager()
  {
    return _injectManager;
  }
  
  public InjectManager getModuleInjectManager()
  {
    return _manager.getModuleInjectManager();
  }

  /*
   * Returns the API for the context
   */
  public Class<T> getApi()
  {
    return _api;
  }
  
  @Override
  public Class<?> getInvokedBusinessInterface()
  {
    return getApi();
  }
  
  void bind()
  {
    if (_proxyFactory == null)
      _proxyFactory = _manager.createProxyFactory(this);
  }
  
  public T createProxy(CreationalContextImpl<T> env)
  {
    if (_proxyFactory == null)
      bind();
    
    return _proxyFactory.__caucho_createProxy(env);
  }
  
  public void destroyProxy(T instance, CreationalContextImpl<T> env)
  {
    if (instance instanceof CandiEnhancedBean) {
      CandiEnhancedBean candiInstance = (CandiEnhancedBean) instance;
      
      candiInstance.__caucho_destroy(env);
    }
  }
  
  public X newInstance(CreationalContextImpl<X> env)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_classLoader);
      
      X instance = _manager.newInstance(env);
      
      return instance;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  @Override
  public void destroy()
    throws Exception
  {
    if (_proxyFactory != null)
      _proxyFactory.__caucho_destroy();
    
    super.destroy();
  }

  /**
   * Returns the EJBHome stub for the container.
   */
  @Override
  public EJBHome getEJBHome()
  {
    throw new EJBException(L.l("EJBHome does not exist for this class"));
  }

  /**
   * Returns the EJBLocalHome stub for the container.
   */
  @Override
  public EJBLocalHome getEJBLocalHome()
  {
    throw new EJBException(L.l("EJBLocalHome does not exist for this class"));
  }

  @Override
  public MessageContext getMessageContext()
  {
    throw new IllegalStateException(getClass().getName());
  }

  @Override
  public boolean wasCancelCalled()
  {
    return false;
  }

  @Override
  public <Z> Z getBusinessObject(Class<Z> businessInterface)
    throws IllegalStateException
  {
    if (businessInterface== null) {
      throw new IllegalStateException(L.l("null is not a valid local interface or no-interface view for {0}",
                                          getServer().getEjbClass().getName()));
    }
    
    AbstractSessionContext<?,Z> context = 
      getServer().getSessionContext(businessInterface);
    
    if (context == null) {
      throw new IllegalStateException(L.l("{0} is not a valid local interface or no-interface view for {1}",
                                          businessInterface.getName(),
                                          getServer().getEjbClass().getName()));
    }
    
    return context.createProxy(null);
  }

  @Override
  public EJBLocalObject getEJBLocalObject() throws IllegalStateException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public EJBObject getEJBObject() throws IllegalStateException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _api.getName() + "]";
  }
}
