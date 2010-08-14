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

package com.caucho.ejb.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.ejb.Timer;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.ConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.inject.ScheduleBean;
import com.caucho.config.timer.ScheduleIntrospector;
import com.caucho.config.timer.TimeoutCaller;
import com.caucho.config.timer.TimerTask;
import com.caucho.ejb.gen.SessionGenerator;
import com.caucho.ejb.session.StatelessContext;
import com.caucho.ejb.session.StatelessManager;
import com.caucho.inject.Module;
import com.caucho.util.L10N;

/**
 * Internal implementation for a Bean
 */
@Module
public class StatelessBeanImpl<X,T> extends SessionBeanImpl<X,T>
                                    implements ScheduleBean
{
  private static final L10N L = new L10N(StatelessBeanImpl.class);
  
  private LinkedHashSet<Annotation> _qualifiers
    = new LinkedHashSet<Annotation>();
  
  private final StatelessManager<X> _manager;

  public StatelessBeanImpl(StatelessManager<X> manager,
                           ManagedBeanImpl<X> bean,
                           Class<T> api,
                           Set<Type> types,
                           StatelessContext<X,T> context,
                           AnnotatedType<X> extAnnType)
  {
    super(context, bean, types, extAnnType);

    _manager = manager;
    _qualifiers.addAll(bean.getQualifiers());
    
    Class<?> scopeType = bean.getScope();
    
    if (scopeType != null
        && ! scopeType.equals(Dependent.class)) {
      throw new ConfigException(L.l("@{0} is an invalid scope for @Stateless session bean {1} because stateless session beans need @Dependent scope",
                                    scopeType.getName(), getBeanClass().getName()));
    }
  }
 
  @Override
  public Set<Annotation> getQualifiers()
  {
    return _qualifiers;
  }

  @Override
  public void scheduleTimers(Object value)
  {
    ScheduleIntrospector introspector = new StatelessScheduleIntrospector();

    TimeoutCaller timeoutCaller = new StatelessTimeoutCaller(value);

    ArrayList<TimerTask> taskList
      = introspector.introspect(timeoutCaller, getBean().getAnnotatedType());

    if (taskList != null) {
      for (TimerTask task : taskList) {
        task.start();
      }
    }
  }
  
  private class StatelessScheduleIntrospector extends ScheduleIntrospector
  {
    @Override
    protected Method getScheduledMethod(AnnotatedMethod<?> method)
    {
      Method javaMethod = method.getJavaMember();
      String methodName = javaMethod.getName();

      if (! SessionGenerator.isBusinessMethod(javaMethod))
        methodName = "__caucho_schedule_" + javaMethod.getName();

      Class<?> proxyClass = _manager.getProxyImplClass();
      
      try {
        Method scheduleMethod 
          = proxyClass.getMethod(methodName, javaMethod.getParameterTypes());

        return scheduleMethod;
      } catch (Exception e) {
        throw new ConfigException(L.l("Cannot find method {0} on generated class {1}.",
                                      methodName, proxyClass.getName()),
                                  e);
      }
    }
  }
  
  private class StatelessTimeoutCaller implements TimeoutCaller {
    private final Object _bean;
    
    /**
     * @param bean
     */
    public StatelessTimeoutCaller(Object bean)
    {
      _bean = bean;
    }

    public void timeout(Method method, Timer timer)
      throws InvocationTargetException, IllegalAccessException
    {
      method.invoke(_bean, timer);
    }

    public void timeout(Method method)
      throws InvocationTargetException, IllegalAccessException
    {
      method.invoke(_bean);
    }
  }
}
