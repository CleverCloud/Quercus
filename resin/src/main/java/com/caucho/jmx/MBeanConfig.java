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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jmx;

import com.caucho.config.program.ConfigProgram;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.NotificationFilter;
import javax.management.ObjectName;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

/**
 * Configuration for the mbean pattern.
 */
public class MBeanConfig {
  private static L10N L = new L10N(MBeanConfig.class);

  private Class _type;
  
  private String _mbeanName;

  private Class _interface;
  
  private String _jndiName;

  private ObjectName _name;
  private MBeanInfo _mbeanInfo;

  private ArrayList<ConfigProgram> _args = new ArrayList<ConfigProgram>();

  private boolean _isInit;

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
   * Sets the mbean name
   */
  public void setName(String name)
  {
    _mbeanName = name;
  }

  /**
   * Gets the mbean name
   */
  public String getName()
  {
    return _mbeanName;
  }

  /**
   * Sets the class
   */
  public void setType(Class mbeanClass)
  {
    _type = mbeanClass;
  }

  /**
   * Gets the type;
   */
  public Class getMBeanClass()
  {
    return _type;
  }

  /**
   * Sets the class
   */
  public void setInterface(Class cl)
  {
    _interface = cl;
  }

  /**
   * Adds an argument.
   */
  public void addArg(ConfigProgram builder)
  {
    _args.add(builder);
  }

  /**
   * Initialize the resource.
   */
  public void init()
    throws Throwable
  {
    if (_isInit)
      return;

    _isInit = true;

    if (_mbeanName == null)
      throw new ConfigException(L.l("<mbean> configuration needs a 'name' attribute.  The 'name' is the MBean ObjectName for the bean."));
    
    MBeanServer server = Jmx.getMBeanServer();

    _name = Jmx.getObjectName(_mbeanName);

    if (_type != null) {
    }
    else if (server.getMBeanInfo(_name) != null) {
      return;
    }
    else {
      throw new ConfigException(L.l("<mbean> configuration needs a 'type' attribute.  The 'class' is the class name of the resource bean."));
    }

    Constructor constructor = getConstructor(_args.size());

    Class []params = constructor.getParameterTypes();
      
    Object []args = new Object[_args.size()];

    for (int i = 0; i < args.length; i++)
      args[0] = _args.get(i).configure(params[i]);

    Object obj = constructor.newInstance(args);
    
    if (_interface != null) {
      Object mbean = new IntrospectionMBean(obj, _interface);
      server.registerMBean(mbean, _name);
    }
    else {
      server.registerMBean(obj, _name);
    }

    _mbeanInfo = server.getMBeanInfo(_name);
  }

  ObjectName getObjectName()
    throws Throwable
  {
    if (_name == null)
      init();

    return _name;
  }

  private Constructor getConstructor(int len)
    throws Throwable
  {
    Constructor []constructors = _type.getConstructors();

    for (int i = 0; i < constructors.length; i++) {
      if (constructors[i].getParameterTypes().length == len)
        return constructors[i];
    }

    throw new ConfigException(L.l("`{0}' has no matching constructors.",
                                  _type.getName()));
  }

  MBeanInfo getMBeanInfo()
    throws Throwable
  {
    if (_mbeanInfo == null)
      init();

    return _mbeanInfo;
  }

  public Init createInit()
  {
    return new Init();
  }

  public Listener createListener()
  {
    return new Listener();
  }

  public String toString()
  {
    return "MBean[" + _mbeanName + "]";
  }

  public class Init {
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
      
      server.setAttribute(_name, new Attribute(attrName, value));
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

      return null;
    }
  }

  public class Listener {
    private String _name;
    private Object _handback;
    private NotificationFilter _filter;

    public void setName(String name)
    {
      _name = name;
    }

    public String getName()
    {
      return _name;
    }

    public void setHandback(Object handback)
    {
      _handback = handback;
    }

    public Object getHandback()
    {
      return _handback;
    }

    public void init()
      throws Throwable
    {
      ObjectName mbeanName = getObjectName();

      ObjectName listenerName = Jmx.getObjectName(_name);

      MBeanServer server = Jmx.getMBeanServer();

      server.addNotificationListener(mbeanName, listenerName,
                                     _filter, _handback);
      
    }
  }
}
