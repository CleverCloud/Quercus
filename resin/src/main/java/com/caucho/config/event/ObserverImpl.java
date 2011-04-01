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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Set;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.event.Reception;
import javax.enterprise.inject.spi.Bean;

import com.caucho.config.ConfigException;
import com.caucho.config.LineConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.util.L10N;

/**
 * Implements a single observer.
 */
public class ObserverImpl<X,T> extends AbstractObserverMethod<T> {
  private static final L10N L = new L10N(ObserverImpl.class);

  private static final Object []NULL_ARGS = new Object[0];

  private final InjectManager _inject;
  private final Bean<X> _bean;

  private final Method _method;
  private final int _paramIndex;

  private boolean _hasBinding;
  private boolean _ifExists;

  private Bean<?> []_args;

  public ObserverImpl(InjectManager webBeans,
                      Bean<X> bean,
                      Method method,
                      int paramIndex)
  {
    _inject = webBeans;
    _bean = bean;
    _method = method;
    _method.setAccessible(true);
    _paramIndex = paramIndex;

    for (Annotation ann : method.getParameterAnnotations()[paramIndex]) {
      if (ann instanceof Observes) {
        Observes observes = (Observes) ann;
        _ifExists = observes.notifyObserver() == Reception.IF_EXISTS;
      }
    }

    bind();
  }

  public Class<?> getType()
  {
    return _method.getParameterTypes()[_paramIndex];
  }

  /**
   * Initialization.
   */
  public void init()
  {
    // _webbeans.addWbComponent(this);

    /*
    if (_name == null) {
      Named named = (Named) _cl.getAnnotation(Named.class);

      if (named != null)
        _name = named.value();

      if (_name == null || "".equals(_name)) {
        String className = _targetType.getName();
        int p = className.lastIndexOf('.');

        char ch = Character.toLowerCase(className.charAt(p + 1));

        _name = ch + className.substring(p + 2);
      }
    }
    */
  }

  public void bind()
  {
    synchronized (this) {
      if (_args != null)
        return;

      Type []param = _method.getGenericParameterTypes();
      Annotation [][]annList = _method.getParameterAnnotations();

      _args = new Bean[param.length];

      String loc = LineConfigException.loc(_method);

      for (int i = 0; i < param.length; i++) {
        if (hasObserves(annList[i]))
          continue;

        Set<Bean<?>> beans = _inject.getBeans(param[i], annList[i]);

        if (beans == null || beans.size() == 0) {
          throw new ConfigException(loc
                                    + L.l("Parameter '{0}' binding does not have a matching component",
                                          getSimpleName(param[i])));
        }

        Bean<?> comp = null;

        // XXX: error checking
        Iterator<Bean<?>> iter = beans.iterator();
        if (iter.hasNext()) {
          comp = iter.next();
        }

        _args[i] = comp;
      }
    }
  }

  private boolean hasObserves(Annotation []annList)
  {
    for (Annotation ann : annList) {
      if (ann instanceof Observes)
        return true;
    }

    return false;
  }

  public void notify(Object event)
  {
    Object obj = null;

    if (_ifExists) {
      Context context = _inject.getContext(_bean.getScope());

      if (context != null && context.isActive())
        obj = context.get(_bean);
    }
    else {
      // XXX: perf
      CreationalContext<X> env = _inject.createCreationalContext(_bean);

      obj = _inject.getReference(_bean, _bean.getBeanClass(), env);
    }

    try {
      if (obj != null) {
        Object []args = new Object[_args.length];

        for (int i = 0; i < _args.length; i++) {
          Bean bean = _args[i];

          if (bean != null) {
            CreationalContext env = _inject.createCreationalContext(bean);

            args[i] = _inject.getReference(bean, bean.getBeanClass(), env);
          }
          else
            args[i] = event;
        }

        _method.invoke(obj, args);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getCause());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    else if (! (obj instanceof ObserverImpl<?,?>))
      return false;

    ObserverImpl<?,?> comp = (ObserverImpl<?,?>) obj;

    if (! _bean.equals(comp._bean)) {
      return false;
    }

    return true;
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");
    sb.append(_method.getDeclaringClass().getSimpleName());
    sb.append(".");
    sb.append(_method.getName());
    sb.append("[");

    sb.append(_method.getParameterTypes()[_paramIndex].getSimpleName());
    sb.append("]");
    sb.append("]");

    return sb.toString();
  }

  protected static String getSimpleName(Type type)
  {
    if (type instanceof Class<?>)
      return ((Class<?>) type).getSimpleName();
    else
      return String.valueOf(type);
  }
}
