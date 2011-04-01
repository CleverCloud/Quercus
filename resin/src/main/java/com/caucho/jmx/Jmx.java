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

package com.caucho.jmx;

import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.management.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;

/**
 * Static convenience methods.
 */
public class Jmx {
  private static final L10N L = new L10N(Jmx.class);
  private static final Logger log 
    = Logger.getLogger(Jmx.class.getName());

  private static EnvironmentMBeanServer _mbeanServer;
  private static MBeanServer _globalMBeanServer;

  /**
   * Sets the server.
   */
  static void setMBeanServer(EnvironmentMBeanServer server)
  {
    _mbeanServer = server;
  }

  /**
   * Returns the context mbean server.
   */
  public static MBeanServer getContextMBeanServer()
  {
    if (_mbeanServer == null)
      _mbeanServer = (EnvironmentMBeanServer) EnvironmentMBeanServerBuilder.getGlobal("resin");
      
    return _mbeanServer;
  }

  /**
   * Returns the global mbean server.
   */
  public static MBeanServer getGlobalMBeanServer()
  {
    if (_globalMBeanServer == null) {
      getContextMBeanServer();

      ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
      _globalMBeanServer = new GlobalMBeanServer(systemLoader);
    }

    return _globalMBeanServer;
  }

  /**
   * Gets the static mbean server.
   */
  public static AbstractMBeanServer getMBeanServer()
  {
    return _mbeanServer;
  }
  
  /**
   * Returns a copy of the context properties.
   */
  public static LinkedHashMap<String,String> copyContextProperties()
  {
    AbstractMBeanServer mbeanServer = getMBeanServer();

    if (mbeanServer != null)
      return mbeanServer.createContext().copyProperties();
    else
      return new LinkedHashMap<String,String>();
  }
  
  /**
   * Returns a copy of the context properties.
   */
  public static LinkedHashMap<String,String>
    copyContextProperties(ClassLoader loader)
  {
    AbstractMBeanServer mbeanServer = getMBeanServer();

    if (mbeanServer != null)
      return mbeanServer.createContext(loader).copyProperties();
    else
      return new LinkedHashMap<String,String>();
  }

  /**
   * Sets the context properties.
   */
  public static void setContextProperties(Map<String,String> properties)
  {
    AbstractMBeanServer mbeanServer = getMBeanServer();

    if (mbeanServer != null)
      mbeanServer.createContext().setProperties(properties);
  }

  /**
   * Sets the context properties.
   */
  public static void setContextProperties(Map<String,String> properties,
                                          ClassLoader loader)
  {
    AbstractMBeanServer mbeanServer = getMBeanServer();

    if (mbeanServer != null)
      mbeanServer.createContext(loader).setProperties(properties);
  }
  
  /**
   * Conditionally registers an MBean with the server.
   *
   * @param object the object to be registered as an MBean
   * @param name the name of the mbean.
   *
   * @return the instantiated object.
   */
  public static ObjectInstance register(Object object, String name)
    throws InstanceAlreadyExistsException,
           MBeanRegistrationException, MalformedObjectNameException,
           NotCompliantMBeanException
  {
    if (name.indexOf(':') < 0) {
      Map<String,String> props = parseProperties(name);

      if (props.get("type") == null) {
        String type = object.getClass().getName();
        int p = type.lastIndexOf('.');
        if (p > 0)
          type = type.substring(p + 1);

        props.put("type", type);
      }

      ObjectName objectName = getObjectName("resin", props);

      return register(object, objectName);
    }
    else
      return register(object, new ObjectName(name));
      
  }
  
  /**
   * Conditionally registers an MBean with the server.
   *
   * @param object the object to be registered as an MBean
   * @param name the name of the mbean.
   *
   * @return the instantiated object.
   */
  public static ObjectInstance register(Object object,
                                        Map<String,String> properties)
    throws InstanceAlreadyExistsException,
           MBeanRegistrationException, MalformedObjectNameException,
           NotCompliantMBeanException
  {
    Map<String,String> props = copyContextProperties();
    props.putAll(properties);

    return register(object, getObjectName("resin", props));
  }
  
  /**
   * Registers an MBean with the server.
   *
   * @param object the object to be registered as an MBean
   * @param name the name of the mbean.
   *
   * @return the instantiated object or null if the object
   * doesn't have an MBean interface.
   */
  public static ObjectInstance register(Object object, ObjectName name)
    throws InstanceAlreadyExistsException,
           MBeanRegistrationException,
           NotCompliantMBeanException
  {
    DynamicMBean dynMBean = createMBean(object, name);

    if (dynMBean != null)
      return getMBeanServer().registerMBean(dynMBean, name);
    else
      return null;
  }
  
  /**
   * Registers an MBean with the server.
   *
   * @param object the object to be registered as an MBean
   * @param name the name of the mbean.
   *
   * @return the instantiated object.
   */
  public static ObjectInstance register(Object object, ObjectName name,
                                        ClassLoader loader)
    throws InstanceAlreadyExistsException,
           MBeanRegistrationException,
           NotCompliantMBeanException
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(loader);

      AbstractMBeanServer mbeanServer = getMBeanServer();

      if (mbeanServer != null) {
        DynamicMBean dynMBean = createMBean(object, name);

        if (dynMBean != null)
          return mbeanServer.registerMBean(dynMBean, name);
      }

      return null;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Creates the dynamic mbean.
   */
  private static DynamicMBean createMBean(Object obj, ObjectName name)
    throws NotCompliantMBeanException
  {
    if (obj == null)
      return null;
    else if (obj instanceof DynamicMBean)
      return (DynamicMBean) obj;

    Class<?> ifc = getMBeanInterface(obj.getClass());

    if (ifc == null)
      return null;

    return new IntrospectionMBean(obj, ifc);
  }

  /**
   * Returns the mbean interface.
   */
  private static Class getMBeanInterface(Class cl)
  {
    for (; cl != null; cl = cl.getSuperclass()) {
      Class []interfaces = cl.getInterfaces();

      for (int i = 0; i < interfaces.length; i++) {
        Class ifc = interfaces[i];

        if (ifc.getName().endsWith("MBean") ||
            ifc.getName().endsWith("MXBean"))
          return ifc;
      }
    }

    return null;
  }
  
  /**
   * Unregisters an MBean with the server.
   *
   * @param name the name of the mbean.
   */
  public static void unregister(ObjectName name)
    throws MBeanRegistrationException,
           InstanceNotFoundException
  {
    getMBeanServer().unregisterMBean(name);
  }
  
  /**
   * Unregisters an MBean with the server.
   *
   * @param name the name of the mbean.
   */
  public static void unregister(ObjectName name, ClassLoader loader)
    throws MBeanRegistrationException,
           InstanceNotFoundException
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(loader);
      
      getMBeanServer().unregisterMBean(name);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  /**
   * Registers an MBean with the server.
   *
   * @param object the object to be registered as an MBean
   * @param name the name of the mbean.
   * @param api the api for the server
   *
   * @return the instantiated object.
   */
  public static ObjectInstance register(Object object,
                                        String name,
                                        Class api)
    throws InstanceAlreadyExistsException,
           MBeanRegistrationException,
           MalformedObjectNameException,
           NotCompliantMBeanException
  {
    return register(object, new ObjectName(name), api);
  }
  
  /**
   * Registers an MBean with the server.
   *
   * @param object the object to be registered as an MBean
   * @param name the name of the mbean.
   * @param api the api for the server
   *
   * @return the instantiated object.
   */
  public static ObjectInstance register(Object object,
                                        ObjectName name,
                                        Class api)
    throws InstanceAlreadyExistsException,
           MBeanRegistrationException, MalformedObjectNameException,
           NotCompliantMBeanException
  {
    IntrospectionMBean mbean = new IntrospectionMBean(object, api);
    
    return getMBeanServer().registerMBean(mbean, name);
  }
  
  /**
   * Conditionally registers an MBean with the server.
   *
   * @param object the object to be registered as an MBean
   * @param name the name of the mbean.
   *
   * @return the instantiated object.
   */
  public static void unregister(String name)
    throws InstanceNotFoundException,
           MalformedObjectNameException,
           MBeanRegistrationException

  {
    ObjectName objectName = getObjectName(name);

    getMBeanServer().unregisterMBean(objectName);
    // return register(object, objectName);
  }

  /**
   * Returns an ObjectName based on a short name.
   */
  public static ObjectName getObjectName(String name)
    throws MalformedObjectNameException
  {
    return getMBeanServer().createContext().getObjectName(name);
  }

  /**
   * Parses a name.
   */
  public static LinkedHashMap<String,String> parseProperties(String name)
  {
    LinkedHashMap<String,String> map = new LinkedHashMap<String,String>();
    
    parseProperties(map, name);

    return map;
  }

  /**
   * Parses a name.
   */
  public static void parseProperties(Map<String,String> properties,
                                     String name)
  {
    parseProperties(properties, name, 0);
  }

  /**
   * Parses a name.
   */
  private static void
    parseProperties(Map<String,String> properties, String name, int i)
  {
    CharBuffer cb = CharBuffer.allocate();
    
    int len = name.length();
    
    while (i < len) {
      for (; i < len && Character.isWhitespace(name.charAt(i)); i++) {
      }

      cb.clear();

      int ch;
      for (; i < len && (ch = name.charAt(i)) != '=' && ch != ',' &&
             ! Character.isWhitespace((char) ch); i++) {
        cb.append((char) ch);
      }

      String key = cb.toString();

      if (key.length() == 0) {
        throw new IllegalArgumentException(L.l("`{0}' is an illegal name syntax.",
                                               name));
      }

      for (; i < len && Character.isWhitespace(name.charAt(i)); i++) {
      }

      if (len <= i || (ch = name.charAt(i)) == ',') {
        properties.put(key, "");
      }
      else if (ch == '=') {
        for (i++; i < len && Character.isWhitespace(name.charAt(i)); i++) {
        }

        if (len <= i || (ch = name.charAt(i)) == ',') {
          properties.put(key, "");
        }
        else if (ch == '"' || ch == '\'') {
          int end = ch;
          cb.clear();

          for (i++; i < len && (ch = name.charAt(i)) != end; i++) {
            if (ch == '\\') {
              ch = name.charAt(++i);
              cb.append((char) ch);
            }
            else
              cb.append((char) ch);
          }

          if (ch != end)
            throw new IllegalArgumentException(L.l("`{0}' is an illegal name syntax.",
                                                   name));

          i++;

          String value = cb.toString();

          properties.put(key, value);
        }
        else {
          cb.clear();

          for (; i < len && (ch = name.charAt(i)) != ','; i++)
            cb.append((char) ch);

          properties.put(key, cb.toString());
        }
      }
      else {
        throw new IllegalArgumentException(L.l("`{0}' is an illegal name syntax.",
                                               name));
      }

      for (; i < len && Character.isWhitespace(name.charAt(i)); i++) {
      }
      
      if (i < len && name.charAt(i) != ',')
        throw new IllegalArgumentException(L.l("`{0}' is an illegal name syntax.",
                                               name));

      i++;
    }
  }

  /**
   * Creates the clean name
   */
  public static ObjectName getObjectName(String domain,
                                         Map<String,String> properties)
    throws MalformedObjectNameException
  {
    StringBuilder cb = new StringBuilder();
    cb.append(domain);
    cb.append(':');

    boolean isFirst = true;

    Pattern escapePattern = Pattern.compile("[,=:\"*?]");

    // sort type first

    String type = properties.get("type");
    if (type != null) {
      cb.append("type=");
      if (escapePattern.matcher(type).find())
        type = ObjectName.quote(type);
      cb.append(type);

      isFirst = false;
    }

    for (String key : properties.keySet()) {
      if (key.equals("type"))
        continue;
      
      if (! isFirst)
        cb.append(',');
      isFirst = false;

      cb.append(key);
      cb.append('=');

      String value = properties.get(key);

      if (value.length() == 0
          || (escapePattern.matcher(value).find()
              && ! (value.startsWith("\"") && value.endsWith("\"")))) {
        value = ObjectName.quote(value);
      }
      
      cb.append(value);
    }

    return new ObjectName(cb.toString());
  }

  /**
   * Returns the local view.
   */
  /*
  public static MBeanView getLocalView()
  {
    MBeanContext context = MBeanContext.getLocal();

    return context.getView();
  }
  */

  /**
   * Returns the local view.
   */
  /*
  public static MBeanView getLocalView(ClassLoader loader)
  {
    MBeanContext context = MBeanContext.getLocal(loader);

    return context.getView();
  }
  */

  /**
   * Returns the local manged object.
   */
  public static Object find(String localName)
    throws MalformedObjectNameException
  {
    return find(getMBeanServer().createContext().getObjectName(localName));
  }

  /**
   * Returns the local manged object.
   */
  public static Object find(ObjectName name)
  {
    return find(name, Thread.currentThread().getContextClassLoader());
  }

  /**
   * Returns the local manged object.
   */
  public static Object findGlobal(String localName)
    throws MalformedObjectNameException
  {
    return findGlobal(getMBeanServer().createContext().getObjectName(localName));
  }

  /**
   * Returns the local manged object.
   */
  public static Object findGlobal(ObjectName name)
  {
    return find(name, ClassLoader.getSystemClassLoader(), getGlobalMBeanServer());
  }

  /**
   * Returns the local manged object.
   */
  public static Object find(ObjectName name, ClassLoader loader)
  {
    return find(name, loader, getMBeanServer());
  }

  /**
   * Returns the local manged object.
   */
  public static Object find(ObjectName name,
                            ClassLoader loader,
                            MBeanServer mbeanServer)
  {
    try {
      ObjectInstance obj = mbeanServer.getObjectInstance(name);

      if (obj == null)
        return null;

      String className = obj.getClassName();

      Class cl = Class.forName(className, false, loader);

      Class ifc;
      
      if (cl.isInterface())
        ifc = cl;
      else
        ifc = getMBeanInterface(cl);

      if (ifc == null)
        return null;

      boolean isBroadcast = true;

      Object proxy;

      proxy = JmxInvocationHandler.newProxyInstance(mbeanServer,
                                                    loader,
                                                    name,
                                                    ifc,
                                                    true);

      return proxy;
    } catch (InstanceNotFoundException e) {
      log.log(Level.FINE, e.toString(), e);
      return null;
    } catch (ClassNotFoundException e) {
      log.log(Level.FINE, e.toString(), e);
      return null;
    }
  }

  /**
   * Returns the local manged object.
   */
  public static ArrayList<Object> query(ObjectName namePattern)
  {
    Set<ObjectName> names = getMBeanServer().queryNames(namePattern, null);

    ArrayList<Object> proxy = new ArrayList<Object>();
    Iterator<ObjectName> iter = names.iterator();

    while (iter.hasNext()) {
      ObjectName name = iter.next();

      proxy.add(find(name));
    }
    
    return proxy;
  }

  /**
   * Queues a task.
   */
  public static void queueAbsolute(TimerTask job, long time)
  {
    JobThread.queue(job, time);
  }

  /**
   * Queues a task.
   */
  public static void queueRelative(TimerTask job, long delta)
  {
    queueAbsolute(job, Alarm.getCurrentTime() + delta);
  }

  /**
   * Dequeues a task.
   */
  public static void dequeue(TimerTask job)
  {
    JobThread.dequeue(job);
  }

  // static
  public Jmx() {}

  private static void initStaticMBeans()
  {
    try {
      Class cl = Class.forName("java.lang.Management.ManagementFactory");

      Method method = cl.getMethod("getPlatformMBeanServer", new Class[0]);

      method.invoke(null, new Object[0]);
    } catch (Throwable e) {
    }
  }
}

