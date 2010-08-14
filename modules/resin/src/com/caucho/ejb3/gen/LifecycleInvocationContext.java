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

package com.caucho.ejb3.gen;

import java.lang.reflect.*;
import java.util.*;
import javax.interceptor.*;

public class LifecycleInvocationContext implements InvocationContext {
  private static final Object []NULL_OBJ = new Object[0];
  
  private final Object _target;
  
  private final Method []_chainMethods;
  private final Object []_chainObjects;
  
  private Object []_param = NULL_OBJ;

  private int _index;
  private HashMap<String,Object> _map;

  public LifecycleInvocationContext(Object target,
                                    Method []chainMethods,
                                    Object []chainObjects)
  {
    _target = target;
    _chainMethods = chainMethods;
    _chainObjects = chainObjects;
  }
  
  public Object getTarget()
  {
    return _target;
  }
  
  public Object getTimer()
  {
    return null;
  }

  public Method getMethod()
  {
    return null;
  }

  public Object[] getParameters()
    throws IllegalStateException
  {
    return _param;
  }

  public void setParameters(Object[] parameters)
    throws IllegalStateException
  {
    _param = parameters;
  }

  public Map<String, Object> getContextData()
  {
    if (_map == null)
      _map = new HashMap<String,Object>();

    return _map;
  }
      
  public Object proceed()
    throws Exception
  {
    try {
      if (_index < _chainMethods.length) {
        int i = _index++;

        return _chainMethods[i].invoke(_chainObjects[i], this);
      }
      else
        return null;
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();

      if (cause instanceof Exception)
        throw (Exception) cause;
      else
        throw e;
    }
  }
}
