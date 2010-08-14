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

import com.caucho.util.L10N;

import javax.management.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Skeleton for an standard MBean.
 */
public class MBeanSkeleton {
  private static L10N L = new L10N(MBeanSkeleton.class);
  
  private Class _mbeanClass;

  private MBeanInfo _mbeanInfo;

  private HashMap<String,Method> _getAttributes = new HashMap<String,Method>();
  private HashMap<String,Method> _setAttributes = new HashMap<String,Method>();
  private Method []_operations;

  /**
   * Creates the skeleton based on the MBean class.
   *
   * @param mbeanClass the _MBean class
   */
  MBeanSkeleton(Class mbeanClass)
    throws IntrospectionException
  {
    _mbeanClass = mbeanClass;

    introspect();
  }

  /**
   * Returns the mbean class.
   */
  public Class getMBeanClass()
  {
    return _mbeanClass;
  }

  /**
   * Returns the MBeanInfo for the class.
   */
  public MBeanInfo getMBeanInfo()
  {
    return _mbeanInfo;
  }

  /**
   * Returns the attribute value.
   */
  public Object getAttribute(Object object, String name)
    throws AttributeNotFoundException, ReflectionException
  {
    Method method = _getAttributes.get(name);

    if (method != null) {
      try {
        return method.invoke(object, (Object []) null);
      } catch (IllegalAccessException e) {
        throw new ReflectionException(e);
      } catch (InvocationTargetException e) {
        throw new ReflectionException(e);
      }
    }
    else if (name.equals("resin-api"))
      return _mbeanClass.getName();
    else
      throw new AttributeNotFoundException(L.l("MBean class `{0}' has no read attribute `{1}'.", _mbeanClass.getName(), name));

  }

  /**
   * Sets the attribute value.
   */
  public void setAttribute(Object object, String name, Object value)
    throws AttributeNotFoundException, ReflectionException
  {
    Method method = _setAttributes.get(name);

    if (method == null)
      throw new AttributeNotFoundException(L.l("MBean class `{0}' has no write attribute `{1}'.", _mbeanClass.getName(), name));

    try {
      method.invoke(object, new Object[] { value });
    } catch (IllegalAccessException e) {
      throw new ReflectionException(e);
    } catch (InvocationTargetException e) {
      throw new ReflectionException(e);
    }
  }

  /**
   * Invokes an operation.
   */
  public Object invoke(Object object, String name, Object []args, String []sig)
    throws MBeanException, ReflectionException
  {
    Method method = findMethod(name, sig);

    if (method == null)
      throw new MBeanException(null, L.l("MBean class `{0}' has no matching operation `{1}'.", _mbeanClass.getName(), name));

    try {
      return method.invoke(object, args);
    } catch (IllegalAccessException e) {
      throw new ReflectionException(e);
    } catch (InvocationTargetException e) {
      throw new ReflectionException(e);
    }
  }

  private Method findMethod(String name, String []sig)
  {
    loop:
    for (int i = 0; i < _operations.length; i++) {
      Method method = _operations[i];

      if (! method.getName().equals(name))
        continue;

      Class []param = method.getParameterTypes();

      if (param.length != sig.length)
        continue;

      for (int j = 0; j < sig.length; j++)
        if (! param[j].getName().equals(sig[j]))
          continue loop;

      return method;
    }

    return null;
  }

  /**
   * Introspects the class.
   */
  private void introspect()
    throws IntrospectionException
  {
    Method []methods = _mbeanClass.getMethods();

    ArrayList<Method> publicMethods = new ArrayList<Method>();

    ArrayList<MBeanAttributeInfo> attributes;
    attributes = new ArrayList<MBeanAttributeInfo>();
    ArrayList<MBeanOperationInfo> operations;
    operations = new ArrayList<MBeanOperationInfo>();

    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];

      if (! Modifier.isPublic(method.getModifiers()))
        continue;

      if (method.getDeclaringClass().getName().startsWith("javax.management."))
        continue;

      String name = method.getName();
      Class []param = method.getParameterTypes();
      Class retType = method.getReturnType();

      if (name.startsWith("get") && param.length == 0 &&
          ! void.class.equals(retType)) {
        String attributeName = name.substring(3);

        Method setter = getSetter(attributeName, retType);

        _getAttributes.put(attributeName, method);

        attributes.add(new MBeanAttributeInfo(attributeName, attributeName,
                                              method, setter));
      }
      else if (name.startsWith("is") && param.length == 0 &&
               (boolean.class.equals(retType) ||
                Boolean.class.equals(retType))) {
        String attributeName = name.substring(2);

        Method setter = getSetter(attributeName, retType);
        
        attributes.add(new MBeanAttributeInfo(attributeName, attributeName,
                                              method, setter));
                       
        _getAttributes.put(attributeName, method);
      }
      else if (name.startsWith("set") && param.length == 1 &&
               void.class.equals(retType)) {
        String attributeName = name.substring(3);
        
        _setAttributes.put(attributeName, method);
      }
      else {
        operations.add(new MBeanOperationInfo(method.getName(), method));
        publicMethods.add(method);
      }
    }

    _operations = new Method[publicMethods.size()];
    publicMethods.toArray(_operations);

    MBeanAttributeInfo []attrs = new MBeanAttributeInfo[attributes.size()];
    attributes.toArray(attrs);
      
    MBeanConstructorInfo []makers = new MBeanConstructorInfo[0];
    
    MBeanOperationInfo []ops = new MBeanOperationInfo[operations.size()];
    operations.toArray(ops);
    
    MBeanNotificationInfo []notifs = new MBeanNotificationInfo[0];

    _mbeanInfo = new MBeanInfo(_mbeanClass.getName(),
                               _mbeanClass.getName(),
                               attrs, makers, ops, notifs);
                               
  }

  private Method getSetter(String attributeName, Class type)
  {
    String setName = "set" + attributeName;
    
    Method []methods = _mbeanClass.getMethods();

    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];

      if (! Modifier.isPublic(method.getModifiers()))
        continue;

      String name = method.getName();

      if (! setName.equals(name))
        continue;

      Class []param = method.getParameterTypes();
      Class ret = method.getReturnType();

      if (param.length != 1)
        continue;
      
      if (! void.class.equals(ret))
        continue;

      if (param[0].equals(type))
        return method;
      else
        return null; // XXX: s/b error?
    }

    return null;
  }
}
