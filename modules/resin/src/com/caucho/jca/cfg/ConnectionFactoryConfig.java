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

package com.caucho.jca.cfg;

import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.CurrentLiteral;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.Names;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.cfg.BeanConfig;
import com.caucho.config.types.*;
import com.caucho.jca.*;
import com.caucho.jca.cfg.JavaMailConfig;
import com.caucho.jca.ra.ResourceAdapterController;
import com.caucho.jca.ra.ResourceArchive;
import com.caucho.jca.ra.ResourceArchiveManager;
import com.caucho.jca.ra.ResourceManagerImpl;
import com.caucho.jmx.IntrospectionMBean;
import com.caucho.jmx.Jmx;
import com.caucho.loader.ClassLoaderListener;
import com.caucho.loader.CloseListener;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentListener;
import com.caucho.loader.StartListener;
import com.caucho.naming.Jndi;
import com.caucho.transaction.ConnectionPool;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.NotificationFilter;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.resource.spi.*;

/**
 * Configuration for the connection-factory pattern.
 */
public class ConnectionFactoryConfig extends BeanConfig {
  private static final Logger log
    = Logger.getLogger(ConnectionFactoryConfig.class.getName());

  private static L10N L = new L10N(ConnectionFactoryConfig.class);

  private ResourceAdapter _ra;

  private int _maxConnections = 1024;
  private long _maxActiveTime = Long.MAX_VALUE / 2;

  private @Inject Instance<ResourceAdapter> _raInstance;

  public ConnectionFactoryConfig()
  {
    setBeanConfigClass(ManagedConnectionFactory.class);
  }

  @Override
  protected String getDefaultScope()
  {
    return null;
  }

  public void setResourceAdapter(ResourceAdapter ra)
  {
    _ra = ra;
  }

  public void setMaxConnections(int max)
  {
    _maxConnections = max;
  }

  public void setMaxActiveTime(Period period)
  {
    _maxActiveTime = period.getPeriod();
  }

  public void init()
  {
    super.init();

    Bean<?> comp = getComponent();

    InjectManager manager = InjectManager.create();

    ManagedConnectionFactory managedFactory
      = (ManagedConnectionFactory) manager.getReference(comp);

    if (managedFactory instanceof ResourceAdapterAssociation) {
      Class<?> cl = managedFactory.getClass();

      ResourceAdapter ra = findResourceAdapter(cl);

      ResourceAdapterAssociation factoryAssoc
        = (ResourceAdapterAssociation) managedFactory;

      try {
        factoryAssoc.setResourceAdapter(ra);
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }

    ResourceManagerImpl rm = ResourceManagerImpl.create();

    ConnectionPool cm = rm.createConnectionPool();

    if (getName() != null)
      cm.setName(getName());

    cm.setMaxConnections(_maxConnections);
    cm.setMaxActiveTime(_maxActiveTime);

    ResourceArchive rar = null;

    if (rar != null) {
      String trans = rar.getTransactionSupport();

      if (trans == null) { // guess XA
        cm.setXATransaction(true);
        cm.setLocalTransaction(true);
      }
      else if (trans.equals("XATransaction")) {
        cm.setXATransaction(true);
        cm.setLocalTransaction(true);
      }
      else if (trans.equals("NoTransaction")) {
        cm.setXATransaction(false);
        cm.setLocalTransaction(false);
      }
      else if (trans.equals("LocalTransaction")) {
        cm.setXATransaction(false);
        cm.setLocalTransaction(true);
      }
    }
    /*
    cm.setLocalTransactionOptimization(getLocalTransactionOptimization());
    cm.setShareable(getShareable());
    */

    Object connectionFactory;

    try {
      connectionFactory = cm.init(managedFactory);
      cm.start();

      BeanBuilder factory
        = manager.createBeanFactory(connectionFactory.getClass());

      if (getName() != null) {
        Jndi.bindDeepShort(getName(), connectionFactory);

        factory.name(getName());
        factory.qualifier(Names.create(getName()));
        // server/30i0
        factory.qualifier(CurrentLiteral.CURRENT);
      }

      Bean bean = factory.singleton(connectionFactory);

      manager.addBean(bean);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  private ResourceAdapter findResourceAdapter(Class cl)
  {
    if (_ra != null)
      return _ra;

    ResourceArchive ra
      = ResourceArchiveManager.findResourceArchive(cl.getName());

    if (ra == null) {
      throw new ConfigException(L.l("'{0}' does not have a defined resource-adapter.  Either define it in a &lt;resource-adapter> property or check the rar or META-INF/resin-ra.xml files",
                                    cl.getName()));
    }

    InjectManager webBeans = InjectManager.create();
    String raName = ra.getResourceAdapterClass().getName();

    Instance<ResourceAdapter> instance
      = _raInstance.select(Names.create(raName));

    ResourceAdapter resourceAdapter = instance.get();

    if (resourceAdapter == null) {
      throw new ConfigException(L.l("'{0}' does not have a configured resource-adapter for '{1}'.",
                                    raName,
                                    cl.getName()));
    }

    return resourceAdapter;
  }

  @Override
  public void deploy()
  {
  }
}


