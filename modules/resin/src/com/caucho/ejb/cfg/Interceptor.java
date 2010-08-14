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
 * @author Rodrigo Westrupp
 */

package com.caucho.ejb.cfg;

import com.caucho.bytecode.*;
import com.caucho.util.L10N;
import com.caucho.vfs.PersistentDependency;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

/**
 * Configuration for an interceptor.
 */
public class Interceptor {
  private static Logger log
    = Logger.getLogger(Interceptor.class.getName());
  private static final L10N L = new L10N(Interceptor.class);

  private String _interceptorClass;
  private String _aroundInvokeMethodName;

  private AroundInvokeConfig _aroundInvokeConfig;

  private PreDestroyConfig _preDestroyConfig;
  private PostConstructConfig _postConstructConfig;

  private PrePassivateConfig _prePassivateConfig;
  private PostActivateConfig _postActivateConfig;

  private String _postConstructMethodName;

  private ClassLoader _loader;
  protected JClassLoader _jClassLoader;

  private JClass _interceptorJClass;

  ArrayList<PersistentDependency> _dependList =
    new ArrayList<PersistentDependency>();

  public Interceptor()
  {
    _loader = Thread.currentThread().getContextClassLoader();

    _jClassLoader = JClassLoaderWrapper.create(_loader);
  }

  public JClass getInterceptorJClass()
  {
    return _interceptorJClass;
  }

  public String getInterceptorClass()
  {
    return _interceptorClass;
  }

  public void setInterceptorClass(String interceptorClass)
  {
    _interceptorClass = interceptorClass;
  }

  public void init()
  {
    // ejb/0fb5
    if (_aroundInvokeConfig != null)
      _aroundInvokeMethodName = _aroundInvokeConfig.getMethodName();

    // ejb/0fbi
    if (_postConstructConfig != null)
      _postConstructMethodName = _postConstructConfig.getLifecycleCallbackMethod();


    if (_aroundInvokeMethodName == null || _postConstructMethodName == null) {
      // XXX: EnhancerManager getJavaClassLoader()
      // ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
      // JClassLoader jClassLoader = EnhancerManager.create(parentLoader).getJavaClassLoader();

      _interceptorJClass = _jClassLoader.forName(_interceptorClass);

      JClass cl = _interceptorJClass;

      do {
        for (JMethod method : _interceptorJClass.getDeclaredMethods()) {
          if (method.isAnnotationPresent(AroundInvoke.class)) {
            // XXX: throw exception for invalid final or static.

            if (_aroundInvokeMethodName == null)
              _aroundInvokeMethodName = method.getName();
            else if (cl == _interceptorJClass) {
              // XXX: throw exception for invalid duplicated @AroundInvoke methods.
            }
          }

          if (method.isAnnotationPresent(PostConstruct.class)) {
            // XXX: throw exception for invalid final or static.

            if (_postConstructMethodName == null)
              _postConstructMethodName = method.getName();
            else if (cl == _interceptorJClass) {
              // XXX: throw exception for invalid duplicated @PostConstruct methods.
            } else {
              // XXX: needs to call superclass lifecycle callback methods.
            }
          }
        }
      } while ((cl = cl.getSuperClass()) != null);
    }
  }

  public static void makeAccessible(final Method method)
  {
    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction() {
          public Object run()
          {
            method.setAccessible(true);
            return null;
          }
        });
    } catch (PrivilegedActionException e) {
      throw new RuntimeException(e.getException());
    }
  }

  public Method getAroundInvokeMethod()
  {
    return getAroundInvokeMethod(_interceptorJClass.getJavaClass(),
                                 _aroundInvokeMethodName);
  }

  public static Method getAroundInvokeMethod(Class cl,
                                             String methodName)
  {
    // ejb/0fbm
    for (Method method : cl.getDeclaredMethods()) {
      if (method.getName().equals(methodName)) {
        Class paramTypes[] = method.getParameterTypes();

        if (paramTypes.length != 1)
          continue;

        if (! paramTypes[0].equals(InvocationContext.class))
          continue;

        method.setAccessible(true);

        return method;
      }
    }

    return null;
  }

  public String getAroundInvokeMethodName()
  {
    return _aroundInvokeMethodName;
  }

  public String getPostConstructMethodName()
  {
    return _postConstructMethodName;
  }

  public PostActivateConfig getPostActivate()
  {
    return _postActivateConfig;
  }

  public PostConstructConfig getPostConstruct()
  {
    return _postConstructConfig;
  }

  public PreDestroyConfig getPreDestroy()
  {
    return _preDestroyConfig;
  }

  public PrePassivateConfig getPrePassivate()
  {
    return _prePassivateConfig;
  }

  public void setAroundInvoke(AroundInvokeConfig aroundInvoke)
  {
    _aroundInvokeConfig = aroundInvoke;
  }

  public void setPostActivate(PostActivateConfig postActivate)
  {
    _postActivateConfig = postActivate;
  }

  public void setPostConstruct(PostConstructConfig postConstruct)
  {
    _postConstructConfig = postConstruct;
  }

  public void setPreDestroy(PreDestroyConfig preDestroy)
  {
    _preDestroyConfig = preDestroy;
  }

  public void setPrePassivate(PrePassivateConfig prePassivate)
  {
    _prePassivateConfig = prePassivate;
  }

  public String toString()
  {
    return "Interceptor[" + _interceptorClass + "]";
  }
}
