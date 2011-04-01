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
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

import com.caucho.inject.Module;

/**
 * NewBean represents the SimpleBean created through the @New interface.
 *
 * <ul>
 * <li>initializer methods and injected fields are defined by annotations
 * <li>interceptor bindings are defined by annotations
 *
 */
@Module
public class NewBean<X> extends AbstractIntrospectedBean<X>
{
  private InjectionTargetBuilder<X> _target;
  private LinkedHashSet<Annotation> _qualifiers;
  private Set<Type> _types;

  NewBean(InjectManager inject,
          Class<?> newType,
          AnnotatedType<X> beanType)
  {
    super(inject, beanType.getBaseType(), beanType);

    _target = new InjectionTargetBuilder<X>(inject, beanType, this);
    
    // validation
    _target.getInjectionPoints();
    
    _types = inject.createSourceBaseType(newType).getTypeClosure(inject);

    _qualifiers = new LinkedHashSet<Annotation>();
    _qualifiers.add(new NewLiteral(beanType.getJavaClass()));
    //addBinding(NewLiteral.NEW);
    //setScopeType(Dependent.class);

    //init();
  }

  @Override
  public void introspect()
  {
  }
  
  /**
   * The @New name is null.
   */
  @Override
  public String getName()
  {
    return null;
  }

  /**
   * The scope for @New is dependent.
   */
  @Override
  public Class<? extends Annotation> getScope()
  {
    return Dependent.class;
  }
  
  /**
   * Returns thetype closure.
   */
  @Override
  public Set<Type> getTypes()
  {
    return _types;
  }
  
  /**
   * The qualifiers are @New
   */
  @Override
  public Set<Annotation> getQualifiers()
  {
    return _qualifiers;
  }

  //
  // introspection overrides
  //

  /**
   * Returns the injection points. 
   */
  @Override
  public Set<InjectionPoint> getInjectionPoints()
  {
    return _target.getInjectionPoints();
  }
  /**
   * Creates a new instance of the component.
   */
  @Override
  public X create(CreationalContext<X> env)
  {
    InjectionTarget<X> target = _target;

    X value = target.produce(env);
    target.inject(value, env);
    target.postConstruct(value);

    return value;
  }
  
  @Override
  public void destroy(X instance, CreationalContext<X> env)
  {
    env.release();
  }
}
