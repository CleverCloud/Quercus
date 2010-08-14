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

package com.caucho.ejb.cfg;

import com.caucho.config.*;
import com.caucho.config.cfg.AbstractBeanConfig;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.types.*;
import com.caucho.ejb.manager.*;

import com.caucho.util.*;

import java.util.*;
import java.util.logging.*;

import javax.annotation.*;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.jms.*;
import javax.resource.spi.*;

/**
 * ejb-message-bean configuration
 */
public class MessageBeanConfig extends AbstractBeanConfig
{
  private static final L10N L = new L10N(MessageBeanConfig.class);
  private static final Logger log
    = Logger.getLogger(MessageBeanConfig.class.getName());

  private ActivationSpec _activationSpec;

  private Class _destinationType;
  private String _destinationName;
  private Object _destination;
  private int _messageConsumerMax;

  /**
   * Sets the activation spec
   */
  public void setActivationSpec(ActivationSpec spec)
  {
    _activationSpec = spec;
  }

  public void setDestinationType(Class type)
  {
    _destinationType = type;
  }

  public void setDestinationName(String name)
  {
    _destinationName = name;
  }

  public void setDestination(Object destination)
  {
    _destination = destination;

    if (destination == null)
      throw new ConfigException(L.l("'destination' attribute may not be null"));
  }

  public void setMessageConsumerMax(int messageConsumerMax)
  {
    _messageConsumerMax = messageConsumerMax;
  }

  public void initImpl()
  {
    if (getInstanceClass() == null)
      throw new ConfigException(L.l("ejb-message-bean requires a 'class' attribute"));

    EjbManager ejbContainer = EjbManager.create();
    EjbConfigManager configManager = ejbContainer.getConfigManager();

    EjbMessageBean bean = new EjbMessageBean(configManager, "config");
    bean.setConfigLocation(getFilename(), getLine());

    bean.setEJBClass(getInstanceClass());

    String name = getName();

    if (name == null)
      name = getJndiName();

    if (name == null)
      name = getInstanceClass().getSimpleName();

    bean.setEJBName(name);

    if (getInit() != null)
      bean.setInit(getInit());

    String loc = getInstanceClass().getName() + ": ";
    InjectManager webBeans = InjectManager.create();

    bean.setMessageConsumerMax(_messageConsumerMax);

    if (_destination != null) {
      bean.setDestinationValue((Destination) _destination);
    }
    else if (_activationSpec != null) {
      bean.setActivationSpec(_activationSpec);
    }
    else {
      Class destinationType = _destinationType;

      if (_destinationType == null)
        destinationType = Destination.class;

      Set<Bean<?>> beanSet;

      if (_destinationName != null)
        beanSet = webBeans.getBeans(_destinationName);
      else
        beanSet = webBeans.getBeans(destinationType);

      Object destComp = null;

      if (beanSet.size() > 0) {
        Bean destBean = webBeans.resolve(beanSet);
        CreationalContext env = webBeans.createCreationalContext(destBean);

        destComp
          = webBeans.getReference(destBean, destBean.getBeanClass(), env);
      }

      if (destComp == null)
        throw new ConfigException(L.l("{0}: '{1}' is an unknown destination type '{2}'",
                                      loc,
                                      _destinationName,
                                      _destinationType.getName()));

      bean.setDestinationValue((Destination) destComp);

      beanSet = webBeans.getBeans(ConnectionFactory.class);

      Bean factoryBean = webBeans.resolve(beanSet);
      CreationalContext env = webBeans.createCreationalContext(factoryBean);

      Object comp = webBeans.getReference(factoryBean);

      if (comp == null)
        throw new ConfigException(L.l("ejb-message-bean requires a configured JMS ConnectionFactory"));
      bean.setConnectionFactoryValue((ConnectionFactory) comp);
    }

    bean.init();

    configManager.setBeanConfig(name, bean);

    // XXX: timing?
    // configManager.start();
  }
}

