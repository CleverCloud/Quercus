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

package com.caucho.config.inject;

import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;

import com.caucho.config.inject.InjectManager.ReferenceFactory;
import com.caucho.config.program.Arg;
import com.caucho.inject.Module;

/*
 * Implements the @Disposes call.
 */
@Module
public class DisposesProducer<T,X> implements Producer<T>
{
  private final Bean<X> _producerBean;
  private final ReferenceFactory<X> _referenceFactory;
  private final AnnotatedMethod<? super X> _disposesMethod;

  private Arg<?> []_disposesArgs;
  
  DisposesProducer(InjectManager manager,
                   Bean<X> producerBean,
                   AnnotatedMethod<? super X> disposesMethod,
                   Arg<?> []disposesArgs)
  {
    _producerBean = producerBean;
    _referenceFactory = manager.getReferenceFactory(producerBean);
    _disposesMethod = disposesMethod;
    _disposesArgs = disposesArgs;

    disposesMethod.getJavaMember().setAccessible(true);
  }


  /**
   * Produces a new bean instance
   */
  @Override
  public T produce(CreationalContext<T> cxt)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
    
  @Override
  public void dispose(T instance)
  {
    destroy(instance, null);
  }

  /**
   * Call destroy
   */
  public void destroy(T instance, CreationalContextImpl<T> cxt)
  {
    if (_disposesMethod != null) {
      try {
        ProducesCreationalContext<X> env
          = new ProducesCreationalContext<X>(_producerBean, cxt);
          
        X producer = _referenceFactory.create(env, null, null);
          
        Object []args = new Object[_disposesArgs.length];
        for (int i = 0; i < args.length; i++) {
          if (_disposesArgs[i] == null)
            args[i] = instance;
          else
            args[i] = _disposesArgs[i].eval((CreationalContext) env);
        }
          
        _disposesMethod.getJavaMember().invoke(producer, args);
          
        if (_producerBean.getScope() == Dependent.class)
          _producerBean.destroy(producer, env);
      } catch (Exception e) {
        throw new RuntimeException(_disposesMethod.getJavaMember() + ":" + e, e);
      }
    }
  }
   
  @Override
  public Set<InjectionPoint> getInjectionPoints()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
