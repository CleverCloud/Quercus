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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.cfg;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;

/**
 * Proxy for an ejb bean configuration.  This proxy is needed to handle
 * the merging of ejb definitions.
 */
public class EjbMessageConfigProxy extends EjbBeanConfigProxy {
  private static final L10N L = new L10N(EjbBeanConfigProxy.class);

  private EjbMessageBean _message;
  
  /**
   * Creates a new message bean configuration.
   */
  public EjbMessageConfigProxy(EjbConfig config, String ejbModuleName)
  {
    super(config, ejbModuleName);
  }

  /**
   * Initializes and configures the message bean.
   */
  @PostConstruct
  public void init()
  {
    EjbBean oldBean = getConfig().getBeanConfig(getEjbName());

    if (oldBean == null) {
      _message = new EjbMessageBean(getConfig(), getEJBModuleName());
      _message.setEJBName(getEjbName());
      _message.setLocation(getLocation());
    }
    else if (! (oldBean instanceof EjbMessageBean)) {
      throw new ConfigException(L.l("message bean '{0}' conflicts with prior {1} bean at {2}.",
                                    getEjbName(), oldBean.getEJBKind(),
                                    oldBean.getLocation()));
    }
    else
      _message = (EjbMessageBean) oldBean;

    _message.addDependencyList(getDependencyList());

    getBuilderProgram().configure(_message);
  }

  /**
   * Returns the message config.
   */
  public EjbMessageBean getMessage()
  {
    return _message;
  }
}
