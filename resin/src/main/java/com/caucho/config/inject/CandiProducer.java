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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

import com.caucho.config.ConfigException;
import com.caucho.config.gen.CandiEnhancedBean;
import com.caucho.config.gen.CandiUtil;
import com.caucho.config.program.Arg;
import com.caucho.config.program.ConfigProgram;
import com.caucho.inject.Module;
import com.caucho.util.L10N;

/**
 * SimpleBean represents a POJO Java bean registered as a WebBean.
 */
@Module
public class CandiProducer<X> implements InjectionTarget<X>
{
  private static final L10N L = new L10N(CandiProducer.class);
  private static final Logger log 
    = Logger.getLogger(CandiProducer.class.getName());
  private static final Object []NULL_ARGS = new Object[0];

  private InjectManager _injectManager;
  private Class<X> _instanceClass;
  
  private Bean<X> _bean;

  private Constructor<X> _javaCtor;
  private Arg []_args;
  
  private ConfigProgram []_injectProgram;
  private ConfigProgram []_initProgram;
  private ConfigProgram []_destroyProgram;

  private Set<InjectionPoint> _injectionPointSet;
  
  private Object _decoratorClass;
  private List<Decorator<?>> _decoratorBeans;
  
  public CandiProducer(Bean<X> bean,
                       Class<X> instanceClass,
                       Constructor<X> javaCtor,
                       Arg []args,
                       ConfigProgram []injectProgram,
                       ConfigProgram []initProgram,
                       ConfigProgram []destroyProgram,
                       Set<InjectionPoint> injectionPointSet)
  {
    _injectManager = InjectManager.create();
    
    _bean = bean;
    _instanceClass = instanceClass;
    
    _javaCtor = javaCtor;
    _args = args;
    _injectProgram = injectProgram;
    _initProgram = initProgram;
    _destroyProgram = destroyProgram;
    _injectionPointSet = injectionPointSet;
    
    for (ConfigProgram program : _injectProgram) {
      program.bind();
    }
    
    if (injectionPointSet == null)
      throw new NullPointerException();
    
    if (instanceClass != null
        && CandiEnhancedBean.class.isAssignableFrom(instanceClass)) {
      try {
        Method method = instanceClass.getMethod("__caucho_decorator_init");

        _decoratorClass = method.invoke(null);
        
        Annotation []qualifiers = new Annotation[bean.getQualifiers().size()];
        bean.getQualifiers().toArray(qualifiers);
        
        _decoratorBeans = _injectManager.resolveDecorators(bean.getTypes(), qualifiers);
        
        method = instanceClass.getMethod("__caucho_init_decorators",
                                         List.class);
        
        method.invoke(null, _decoratorBeans);
      } catch (InvocationTargetException e) {
        throw ConfigException.create(e.getCause());
      } catch (Exception e) {
        log.log(Level.FINEST, e.toString(), e);
      }
    }
  }

  /**
   * Returns the injection points.
   */
  @Override
  public Set<InjectionPoint> getInjectionPoints()
  {
    return _injectionPointSet;
  }

  @Override
  public X produce(CreationalContext<X> ctx)
  {
    try {
      CreationalContextImpl<X> env = null;
      
      if (ctx instanceof CreationalContextImpl<?>)
        env = (CreationalContextImpl<X>) ctx;
      
      Object []delegates = null;
      
      InjectionPoint oldPoint = null;
      InjectionPoint ip = null;
      
      if (env != null) {
        oldPoint = env.findInjectionPoint();
        ip = oldPoint;
      }
      
      
      if (_decoratorBeans != null && _decoratorBeans.size() > 0) {
        Decorator dec = (Decorator) _decoratorBeans.get(_decoratorBeans.size() - 1);
        
        if (dec instanceof DecoratorBean && env != null) {
          ip = ((DecoratorBean) dec).getDelegateInjectionPoint();
          env.setInjectionPoint(ip);
        }
      }
      
      Object []args = evalArgs(env);

      X value;
      
      if (_javaCtor != null) {
        value = _javaCtor.newInstance(args);
      }
      else
        value = _instanceClass.newInstance();

      if (env != null)
        env.push(value);
      
      if (_decoratorBeans != null) {
        if (env != null)
          env.setInjectionPoint(oldPoint);
        
        delegates = CandiUtil.generateProxyDelegate(_injectManager,
                                                    _decoratorBeans,
                                                    _decoratorClass,
                                                    env);
        
        if (env != null)
          env.setInjectionPoint(ip);
      }
      
      // server/4750
      if (value instanceof CandiEnhancedBean) {
        CandiEnhancedBean enhancedBean = (CandiEnhancedBean) value;
        
        enhancedBean.__caucho_inject(delegates, env);
      }

      return value;
    } catch (RuntimeException e) {
      throw e;
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException)
        throw (RuntimeException) e.getCause();
      else
        throw new CreationException(e.getCause());
    } catch (InstantiationException e) {
      throw new CreationException(L.l("Exception while creating {0}",
                                      _javaCtor != null ? _javaCtor : _instanceClass),
                                  e);
    } catch (Exception e) {
      throw new CreationException(e);
    } catch (ExceptionInInitializerError e) {
      throw new CreationException(e);
    }
  }
  
  private Object []evalArgs(CreationalContextImpl<?> env)
  {
    Arg []args = _args;
    
    if (args == null)
      return NULL_ARGS;
    
    int size = args.length;
    
    if (size > 0) {
      Object []argValues = new Object[size];

      for (int i = 0; i < size; i++) {
        argValues[i] = args[i].eval(env);
      }
      
      return argValues;
    }
    else
      return NULL_ARGS;
  }

  @Override
  public void inject(X instance, CreationalContext<X> env)
  {
    try {
      for (ConfigProgram program : _injectProgram) {
        // log.info("INJECT: " + program);
        program.inject(instance, env);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new CreationException(e);
    }
  }

  @Override
  public void postConstruct(X instance)
  {
    try {
      CreationalContext<X> env = null;

      // server/4750, ioc/0c29
      if (instance instanceof CandiEnhancedBean) {
        CandiEnhancedBean bean = (CandiEnhancedBean) instance;
        bean.__caucho_postConstruct();
      }
      else {
        for (ConfigProgram program : _initProgram) {
          // log.info("POST: " + program);
          program.inject(instance, env);
        }
      }

      /*
      if (instance instanceof HandleAware) {
        SerializationAdapter.setHandle(instance, getHandle());
      }
      */
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new CreationException(e);
    }
  }

  /**
   * Call pre-destroy
   */
  @Override
  public void preDestroy(X instance)
  {
    try {
      CreationalContextImpl<X> env = null;

      for (ConfigProgram program : _destroyProgram) {
        program.inject(instance, env);
      }

      // server/4750
      if (instance instanceof CandiEnhancedBean) {
        CandiEnhancedBean bean = (CandiEnhancedBean) instance;
        bean.__caucho_destroy(env);
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new CreationException(e);
    }
  }

  public void dispose(X instance)
  {
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _bean + "]";
  }
}
