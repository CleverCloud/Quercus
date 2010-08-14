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
 * @author Emil Ong
 */

package com.caucho.remote.client;

import java.lang.annotation.Annotation;

import com.caucho.config.ConfigException;
import com.caucho.config.Configured;
import com.caucho.config.Names;
import com.caucho.config.cfg.BeanConfig;
import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.DefaultLiteral;
import com.caucho.config.inject.InjectManager;
import com.caucho.util.L10N;

/**
 * Configuration class for a remote client
 */
public class RemoteClient extends BeanConfig
{
  private static final L10N L = new L10N(RemoteClient.class);

  private Class<?> _interface;

  /**
   * Creates a new protocol configuration object.
   */
  public RemoteClient()
  {
    setBeanConfigClass(ProtocolProxyFactory.class);
  }

  /**
   * Sets the proxy interface class.
   */
  public void setInterface(Class<?> type)
  {
    _interface = type;

    if (! type.isInterface())
      throw new ConfigException(L.l("remote-client interface '{0}' must be an interface",
                                    type.getName()));
  }

  @Override
  protected void deploy()
  {
    deployBean(_interface);
  }
  
  private <T> void deployBean(Class<T> iface)
  {
    ProtocolProxyFactory proxyFactory = (ProtocolProxyFactory) getObject();

    Object proxy = proxyFactory.createProxy(iface);

    InjectManager beanManager = InjectManager.create();
    
    BeanBuilder<T> builder = beanManager.createBeanFactory(iface);

    if (getName() != null) {
      builder = builder.name(getName());

      addOptionalStringProperty("name", getName());
      
      builder.qualifier(Names.create(getName()));
      
      if (getBindingList().size() == 0)
        builder.qualifier(DefaultLiteral.DEFAULT);
    }

    for (Annotation binding : getBindingList()) {
      builder = builder.qualifier(binding);
    }

    for (Annotation stereotype : getStereotypeList()) {
      builder = builder.stereotype(stereotype.annotationType());
    }

    builder.stereotype(Configured.class);

    _bean = builder.singleton(proxy);

    beanManager.addBean(_bean);
  }
}

