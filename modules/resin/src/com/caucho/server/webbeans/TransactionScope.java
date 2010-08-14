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

package com.caucho.server.webbeans;

import java.lang.annotation.Annotation;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.transaction.Synchronization;

import com.caucho.inject.TransactionScoped;
import com.caucho.config.scope.AbstractScopeContext;
import com.caucho.config.scope.ContextContainer;
import com.caucho.inject.Module;
import com.caucho.transaction.TransactionImpl;
import com.caucho.transaction.TransactionManagerImpl;
import com.caucho.util.L10N;

/**
 * Scope based on the current transaction.
 */
@Module
public class TransactionScope extends AbstractScopeContext
{
  private static final L10N L = new L10N(TransactionScope.class);
  private TransactionManagerImpl _xaManager;
  
  public TransactionScope()
  {
    _xaManager = TransactionManagerImpl.getInstance();
  }
  
  /**
   * Returns true if the scope is currently active.
   */
  @Override
  public boolean isActive()
  {
    return _xaManager.getTransaction() != null;
  }

  /**
   * Returns the scope annotation type.
   */
  @Override
  public Class<? extends Annotation> getScope()
  {
    return TransactionScoped.class;
  }

  @Override
  protected ContextContainer getContextContainer()
  {
    TransactionImpl xa = _xaManager.getCurrent();
    
    if (xa == null || ! xa.isActive()) {
      return null;
    }
    
    return (ContextContainer) xa.getResource("caucho.xa.scope");
  }

  @Override
  protected ContextContainer createContextContainer()
  {
    TransactionImpl xa = _xaManager.getCurrent();
    
    if (xa == null || ! xa.isActive()) {
      return null;
    }
    
    XAContextContainer context 
      = (XAContextContainer) xa.getResource("caucho.xa.scope");
    
    if (context == null) {
      context = new XAContextContainer();
      xa.setAttribute("caucho.xa.scope", context);
      xa.registerSynchronization(context);
    }
    
    return context;
  }

  @Override
  public <T> T get(Contextual<T> bean,
                   CreationalContext<T> creationalContext)
  {
    TransactionImpl xa = _xaManager.getCurrent();

    if (xa == null || ! xa.isActive()) {
      throw new ContextNotActiveException(L.l("'{0}' cannot be created because @TransactionScoped requires an active transaction.",
                                              bean));
    }
    
    ScopeContext cxt = (ScopeContext) xa.getResource("caucho.xa.scope");
    
    if (cxt == null) {
      cxt = new ScopeContext();
      xa.putResource("caucho.xa.scope", cxt);
      xa.registerSynchronization(cxt);
    }
    
    T result = cxt.get(bean);

    if (result != null || creationalContext == null)
      return (T) result;

    result = bean.create(creationalContext);

    String id = null;
    cxt.put(bean, id, result, creationalContext);

    return (T) result;
  }

  /*
  public void addDestructor(Bean comp, Object value)
  {
    ServletRequest request = ServletInvocation.getContextRequest();

    if (request != null) {
      DestructionListener listener
        = (DestructionListener) request.getAttribute("caucho.destroy");

      if (listener == null) {
        listener = new DestructionListener();
        request.setAttribute("caucho.destroy", listener);
      }

      // XXX:
      listener.addValue(comp, value);
    }
  }
  */
  
  @SuppressWarnings("serial")
  static class ScopeContext extends ContextContainer 
    implements Synchronization {
    @Override
    public void beforeCompletion()
    {
    }

    @Override
    public void afterCompletion(int status)
    {
      close();
    }
  }
}
