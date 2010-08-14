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

package com.caucho.jca.cfg;

import com.caucho.config.Config;
import com.caucho.config.CauchoDeployment;
import com.caucho.config.ConfigException;
import com.caucho.config.Names;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.CurrentLiteral;
import com.caucho.config.inject.SingletonBean;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.jca.ra.ResourceManagerImpl;
import com.caucho.jmx.IntrospectionMBean;
import com.caucho.jmx.Jmx;
import com.caucho.loader.ClassLoaderListener;
import com.caucho.loader.CloseListener;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentListener;
import com.caucho.loader.StartListener;
import com.caucho.naming.Jndi;
import com.caucho.transaction.ConnectionPool;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;
import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.NotificationFilter;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;

/**
 * Configuration for the init-param pattern.
 */
public class Resource {
  private static final Logger log
    = Logger.getLogger(Resource.class.getName());
  
  private static L10N L = new L10N(Resource.class);

  private Class<?> _type;

  private String _var;
  private String _name;
  private String _jndiName;
  
  private String _mbeanName;
  
  private Class<?> _mbeanInterface;

  // private ArrayList<BuilderProgram> _args = new ArrayList<BuilderProgram>();
  private ArrayList<Object> _args = new ArrayList<Object>();

  private boolean _isPreInit;
  private boolean _localTransactionOptimization = true;
  private boolean _shareable = true;

  private Object _object;
  private MBeanInfo _mbeanInfo;

  /**
   * Sets the config variable name.
   */
  public void setVar(String var)
  {
    _var = var;
  }

  /**
   * Sets the JNDI name
   */
  public void setJndiName(String name)
  {
    _jndiName = name;
  }

  /**
   * Gets the JNDI name
   */
  public String getJndiName()
  {
    return _jndiName;
  }

  /**
   * Sets the WebBeans name
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the WebBeans name
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the mbean name
   */
  public void setMbeanName(String name)
  {
    _mbeanName = name;
  }

  /**
   * Gets the mbean name
   */
  public String getMbeanName()
  {
    return _mbeanName;
  }

  /**
   * Sets the class
   */
  public void setType(Class<?> resourceClass)
  {
    if (resourceClass.getName().equals("javax.mail.Session"))
      _type = JavaMailConfig.class;
    else
      _type = resourceClass;
  }

  /**
   * Sets the class
   */
  public void setClass(Class<?> resourceClass)
  {
    _type = resourceClass;
  }

  /**
   * Gets the type;
   */
  public Class<?> getType()
  {
    return _type;
  }

  /**
   * Sets the class
   */
  public void setMbeanInterface(Class<?> cl)
  {
    _mbeanInterface = cl;
  }

  /**
   * Adds an argument.
   */
  /*
  public void addArg(BuilderProgram builder)
  {
    _args.add(builder);
  }
  */
  public void addArg(Object arg)
  {
    _args.add(arg);
  }

  /**
   * Sets the local-transaction-optimization flag
   */
  public void setLocalTransactionOptimization(boolean enable)
  {
    _localTransactionOptimization = enable;
  }

  /**
   * Sets the shareable
   */
  public void setShareable(boolean shareable)
  {
    _shareable = shareable;
  }

  /**
   * Adds the init program
   */
  public void addInit(ContainerProgram init)
  {
    preInit();

    init.configure(_object);
  }

  /**
   * Adds the listener program
   */
  public Object createListener()
    throws Exception
  {
    return createMbeanListener();
  }

  /**
   * Adds the listener program
   */
  public Object createMbeanListener()
    throws Exception
  {
    preInit();

    if (_mbeanName != null)
      return new MBeanListener();
    else
      throw new ConfigException(L.l("<listener> needs a <resource> with an mbean-name."));
  }

  ObjectName getObjectName()
    throws Exception
  {
    preInit();

    if (_mbeanName != null)
      return Jmx.getObjectName(_mbeanName);
    else
      return null;
  }

  MBeanInfo getMBeanInfo()
    throws Exception
  {
    preInit();

    return _mbeanInfo;
  }

  /**
   * Initialize the resource.
   */
  private void preInit()
  {
    try {
      if (_isPreInit)
        return;
      _isPreInit = true;

      Object oldObject = null;

      if (_jndiName != null) {
        try {
          String jndiName = Jndi.getFullName(_jndiName);

          Context ic = new InitialContext();
          oldObject = ic.lookup(_jndiName);
        } catch (Exception e) {
        }
      }
    
      MBeanServer mbeanServer = Jmx.getMBeanServer();

      ObjectName mbeanName = null;

      if (_mbeanName != null)
        mbeanName = Jmx.getObjectName(_mbeanName);

      if (_type != null) {
      }
      else if (oldObject != null) {
        _object = oldObject;
        return;
      }
      else if (mbeanName != null &&
               mbeanServer.getMBeanInfo(mbeanName) != null) {
        return;
      }
      else
        throw new ConfigException(L.l("<resource> configuration needs a <type>.  The <type> is the class name of the resource bean."));

      Constructor constructor = getConstructor(_args.size());

      Class []params = constructor.getParameterTypes();
      
      Object []args = new Object[_args.size()];

      /*
        for (int i = 0; i < args.length; i++)
        args[i] = _args.get(i).configure(params[i]);
      */
      for (int i = 0; i < args.length; i++)
        args[i] = _args.get(i);

      _object = constructor.newInstance(args);

      if (mbeanName != null) {
        Object mbean = _object;

        if (_mbeanInterface != null)
          mbean = new IntrospectionMBean(mbean, _mbeanInterface);
      
        Jmx.register(mbean, mbeanName);
        _mbeanInfo = mbeanServer.getMBeanInfo(mbeanName);
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Returns the constructor based on the length.
   */
  private Constructor getConstructor(int len)
    throws Exception
  {
    Constructor []constructors = _type.getConstructors();

    for (int i = 0; i < constructors.length; i++) {
      if (constructors[i].getParameterTypes().length == len)
        return constructors[i];
    }

    throw new ConfigException(L.l("`{0}' has no matching constructors.",
                                  _type.getName()));
  }

  /**
   * Initialize the resource.
   */
  @PostConstruct
  public void init()
    throws Throwable
  {
    preInit();
    
    if (_type == null || _object == null)
      return;
    
    Config.init(_object);
    _object = Config.replaceObject(_object);

    if (_object instanceof ClassLoaderListener) {
      ClassLoaderListener listener = (ClassLoaderListener) _object;

      Environment.addClassLoaderListener(listener);
    }

    if (_object instanceof EnvironmentListener) {
      EnvironmentListener listener = (EnvironmentListener) _object;

      Environment.addEnvironmentListener(listener);
    }

    Object jndiObject = _object;
    boolean isStart = false;

    if (_object instanceof ResourceAdapter) {
      ResourceManagerImpl.addResource((ResourceAdapter) _object);
      isStart = true;
    }

    if (_object instanceof ManagedConnectionFactory) {
      ResourceManagerImpl rm = ResourceManagerImpl.createLocalManager();

      ManagedConnectionFactory mcf;
      mcf = (ManagedConnectionFactory) _object;

      ConnectionPool cm = rm.createConnectionPool();

      cm.setShareable(_shareable);
      cm.setLocalTransactionOptimization(_localTransactionOptimization);
      Object connectionFactory = cm.init(mcf);
      cm.start();

      jndiObject = connectionFactory;

      isStart = true;
    }

    Method start = null;
    try {
      start = _object.getClass().getMethod("start", new Class[0]);
    } catch (Throwable e) {
    }

    Method stop = null;
    try {
      stop = _object.getClass().getMethod("stop", new Class[0]);
    } catch (Throwable e) {
    }

    if (_jndiName != null)
      Jndi.bindDeepShort(_jndiName, jndiObject);

    if (isStart) {
    }
    else if (start != null || stop != null)
      Environment.addEnvironmentListener(new StartListener(_object));
    else if (CloseListener.getDestroyMethod(_object.getClass()) != null)
      Environment.addClassLoaderListener(new CloseListener(_object));

    String name = _name;
    
    if (_name == null)
      name = _var;
    
    if (_name == null)
      name = _jndiName;
    
    InjectManager beanManager = InjectManager.create();

    BeanBuilder factory = beanManager.createBeanFactory(_object.getClass());

    if (name != null) {
      factory.name(name);
      factory.qualifier(CurrentLiteral.CURRENT);
      factory.qualifier(Names.create(name));
    }

    // server/12dt
    // for backward compatibility <resource> is always ApplicationScoped
    // factory.scope(ApplicationScoped.class);
    factory.scope(Singleton.class);

    if (_object != null)
      beanManager.addBean(factory.singleton(_object));
    else
      beanManager.addBean(factory.bean());

    if (log.isLoggable(Level.CONFIG))
      logConfig();
  }

  private void logConfig()
  {
    StringBuilder sb = new StringBuilder();

    if (_object instanceof ResourceAdapter)
      sb.append("jca-resource");
    else if (_object instanceof ManagedConnectionFactory)
      sb.append("jca-resource");
    else
      sb.append("resource");

    sb.append("[");
    boolean hasValue = false;
      
    if (_jndiName != null) {
      if (hasValue) sb.append(", ");
      hasValue = true;
      sb.append("jndi-name=" + _jndiName);
    }
    
    if (_var != null) {
      if (hasValue) sb.append(", ");
      hasValue = true;
      sb.append("var=" + _var);
    }
    
    if (_mbeanName != null) {
      if (hasValue) sb.append(", ");
      hasValue = true;
      sb.append("mbean-name=" + _mbeanName);
    }
      
    if (_type != null) {
      if (hasValue) sb.append(", ");
      hasValue = true;
      sb.append("type=" + _type);
    }
      
    sb.append("]");

    log.config(sb.toString() + " configured");
  }

  public String toString()
  {
    if (_mbeanName != null)
      return "Resource[" + _mbeanName + "]";
    else
      return "Resource[" + _jndiName + "]";
  }

  public class MBeanInit {
    public void setProperty(String attrName, ConfigProgram program)
      throws Throwable
    {
      MBeanAttributeInfo attr = getAttribute(attrName);
      if (attr == null)
        throw new ConfigException(L.l("`{0}' is an unknown attribute for {1}",
                                      attrName, _mbeanName));

      String typeName = attr.getType();
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Class type = Class.forName(typeName, false, loader);

      Object value = program.configure(type);
      
      MBeanServer server = Jmx.getMBeanServer();
      
      server.setAttribute(getObjectName(),
                          new Attribute(attr.getName(), value));
    }

    private MBeanAttributeInfo getAttribute(String key)
      throws Throwable
    {
      MBeanInfo info = getMBeanInfo();

      MBeanAttributeInfo []attrs = info.getAttributes();

      if (attrs == null)
        return null;

      for (int i = 0; i < attrs.length; i++) {
        if (attrs[i].getName().equals(key))
          return attrs[i];
      }

      for (int i = 0; i < attrs.length; i++) {
        if (convertName(attrs[i].getName()).equals(key))
          return attrs[i];
      }

      return null;
    }

    private String convertName(String key)
    {
      CharBuffer cb = CharBuffer.allocate();

      for (int i = 0; i < key.length(); i++) {
        char ch = key.charAt(i);

        if (! Character.isUpperCase(ch))
          cb.append(ch);
        else if (i == 0)
          cb.append(Character.toLowerCase(ch));
        else if (Character.isLowerCase(key.charAt(i - 1))) {
          cb.append('-');
          cb.append(Character.toLowerCase(ch));
        }
        else if (i + 1 != key.length() &&
                 Character.isLowerCase(key.charAt(i + 1))) {
          cb.append('-');
          cb.append(Character.toLowerCase(ch));
        }
        else
          cb.append(Character.toLowerCase(ch));
      }

      return cb.close();
    }
  }

  public class MBeanListener {
    private String _mbeanName;
    private Object _handback;
    private NotificationFilter _filter;

    public void setMbeanName(String name)
    {
      _mbeanName = name;
    }

    public String getMBeanName()
    {
      return _mbeanName;
    }

    public void setHandback(Object handback)
    {
      _handback = handback;
    }

    public Object getHandback()
    {
      return _handback;
    }

    @PostConstruct
    public void init()
    {
      try {
        if (_mbeanName != null) {
          ObjectName mbeanName = Jmx.getObjectName(_mbeanName);

          ObjectName listenerName = getObjectName();

          MBeanServer server = Jmx.getMBeanServer();

          server.addNotificationListener(mbeanName, listenerName,
                                         _filter, _handback);

        }
        else
          throw new ConfigException(L.l("mbean name is required"));
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }
  }
}

  
