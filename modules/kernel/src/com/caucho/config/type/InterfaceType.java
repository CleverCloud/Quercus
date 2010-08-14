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

package com.caucho.config.type;

import com.caucho.config.*;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.types.*;
import com.caucho.naming.*;
import com.caucho.util.*;
import com.caucho.xml.QName;

import java.util.Set;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

/**
 * Represents an interface.  The interface will try to lookup the
 * value in webbeans.
 */
public class InterfaceType extends ConfigType
{
  private static final L10N L = new L10N(InterfaceType.class);

  private final Class _type;

  /**
   * Create the interface type
   */
  public InterfaceType(Class type)
  {
    _type = type;
  }

  /**
   * Returns the Java type.
   */
  public Class getType()
  {
    return _type;
  }

  /**
   * Returns an InterfaceConfig object
   */
  @Override
  public Object create(Object parent, QName name)
  {
    InterfaceConfig cfg = new InterfaceConfig(_type, _type.getSimpleName());

    return cfg;
  }

  /**
   * Replace the type with the generated object
   */
  public void init(Object bean)
  {
    if (bean instanceof InterfaceConfig)
      ((InterfaceConfig) bean).init();
    else
      super.init(bean);
  }

  /**
   * Replace the type with the generated object
   */
  public Object replaceObject(Object bean)
  {
    if (bean instanceof InterfaceConfig)
      return ((InterfaceConfig) bean).replaceObject();
    else
      return bean;
  }

  /**
   * Converts the string to a value of the type.
   */
  @Override
  public Object valueOf(String text)
  {
    if (text == null)
      return null;

    InjectManager beanManager = InjectManager.create();

    Object value;

    Set<Bean<?>> beans;

    if (! text.equals(""))
      beans = beanManager.getBeans(_type, Names.create(text));
    else
      beans = beanManager.getBeans(_type);

    if (beans.iterator().hasNext()) {
      Bean bean = beanManager.resolve(beans);

      CreationalContext<?> env = beanManager.createCreationalContext(bean);

      value = beanManager.getReference(bean, _type, env);

      return value;
    }

    value = Jndi.lookup(text);

    if (value != null)
      return value;

    throw new ConfigException(L.l("{0}: '{1}' is an unknown bean.",
                                  _type.getName(), text));
  }

  /**
   * Converts the value to a value of the type.
   */
  public Object valueOf(Object value)
  {
    if (value == null)
      return null;
    else if (value instanceof String)
      return valueOf((String) value);
    else if (_type.isAssignableFrom(value.getClass()))
      return value;
    else
      throw new ConfigException(L.l("{0}: '{1}' is an invalid value.",
                                    _type.getName(), value));
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _type.getName() + "]";
  }
}
