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

package com.caucho.server.webapp;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.types.DescriptionGroupConfig;
import com.caucho.util.L10N;

/**
 * Configuration for the listener
 */
@Configurable
public class ListenerConfig<T> extends DescriptionGroupConfig {
  static L10N L = new L10N(ListenerConfig.class);

  // The listener class
  private Class<T> _listenerClass;

  // The listener object
  private T _object;

  private InjectionTarget<T> _target;

  private ContainerProgram _init;

  /**
   * Sets the listener class.
   */
  public void setListenerClass(Class<T> cl)
    throws ConfigException
  {
    Config.checkCanInstantiate(cl);

    if (ServletContextListener.class.isAssignableFrom(cl)) {
    }
    else if (ServletContextAttributeListener.class.isAssignableFrom(cl)) {
    }
    else if (ServletRequestListener.class.isAssignableFrom(cl)) {
    }
    else if (ServletRequestAttributeListener.class.isAssignableFrom(cl)) {
    }
    else if (HttpSessionListener.class.isAssignableFrom(cl)) {
    }
    else if (HttpSessionAttributeListener.class.isAssignableFrom(cl)) {
    }
    else if (HttpSessionActivationListener.class.isAssignableFrom(cl)) {
    }
    else
      throw new ConfigException(L.l("listener-class '{0}' does not implement any web-app listener interface.",
                                    cl.getName()));

    _listenerClass = cl;
  }

  /**
   * Gets the listener class.
   */
  public Class<?> getListenerClass()
  {
    return _listenerClass;
  }

  /**
   * Sets the init block
   */
  public void setInit(ContainerProgram init)
  {
    _init = init;
  }

  /**
   * Gets the init block
   */
  public ContainerProgram getInit()
  {
    return _init;
  }

  /**
   * Initialize.
   */
  public Object createListenerObject()
    throws Exception
  {
    if (_object != null)
      return _object;

    InjectManager cdiManager = InjectManager.create();
    _target = cdiManager.createInjectionTarget(_listenerClass);

    CreationalContext<T> env = cdiManager.createCreationalContext(null);

    _object = _target.produce(env);
    _target.inject(_object, env);

    if (_init != null) {
      // jsp/18n2
      _init.configure(_object);

      _init.init(_object);
    }

    _target.postConstruct(_object);

    return _object;
  }

  public void destroy()
  {
    if (_target != null)
      _target.preDestroy(_object);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _listenerClass + "]";
  }
}
