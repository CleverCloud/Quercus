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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config.gen;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;

import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.L10N;

public class CandiInvocationContext implements InvocationContext {
  private static final L10N L = new L10N(CandiInvocationContext.class);
  private static final Logger log
    = Logger.getLogger(CandiInvocationContext.class.getName());
  
  private static final EnvironmentLocal<Map<String,Object>> _contextDataLocal
  = new EnvironmentLocal<Map<String,Object>>();
  
  private final Object _target;
  private final Method _apiMethod;
  private final Method _implMethod;

  private final Interceptor []_chainMethods;
  private final Object []_chainObjects;
  private final int []_chainIndex;
  
  private final InterceptionType _type;

  private Object []_param;

  private int _index;
  private HashMap<String,Object> _map;

  public CandiInvocationContext(InterceptionType type,
                                Object target,
                                Method apiMethod,
                                Method implMethod,
                                Interceptor []chainMethods,
                                Object []chainObjects,
                                int []chainIndex,
                                Object []param)
  {
    _target = target;
    _type = type;
    
    _apiMethod = apiMethod;
    _implMethod = implMethod;
    _chainMethods = chainMethods;
    _chainObjects = chainObjects;
    _chainIndex = chainIndex;
    _param = param;
  }

  @Override
  public Object getTarget()
  {
    return _target;
  }

  @Override
  public Method getMethod()
  {
    return _apiMethod;
  }

  @Override
  public Object getTimer()
  {
    return null;
  }

  @Override
  public Object[] getParameters()
    throws IllegalStateException
  {
    return _param;
  }

  @Override
  public void setParameters(Object[] parameters)
    throws IllegalStateException
  {
    Class<?> []paramType = _apiMethod.getParameterTypes();
    
    if (parameters != null
        && paramType.length != parameters.length) {
      throw new IllegalArgumentException(L.l("{0}.{1}: interception parameters '{2}' do not match the expected '{3}'",
                                          _apiMethod.getDeclaringClass().getName(),
                                          _apiMethod.getName(),
                                          parameters.length,
                                          paramType.length));
    }
    
    _param = parameters;
  }
  
  public static Map<String,Object> getCurrentContextData()
  {
    Map<String,Object> contextData = _contextDataLocal.get();
    
    if (contextData == null) {
      contextData = new HashMap<String, Object>();
      
      _contextDataLocal.set(contextData);
    }

    return contextData;
  }

  @Override
  public Map<String, Object> getContextData()
  {
    return getCurrentContextData();
  }

  @Override
  public Object proceed()
    throws Exception
  {
    try {
      // ioc/0c57
      if (_chainObjects != null && _index < _chainIndex.length) {
        int i = _index++;
        
        if (_chainObjects[_chainIndex[i]] == null)
          throw new NullPointerException(i + " index[i]=" + _chainIndex[i] + " " + _type + " " + _chainMethods[i]);

        return _chainMethods[i].intercept(_type,
                                          _chainObjects[_chainIndex[i]], 
                                          this);
      }
      else {
        return _implMethod.invoke(_target, _param);
      }
    } catch (InterceptorException e) {
      Throwable cause = e.getCause();

      if (cause instanceof Exception)
        throw (Exception) cause;
      else
        throw e;
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();

      if (cause instanceof Exception)
        throw (Exception) cause;
      else
        throw e;
    }
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _implMethod.getName() + "]";
  }
}
