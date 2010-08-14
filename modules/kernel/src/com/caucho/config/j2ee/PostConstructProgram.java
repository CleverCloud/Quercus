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
 * @author Scott Ferguson;
 */

package com.caucho.config.j2ee;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Qualifier;

import com.caucho.config.ConfigException;
import com.caucho.config.inject.AbstractInjectionPoint;
import com.caucho.config.inject.CurrentLiteral;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ConfigProgram;
import com.caucho.inject.Module;

@Module
public class PostConstructProgram extends ConfigProgram
{
  private Method _init;
  private ParamProgram []_program;

  public PostConstructProgram(Method init)
  {
    _init = init;
    _init.setAccessible(true);

    introspect();
  }
  
  @Override
  public Class<?> getDeclaringClass()
  {
    return _init.getDeclaringClass();
  }
  
  @Override
  public String getName()
  {
    return _init.getName();
  }

  protected void introspect()
  {
    // XXX: type
    Type []paramTypes = _init.getGenericParameterTypes();

    if (paramTypes.length == 0)
      return;

    _program = new ParamProgram[paramTypes.length];
    
    Annotation [][]paramAnns = _init.getParameterAnnotations();

    InjectManager webBeans = InjectManager.create();
    
    for (int i = 0; i < paramTypes.length; i++) {
      Annotation []bindings = createBindings(paramAnns[i]);
      
      _program[i] = new ParamProgram(webBeans, paramTypes[i],
                                     bindings, paramAnns[i]);
    }
  }

  Annotation []createBindings(Annotation []annotations)
  {
    ArrayList<Annotation> bindingList = new ArrayList<Annotation>();

    for (Annotation ann : annotations) {
      if (ann.annotationType().isAnnotationPresent(Qualifier.class))
        bindingList.add(ann);
    }

    if (bindingList.size() == 0)
      return null;

    Annotation []bindings = new Annotation[bindingList.size()];
    bindingList.toArray(bindings);

    return bindings;
  }

  @Override
  public <T> void inject(T bean, CreationalContext<T> env)
    throws ConfigException
  {
    try {
      if (_program != null) {
        Object []args = new Object[_program.length];

        for (int i = 0; i < args.length; i++) {
          args[i] = _program[i].eval(env);
        }

        _init.invoke(bean, args);
      }
      else
        _init.invoke(bean);
    } catch (Exception e) {
      throw ConfigException.create(_init, e);
    }
  }

  public int hashCode()
  {
    return _init.getName().hashCode();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof PostConstructProgram))
      return false;

    PostConstructProgram program = (PostConstructProgram) o;
    Method init = program._init;

    if (! _init.getName().equals(init.getName()))
      return false;
    
    if (! init.getDeclaringClass().equals(_init.getDeclaringClass()))
      return false;

    Class<?> []aParam = _init.getParameterTypes();
    Class<?> []bParam = init.getParameterTypes();

    if (aParam.length != bParam.length)
      return false;

    for (int i = 0; i < aParam.length; i++) {
      if (! aParam[i].equals(bParam[i]))
        return false;
    }

    return true;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _init + "]";
  }

  private static class ParamProgram {
    private final InjectManager _inject;
    private final AbstractInjectionPoint _injectionPoint;

    ParamProgram(InjectManager inject,
                 Type type,
                 Annotation []bindings,
                 Annotation []annList)
    {
      _inject = inject;
      Bean<?> bean = null;
      Member member = null;
      HashSet<Annotation> bindingSet = new HashSet<Annotation>();

      if (bindings != null) {
        for (Annotation ann :  bindings)
          bindingSet.add(ann);
      }
      else
        bindingSet.add(CurrentLiteral.CURRENT);

      _injectionPoint = new AbstractInjectionPoint(inject,
                                                   bean, member, type,
                                                   bindingSet, annList);
    }

    public Object eval(CreationalContext<?> env)
    {
      return _inject.getInjectableReference(_injectionPoint, env);
    }
  }
}
