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

import javax.enterprise.context.spi.CreationalContext;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.inject.OwnerCreationalContext;
import com.caucho.config.type.ConfigType;
import com.caucho.config.xml.XmlConfigContext;
import com.caucho.xml.QName;

/**
 * A saved program for configuring an object.
 */
public abstract class ConfigProgram implements Comparable<ConfigProgram> {
  /**
   * Returns the program's QName
   */
  public QName getQName()
  {
    return null;
  }
  
  public int getPriority()
  {
    return 0;
  }
  
  /**
   * Returns the declaring class.
   */
  public Class<?> getDeclaringClass()
  {
    return getClass();
  }
  
  /**
   * Returns the name.
   */
  public String getName()
  {
    return getClass().getName();
  }
  
  /**
   * Configures the bean using the current program.
   * 
   * @param bean the bean to configure
   * @param env the Config environment
   */
  abstract public <T> void inject(T bean, 
                                  CreationalContext<T> createContext);

  /**
   * Binds the injection point
   */
  public void bind()
  {
  }

  public void addProgram(ConfigProgram program)
  {
    throw new UnsupportedOperationException("Cannot add a program to a BuilderProgram. You probably need a BuilderProgramContainer.");
  }

  /**
   * Configures the object.
   */
  final
  public void configure(Object bean)
    throws ConfigException
  {
    // ioc/23e7
    inject(bean, new OwnerCreationalContext(null));
  }

  final
  public <T> T configure(Class<T> type)
    throws ConfigException
  {
    return configure(type, XmlConfigContext.create());
  }


  /**
   * Configures a bean given a class to instantiate.
   */
  final
  protected <T> T configure(Class<T> type, XmlConfigContext env)
    throws ConfigException
  {
    try {
      T value = type.newInstance();

      inject(value, new OwnerCreationalContext<T>(null));

      return value;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Configures a bean given a class to instantiate.
   */
  final
  protected <T> T create(Class<T> type, CreationalContext<T> env)
    throws ConfigException
  {
    try {
      T value = type.newInstance();

      inject(value, env);

      return value;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  final
  public <T> T create(ConfigType<T> type)
    throws ConfigException
  {
    return create(type, new OwnerCreationalContext<T>(null));
  }

  public <T> T create(ConfigType<T> type, CreationalContext<T> env)
    throws ConfigException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void init(Object bean)
    throws ConfigException
  {
    Config.init(bean);
  }
  
  public int compareTo(ConfigProgram peer)
  {
    // ioc/0119
    int cmp = getPriority() - peer.getPriority();
    
    if (cmp != 0)
      return cmp;

    Class<?> selfClass = getDeclaringClass();
    Class<?> peerClass = peer.getDeclaringClass();
    
    if (selfClass == peerClass) {
      return getName().compareTo(peer.getName());
    }
    else if (selfClass.isAssignableFrom(peerClass))
      return -1;
    else if (peerClass.isAssignableFrom(selfClass))
      return 1;
    else
      return selfClass.getName().compareTo(peerClass.getName());
  }
}
