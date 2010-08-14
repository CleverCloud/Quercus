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

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.MessageDriven;
import javax.ejb.Startup;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Scope;
import javax.inject.Singleton;

import com.caucho.config.ConfigException;
import com.caucho.config.Names;
import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.DefaultLiteral;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.program.PropertyStringProgram;
import com.caucho.config.program.PropertyValueProgram;
import com.caucho.config.type.TypeFactory;
import com.caucho.config.xml.XmlBeanConfig;
import com.caucho.inject.Module;
import com.caucho.naming.Jndi;
import com.caucho.util.L10N;

/**
 * Backward-compat configuration for the xml web bean component.
 */
@Module
public class BeanConfig {
  private static final L10N L = new L10N(BeanConfig.class);

  private String _filename;
  private int _line;

  private String _uri;

  private String _jndiName;

  private String _mbeanName;
  private Class<?> _beanConfigClass;

  private XmlBeanConfig _customBean;

  private InjectManager _beanManager;

  private Class<?> _cl;

  private String _name;

  private ArrayList<Annotation> _qualifierList
    = new ArrayList<Annotation>();

  private ArrayList<Annotation> _stereotypeList
    = new ArrayList<Annotation>();

  private Class<?> _scope;

  private ArrayList<ConfigProgram> _newArgs;
  private ContainerProgram _init;

  private AnnotatedType<?> _annotatedType;
  private Annotated _extAnnotated;
  protected Bean<?> _bean;

  // XXX: temp for osgi
  private boolean _isService;

  public BeanConfig()
  {
    _beanManager = InjectManager.create();

    if (getDefaultScope() != null)
      setScope(getDefaultScope());

    setService(isDefaultService());
  }

  public InjectManager getBeanManager()
  {
    return _beanManager;
  }

  protected String getDefaultScope()
  {
    return "singleton";
  }

  protected boolean isDefaultService()
  {
    return true;
  }

  /**
   * Returns the component's EL binding name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the component's EL binding name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * backwards compat
   */
  public void setType(Class<?> cl)
  {
    setClass(cl);
  }

  /**
   * Sets the component implementation class.
   */
  public void setClass(Class<?> cl)
  {
    _cl = cl;

    if (_name == null)
      _name = Introspector.decapitalize(cl.getSimpleName());

    Class<?> type = getBeanConfigClass();

    if (type != null && ! type.isAssignableFrom(cl))
      throw new ConfigException(L.l("'{0}' is not a valid instance of '{1}'",
                                    cl.getName(), type.getName()));
  }

  public Class<?> getClassType()
  {
    if (_customBean != null)
      return _customBean.getClassType();
    else
      return _cl;
  }

  public Bean<?> getComponent()
  {
    return _bean;
  }

  /**
   * Adds a component binding.
   */
  public void addBinding(Annotation binding)
  {
    _qualifierList.add(binding);
  }

  public ArrayList<Annotation> getBindingList()
  {
    return _qualifierList;
  }

  public ArrayList<Annotation> getStereotypeList()
  {
    return _stereotypeList;
  }

  /**
   * Sets the scope attribute.
   */
  public void setScope(String scope)
  {
    if ("singleton".equals(scope)) {
      _scope = javax.inject.Singleton.class;
     //  addAnnotation(new AnnotationLiteral<Startup>() {});
    }
    else if ("dependent".equals(scope))
      _scope = Dependent.class;
    else if ("request".equals(scope))
      _scope = RequestScoped.class;
    else if ("session".equals(scope))
      _scope = SessionScoped.class;
    else if ("application".equals(scope))
      _scope = ApplicationScoped.class;
    else if ("conversation".equals(scope))
      _scope = ConversationScoped.class;
    else {
      Class cl = null;

      try {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        cl = Class.forName(scope, false, loader);
      } catch (ClassNotFoundException e) {
      }

      setScopeType(cl);
    }
  }

  public void setScopeType(Class<?> cl)
  {
    if (cl == null)
      throw new ConfigException(L.l("'{0}' is an invalid scope.  The scope must be a valid @Scope annotation."));

    if (! Annotation.class.isAssignableFrom(cl))
      throw new ConfigException(L.l("'{0}' is an invalid scope.  The scope must be a valid @Scope annotation."));

    if (! cl.isAnnotationPresent(Scope.class)
        && ! cl.isAnnotationPresent(NormalScope.class))
      throw new ConfigException(L.l("'{0}' is an invalid scope.  The scope must be a valid @Scope annotation."));

    _scope = cl;
  }

  /**
   * Sets any new values
   */
  public void addParam(ConfigProgram param)
  {
    if (_newArgs == null)
      _newArgs = new ArrayList<ConfigProgram>();

    _newArgs.add(param);
  }

  /**
   * Sets the init program.
   */
  public void setInit(ContainerProgram init)
  {
    if (_init == null)
      _init = new ContainerProgram();

    _init.addProgram(init);
  }

  public void addInitProgram(ConfigProgram program)
  {
    if (_init == null)
      _init = new ContainerProgram();

    _init.addProgram(program);
  }

  public ContainerProgram getInit()
  {
    return _init;
  }

  /**
   * Adds an init property
   */
  public void addStringProperty(String name, String value)
  {
    if (_init == null)
      _init = new ContainerProgram();

    _init.addProgram(new PropertyStringProgram(name, value));
  }

  /**
   * Adds an init property
   */
  public void addProperty(String name, Object value)
  {
    if (_init == null)
      _init = new ContainerProgram();

    _init.addProgram(new PropertyValueProgram(name, value));
  }

  /**
   * Adds an init property
   */
  public void addOptionalStringProperty(String name, String value)
  {
    if (_init == null)
      _init = new ContainerProgram();

    _init.addProgram(0, new PropertyStringProgram(name, value, true));
  }

  /**
   * Returns the configured component factory.
   */
  private Bean getComponentFactory()
  {
    return _bean;
  }

  // XXX: temp for OSGI
  private boolean isService()
  {
    return _isService;
  }

  public void setService(boolean isService)
  {
    _isService = isService;
  }

  /**
   * Resin Config location
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

  public void setJndiName(String jndiName)
  {
    _jndiName = jndiName;

    if (getName() == null)
      setName(jndiName);
  }

  public void setMbeanName(String mbeanName)
  {
    _mbeanName = mbeanName;
  }

  public String getMBeanName()
  {
    return _mbeanName;
  }

  public void setMbeanClass(Class cl)
  {
    setMbeanInterface(cl);
  }

  public void setMbeanInterface(Class<?> cl)
  {
  }

  public Class<?> getBeanConfigClass()
  {
    return _beanConfigClass;
  }

  public void setBeanConfigClass(Class<?> cl)
  {
    _beanConfigClass = cl;
  }

  /**
   * uri-style configuration like the jms-queue url="memory:"
   */
  public void setUri(String uri)
  {
    Class<?> beanConfigClass = getBeanConfigClass();

    if (beanConfigClass == null) {
      throw new ConfigException(L.l("'{0}' does not support the 'uri' attribute because its bean-config-class is undefined",
                                    getClass().getName()));
    }

    _uri = uri;

    String scheme;
    String properties = "";

    int p = uri.indexOf(':');
    if (p >= 0) {
      scheme = uri.substring(0, p);
      properties = uri.substring(p + 1);
    }
    else
      scheme = uri;

    TypeFactory factory = TypeFactory.create();

    setClass(factory.getDriverClassByUrl(beanConfigClass, uri));

    String []props = properties.split("[;]");

    for (String prop : props) {
      if (prop.length() == 0)
        continue;

      String []values = prop.split("[=]");

      if (values.length != 2)
        throw new ConfigException(L.l("'{0}' is an invalid URI.  Bean URI syntax is 'scheme:prop1=value1;prop2=value2'", uri));

      addStringProperty(values[0], values[1]);
    }
  }

  /**
   * Returns the uri
   */
  public String getUri()
  {
    return _uri;
  }

  public void addCustomBean(XmlBeanConfig<?> customBean)
  {
    _customBean = customBean;
  }

  protected String getTagName()
  {
    return "bean";
  }

  protected boolean isStartup()
  {
    return true;
  }

  @PostConstruct
  public void init()
  {
    if (_customBean != null) {
      // server/1a37
      // _customBean.initComponent();

      return;
    }

    if (_cl == null)
      throw new ConfigException(L.l("<{0}> requires a class attribute",
                                    getTagName()));

    /* XXX:
    if (_cl.isAnnotationPresent(Stateless.class)) {
      StatelessBeanConfig cfg = new StatelessBeanConfig(this);
      cfg.init();
      return;
    }
    else if (_cl.isAnnotationPresent(Stateful.class)) {
      StatefulBeanConfig cfg = new StatefulBeanConfig(this);
      cfg.init();
      return;
    }
    */

    introspect();

    InjectManager beanManager = InjectManager.create();
    BeanBuilder builder =  beanManager.createBeanFactory(_cl);

    if (builder == null)
      return;
    _annotatedType = builder.getAnnotatedType();

    if (_name != null) {
      // server/2n00
      if (! Map.class.isAssignableFrom(_cl))
        addOptionalStringProperty("name", _name);
    }
    
    if (getCdiNamed() != null) {
      // env/02s7
      builder.name(getCdiNamed());
    }

    if (_annotatedType.isAnnotationPresent(javax.ejb.Singleton.class)
        || _annotatedType.isAnnotationPresent(Stateful.class) 
        || _annotatedType.isAnnotationPresent(Stateless.class) 
        || _annotatedType.isAnnotationPresent(MessageDriven.class)) {
      throw new ConfigException(L.l("{0} cannot be configured by <bean> because it has an EJB annotation.  Use CDI syntax instead.",
                                    _annotatedType));
    }
    /*
    if (getMBeanName() != null)
      comp.setMBeanName(getMBeanName());
    */

    // server/21q1
    if (isStartup()
        && ! _annotatedType.isAnnotationPresent(Stateful.class)
        && ! _annotatedType.isAnnotationPresent(Stateless.class)
        && ! _annotatedType.isAnnotationPresent(MessageDriven.class)) {
      builder.annotation(new StartupLiteral());
    }

    for (Annotation qualifier : _qualifierList) {
      builder.qualifier(qualifier);
    }
    
    if (_name != null)
      builder.qualifier(Names.create(_name));
    
    if (_qualifierList.size() == 0)
      builder.qualifier(DefaultLiteral.DEFAULT);

    for (Annotation stereotype : _stereotypeList) {
      builder.stereotype(stereotype.annotationType());
    }

    if (_scope != null) {
      builder.scope(_scope);
      // comp.setScope(_beanManager.getScopeContext(_scope));
    }
    
    if (Singleton.class == _scope) {
      builder.annotation(new StartupLiteral());
    }

    /*
    if (_isService) {
      comp.addAnnotation(new AnnotationLiteral<Service>() {});
    }
    */

    /*
    if (_newArgs != null)
      comp.setNewArgs(_newArgs);
    */

    if (_init != null)
      builder.init(_init);

    _bean = builder.bean();
    _extAnnotated = builder.getExtendedAnnotated();
    
    introspectPostInit();

    deploy();

    try {
      if (_bean == null) {
      }
      else if (_jndiName != null) {
        Jndi.bindDeepShort(_jndiName, _bean);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  
  protected String getCdiNamed()
  {
    return _name;
  }

  /**
   * Introspection after the init has been set and before the @PostConstruct
   * for additional interception
   */
  protected void introspectPostInit()
  {
  }

  protected void deploy()
  {
    if (_bean != null) {
      // ejb/1030
      getBeanManager().addBean(_bean,  _extAnnotated);
    }
  }

  public Object getObject()
  {
    if (_bean != null) {
      CreationalContext<?> env = _beanManager.createCreationalContext(_bean);

      Object value = _beanManager.getReference(_bean, _bean.getBeanClass(), env);

      /*
      if (_init != null)
        _init.inject(value, (ConfigContext) env);
      */

      return value;
    }
    else
      return null;
  }

  public Object createObjectNoInit()
  {
    if (_bean != null) {
      CreationalContext<?> env = _beanManager.createCreationalContext(_bean);
      // XXX:
      return _beanManager.getReference(_bean, (Class<?>) null, env);
      // return _bean.createNoInit();
    }
    else
      return null;
  }

  private void introspect()
  {
    if (_scope == null) {
      for (Annotation ann : _cl.getDeclaredAnnotations()) {
        if (ann.annotationType().isAnnotationPresent(Scope.class)
            || ann.annotationType().isAnnotationPresent(NormalScope.class)) {
          if (_scope != null) {
            throw new ConfigException(L.l("{0}: multiple scope annotations are forbidden ({1} and {2}).",
                                          _cl.getName(),
                                          _scope.getSimpleName(),
                                          ann.annotationType().getSimpleName()));
          }

          _scope = ann.annotationType();
        }
      }
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _cl + "]";
  }
  
  static class StartupLiteral extends AnnotationLiteral<Startup> implements Startup {
    
  }
}
