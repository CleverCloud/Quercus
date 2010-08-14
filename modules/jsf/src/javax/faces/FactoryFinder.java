/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package javax.faces;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

import javax.faces.application.*;
import javax.faces.context.*;
import javax.faces.lifecycle.*;
import javax.faces.render.*;

public final class FactoryFinder
{
  private static final Logger log
    = Logger.getLogger(FactoryFinder.class.getName());

  public static final String APPLICATION_FACTORY
    = "javax.faces.application.ApplicationFactory";
  public static final String FACES_CONTEXT_FACTORY
    = "javax.faces.context.FacesContextFactory";
  public static final String LIFECYCLE_FACTORY
    = "javax.faces.lifecycle.LifecycleFactory";
  public static final String RENDER_KIT_FACTORY
    = "javax.faces.render.RenderKitFactory";

  private static final HashMap<String,Class> _factoryClassMap
    = new HashMap<String,Class>();

  private static final
    WeakHashMap<ClassLoader,HashMap<String,String>> _factoryNameMap
    = new WeakHashMap<ClassLoader,HashMap<String,String>>();

  private static final
    WeakHashMap<ClassLoader,HashMap<String,Object>> _factoryMap
    = new WeakHashMap<ClassLoader,HashMap<String,Object>>();

  FactoryFinder(){
  }

  public static Object getFactory(String factoryName)
  {
    if (factoryName == null)
      throw new NullPointerException();

    Class factoryClass = _factoryClassMap.get(factoryName);

    if (factoryClass == null)
      throw new IllegalArgumentException(factoryName + " is an unknown JSF factory");

    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    synchronized (_factoryNameMap) {
      HashMap<String,Object> objMap = _factoryMap.get(loader);

      if (objMap != null) {
        Object factory = objMap.get(factoryName);

        if (factory != null)
          return factory;
      }

      String className = null;

      HashMap<String,String> nameMap = _factoryNameMap.get(loader);
      if (nameMap != null) {
        className = nameMap.get(factoryName);
      }

      Object factory = null;
      if (className != null)
        factory = createFactory(className, factoryClass, factory, loader);

      if (factory == null)
        throw new FacesException("No factory found for " + factoryName);

      if (objMap == null) {
        objMap = new HashMap<String,Object>();
        _factoryMap.put(loader, objMap);
      }

      objMap.put(factoryName, factory);

      return factory;
    }
  }

  public static void setFactory(String factoryName, String implName)
  {
    if (log.isLoggable(Level.FINER))
      log.finer("FactoryFinder[] setting '" + factoryName + "' to implementation '" + implName + "'");

    Class factoryClass = _factoryClassMap.get(factoryName);

    if (factoryClass == null)
      throw new IllegalArgumentException(factoryName + " is an unknown JSF factory");

    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    synchronized (_factoryNameMap) {
      HashMap<String,Object> objectMap = _factoryMap.get(loader);

      if (objectMap == null) {
        objectMap = new HashMap<String,Object>();
        _factoryMap.put(loader, objectMap);
      }

      Object oldFactory = objectMap.get(factoryName);

      if (oldFactory == null)
        oldFactory = _factoryMap.get(factoryName);

      HashMap<String,String> map = _factoryNameMap.get(loader);

      if (map == null) {
        map = new HashMap<String,String>();
        _factoryNameMap.put(loader, map);
      }

      map.put(factoryName, implName);

      Object factory = createFactory(implName, factoryClass, oldFactory,
                                     loader);

      if (factory == null)
        throw new FacesException("No factory found for " + factoryName);

      objectMap.put(factoryName, factory);
    }
  }

  public static void releaseFactories()
    throws FacesException
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    synchronized (_factoryNameMap) {
      _factoryMap.remove(loader);
      _factoryNameMap.remove(loader);
    }
  }

  private static Object createFactory(String className,
                                      Class factoryClass,
                                      Object previous,
                                      ClassLoader loader)
    throws FacesException
  {
    if (className == null)
      return previous;

    try {
      Class cl = Class.forName(className, false, loader);

      if (! factoryClass.isAssignableFrom(cl))
        throw new FacesException(className + " is not assignable to " + factoryClass.getName());

      Constructor ctor0 = null;
      Constructor ctor1 = null;

      try {
        ctor0 = cl.getConstructor(new Class[] { });
      } catch (Exception e) {
        log.log(Level.FINEST, e.toString(), e);
      }

      try {
        ctor1 = cl.getConstructor(new Class[] { factoryClass });
      } catch (Exception e) {
        log.log(Level.FINEST, e.toString(), e);
      }

      Object obj;

      if (ctor1 == null)
        obj = cl.newInstance();
      else if (previous != null)
        obj = ctor1.newInstance(previous);
      else if (ctor0 != null)
        obj = cl.newInstance();
      else
        obj = ctor1.newInstance(new Object[1]);

      return obj;
    } catch (ClassNotFoundException e) {
      throw new FacesException(e);
    } catch (InstantiationException e) {
      throw new FacesException(e);
    } catch (InvocationTargetException e) {
      throw new FacesException(e);
    } catch (IllegalAccessException e) {
      throw new FacesException(e);
    }
  }

  static {
    _factoryClassMap.put(APPLICATION_FACTORY, ApplicationFactory.class);
    _factoryClassMap.put(FACES_CONTEXT_FACTORY, FacesContextFactory.class);
    _factoryClassMap.put(LIFECYCLE_FACTORY, LifecycleFactory.class);
    _factoryClassMap.put(RENDER_KIT_FACTORY, RenderKitFactory.class);
  }
}
