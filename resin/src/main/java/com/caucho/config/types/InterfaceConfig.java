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

package com.caucho.config.types;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.spi.Bean;

import com.caucho.config.ConfigException;
import com.caucho.config.cfg.BeanConfig;
import com.caucho.config.inject.InjectManager;
import com.caucho.naming.Jndi;
import com.caucho.util.L10N;

/**
 * Configures an interface type.  Allows class and uri syntax
 */
public class InterfaceConfig extends BeanConfig {
  private static final L10N L = new L10N(InterfaceConfig.class);

  private boolean _isDeploy;
  private boolean _isFactory = true;

  private String _tagName = "bean";

  private String _valueName;
  private Object _value;
  
  public InterfaceConfig()
  {
  }
  
  public InterfaceConfig(Class<?> type)
  {
    setBeanConfigClass(type);
  }
  
  public InterfaceConfig(Class<?> type, String tagName)
  {
    setBeanConfigClass(type);
    setTagName(tagName);
  }

  @Override
  protected String getDefaultScope()
  {
    return null;
  }

  /**
   * Override the old meaning of type for backward compat.
   */
  @Override
  public void setType(Class<?> cl)
  {
    setClass(cl);
  }

  /**
   * Check for correct type.
   */
  @Override
  public void setClass(Class<?> cl)
  {
    super.setClass(cl);

    if (! getBeanConfigClass().isAssignableFrom(cl))
      throw new ConfigException(L.l("instance class '{0}' must implement '{1}'",
                                    cl.getName(), getBeanConfigClass().getName()));
  }

  /**
   * Sets the default deploy value
   */
  public void setDeploy(boolean isDeploy)
  {
    _isDeploy = isDeploy;
    if (_isDeploy)
      setScope("singleton");
  }

  /**
   * Sets the default factory value
   */
  public void setFactory(boolean isFactory)
  {
    _isFactory = isFactory;
  }

  /**
   * Sets the tag name.
   */
  public void setTagName(String tagName)
  {
    _tagName = tagName;
  }

  /**
   * Sets the tag name.
   */
  @Override
  public String getTagName()
  {
    return _tagName;
  }

  /**
   * If the name is set, the bean will get deployed
   */
  @Override
  public void setName(String name)
  {
    super.setName(name);

    setDeploy(true);
  }

  /**
   * If the name is set, the bean will get deployed
   */
  @Override
  public void setJndiName(String name)
  {
    super.setJndiName(name);

    setDeploy(true);
  }

  /**
   * Sets the value for old-style jndi lookup
   */
  public void setValue(Object value)
  {
    if (value instanceof String)
      _valueName = (String) value;
    else
      _value = value;
  }

  /**
   * Override init to handle value
   */
  @Override
  @PostConstruct
  public void init()
  {
    if (_valueName != null) {
      InjectManager webBeans = InjectManager.create();

      Set<Bean<?>> beans = webBeans.getBeans(_valueName);

      if (beans.size() > 0) {
        _bean = beans.iterator().next();
      }

      if (_bean == null) {
        _value = Jndi.lookup(_valueName);
      }

      if (_bean == null && _value == null)
        throw new ConfigException(L.l("'{0}' is an unknown bean",
                                      _valueName));
    }
    else if (getClassType() != null)
      super.init();
    else {
      // ioc/2130
    }
  }
  

  @Override
  public void deploy()
  {
    if (_isDeploy)
      super.deploy();
  }

  public Object getObject()
  {
    if (_value != null)
      return _value;
    else if (getClassType() != null)
      return super.getObject();
    else if (getBeanConfigClass().isAssignableFrom(String.class))
      return _valueName;
    else
      return null;
  }

  public Object getObjectNoInit()
  {
    if (_value != null)
      return _value;
    else if (getClassType() != null)
      return super.createObjectNoInit();
    else if (getBeanConfigClass().isAssignableFrom(String.class))
      return _valueName;
    else
      return null;
  }

  /**
   * Returns the configured object for configuration
   */
  public Object replaceObject()
  {
    if (_isFactory)
      return getObject();
    else
      return this;
  }

  /**
   * Returns the configured object for configuration
   */
  public Object replaceObjectNoInit()
  {
    if (_isFactory)
      return getObjectNoInit();
    else
      return this;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getBeanConfigClass().getName() + "]";
  }
}

