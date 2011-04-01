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

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.WeakCloseListener;
import com.caucho.util.L10N;

import javax.management.*;
import javax.management.loading.ClassLoaderRepository;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main interface for retrieving and managing JMX objects.
 */
abstract public class AbstractMBeanServer implements MBeanServer {
  private static final L10N L = new L10N(AbstractMBeanServer.class);
  private static final Logger log
    = Logger.getLogger(AbstractMBeanServer.class.getName());

  static ObjectName SERVER_DELEGATE_NAME;

  private EnvironmentLocal<MBeanContext> _currentContext
    = new EnvironmentLocal<MBeanContext>();

  // default domain
  private String _defaultDomain;

  /**
   * Creats a new MBeanServer implementation.
   */
  public AbstractMBeanServer(String defaultDomain)
  {
    _defaultDomain = defaultDomain;

    Environment.addClassLoaderListener(new WeakCloseListener(this));
  }

  /**
   * Returns the context implementation.
   */
  protected MBeanContext createContext()
  {
    return createContext(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Returns the context implementation.
   */
  protected final MBeanContext getCurrentContext()
  {
    return getCurrentContext(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Returns the context implementation.
   */
  protected final MBeanContext getGlobalContext()
  {
    return createContext(ClassLoader.getSystemClassLoader());
  }

  /**
   * Returns the context implementation, creating if necessary.
   */
  abstract protected MBeanContext createContext(ClassLoader loader);

  /**
   * Returns the context implementation.
   */
  abstract protected MBeanContext getCurrentContext(ClassLoader loader);

  /**
   * Sets the context implementation.
   */
  abstract protected void setCurrentContext(MBeanContext context,
                                            ClassLoader loader);

  /**
   * Returns the context implementation.
   */
  abstract protected MBeanContext getContext(ClassLoader loader);

  /**
   * Removes the context implementation.
   */
  protected void removeContext(MBeanContext context, ClassLoader loader)
  {
  }

  /**
   * Returns the view implementation.
   */
  protected MBeanView getView()
  {
    return createContext().getView();
  }

  /**
   * Returns the view implementation.
   */
  protected MBeanView getGlobalView()
  {
    return getGlobalContext().getView();
  }

  /**
   * Returns the view implementation.
   */
  protected MBeanView getParentView()
  {
    return null;
  }

  /**
   * Instantiate an MBean object to be registered with the server.
   *
   * @param className the className to be instantiated.
   *
   * @return the instantiated object.
   */
  public Object instantiate(String className)
    throws ReflectionException, MBeanException
  {
    try {
      Class cl = getClassLoaderRepository().loadClass(className);

      return cl.newInstance();
    } catch (ClassNotFoundException e) {
      throw new ReflectionException(e);
    } catch (InstantiationException e) {
      throw new ReflectionException(e);
    } catch (ExceptionInInitializerError e) {
      Throwable cause = e.getCause();
      if (cause instanceof Exception)
        throw new MBeanException((Exception) cause);
      else
        throw e;
    } catch (IllegalAccessException e) {
      throw new ReflectionException(e);
    }
  }

  /**
   * Instantiate an MBean object to be registered with the server.
   *
   * @param className the className to be instantiated.
   * @param loaderName names the classloader to be used
   *
   * @return the instantiated object.
   */
  public Object instantiate(String className, ObjectName loaderName)
    throws ReflectionException, MBeanException, InstanceNotFoundException
  {
    MBeanWrapper mbean = getMBean(loaderName);

    if (mbean == null)
      throw new InstanceNotFoundException(String.valueOf(loaderName));
    else if (! (mbean.getObject() instanceof ClassLoader))
      throw new InstanceNotFoundException(L.l("{0} is not a class loader",
                                              loaderName));

    try {
      ClassLoader loader = (ClassLoader) mbean.getObject();

      Class cl = loader.loadClass(className);

      return cl.newInstance();
    } catch (ClassNotFoundException e) {
      throw new ReflectionException(e);
    } catch (InstantiationException e) {
      throw new ReflectionException(e);
    } catch (IllegalAccessException e) {
      throw new ReflectionException(e);
    }
  }

  /**
   * Instantiate an MBean object with the given arguments to be
   * passed to the constructor.
   *
   * @param className the className to be instantiated.
   * @param params the parameters for the constructor.
   * @param signature the signature of the constructor
   *
   * @return the instantiated object.
   */
  public Object instantiate(String className,
                            Object []params, String []signature)
    throws ReflectionException, MBeanException
  {
    try {
      Class cl = getClassLoaderRepository().loadClass(className);

      Constructor constructor = getConstructor(cl, signature);

      return constructor.newInstance(params);
    } catch (ClassNotFoundException e) {
      throw new ReflectionException(e);
    } catch (InstantiationException e) {
      throw new ReflectionException(e);
    } catch (InvocationTargetException e) {
        throw new MBeanException(e);
    } catch (IllegalAccessException e) {
      throw new ReflectionException(e);
    }
  }

  /**
   * Instantiate an MBean object with the given arguments to be
   * passed to the constructor.
   *
   * @param className the className to be instantiated.
   * @param loaderName names the classloader to be used
   * @param params the parameters for the constructor.
   * @param signature the signature of the constructor
   *
   * @return the instantiated object.
   */
  public Object instantiate(String className, ObjectName loaderName,
                            Object []params, String []signature)
    throws ReflectionException, MBeanException, InstanceNotFoundException
  {
    MBeanWrapper mbean = getMBean(loaderName);

    if (mbean == null)
      throw new InstanceNotFoundException(String.valueOf(loaderName));
    else if (! (mbean.getObject() instanceof ClassLoader))
      throw new InstanceNotFoundException(L.l("{0} is not a class loader",
                                              loaderName));

    try {
      ClassLoader loader = (ClassLoader) mbean.getObject();

      Class cl = loader.loadClass(className);

      Constructor constructor = getConstructor(cl, signature);

      return constructor.newInstance(params);
    } catch (ClassNotFoundException e) {
      throw new ReflectionException(e);
    } catch (InstantiationException e) {
      throw new ReflectionException(e);
    } catch (InvocationTargetException e) {
        throw new MBeanException(e);
    } catch (IllegalAccessException e) {
      throw new ReflectionException(e);
    }
  }

  /**
   * Returns the class's constructor with the matching sig.
   */
  private Constructor getConstructor(Class cl, String []sig)
  {
    Constructor []constructors = cl.getConstructors();

    for (int i = 0; i < constructors.length; i++) {
      if (! Modifier.isPublic(constructors[i].getModifiers()))
        continue;

      if (isMatch(constructors[i].getParameterTypes(), sig))
        return constructors[i];
    }

    return null;
  }

  /**
   * Matches the parameters the sig.
   */
  private boolean isMatch(Class []param, String []sig)
  {
    if (param.length != sig.length)
      return false;

    for (int i = 0; i < param.length; i++) {
      if (! param[i].getName().equals(sig[i]))
        return false;
    }

    return true;
  }

  /**
   * Instantiate and register an MBean.
   *
   * @param className the className to be instantiated.
   * @param name the name of the mbean.
   *
   * @return the instantiated object.
   */
  public ObjectInstance createMBean(String className, ObjectName name)
    throws ReflectionException, InstanceAlreadyExistsException,
           MBeanException, NotCompliantMBeanException
  {
    return registerMBean(instantiate(className), name);
  }


  /**
   * Instantiate and register an MBean.
   *
   * @param className the className to be instantiated.
   * @param name the name of the mbean.
   * @param loaderName the name of the class loader to user
   *
   * @return the instantiated object.
   */
  public ObjectInstance createMBean(String className, ObjectName name,
                                    ObjectName loaderName)
    throws ReflectionException, InstanceAlreadyExistsException,
           MBeanException, NotCompliantMBeanException, InstanceNotFoundException
  {
    return registerMBean(instantiate(className, loaderName), name);
  }

  /**
   * Instantiate and register an MBean.
   *
   * @param className the className to be instantiated.
   * @param name the name of the mbean.
   * @param params the parameters for the constructor.
   * @param signature the signature of the constructor
   *
   * @return the instantiated object.
   */
  public ObjectInstance createMBean(String className, ObjectName name,
                                    Object []params, String []signature)
    throws ReflectionException, InstanceAlreadyExistsException,
           MBeanException, NotCompliantMBeanException
  {
    return registerMBean(instantiate(className, params, signature),
                         name);
  }

  /**
   * Instantiate and register an MBean.
   *
   * @param className the className to be instantiated.
   * @param name the name of the mbean.
   * @param loaderName the loader name for the mbean.
   * @param params the parameters for the constructor.
   * @param signature the signature of the constructor
   *
   * @return the instantiated object.
   */
  public ObjectInstance createMBean(String className, ObjectName name,
                                    ObjectName loaderName,
                                    Object []params, String []signature)
    throws ReflectionException, InstanceAlreadyExistsException,
           MBeanException, NotCompliantMBeanException, InstanceNotFoundException
  {
    return registerMBean(instantiate(className, loaderName, params, signature),
                         name);
  }

  /**
   * Registers an MBean with the server.
   *
   * @param object the object to be registered as an MBean
   * @param name the name of the mbean.
   *
   * @return the instantiated object.
   */
  public ObjectInstance registerMBean(Object object, ObjectName name)
    throws InstanceAlreadyExistsException,
           MBeanRegistrationException,
           NotCompliantMBeanException
  {
    if (object == null)
      throw new NullPointerException();

    MBeanContext context;

    context = createContext();

    if (context.getMBean(name) != null) {
      throw new InstanceAlreadyExistsException(L.l("'{0}' in {1}",
                                                   name, context, this));
    }

    DynamicMBean dynMBean = createMBean(object, name);

    if (object instanceof IntrospectionMBean)
      object = ((IntrospectionMBean) object).getImplementation();
    else if (object instanceof StandardMBean) {
      object = ((StandardMBean) object).getImplementation();
    }

    MBeanWrapper mbean = new MBeanWrapper(context, name, object, dynMBean);
    return context.registerMBean(mbean, name);
  }

  /**
   * Creates the dynamic mbean.
   */
  private DynamicMBean createMBean(Object obj, ObjectName name)
    throws NotCompliantMBeanException
  {
    if (obj == null)
      throw new NotCompliantMBeanException(L.l("{0} mbean is null", name));
    else if (obj instanceof DynamicMBean)
      return (DynamicMBean) obj;

    Class ifc = getMBeanInterface(obj.getClass());

    if (ifc == null)
      throw new NotCompliantMBeanException(L.l("{0} mbean has no MBean interface for class {1}", name, obj.getClass().getName()));

    return new IntrospectionMBean(obj, ifc);
  }

  /**
   * Returns the mbean interface.
   */
  private Class getMBeanInterface(Class cl)
  {
    for (; cl != null; cl = cl.getSuperclass()) {
      Class []interfaces = cl.getInterfaces();

      String mbeanName = cl.getName() + "MBean";
      String mxbeanName = cl.getName() + "MXBean";

      int p = mbeanName.lastIndexOf('.');
      mbeanName = mbeanName.substring(p);

      p = mxbeanName.lastIndexOf('.');
      mxbeanName = mxbeanName.substring(p);

      for (int i = 0; i < interfaces.length; i++) {
        Class ifc = interfaces[i];

        if (ifc.getName().endsWith(mbeanName)
            || ifc.getName().endsWith(mxbeanName))
          return ifc;
      }
    }

    return null;
  }

  /**
   * Unregisters an MBean from the server.
   *
   * @param name the name of the mbean.
   */
  public void unregisterMBean(ObjectName name)
    throws InstanceNotFoundException,
           MBeanRegistrationException
  {
    MBeanContext context = getCurrentContext();

    if (context != null) {
      context.unregisterMBean(name);

      log.finest(name + " unregistered from " + this);
    }

    // XXX: getDelegate().sendUnregisterNotification(name);
  }

  /**
   * Returns the MBean registered with the given name.
   *
   * @param name the name of the mbean.
   *
   * @return the matching mbean object.
   */
  public ObjectInstance getObjectInstance(ObjectName name)
    throws InstanceNotFoundException
  {
    MBeanWrapper mbean = getMBean(name);

    if (mbean == null)
      throw new InstanceNotFoundException(String.valueOf(name));

    return mbean.getObjectInstance();
  }

  /**
   * Returns a set of MBeans matching the query.
   *
   * @param name the name of the mbean to match.
   * @param query the queryd to match.
   *
   * @return the set of matching mbean object.
   */
  public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query)
  {
    try {
      if (query != null) {
        query.setMBeanServer(this);
      }

      return getView().queryMBeans(name, query);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  /**
   * Returns a set of names for MBeans matching the query.
   *
   * @param name the name of the mbean to match.
   * @param query the query to match.
   *
   * @return the set of matching mbean names.
   */
  public Set<ObjectName> queryNames(ObjectName name, QueryExp query)
  {
    try {
      if (query != null) {
        query.setMBeanServer(this);
      }

      return getView().queryNames(name, query);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  /**
   * Returns true if the given object is registered with the server.
   *
   * @param name the name of the mbean to test.
   *
   * @return true if the object is registered.
   */
  public boolean isRegistered(ObjectName name)
  {
    return getView().getMBean(name) != null;
  }

  /**
   * Returns the number of MBeans registered.
   *
   * @return the number of registered mbeans.
   */
  public Integer getMBeanCount()
  {
    return new Integer(getView().getMBeanCount());
  }

  /**
   * Returns a specific attribute of a named MBean.
   *
   * @param name the name of the mbean to test
   * @param attribute the name of the attribute to retrieve
   *
   * @return the attribute value
   */
  public Object getAttribute(ObjectName name, String attribute)
    throws MBeanException, AttributeNotFoundException,
           InstanceNotFoundException, ReflectionException
  {
    MBeanWrapper mbean = getMBean(name);

    if (mbean == null) {
      throw new InstanceNotFoundException(String.valueOf(name));
    }

    return mbean.getAttribute(attribute);
  }

  /**
   * Returns a list of several MBean attributes.
   *
   * @param name the name of the mbean
   * @param attributes the name of the attributes to retrieve
   *
   * @return the attribute value
   */
  public AttributeList getAttributes(ObjectName name, String []attributes)
    throws InstanceNotFoundException, ReflectionException
  {
    MBeanWrapper mbean = getMBean(name);

    if (mbean == null)
      throw new InstanceNotFoundException(String.valueOf(name));

    return mbean.getAttributes(attributes);
  }

  /**
   * Sets an attribute in the MBean.
   *
   * @param name the name of the mbean
   * @param attribute the name/value of the attribute to set.
   */
  public void setAttribute(ObjectName name, Attribute attribute)
    throws InstanceNotFoundException, AttributeNotFoundException,
           InvalidAttributeValueException, MBeanException, ReflectionException
  {
    MBeanWrapper mbean = getMBean(name);

    if (mbean == null)
      throw new InstanceNotFoundException(String.valueOf(name));

    mbean.setAttribute(attribute);
  }

  /**
   * Set an attributes in the MBean.
   *
   * @param name the name of the mbean
   * @param attributes the name/value list of the attribute to set.
   */
  public AttributeList setAttributes(ObjectName name, AttributeList attributes)
    throws InstanceNotFoundException, ReflectionException
  {
    MBeanWrapper mbean = getMBean(name);

    if (mbean == null)
      throw new InstanceNotFoundException(String.valueOf(name));

    return mbean.setAttributes(attributes);
  }

  /**
   * Invokers an operation on an MBean.
   *
   * @param name the name of the mbean
   * @param operationName the name of the method to invoke
   * @param params the parameters for the invocation
   * @param signature the signature of the operation
   */
  public Object invoke(ObjectName name,
                       String operationName,
                       Object []params,
                       String []signature)
    throws InstanceNotFoundException, MBeanException, ReflectionException
  {
    MBeanWrapper mbean = getMBean(name);

    if (mbean == null)
      throw new InstanceNotFoundException(String.valueOf(name));

    return mbean.invoke(operationName, params, signature);
  }

  /**
   * Returns the default domain for naming the MBean
   */
  public String getDefaultDomain()
  {
    return _defaultDomain;
  }

  /**
   * Adds a listener to a registered MBean
   *
   * @param name the name of the mbean
   * @param listener the listener object
   * @param filter filters events the listener is interested in
   * @param handback context to be returned to the listener
   */
  public void addNotificationListener(ObjectName name,
                                      NotificationListener listener,
                                      NotificationFilter filter,
                                      Object handback)
    throws InstanceNotFoundException
  {
    MBeanWrapper mbean = getMBean(name);

    if (mbean == null)
      throw new InstanceNotFoundException(String.valueOf(name));

    mbean.addNotificationListener(listener, filter, handback);

    createContext().addNotificationListener(name, listener, filter, handback);
  }

  /**
   * Adds a listener to a registered MBean
   *
   * @param name the name of the mbean
   * @param listenerName the name of the listener
   * @param filter filters events the listener is interested in
   * @param handback context to be returned to the listener
   */
  public void addNotificationListener(ObjectName name,
                                      ObjectName listenerName,
                                      NotificationFilter filter,
                                      Object handback)
    throws InstanceNotFoundException
  {
    MBeanWrapper listenerMBean = getMBean(listenerName);

    if (listenerMBean == null)
      throw new InstanceNotFoundException(String.valueOf(listenerName));

    NotificationListener listener = listenerMBean.getListener();

    if (listener == null) {
      IllegalArgumentException exn = new IllegalArgumentException(L.l("{0} does not implement NotificationListener.", listenerName));
      throw new RuntimeOperationsException(exn);
    }

    addNotificationListener(name, listener, filter, handback);
  }

  /**
   * Removes a listener from a registered MBean
   *
   * @param name the name of the mbean
   * @param listener the listener object
   */
  public void removeNotificationListener(ObjectName name,
                                         NotificationListener listener)
    throws InstanceNotFoundException, ListenerNotFoundException
  {
    MBeanWrapper mbean = getMBean(name);

    if (mbean == null)
      throw new InstanceNotFoundException(String.valueOf(name));

    mbean.removeNotificationListener(listener);

    createContext().removeNotificationListener(name, listener);
  }

  /**
   * Removes a listener from a registered MBean
   *
   * @param name the name of the mbean
   * @param listenerName the name of the listener
   */
  public void removeNotificationListener(ObjectName name,
                                         ObjectName listenerName)
    throws InstanceNotFoundException, ListenerNotFoundException
  {
    MBeanWrapper listenerMBean = getMBean(listenerName);

    if (listenerMBean == null)
      throw new InstanceNotFoundException(String.valueOf(listenerName));

    NotificationListener listener = listenerMBean.getListener();

    if (listener == null) {
      IllegalArgumentException exn = new IllegalArgumentException(L.l("{0} does not implement NotificationListener."));
      throw new RuntimeOperationsException(exn);
    }

    removeNotificationListener(name, listener);
  }

  /**
   * Removes a listener from a registered MBean
   *
   * @param name the name of the mbean
   * @param listenerName the name of the listener
   * @param filter the notification filter
   * @param handback context to the listener
   *
   * @since JMX 1.2
   */
  public void removeNotificationListener(ObjectName name,
                                         ObjectName listenerName,
                                         NotificationFilter filter,
                                         Object handback)
    throws InstanceNotFoundException, ListenerNotFoundException
  {
    MBeanWrapper listenerMBean = getMBean(listenerName);

    if (listenerMBean == null)
      throw new InstanceNotFoundException(String.valueOf(listenerName));

    NotificationListener listener = listenerMBean.getListener();

    if (listener == null) {
      IllegalArgumentException exn = new IllegalArgumentException(L.l("{0} does not implement NotificationListener."));
      throw new RuntimeOperationsException(exn);
    }

    removeNotificationListener(name, listener, filter, handback);
  }

  /**
   * Removes a listener from a registered MBean
   *
   * @param name the name of the mbean
   * @param listenerName the name of the listener
   * @param filter the notification filter
   * @param handback context to the listener
   *
   * @since JMX 1.2
   */
  public void removeNotificationListener(ObjectName name,
                                         NotificationListener listener,
                                         NotificationFilter filter,
                                         Object handback)
    throws InstanceNotFoundException, ListenerNotFoundException
  {
    MBeanWrapper mbean = getMBean(name);

    if (mbean == null)
      throw new InstanceNotFoundException(String.valueOf(name));

    createContext().removeNotificationListener(name, listener, filter, handback);

    mbean.removeNotificationListener(listener, filter, handback);
  }

  /**
   * Returns the analyzed information for an MBean
   *
   * @param name the name of the mbean
   *
   * @return the introspected information
   */
  public MBeanInfo getMBeanInfo(ObjectName name)
    throws InstanceNotFoundException, IntrospectionException,
           ReflectionException
  {
    MBeanWrapper mbean = getMBean(name);

    if (mbean == null)
      throw new InstanceNotFoundException(String.valueOf(name));

    MBeanInfo info = mbean.getMBeanInfo();

    return info;
  }

  /**
   * Returns true if the MBean is an instance of the specified class.
   *
   * @param name the name of the mbean
   * @param className the className to test.
   *
   * @return true if the bean is an instance
   */
  public boolean isInstanceOf(ObjectName name, String className)
    throws InstanceNotFoundException
  {
    MBeanWrapper mbean = getMBean(name);

    if (mbean == null)
      throw new InstanceNotFoundException(String.valueOf(name));

    Object obj = mbean.getObject();
    Class cl = obj.getClass();

    return isInstanceOf(cl, className);
  }

  private boolean isInstanceOf(Class cl, String className)
  {
    if (cl == null)
      return false;

    if (cl.getName().equals(className))
      return true;

    if (isInstanceOf(cl.getSuperclass(), className))
      return true;

    Class []ifs = cl.getInterfaces();
    for (int i = 0; i < ifs.length; i++) {
      if (isInstanceOf(ifs[i], className))
        return true;
    }

    return false;
  }

  /**
   * Returns the ClassLoader that was used for loading the MBean.
   *
   * @param mbeanName the name of the mbean
   *
   * @return the class loader
   *
   * @since JMX 1.2
   */
  public ClassLoader getClassLoaderFor(ObjectName name)
    throws InstanceNotFoundException
  {
    MBeanWrapper mbean = getMBean(name);

    if (mbean == null)
      throw new InstanceNotFoundException(String.valueOf(name));

    return mbean.getContext().getClassLoader();
  }

  /**
   * Returns the named ClassLoader.
   *
   * @param loaderName the name of the class loader
   *
   * @return the class loader
   *
   * @since JMX 1.2
   */
  public ClassLoader getClassLoader(ObjectName loaderName)
    throws InstanceNotFoundException
  {
    return null;
  }

  /**
   * Returns the ClassLoaderRepository for this MBeanServer
   *
   * @since JMX 1.2
   */
  public ClassLoaderRepository getClassLoaderRepository()
  {
    return createContext().getClassLoaderRepository();
  }

  /**
   * Deserializes a byte array in the class loader of the mbean.
   *
   * @param name the name of the mbean
   * @param data the data to deserialize
   *
   * @return the deserialization stream
   */
  public ObjectInputStream deserialize(ObjectName name, byte []data)
    throws InstanceNotFoundException, OperationsException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Deserializes a byte array in the class loader of the mbean.
   *
   * @param className the className matches to the loader
   * @param data the data to deserialize
   *
   * @return the deserialization stream
   */
  public ObjectInputStream deserialize(String className, byte []data)
    throws OperationsException, ReflectionException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Deserializes a byte array in the class loader of the mbean.
   *
   * @param className the className matches to the loader
   * @param loaderName the loader to use for deserialization
   * @param data the data to deserialize
   *
   * @return the deserialization stream
   */
  public ObjectInputStream deserialize(String className,
                                       ObjectName loaderName,
                                       byte []data)
    throws OperationsException, ReflectionException,
           InstanceNotFoundException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the domains for all registered MBeans
   *
   * @since JMX 1.2
   */
  public String []getDomains()
  {
    return getView().getDomains();
  }

  /**
   * Finds the MBean implementation.
   */
  MBeanWrapper getMBean(ObjectName name)
  {
    return getView().getMBean(name);
  }

  /**
   * Handles the case where a class loader is dropped.
   */
  public void destroy()
  {
    try {
      MBeanServerFactory.releaseMBeanServer(this);
    } catch (IllegalArgumentException e) {
      log.log(Level.FINEST, e.toString(), e);
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  /**
   * Returns the string form.
   */
  @Override
  public String toString()
  {
    if (_defaultDomain != null)
      return "MBeanServerImpl[domain=" + _defaultDomain + "]";
    else
      return "MBeanServerImpl[]";
  }

  static {
    try {
      SERVER_DELEGATE_NAME = new ObjectName("JMImplementation:type=MBeanServerDelegate");
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
}
