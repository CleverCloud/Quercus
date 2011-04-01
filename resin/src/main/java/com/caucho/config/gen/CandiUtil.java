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

package com.caucho.config.gen;

import com.caucho.config.ConfigException;
import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.inject.DecoratorBean;
import com.caucho.config.inject.DelegateProxyBean;
import com.caucho.config.inject.DependentCreationalContext;
import com.caucho.config.inject.InterceptorBean;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.InterceptorRuntimeBean;
import com.caucho.config.inject.OwnerCreationalContext;
import com.caucho.config.reflect.AnnotatedTypeUtil;
import com.caucho.util.L10N;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.InterceptionType;

/**
 * Utilities
 */
public class CandiUtil {
  private static final L10N L = new L10N(CandiUtil.class);
  private static final Logger log = Logger.getLogger(CandiUtil.class.getName());

  public static final Object []NULL_OBJECT_ARRAY = new Object[0];

  private CandiUtil()
  {
  }
  
  public static Object invoke(Method method, Object bean, Object ...args)
  {
    try {
      return method.invoke(bean, args);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException)
        throw (RuntimeException) e.getCause();
      else
        throw new RuntimeException(method.getName() + ": " + e, e.getCause());
    } catch (Exception e) {
      throw new RuntimeException(method.getName() + ": " + e, e);
    }
  }

  public static int []createInterceptors(InjectManager manager,
                                         ArrayList<InterceptorRuntimeBean<?>> staticBeans,
                                         ArrayList<Interceptor<?>> beans,
                                         int []staticIndexList,
                                         InterceptionType type,
                                         Annotation ...bindings)
  {
    ArrayList<Integer> indexList = new ArrayList<Integer>();
    
    List<Interceptor<?>> interceptors;

    if (bindings != null && bindings.length > 0) {
      interceptors = manager.resolveInterceptors(type, bindings);
    }
    else
      interceptors = new ArrayList<Interceptor<?>>();

    if (staticIndexList != null) {
      for (int i = 0; i < staticIndexList.length; i++) {
        int staticIndex = staticIndexList[i];
        InterceptorRuntimeBean<?> staticBean = staticBeans.get(staticIndex);
      
        addStaticBean(staticBean, indexList, beans, type);
      }
    }

    for (int i = 0; i < interceptors.size(); i++) {
      Interceptor<?> interceptor = interceptors.get(i);

      int index = beans.indexOf(interceptor);

      if (index >= 0)
        indexList.add(index);
      else {
        indexList.add(beans.size());
        beans.add(interceptor);
      }
    }

    int []indexArray = new int[indexList.size()];
    for (int i = 0; i < indexList.size(); i++) {
      indexArray[i] = indexList.get(i);
    }
    
    return indexArray;
  }
  
  private static void 
  addStaticBean(InterceptorRuntimeBean<?> staticBean,
                ArrayList<Integer> indexList,
                ArrayList<Interceptor<?>> beans,
                InterceptionType type)
  {
    if (staticBean == null)
      return;
    
    if (! beans.contains(staticBean))
      beans.add(staticBean);
    
    addStaticBean(staticBean.getParent(), indexList, beans, type);

    if (staticBean.intercepts(type)) {
      int index = beans.indexOf(staticBean);
      indexList.add(index);
    }
  }

  public static void createInterceptors(InjectManager manager,
                                        ArrayList<Interceptor<?>> beans,
                                        Annotation ...bindings)
  {
    if (bindings == null || bindings.length == 0)
      return;
    
    createInterceptors(manager, beans, InterceptionType.AROUND_INVOKE, bindings);
    createInterceptors(manager, beans, InterceptionType.POST_CONSTRUCT, bindings);
    createInterceptors(manager, beans, InterceptionType.PRE_DESTROY, bindings);
  }
  
  public static void createInterceptors(InjectManager manager,
                                        ArrayList<Interceptor<?>> beans,
                                        InterceptionType type,
                                        Annotation ...bindings)
  {
    List<Interceptor<?>> interceptors;

    interceptors = manager.resolveInterceptors(type, bindings);

    for (Interceptor<?> bean : interceptors) {
       int index = beans.indexOf(bean);

      if (index < 0)
        beans.add(bean);
    }
  }

  public static void validatePassivating(Class<?> cl, 
                                         ArrayList<Interceptor<?>> beans)
  {
    for (Interceptor<?> interceptor : beans) {
      validatePassivating(cl, interceptor, "interceptor");
    }
  }

  public static void validatePassivatingDecorators(Class<?> cl, 
                                                   List<Decorator<?>> beans)
  {
    for (Decorator<?> decorator : beans) {
      validatePassivating(cl, decorator, "decorator");
    }
  }
  
  public static void validatePassivating(Class<?> cl,
                                         Bean<?> bean,
                                         String typeName)
  {
    Class<?> beanClass = bean.getBeanClass();

    if (! Serializable.class.isAssignableFrom(beanClass)
        && false) {
      ConfigException exn
      = new ConfigException(L.l("{0}: {1} is an invalid {2} because it is not serializable.",
                                cl.getName(),
                                bean,
                                typeName));

      throw exn;
      // InjectManager.create().addDefinitionError(exn);
    }

    for (InjectionPoint ip : bean.getInjectionPoints()) {
      if (ip.isTransient() || ip.isDelegate())
        continue;
      
      Class<?> type = getRawClass(ip.getType());
      
      if (type.isInterface())
        continue;

      if (! Serializable.class.isAssignableFrom(type)) {
        ConfigException exn
          = new ConfigException(L.l("{0}: {1} is an invalid {4} because its injection point '{2}' of type {3} is not serializable.",
                                    cl.getName(),
                                    bean,
                                    ip.getMember().getName(),
                                    ip.getType(),
                                    typeName));

        throw exn;
      }
    }
  }
  
  public static Class<?> getRawClass(Type type)
  {
    if (type instanceof Class<?>)
      return (Class<?>) type;
    else if (type instanceof ParameterizedType)
      return (Class<?>) ((ParameterizedType) type).getRawType();
    else
      return Object.class;
  }

  public static Interceptor<?> []createMethods(ArrayList<Interceptor<?>> beans,
                                               InterceptionType type,
                                               int []indexChain)
  {
    Interceptor<?> []methods = new Interceptor<?>[indexChain.length];

    for (int i = 0; i < indexChain.length; i++) {
      int index = indexChain[i];

      methods[i] = beans.get(index);
    }

    return methods;
  }

  public static Method []createDecoratorMethods(List<Decorator<?>> decorators,
                                                String methodName,
                                                Class<?> ...paramTypes)
  {
    Method []methods = new Method[decorators.size()];
    
    for (int i = 0; i < decorators.size(); i++) {
      Decorator<?> decorator = decorators.get(i);
      Class<?> beanClass = decorator.getBeanClass();
      
      try {
        methods[decorators.size() - i - 1] = beanClass.getMethod(methodName, paramTypes);
        methods[decorators.size() - i - 1].setAccessible(true);
      } catch (Exception e) {
        log.log(Level.FINEST, e.toString(), e);
      }
    }

    return methods;
  }

  public static Method getMethod(Class<?> cl,
                                 String methodName,
                                 Class<?> ...paramTypes)
    throws Exception
  {
    Method method = null;
    Exception firstException = null;

    do {
      try {
        method = cl.getDeclaredMethod(methodName, paramTypes);
      } catch (Exception e) {
        if (firstException == null)
          firstException = e;

        cl = cl.getSuperclass();
      }
    } while (method == null && cl != null);

    if (method == null)
      throw firstException;

    method.setAccessible(true);

    return method;
  }

  public static Method findMethod(Class<?> cl,
                                  String methodName,
                                  Class<?> ...paramTypes)
  {
    for (Class<?> ptr = cl; ptr != null; ptr = ptr.getSuperclass()) {
      for (Method method : ptr.getDeclaredMethods()) {
        if (AnnotatedTypeUtil.isMatch(method, methodName, paramTypes)) {
          return method;
        }
      }
    }

    log.warning(L.l("'{0}' is an unknown method in {1}",
                    methodName, cl.getName()));
    
    return null;
  }

  public static Object []generateProxyDelegate(InjectManager manager,
                                               List<Decorator<?>> beans,
                                               Object delegateProxy,
                                               CreationalContextImpl<?> parentEnv)
  {
    Object []instances = new Object[beans.size()];

    DependentCreationalContext<Object> proxyEnv
      = new DependentCreationalContext<Object>(DelegateProxyBean.BEAN, parentEnv, null);
    
    proxyEnv.push(delegateProxy);
    
    for (int i = 0; i < beans.size(); i++) {
      Decorator<?> bean = beans.get(i);
      
      CreationalContextImpl<?> env = new DependentCreationalContext(bean, proxyEnv, null);
      
      Object instance = manager.getReference(bean, bean.getBeanClass(), env);
      
      // XXX:
      InjectionPoint ip = getDelegate(bean);

      if (ip.getMember() instanceof Field) {
        Field field = (Field) ip.getMember();
        field.setAccessible(true);
      
        try {
          field.set(instance, delegateProxy);
        } catch (Exception e) {
          throw new InjectionException(e);
        }
      } else if (ip.getMember() instanceof Method) {
        Method method = (Method) ip.getMember();
        method.setAccessible(true);
      
        try {
          method.invoke(instance, delegateProxy);
        } catch (Exception e) {
          throw new InjectionException(e);
        }
      }
      
      /*
      DecoratorBean<?> decoratorBean = (DecoratorBean<?>) bean;
      decoratorBean.setDelegate(instance, proxy);
      */

      instances[beans.size() - i - 1] = instance;
      
      if (parentEnv instanceof CreationalContextImpl<?>) {
        // InjectionPoint ip = decoratorBean.getDelegateInjectionPoint();
      
        ((CreationalContextImpl<?>) parentEnv).setInjectionPoint(ip);
      }
    }

    return instances;
  }
  
  private static InjectionPoint getDelegate(Decorator<?> bean)
  {
    if (bean instanceof DecoratorBean)
      return ((DecoratorBean) bean).getDelegateInjectionPoint();

    for (InjectionPoint ip : bean.getInjectionPoints()) {
      if (ip.isDelegate())
        return ip;
    }
    
    throw new IllegalStateException(String.valueOf(bean));
  }

  public static int nextDelegate(Object []beans,
                                 Method []methods,
                                 int index)
  {
    for (index--; index >= 0; index--) {
      if (methods[index] != null) {
        return index;
      }
    }

    return index;
  }

  public static int nextDelegate(Object []beans,
                                 Class<?> []apis,
                                 int index)
  {
    for (index--; index >= 0; index--) {
      for (Class<?> api : apis) {
        if (api.isAssignableFrom(beans[index].getClass()))
          return index;
      }
    }

    return index;
  }
}
