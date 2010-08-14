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

package com.caucho.ejb.session;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.SessionBeanType;
import javax.inject.Named;

import com.caucho.config.ConfigException;
import com.caucho.config.gen.BeanGenerator;
import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.j2ee.BeanName;
import com.caucho.config.j2ee.BeanNameLiteral;
import com.caucho.config.reflect.AnnotatedElementImpl;
import com.caucho.config.reflect.BaseType;
import com.caucho.ejb.cfg.EjbLazyGenerator;
import com.caucho.ejb.inject.ProcessSessionBeanImpl;
import com.caucho.ejb.inject.SessionRegistrationBean;
import com.caucho.ejb.manager.EjbManager;
import com.caucho.ejb.server.AbstractEjbBeanManager;
import com.caucho.java.gen.JavaClassGenerator;

/**
 * Server container for a session bean.
 */
public class ExtAnnotatedType<X> extends AnnotatedElementImpl implements AnnotatedType<X> {
  private Class<X> _cl;
  
  private Set<AnnotatedField<? super X>> _fields
    = new LinkedHashSet<AnnotatedField<? super X>>();
  
  private Set<AnnotatedMethod<? super X>> _methods
    = new LinkedHashSet<AnnotatedMethod<? super X>>();
  
  public ExtAnnotatedType(AnnotatedType<X> baseType)
  {
    super(baseType);
    
    _cl = baseType.getJavaClass();
  }

  @Override
  public Class<X> getJavaClass()
  {
    return _cl;
  }

  @Override
  public Set<AnnotatedConstructor<X>> getConstructors()
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }
  
  void addField(AnnotatedField<? super X> field)
  {
    _fields.add(field);
  }

  @Override
  public Set<AnnotatedField<? super X>> getFields()
  {
    return _fields;
  }
  
  void addMethod(AnnotatedMethod<? super X> method)
  {
    _methods.add(method);
  }

  @Override
  public Set<AnnotatedMethod<? super X>> getMethods()
  {
    return _methods;
  }
}
