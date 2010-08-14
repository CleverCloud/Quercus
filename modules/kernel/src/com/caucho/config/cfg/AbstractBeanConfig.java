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

package com.caucho.config.cfg;

import com.caucho.config.*;
import com.caucho.config.inject.AbstractBean;
import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.reflect.AnnotatedTypeImpl;
import com.caucho.config.reflect.ReflectionAnnotatedFactory;
import com.caucho.config.types.*;
import com.caucho.naming.*;
import com.caucho.util.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.ArrayList;

import javax.annotation.*;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import javax.naming.*;

/**
 * Backwards compatibility class for 3.1-style &lt;jms-queue>, etc.
 */
abstract public class AbstractBeanConfig {
  private static final L10N L = new L10N(AbstractBeanConfig.class);

  private String _filename;
  private int _line;

  private String _name;
  private String _jndiName;

  private Class<?> _cl;

  private ArrayList<Annotation> _annotations
    = new ArrayList<Annotation>();

  private ArrayList<Annotation> _qualifiers
    = new ArrayList<Annotation>();

  private ArrayList<Annotation> _stereotypes
    = new ArrayList<Annotation>();

  private Class<? extends Annotation> _scope;

  private ContainerProgram _init;

  protected AbstractBeanConfig()
  {
  }

  /**
   * Sets the configuration location
   */
  public void setConfigLocation(String filename, int line)
  {
    _filename = filename;
    _line = line;
  }

  public String getFilename()
  {
    return _filename;
  }

  public int getLine()
  {
    return _line;
  }

  /**
   * Returns the component's EL binding name.
   */
  public void setName(String name)
  {
    _name = name;

    // _bindingList.add(Names.create(name));
  }

  /**
   * Gets the component's EL binding name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the component's EL binding name.
   */
  public void setJndiName(String name)
  {
    _jndiName = name;
  }

  /**
   * Gets the component's EL binding name.
   */
  public String getJndiName()
  {
    return _jndiName;
  }

  /**
   * Assigns the class
   */
  public void setClass(Class<?> cl)
  {
    _cl = cl;
  }

  /**
   * Returns the instance class
   */
  public Class<?> getInstanceClass()
  {
    return _cl;
  }

  /**
   * Adds a component binding.
   */
  public void addBinding(Annotation binding)
  {
    _annotations.add(binding);
  }

  /**
   * Adds a component binding.
   */
  public void add(Annotation binding)
  {
    _annotations.add(binding);

    if (binding.annotationType().isAnnotationPresent(Qualifier.class))
      _qualifiers.add(binding);
  }

  /**
   * Sets the scope attribute.
   */
  public void setScope(String scope)
  {
    if ("singleton".equals(scope))
      add(new AnnotationLiteral<Singleton>() {});
    else if ("dependent".equals(scope))
      add(new AnnotationLiteral<Dependent>() {});
    else if ("request".equals(scope))
      add(new AnnotationLiteral<RequestScoped>() {});
    else if ("session".equals(scope))
      add(new AnnotationLiteral<SessionScoped>() {});
    else if ("application".equals(scope))
      add(new AnnotationLiteral<ApplicationScoped>() {});
    else if ("conversation".equals(scope))
      add(new AnnotationLiteral<ConversationScoped>() {});
    else {
      throw new ConfigException(L.l("'{0}' is an invalid scope.  The scope must be a valid @Scope annotation."));
    }
  }

  /**
   * Sets the init program.
   */
  public void setInit(ContainerProgram init)
  {
    _init = init;
  }

  /**
   * Sets the init program.
   */
  public ContainerProgram getInit()
  {
    return _init;
  }

  protected void initImpl()
  {
  }

  @PostConstruct
  public final void init()
  {
    initImpl();

    if (_cl == null) {
      throw new ConfigException(L.l("{0} requires a 'class' attribute",
                                    getClass().getSimpleName()));
    }
  }
  
  protected <X> void deploy()
  {
    InjectManager beanManager = InjectManager.create();

    AnnotatedTypeImpl<X> beanType = buildAnnotatedType();

    BeanBuilder<X> builder = beanManager.createBeanFactory(beanType);

    if (_scope != null)
      builder.scope(_scope);

    if (_init != null)
      builder.init(_init);
    
    for (Annotation qualifier : _qualifiers)
      builder.qualifier(qualifier);

    Object value = replaceObject();
    Bean<X> bean = null;

    if (value != null) {
      bean = builder.singleton(value);
      beanManager.addBean(bean);
    }
    else {
      bean = builder.bean();
      beanManager.addBean(bean);
    }


    // XXXX: JNDI isn't right
    if (_jndiName != null) {
      try {
        Jndi.bindDeepShort(_jndiName, bean);
      } catch (NamingException e) {
        throw ConfigException.create(e);
      }
    }
  }
  
  protected <X> AnnotatedTypeImpl<X> buildAnnotatedType()
  {
    InjectManager beanManager = InjectManager.create();

    AnnotatedType<X> annType = (AnnotatedType<X>) ReflectionAnnotatedFactory.introspectType(_cl);
    AnnotatedTypeImpl<X> beanType;
    
    beanType = new AnnotatedTypeImpl<X>(annType);

    if (_name != null) {
      beanType.addAnnotation(Names.create(_name));
    }

    for (Annotation binding : _qualifiers) {
      beanType.addAnnotation(binding);
    }

    for (Annotation stereotype : _stereotypes) {
      beanType.addAnnotation(stereotype);
    }

    for (Annotation ann : _annotations) {
      beanType.addAnnotation(ann);
    }
    
    return beanType;
  }

  protected Object replaceObject()
  {
    return null;
  }
}
