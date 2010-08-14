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

package com.caucho.hemp.broker;

import com.caucho.config.ConfigException;
import com.caucho.config.cfg.AbstractBeanConfig;
import com.caucho.remote.BamService;
import com.caucho.util.L10N;

/**
 * bam-service configuration
 */
public class BamServiceConfig extends AbstractBeanConfig
{
  private static final L10N L = new L10N(BamServiceConfig.class);
  public BamServiceConfig()
  {
    HempBroker.getCurrent();

    setScope("singleton");
  }

  /*
  @Override
  public Class getBeanConfigClass()
  {
    return Actor.class;
  }
  */

  public void setThreadMax(int threadMax)
  {
  }

  protected void initImpl()
  {
    if (getInstanceClass() == null)
      throw new ConfigException(L.l("ejb-stateful-bean requires a 'class' attribute"));

    final String name = getName();

    add(new BamService() {
        public Class annotationType() { return BamService.class; }
        public int threadMax() { return 5; }
        public String name() { return name; }
      });
  }

  /*
  @PostConstruct
  public void init()
  {
    super.init();

    // XXX: 3.2.0 temp
    com.caucho.loader.Environment.addCloseListener(this);

    start();
  }
  */

  /*
  @PreDestroy
  public void destroy()
  {
    if (_service != null)
      _broker.removeActor(_service);
  }
  */

  /*
  @Start
  public void start()
  {
    if (_service != null)
      return;

    Actor service = (Actor) getObject();

    // XXX: jms/3a14 - needs to be cleaned up
    if (service instanceof SimpleActor) {
      SimpleActor simpleService = (SimpleActor) service;

      simpleService.setBrokerStream(_broker.getBrokerStream());
    }

    String name = getName();
    if (name == null)
      name = service.getClass().getSimpleName();

    String jid = name;
    if (jid.indexOf('@') < 0 && jid.indexOf('/') < 0)
      jid = name + "@" + _broker.getJid();

    service.setJid(jid);

    // queue
    if (_threadMax > 0) {
      service = new MemoryQueueServiceFilter(service,
                                             _broker,
                                             _threadMax);
    }

    _service = service;

    _broker.addActor(service);
  }
  */
}

