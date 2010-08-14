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

package com.caucho.server.webbeans;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;

import com.caucho.config.ConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.inject.Module;
import com.caucho.util.L10N;

/**
 * Jndi proxy class for injection.
 */
@Module
public class BeanMBean<T> implements DynamicMBean {
  private static final L10N L = new L10N(BeanMBean.class);
  private static final Logger log = Logger.getLogger(BeanMBean.class.getName());
  
  private InjectManager _injectManager;
  private Bean<T> _bean;
  private MBeanInfo _info;
  private HashMap<String,Method> _attrMap
    = new HashMap<String,Method>();
  
  BeanMBean(InjectManager manager, Bean<T> bean, AnnotatedType<T> type)
  {
    try {
      _injectManager = manager;
      _bean = bean;
      MBeanAttributeInfo []attrInfo;
    
      attrInfo = parseMethods(_bean, type);
    
      _info = new MBeanInfo(_bean.getBeanClass().getName(),
                            null,
                            attrInfo,
                            new MBeanConstructorInfo[0],
                            new MBeanOperationInfo[0],
                            new MBeanNotificationInfo[0]);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  
  private MBeanAttributeInfo []parseMethods(Bean<T> bean, AnnotatedType<T> type)
    throws IntrospectionException
  {
    ArrayList<MBeanAttributeInfo> list = new ArrayList<MBeanAttributeInfo>();
    
    // for (AnnotatedMethod<? super T> annMethod : type.getMethods()) {
    for (Method method : bean.getBeanClass().getMethods()) {
      String name = method.getName();
      
      if (method.getParameterTypes().length == 0
          && Modifier.isPublic(method.getModifiers())
          && name.startsWith("get")) {
        name = name.substring(3);
        
        MBeanAttributeInfo attr;
        
        attr = new MBeanAttributeInfo(name,
                                      null, 
                                      method,
                                      null);
        
        list.add(attr);
        
        _attrMap.put(name, method);
      }
    }
    
    MBeanAttributeInfo []info = new MBeanAttributeInfo[list.size()];
    list.toArray(info);
    
    return info;
  }

  @Override
  public Object getAttribute(String attribute)
    throws AttributeNotFoundException,
           MBeanException,
           ReflectionException
  {
    Method method = _attrMap.get(attribute);
    
    if (method == null)
      throw new AttributeNotFoundException(L.l("'{0}' is an unknown attribute in {1}",
                                               attribute, this));
    
    CreationalContext<T> env = _injectManager.createCreationalContext(_bean);
    Object value = _injectManager.getReference(_bean, _bean.getBeanClass(), env);
    
    try {
      return method.invoke(value);
    } catch (IllegalAccessException e) {
      throw new ReflectionException(e);
    } catch (InvocationTargetException e) {
      throw new ReflectionException((Exception) e.getCause());
    }
  }

  @Override
  public AttributeList getAttributes(String[] attributes)
  {
    AttributeList list = new AttributeList();
    
    for (String attr : attributes) {
      try {
        list.add(getAttribute(attr));
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
    
    return list;
  }

  @Override
  public MBeanInfo getMBeanInfo()
  {
    return _info;
  }

  @Override
  public Object invoke(String actionName, Object[] params, String[] signature)
    throws MBeanException,
           ReflectionException
  {
    return null;
  }

  @Override
  public void setAttribute(Attribute attribute)
    throws AttributeNotFoundException,
           InvalidAttributeValueException,
           MBeanException,
           ReflectionException
  {
  }

  @Override
  public AttributeList setAttributes(AttributeList attributes)
  {
    return null;
  }
}
