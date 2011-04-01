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

import com.caucho.util.L10N;

import javax.management.*;
import java.util.logging.Logger;

/**
 * Wrapper around the dynamic mbean to handle classloader lifecycle.
 */
class MBeanWrapper implements DynamicMBean {
  private static L10N L = new L10N(MBeanWrapper.class);
  private static Logger log
    = Logger.getLogger(MBeanWrapper.class.getName());
  
  private MBeanContext _context;

  private ObjectName _name;
  
  protected Object _object;
  protected DynamicMBean _mbean;

  private ObjectInstance _instance;
  
  protected MBeanWrapper(MBeanContext context,
                         ObjectName name,
                         Object object,
                         DynamicMBean mbean)
  {
    _context = context;

    _name = name;

    _object = object;
    _mbean = mbean;
  }

  /**
   * Returns the object instance for the mbean.
   */
  public ObjectInstance getObjectInstance()
  {
    if (_instance == null)
      _instance = new ObjectInstance(_name, getMBeanInfo().getClassName());
    
    return _instance;
  }

  /**
   * Returns the context.
   */
  MBeanContext getContext()
  {
    return _context;
  }

  /**
   * Returns the object.
   */
  Object getObject()
  {
    return _object;
  }

  /**
   * Returns the object as a broadcaster.
   */
  private NotificationBroadcaster getBroadcaster()
  {
    return (NotificationBroadcaster) _object;
  }

  /**
   * Returns the object as an emitter.
   */
  private NotificationEmitter getEmitter()
  {
    return (NotificationEmitter) _object;
  }

  /**
   * Returns the name.
   */
  public ObjectName getObjectName()
  {
    return _name;
  }

  /**
   * Returns the class loader.
   */
  public ClassLoader getClassLoader()
  {
    return _context.getClassLoader();
  }

  /**
   * Returns the MBeanInfo meta-data.
   */
  public MBeanInfo getMBeanInfo()
  {
    return _mbean.getMBeanInfo();
  }

  /**
   * Returns the named attribute.
   */
  public Object getAttribute(String name)
    throws ReflectionException, AttributeNotFoundException, MBeanException
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_context.getClassLoader());
      
      return _mbean.getAttribute(name);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  /**
   * Returns an array of attributes.
   */
  public AttributeList getAttributes(String []names)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_context.getClassLoader());
      
      return _mbean.getAttributes(names);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Sets the named attribute.
   */
  public void setAttribute(Attribute attr)
    throws ReflectionException,
           AttributeNotFoundException,
           InvalidAttributeValueException,
           MBeanException
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_context.getClassLoader());
      
      _mbean.setAttribute(attr);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  /**
   * Returns an array of attributes.
   */
  public AttributeList setAttributes(AttributeList list)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_context.getClassLoader());
      
      return _mbean.setAttributes(list);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Invokes the operation.
   */
  public Object invoke(String operation,
                       Object []params,
                       String[]signature)
    throws ReflectionException, MBeanException
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_context.getClassLoader());
      
      return _mbean.invoke(operation, params, signature);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns as listener.
   */
  public NotificationListener getListener()
  {
    Object obj = getObject();
    
    if (obj instanceof NotificationListener)
      return (NotificationListener) obj;
    else
      return null;
  }

  /**
   * Adds a notification listener.
   */
  public void addNotificationListener(NotificationListener listener,
                                      NotificationFilter filter,
                                      Object handback)
  {
    getBroadcaster().addNotificationListener(listener, filter, handback);
  }

  /**
   * Removes a notification listener.
   */
  public void removeNotificationListener(NotificationListener listener)
    throws ListenerNotFoundException
  {
    getBroadcaster().removeNotificationListener(listener);
  }

  /**
   * Removes a notification listener.
   */
  public void removeNotificationListener(NotificationListener listener,
                                         NotificationFilter filter,
                                         Object handback)
    throws ListenerNotFoundException
  {
    Object obj = getObject();

    if (obj instanceof NotificationEmitter)
      getEmitter().removeNotificationListener(listener, filter, handback);
    else
      getBroadcaster().removeNotificationListener(listener);
  }
}
