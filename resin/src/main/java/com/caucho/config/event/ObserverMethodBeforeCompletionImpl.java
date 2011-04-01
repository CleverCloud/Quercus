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

package com.caucho.config.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Bean;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;

import com.caucho.config.inject.InjectManager;
import com.caucho.transaction.TransactionImpl;
import com.caucho.transaction.TransactionManagerImpl;

/**
 * Internal implementation for a producer Bean
 */
public class ObserverMethodBeforeCompletionImpl<X,T>
  extends ObserverMethodImpl<X,T>
{
  private static final Logger log 
    = Logger.getLogger(ObserverMethodBeforeCompletionImpl.class.getName());
  
  private TransactionManagerImpl _tm;
  
  public ObserverMethodBeforeCompletionImpl(InjectManager beanManager,
                                            Bean<X> bean,
                                            AnnotatedMethod<X> method,
                                            Type type,
                                            Set<Annotation> qualifiers)
  {
    super(beanManager, bean, method, type, qualifiers);
    
    _tm = TransactionManagerImpl.getInstance();
  }

  /**
   * Send the event notification.
   */
  @Override
  public void notify(T event)
  {
    TransactionImpl xa = _tm.getCurrent();
    
    try {
      if (xa != null && xa.isActive())
        xa.registerSynchronization(new BeforeCompletion(event));
      else
        notifyImpl(event);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  class BeforeCompletion implements Synchronization {
    private T _event;
    
    BeforeCompletion(T event)
    {
      _event = event;
    }
    
    @Override
    public void beforeCompletion()
    {
      notifyImpl(_event);
    }

    @Override
    public void afterCompletion(int status)
    {
    }
  }
}
