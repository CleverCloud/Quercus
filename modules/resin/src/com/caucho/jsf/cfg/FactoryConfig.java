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

package com.caucho.jsf.cfg;

import com.caucho.config.program.ConfigProgram;
import java.util.*;
import java.lang.reflect.*;

import javax.annotation.*;
import javax.el.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.convert.*;
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.lifecycle.*;
import javax.faces.render.*;
import javax.faces.validator.*;

import com.caucho.config.*;
import com.caucho.util.*;

public class FactoryConfig
{
  private static final L10N L = new L10N(FactoryConfig.class);
  
  private String _id;

  private Class _applicationFactory;
  
  private Class _facesContextFactory;
  
  private Class _lifecycleFactory;
  
  private Class _renderKitFactory;

  public void setId(String id)
  {
    _id = id;
  }

  public void setApplicationFactory(Class factory)
    throws ConfigException
  {
    if (! ApplicationFactory.class.isAssignableFrom(factory))
      throw new ConfigException(L.l("application-factory '{0}' class must extend ApplicationFactory.",
                                    factory.getName()));

    if (! hasConstructor(factory, ApplicationFactory.class))
      throw new ConfigException(L.l("application-factory class '{0}' must either have an (ApplicationFactory) constructor or a null-arg constructor.",
                                    factory.getName()));

    _applicationFactory = factory;
  }
  
  private Class getApplicationFactory()
    throws ConfigException
  {
    return _applicationFactory;
  }

  public void setFacesContextFactory(Class factory)
    throws ConfigException
  {
    if (! FacesContextFactory.class.isAssignableFrom(factory))
      throw new ConfigException(L.l("faces-context-factory '{0}' class must extend FacesContextFactory.",
                                    factory.getName()));

    if (! hasConstructor(factory, FacesContextFactory.class))
      throw new ConfigException(L.l("faces-context-factory class '{0}' must either have an (FacesContextFactory) constructor or a null-arg constructor.",
                                    factory.getName()));

    _facesContextFactory = factory;
  }
  
  private Class getFacesContextFactory()
    throws ConfigException
  {
    return _facesContextFactory;
  }

  public void setLifecycleFactory(Class factory)
    throws ConfigException
  {
    if (! LifecycleFactory.class.isAssignableFrom(factory))
      throw new ConfigException(L.l("lifecycle-factory '{0}' class must extend LifecycleFactory.",
                                    factory.getName()));

    if (! hasConstructor(factory, LifecycleFactory.class))
      throw new ConfigException(L.l("lifecycle-factory class '{0}' must either have an (LifecycleFactory) constructor or a null-arg constructor.",
                                    factory.getName()));

    _lifecycleFactory = factory;
  }
  
  private Class getLifecycleFactory()
    throws ConfigException
  {
    return _lifecycleFactory;
  }

  public void setRenderKitFactory(Class factory)
    throws ConfigException
  {
    if (! RenderKitFactory.class.isAssignableFrom(factory))
      throw new ConfigException(L.l("render-kit-factory '{0}' class must extend RenderKitFactory.",
                                    factory.getName()));

    if (! hasConstructor(factory, RenderKitFactory.class))
      throw new ConfigException(L.l("render-kit-factory class '{0}' must either have an (RenderKitFactory) constructor or a null-arg constructor.",
                                    factory.getName()));

    _renderKitFactory = factory;
  }

  private Class getRenderKitFactory()
    throws ConfigException
  {
    return _renderKitFactory;
  }

  public void setFactoryExtension(ConfigProgram program)
    throws ConfigException
  {
  }

  void init()
  {
    try {
      if (_applicationFactory != null) {
        FactoryFinder.setFactory(FactoryFinder.APPLICATION_FACTORY,
                                 _applicationFactory.getName());

      }
      
      if (_facesContextFactory != null) {
        FactoryFinder.setFactory(FactoryFinder.FACES_CONTEXT_FACTORY,
                                 _facesContextFactory.getName());

      }
      
      if (_lifecycleFactory != null) {
        FactoryFinder.setFactory(FactoryFinder.LIFECYCLE_FACTORY,
                                 _lifecycleFactory.getName());

      }
      
      if (_renderKitFactory != null) {
        FactoryFinder.setFactory(FactoryFinder.RENDER_KIT_FACTORY,
                                 _renderKitFactory.getName());

      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  private boolean hasConstructor(Class factoryClass, Class api)
  {
    try {
      Constructor ctor = factoryClass.getConstructor(api);

      if (ctor != null)
        return true;
    } catch (NoSuchMethodException e) {
    }
    
    try {
      Constructor ctor = factoryClass.getConstructor();

      if (ctor != null)
        return true;
    } catch (NoSuchMethodException e) {
    }

    return false;
  }
}
