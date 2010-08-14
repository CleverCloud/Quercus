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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.IllegalProductException;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;
import javax.inject.Named;

import com.caucho.config.ConfigException;
import com.caucho.config.bytecode.ScopeAdapter;
import com.caucho.config.program.Arg;
import com.caucho.config.reflect.AnnotatedTypeUtil;
import com.caucho.config.reflect.BaseType;
import com.caucho.inject.Module;
import com.caucho.util.L10N;

/*
 * Configuration for a @Produces method
 */
@Module
public class ProducesMethodBean<X,T> extends AbstractIntrospectedBean<T>
  implements ScopeAdapterBean<X>
{
  private static final Logger log = Logger.getLogger(ProducesMethodBean.class.getName());
  
  private static final L10N L = new L10N(ProducesMethodBean.class);

  private static final Object []NULL_ARGS = new Object[0];

  private final Bean<X> _producerBean;
  private final AnnotatedMethod<? super X> _producesMethod;
  private AnnotatedParameter<? super X> _disposedParam;
  
  private LinkedHashSet<InjectionPoint> _injectionPointSet
    = new LinkedHashSet<InjectionPoint>();

  private MethodProducer _methodProducer;
  private DisposesProducer<T,X> _disposesProducer;
  private Producer<T> _producer;
  
  private boolean _isPassivating;

  private Arg<?> []_producesArgs;

  private boolean _isBound;

  private Object _scopeAdapter;

  private ProducesMethodBean(InjectManager manager,
                             Bean<X> producerBean,
                             AnnotatedMethod<? super X> producesMethod,
                             Arg<?> []producesArgs,
                             AnnotatedMethod<? super X> disposesMethod,
                             Arg<?> []disposesArgs)
  {
    super(manager, producesMethod.getBaseType(), producesMethod);

    _producerBean = producerBean;
    _producesMethod = producesMethod;
    _producesArgs = producesArgs;
    
    if (producesMethod == null)
      throw new NullPointerException();

    if (producesArgs == null)
      throw new NullPointerException();

    producesMethod.getJavaMember().setAccessible(true);
    
    if (disposesMethod != null) {
      _disposesProducer
        = new DisposesProducer<T,X>(manager, producerBean,
                                    disposesMethod, disposesArgs);
      
      for (AnnotatedParameter<? super X> param : disposesMethod.getParameters()) {
        if (param.isAnnotationPresent(Disposes.class))
          _disposedParam = param;
      }
    }
    
    introspectInjectionPoints();
    
    _methodProducer = new MethodProducer();
    _producer = _methodProducer;
    
    Method javaMethod = producesMethod.getJavaMember();
    int modifiers = javaMethod.getModifiers();
    
    if (producesMethod.isAnnotationPresent(Specializes.class)) {
      if (Modifier.isStatic(modifiers)) {
        throw new ConfigException(L.l("{0}.{1} is an invalid @Specializes @Producer because the method is static.",
                                      javaMethod.getDeclaringClass().getName(),
                                      javaMethod.getName()));
      }
      
      Method parentMethod = getSpecializedMethod(javaMethod);
      
      if (parentMethod == null) {
        throw new ConfigException(L.l("{0}.{1} is an invalid @Specializes @Producer because it does not directly specialize a parent method",
                                      javaMethod.getDeclaringClass().getName(),
                                      javaMethod.getName()));

      }
      
      if (producesMethod.getJavaMember().isAnnotationPresent(Named.class)
          && parentMethod.isAnnotationPresent(Named.class)) {
        throw new ConfigException(L.l("{0}.{1} is an invalid @Specializes @Producer because both it and its parent defines @Named",
                                      javaMethod.getDeclaringClass().getName(),
                                      javaMethod.getName()));


      }
    }
  }
  
  private Method getSpecializedMethod(Method javaMethod)
  {
    Class<?> childClass = javaMethod.getDeclaringClass();
    Class<?> parentClass = childClass.getSuperclass();
    
    return AnnotatedTypeUtil.findMethod(parentClass.getDeclaredMethods(),
                                        javaMethod);
  }

  public static <X,T> ProducesMethodBean<X,T> 
  create(InjectManager manager,
         Bean<X> producer,
         AnnotatedMethod<? super X> producesMethod,
         Arg<? super X> []producesArgs,
         AnnotatedMethod<? super X> disposesMethod,
         Arg<? super X> []disposesArgs)
  {
    ProducesMethodBean<X,T> bean = new ProducesMethodBean<X,T>(manager, producer, 
                                                   producesMethod, producesArgs,
                                                   disposesMethod, disposesArgs);
    bean.introspect();
    bean.introspect(producesMethod);
    
    BaseType type = manager.createSourceBaseType(producesMethod.getBaseType());
    
    if (type.isGeneric()) {
      // ioc/07f0
      throw new InjectionException(L.l("'{0}' is an invalid @Produces method because it returns a generic type {1}",
                                       producesMethod.getJavaMember(),
                                       type));
    }

    return bean;
  }

  public Producer<T> getProducer()
  {
    return _producer;
  }

  public void setProducer(Producer<T> producer)
  {
    _producer = producer;
  }
  
  public Bean<?> getProducerBean()
  {
    return _producerBean;
  }
  
  @Override
  protected String getDefaultName()
  {
    String methodName = _producesMethod.getJavaMember().getName();

    if (methodName.startsWith("get") && methodName.length() > 3) {
      return (Character.toLowerCase(methodName.charAt(3))
              + methodName.substring(4));
    }
    else
      return methodName;
  }

  public boolean isInjectionPoint()
  {
    for (Class<?> paramType : _producesMethod.getJavaMember().getParameterTypes()) {
      if (InjectionPoint.class.equals(paramType))
        return true;
    }

    return false;
  }

  @Override
  public boolean isNullable()
  {
    return ! getBaseType().isPrimitive();
  }

  @Override
  public Class<?> getBeanClass()
  {
    return _producerBean.getBeanClass();
  }
  
  public AnnotatedMethod<? super X> getProducesMethod()
  {
    return _producesMethod;
  }
  
  public AnnotatedParameter<? super X> getDisposedParameter()
  {
    return _disposedParam;
  }
  
  @Override
  public Set<InjectionPoint> getInjectionPoints()
  {
    return _injectionPointSet;
  }
  
  //
  // introspection
  //

  /**
   * Adds the stereotypes from the bean's annotations
   */
  @Override
  protected void introspectSpecializes(Annotated annotated)
  {
    if (! annotated.isAnnotationPresent(Specializes.class))
      return;
  }
  
  private void introspectInjectionPoints()
  {
    for (AnnotatedParameter<?> param : _producesMethod.getParameters()) {
      InjectionPointImpl ip = new InjectionPointImpl(getBeanManager(), this, param);

      _injectionPointSet.add(ip);
    }
  }
  
  @Override
  public void introspect()
  {
    super.introspect();
    
    _isPassivating = getBeanManager().isPassivatingScope(getScope());
  }
  
  //
  // Bean creation methods
  //


  @Override
  public T create(CreationalContext<T> createEnv)
  {
    T value = _producer.produce(createEnv);
    
    createEnv.push(value);
    
    return value;
  }

  @Override
  public X getScopeAdapter(Bean<?> topBean, CreationalContextImpl<X> cxt)
  {
    NormalScope scopeType = getScope().getAnnotation(NormalScope.class);

    // ioc/0520
    if (scopeType != null) {
      //  && ! getScope().equals(ApplicationScoped.class)) {
      // && scopeType.normal()
      //  && ! env.canInject(getScope())) {

      Object value = _scopeAdapter;

      if (value == null) {
        ScopeAdapter scopeAdapter = ScopeAdapter.create(getJavaClass());
        _scopeAdapter = scopeAdapter.wrap(getBeanManager().createNormalInstanceFactory(topBean));
        value = _scopeAdapter;
      }

      return (X) value;
    }

    return null;
  } 

  @Override
  public void bind()
  {
    synchronized (this) {
      if (_isBound)
        return;

      _isBound = true;
    }
  }

  /**
   * Call destroy
   */
  @Override
  public void destroy(T instance, CreationalContext<T> cxt)
  {
    if (_producer == _methodProducer)
      _methodProducer.destroy(instance, (CreationalContextImpl<T>) cxt);
    else
      _producer.dispose(instance);
    
    if (cxt instanceof CreationalContextImpl<?>) {
      CreationalContextImpl<?> env = (CreationalContextImpl<?>) cxt;
      
      env.clearTarget();
    }
    
    cxt.release();
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");

    Method method = _producesMethod.getJavaMember();

    sb.append(getTargetSimpleName());
    sb.append(", ");
    sb.append(method.getDeclaringClass().getSimpleName());
    sb.append(".");
    sb.append(method.getName());
    sb.append("()");

    sb.append(", {");

    boolean isFirst = true;
    for (Annotation ann : getQualifiers()) {
      if (! isFirst)
        sb.append(", ");

      sb.append(ann);

      isFirst = false;
    }

    sb.append("}");

    if (getName() != null) {
      sb.append(", name=");
      sb.append(getName());
    }

    sb.append("]");

    return sb.toString();
  }
  
  class MethodProducer implements Producer<T> {
    /**
     * Produces a new bean instance
     */
    @Override
    public T produce(CreationalContext<T> cxt)
    {
      Class<?> type = _producerBean.getBeanClass();
      
      // factory instance owns its own dependency chain; it's not one of the
      // context bean's dependencies.
      CreationalContextImpl<T> env = null;
      
      if (cxt instanceof CreationalContextImpl<?>) {
        env = (CreationalContextImpl<T>) cxt;
      }
      
      ProducesCreationalContext<X> factoryEnv = null;
      
      X factory = CreationalContextImpl.find(env, _producerBean);
      
      if (factory == null) {
        factoryEnv = new ProducesCreationalContext<X>(_producerBean, env);
        
        factory = getBeanManager().getReference(_producerBean, factoryEnv);
      }
      
      if (factory == null) {
        throw new IllegalStateException(L.l("{0}: unexpected null factory for {1}",
                                            this, _producerBean));
      }
      
      T instance = produce(factory, env);
      
      if (env != null && _producerBean.getScope() == Dependent.class) {
        factoryEnv.release();
        // _producerBean.destroy(factory, factoryEnv);
      }

      
      if (_isPassivating && ! (instance instanceof Serializable))
        throw new IllegalProductException(L.l("'{0}' is an invalid @{1} instance because it's not serializable for bean {2}",
                                              instance, getScope().getSimpleName(), this));
      
      return instance;
    }

    /**
     * Produces a new bean instance
     */
    private T produce(X bean, CreationalContextImpl<T> env)
    
    {
      try {
        // InjectManager inject = getBeanManager();

        Object []args;

        if (_producesArgs.length > 0) {
          args = new Object[_producesArgs.length];

          for (int i = 0; i < args.length; i++) {
            if (_producesArgs[i] instanceof InjectionPointArg<?>)
              args[i] = env.findInjectionPoint();
            else
              args[i] = _producesArgs[i].eval((CreationalContext) env);
          }
        }
        else
          args = NULL_ARGS;

        T value = (T) _producesMethod.getJavaMember().invoke(bean, args);
        
        env.push(value);
        
        if (value != null)
          return value;
        
        if (Dependent.class.equals(getScope()))
          return null;
        
        throw new IllegalProductException(L.l("producer {0} returned null, which is not allowed by the CDI spec.",
                                              this));
      } catch (RuntimeException e) {
        throw e;
      } catch (InvocationTargetException e) {
        if (e.getCause() instanceof RuntimeException)
          throw (RuntimeException) e.getCause();
        else
          throw new CreationException(e.getCause());
      } catch (Exception e) {
        throw new CreationException(e);
      }
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
      if (_disposesProducer != null)
        _disposesProducer.destroy(instance, cxt);
    }
   
    @Override
    public Set<InjectionPoint> getInjectionPoints()
    {
      return ProducesMethodBean.this.getInjectionPoints();
    }

    @Override
    public String toString()
    {
      Method javaMethod = _producesMethod.getJavaMember();
      
      return (getClass().getSimpleName()
          + "[" + javaMethod.getDeclaringClass().getSimpleName()
          + "." + javaMethod.getName() + "]");
    }
  }

}
