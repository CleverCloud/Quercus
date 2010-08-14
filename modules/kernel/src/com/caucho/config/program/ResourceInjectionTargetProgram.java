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
 * @author Scott Ferguson;
 */

package com.caucho.config.program;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Hashtable;

import javax.enterprise.context.spi.CreationalContext;
import javax.naming.NamingException;

import com.caucho.config.ConfigException;
import com.caucho.config.types.ResourceGroupConfig;
import com.caucho.naming.ObjectProxy;
import com.caucho.util.L10N;

/**
 * JavaEE resource program
 */
public class ResourceInjectionTargetProgram extends ConfigProgram
  implements ObjectProxy
{
  private static final L10N L = new L10N(ResourceInjectionTargetProgram.class);
  
  private ResourceGroupConfig _resourceConfig;
  
  private Class<?> _targetClass;
  private String _targetName;
  
  private Field _field;
  private Method _method;
  
  public ResourceInjectionTargetProgram(ResourceGroupConfig resourceConfig,
                                        Class<?> targetClass,
                                        String targetName)
  {
    _resourceConfig = resourceConfig;
    
    _targetClass = targetClass;
    _targetName = targetName;
    
    _field = findField(targetClass, targetName);
    _method = findMethod(targetClass, targetName);
    
    if (_method == null && _field == null) {
      throw new ConfigException(L.l("{0}.{1} is an unknown target for {2}",
                                    targetClass.getName(),
                                    targetName,
                                    resourceConfig));
    }
  }
  
  public Class<?> getTargetClass()
  {
    return _targetClass;
  }
  
  public String getTargetName()
  {
    return _targetName;
  }
  
  private Method findMethod(Class<?> targetClass, String targetMethod)
  {
    for (Method method : targetClass.getMethods()) {
      if (method.getName().equals(targetMethod)
          && method.getParameterTypes().length == 1) {
        method.setAccessible(true);
        
        return method;
      }
    }
    
    return null;
  }
  
  private Field findField(Class<?> targetClass, String targetField)
  {
    for (Field field: targetClass.getDeclaredFields()) {
      if (field.getName().equals(targetField)) {
        field.setAccessible(true);

        return field;
      }
    }
    
    return null;
  }
  
  @Override
  public <T> void inject(T bean, CreationalContext<T> env)
  {
    Object value = _resourceConfig.getValue();
    
    if (value == null)
      return;
    
    if (_field != null) {
      try {
        _field.set(bean, value);
      } catch (Exception e) {
        throw new ConfigException(L.l("{0}.{1} cannot be assigned the value {2}",
                                      _field.getDeclaringClass(),
                                      _field.getName(),
                                      value),
                                  e);
      }
    } else {
      try {
        _method.invoke(bean, value);
      } catch (Exception e) {
        throw new ConfigException(L.l("{0}.{1} cannot be assigned the value {2}",
                                      _method.getDeclaringClass(),
                                      _method.getName(),
                                      value),
                                  e);
      }
    }
  }

  @Override
  public Object createObject(Hashtable<?, ?> env) throws NamingException
  {
    Object value = _resourceConfig.getValue();
    
    if (value == null)
      throw new ConfigException(L.l("Null value returned from {0}",
                                    _resourceConfig));
    
    return value;
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _targetClass.getSimpleName()
            + ", " + _targetName
            + ", " + _resourceConfig
            + "]");
  }
}
