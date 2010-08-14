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
import java.lang.reflect.Field;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.IllegalProductException;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;

import com.caucho.config.program.Arg;
import com.caucho.config.reflect.BaseType;
import com.caucho.inject.Module;
import com.caucho.util.L10N;

/*
 * Configuration for a @Produces method
 */
@Module
public class ProducesFieldBean<X,T> extends AbstractIntrospectedBean<T>
{
  private static final L10N L = new L10N(ProducesFieldBean.class);

  private final Bean<X> _producerBean;
  private final AnnotatedField<X> _beanField;

  private FieldProducer _fieldProducer = new FieldProducer();
  private DisposesProducer _disposesProducer;
  
  private Producer<T> _producer = _fieldProducer;

  private boolean _isBound;
  private boolean _isPassivating;
  private boolean _isStatic;

  protected ProducesFieldBean(InjectManager manager,
                              Bean<X> producerBean,
                              AnnotatedField<X> beanField,
                              AnnotatedMethod<X> disposesMethod,
                              Arg []disposesArgs)
  {
    super(manager, beanField.getBaseType(), beanField);
    
    _producerBean = producerBean;
    _beanField = beanField;
    _isStatic = beanField.isStatic(); 
    
    if (disposesMethod != null)
      _disposesProducer = new DisposesProducer(manager, producerBean, 
                                               disposesMethod, disposesArgs);

    if (beanField == null)
      throw new NullPointerException();
  }

  public static ProducesFieldBean create(InjectManager manager,
                                         Bean producer,
                                         AnnotatedField beanField,
                                         AnnotatedMethod disposesMethod,
                                         Arg []disposesArgs)
  {
    ProducesFieldBean bean
      = new ProducesFieldBean(manager, producer, beanField,
                              disposesMethod, disposesArgs);
    bean.introspect();
    bean.introspect(beanField);
    
    BaseType type = manager.createSourceBaseType(beanField.getBaseType());

    if (type.isGeneric()) {
      // ioc/07f1
      throw new InjectionException(L.l("'{0}' is an invalid @Produces field because it returns a generic type {1}",
                                       beanField.getJavaMember(),
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
  
  public Bean<X> getProducerBean()
  {
    return _producerBean;
  }

  @Override
  public Class<?> getBeanClass()
  {
    return _producerBean.getBeanClass();
  }

  public AnnotatedField<X> getField()
  {
    return _beanField;
  }

  @Override
  protected String getDefaultName()
  {
    return _beanField.getJavaMember().getName();
  }
  
  @Override
  public void introspect()
  {
    super.introspect();
   
    _isPassivating = getBeanManager().isPassivatingScope(getScope());
  }

  @Override
  public T create(CreationalContext<T> createEnv)
  {
    return _producer.produce(createEnv);
  }

  @Override
  public void destroy(T instance, CreationalContext<T> cxt)
  {
    if (_producer == _fieldProducer)
      _fieldProducer.destroy(instance, (CreationalContextImpl<T>) cxt);
    else
      _producer.dispose(instance);
    
    if (cxt instanceof CreationalContextImpl<?>) {
      CreationalContextImpl<?> env = (CreationalContextImpl<?>) cxt;
      
      env.clearTarget();
    }
    
    cxt.release();
  }

  @Override
  public void bind()
  {
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");

    Field field = _beanField.getJavaMember();

    sb.append(getTargetSimpleName());
    sb.append(", ");
    sb.append(field.getDeclaringClass().getSimpleName());
    sb.append(".");
    sb.append(field.getName());
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

  class FieldProducer implements Producer<T> {
    /**
     * Produces a new bean instance
     */
    @Override
    public T produce(CreationalContext<T> cxt)
    {
      Class<?> type = _producerBean.getBeanClass();
    
      ProducesCreationalContext<X> producerCxt;
      
      if (cxt instanceof CreationalContextImpl<?>) {
        CreationalContextImpl<?> parentCxt = (CreationalContextImpl<?>) cxt;
      
        producerCxt = new ProducesCreationalContext<X>(_producerBean, parentCxt);
      }
      else
        producerCxt = new ProducesCreationalContext<X>(_producerBean, null);

      X factory;
      
      if (_isStatic) {
        factory = null;
      }
      else {
        factory = (X) getBeanManager().getReference(_producerBean, type, producerCxt);
      
        if (factory == null) {
          throw new IllegalStateException(L.l("{0}: unexpected null factory for {1}",
                                              this, _producerBean));
        }
      }
      
      CreationalContextImpl<T> env = (CreationalContextImpl<T>) cxt;

      T instance = produce(factory, env.findInjectionPoint());
      
      if (_producerBean.getScope() == Dependent.class)
        _producerBean.destroy(factory, producerCxt);
      
      if (_isPassivating && ! (instance instanceof Serializable))
        throw new IllegalProductException(L.l("'{0}' is an invalid @{1} instance because it's not serializable for bean {2}",
                                              instance, getScope().getSimpleName(), this));
      
      
      return instance;
    }

    /**
     * Produces a new bean instance
     */
    private T produce(X bean, InjectionPoint ij)
    {
      try {
        Field field = _beanField.getJavaMember();
        field.setAccessible(true);
      
        T value = (T) _beanField.getJavaMember().get(bean);
      
        if (value != null)
          return value;
      
        if (! Dependent.class.equals(getScope()))
          throw new IllegalProductException(L.l("'{0}' is an invalid producer because it returns null",
                                              bean));

        return value;
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    
    public void destroy(T instance, CreationalContextImpl<T> cxt)
    {
      if (_disposesProducer != null)
        _disposesProducer.destroy(instance, cxt);
    }

    @Override
    public void dispose(T instance)
    {
      if (_disposesProducer != null)
        _disposesProducer.dispose(instance);
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints()
    {
      return ProducesFieldBean.this.getInjectionPoints();
    }

    @Override
    public String toString()
    {
      Field javaField = _beanField.getJavaMember();
      
      return (getClass().getSimpleName()
          + "[" + javaField.getDeclaringClass().getSimpleName()
          + "." + javaField.getName() + "]");
    }
  }
}
