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

package com.caucho.jmx.remote;

import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.jmx.JMXSerializerFactory;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.vfs.IOExceptionWrapper;

import javax.management.*;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Static convenience methods.
 */
public class HessianMBeanServerConnection
  implements MBeanServerConnection
{
  private static final L10N L = new L10N(HessianMBeanServerConnection.class);
  private static final Logger log = Log.open(HessianMBeanServerConnection.class);

  private String _url;
  private RemoteJMX _jmxProxy;

  /**
   * Creates the MBeanClient.
   */
  public HessianMBeanServerConnection()
  {
  }

  /**
   * Creates the MBeanClient.
   */
  public HessianMBeanServerConnection(String url)
  {
    _url = url;
  }

  /**
   * Sets the proxy
   */
  public void setProxy(RemoteJMX proxy)
  {
    _jmxProxy = proxy;
  }

  /**
   * Returns the mbean info
   */
  public MBeanInfo getMBeanInfo(ObjectName objectName)
    throws InstanceNotFoundException, IntrospectionException,
           ReflectionException, IOException
  {
    try {
      return getProxy().getMBeanInfo(objectName.getCanonicalName());
    }
    catch (JMException ex) {
      if (ex instanceof InstanceNotFoundException)
        throw (InstanceNotFoundException) ex;

      if (ex instanceof IntrospectionException)
        throw (IntrospectionException) ex;

      if (ex instanceof ReflectionException)
        throw (ReflectionException) ex;

      throw new RuntimeException(ex);
    }
  }

  public boolean isInstanceOf(ObjectName name, String className)
    throws InstanceNotFoundException, IOException
  {

    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");

    return false;
  }

  /**
   * Gets an attribute.
   */
  public Object getAttribute(ObjectName objectName, String attrName)
    throws MBeanException, AttributeNotFoundException,
           InstanceNotFoundException, ReflectionException,
           IOException
  {
    try {
      return getProxy().getAttribute(objectName.getCanonicalName(), attrName);
    }
    catch (JMException ex) {
      if (ex instanceof MBeanException)
        throw (MBeanException) ex;

      if (ex instanceof AttributeNotFoundException)
        throw (AttributeNotFoundException) ex;

      if (ex instanceof InstanceNotFoundException)
        throw (InstanceNotFoundException) ex;

      if (ex instanceof ReflectionException)
        throw (ReflectionException) ex;

      throw new RuntimeException(ex);
    }
  }

  public AttributeList getAttributes(ObjectName name, String[] attributes)
    throws InstanceNotFoundException, ReflectionException, IOException
  {

    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");

    return null;
  }

  public void setAttribute(ObjectName name, Attribute attribute)
    throws
    InstanceNotFoundException,
    AttributeNotFoundException,
    InvalidAttributeValueException,
    MBeanException,
    ReflectionException,
    IOException
  {

    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");


  }

  public AttributeList setAttributes(ObjectName name, AttributeList attributes)
    throws InstanceNotFoundException, ReflectionException, IOException
  {

    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");

    return null;
  }

  public Object invoke(ObjectName name,
                       String operationName,
                       Object params[],
                       String signature[])
    throws
    InstanceNotFoundException,
    MBeanException,
    ReflectionException,
    IOException
  {

    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");

    return null;
  }

  public String getDefaultDomain()
    throws IOException
  {

    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");

    return null;
  }

  public String[] getDomains()
    throws IOException
  {

    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");

    return new String[0];
  }

  public void addNotificationListener(ObjectName name,
                                      NotificationListener listener,
                                      NotificationFilter filter,
                                      Object handback)
    throws InstanceNotFoundException, IOException
  {

    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");


  }

  public void addNotificationListener(ObjectName name,
                                      ObjectName listener,
                                      NotificationFilter filter,
                                      Object handback)
    throws InstanceNotFoundException, IOException
  {

    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");


  }

  public void removeNotificationListener(ObjectName name, ObjectName listener)
    throws InstanceNotFoundException, ListenerNotFoundException, IOException
  {

    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");


  }

  public void removeNotificationListener(ObjectName name,
                                         ObjectName listener,
                                         NotificationFilter filter,
                                         Object handback)
    throws InstanceNotFoundException, ListenerNotFoundException, IOException
  {

    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");


  }

  public void removeNotificationListener(ObjectName name,
                                         NotificationListener listener)
    throws InstanceNotFoundException, ListenerNotFoundException, IOException
  {

    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");


  }

  public void removeNotificationListener(ObjectName name,
                                         NotificationListener listener,
                                         NotificationFilter filter,
                                         Object handback)
    throws InstanceNotFoundException, ListenerNotFoundException, IOException
  {

    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");


  }

  public ObjectInstance createMBean(String className, ObjectName name)
    throws
    ReflectionException,
    InstanceAlreadyExistsException,
    MBeanRegistrationException,
    MBeanException,
    NotCompliantMBeanException,
    IOException
  {
    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");

    return null;
  }

  public ObjectInstance createMBean(String className,
                                    ObjectName name,
                                    ObjectName loaderName)
    throws
    ReflectionException,
    InstanceAlreadyExistsException,
    MBeanRegistrationException,
    MBeanException,
    NotCompliantMBeanException,
    InstanceNotFoundException,
    IOException
  {

    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");

    return null;
  }

  public ObjectInstance createMBean(String className,
                                    ObjectName name,
                                    Object params[],
                                    String signature[])
    throws
    ReflectionException,
    InstanceAlreadyExistsException,
    MBeanRegistrationException,
    MBeanException,
    NotCompliantMBeanException,
    IOException
  {

    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");

    return null;
  }

  public ObjectInstance createMBean(String className,
                                    ObjectName name,
                                    ObjectName loaderName,
                                    Object params[],
                                    String signature[])
    throws
    ReflectionException,
    InstanceAlreadyExistsException,
    MBeanRegistrationException,
    MBeanException,
    NotCompliantMBeanException,
    InstanceNotFoundException,
    IOException
  {

    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");

    return null;
  }

  public void unregisterMBean(ObjectName name)
    throws InstanceNotFoundException, MBeanRegistrationException, IOException
  {

    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");


  }

  /**
   * Returns the object instance
   */
  public ObjectInstance getObjectInstance(ObjectName objectName)
    throws InstanceNotFoundException, IOException
  {
    MBeanInfo info = null;

    try {
      info = getMBeanInfo(objectName);
    }
    catch (IntrospectionException e) {
      throw new IOExceptionWrapper(e);
    }
    catch (ReflectionException e) {
      throw new IOExceptionWrapper(e);
    }

    String className = info.getClassName();

    return new ObjectInstance(objectName, className);
  }

  public Set queryMBeans(ObjectName name, QueryExp query)
    throws IOException
  {

    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");

    return null;
  }

  public Set queryNames(ObjectName name, QueryExp query)
    throws IOException
  {

    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");

    return null;
  }

  public boolean isRegistered(ObjectName name)
    throws IOException
  {
    return true;
  }

  public Integer getMBeanCount()
    throws IOException
  {

    // XXX: unimplemented
    if (true) throw new UnsupportedOperationException("unimplemented");

    return null;
  }

  private RemoteJMX getProxy()
  {
    if (_jmxProxy == null) {
      try {
        HessianProxyFactory proxy = new HessianProxyFactory();
        proxy.getSerializerFactory().addFactory(new JMXSerializerFactory());
        _jmxProxy = (RemoteJMX) proxy.create(_url);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    return _jmxProxy;
  }
}

