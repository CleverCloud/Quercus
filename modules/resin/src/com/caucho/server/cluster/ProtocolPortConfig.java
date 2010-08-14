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

package com.caucho.server.cluster;

import javax.annotation.PostConstruct;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionTarget;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ContainerProgram;
import com.caucho.network.listen.AbstractProtocol;
import com.caucho.network.listen.Protocol;
import com.caucho.network.listen.SocketLinkListener;
import com.caucho.util.L10N;

/**
 * Represents a protocol connection.
 */
public class ProtocolPortConfig extends SocketLinkListener
{
  private static final L10N L = new L10N(ProtocolPortConfig.class);

  // The protocol
  private Class<?> _protocolClass;
  private ContainerProgram _init;
  
  public ProtocolPortConfig()
  {
  }

  /**
   * Sets protocol class.
   */
  public void setType(Class<?> cl)
  {
    setClass(cl);
  }

  /**
   * Sets protocol class.
   */
  public void setClass(Class<?> cl)
  {
    Config.validate(cl, AbstractProtocol.class);

    _protocolClass = cl;
  }

  public void setInit(ContainerProgram init)
  {
    if (_protocolClass == null)
      throw new ConfigException(L.l("<init> requires a protocol class"));

    _init = init;
  }
  
  public void add(Protocol protocol)
  {
    setProtocol(protocol);
  }

  /**
   * Initializes the port.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (getProtocol() != null) {

    }
    else if (_protocolClass != null) {
      InjectManager webBeans = InjectManager.create();

      /*
      Protocol protocol
        = (Protocol) webBeans.createTransientObjectNoInit(_protocolClass);
      */
      InjectionTarget target = webBeans.createInjectionTarget(_protocolClass);
      CreationalContext env = webBeans.createCreationalContext(null);

      AbstractProtocol protocol = (AbstractProtocol) target.produce(env);
      target.inject(protocol, env);

      if (_init != null)
        _init.configure(protocol);

      target.postConstruct(protocol);

      setProtocol(protocol);
    }
    else
      throw new ConfigException(L.l("protocol requires either a class"));
    
    super.init();
  }
}
