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

import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.ConfigException;
import com.caucho.config.reflect.AnnotatedTypeImpl;
import com.caucho.config.reflect.ReflectionAnnotatedFactory;
import com.caucho.util.L10N;

/**
 * Proxy for an ejb bean configuration.  This proxy is needed to handle
 * the merging of ejb definitions.
 */
public class EjbSessionConfigProxy extends EjbBeanConfigProxy {
  private static final L10N L = new L10N(EjbSessionConfigProxy.class);
  
  private String _sessionType;
  
  /**
   * Creates a new session bean configuration.
   */
  public EjbSessionConfigProxy(EjbConfig config, String ejbModuleName)
  {
    super(config, ejbModuleName);
  }
  
  public void setSessionType(String sessionType)
  {
    if ("Stateless".equals(sessionType))
      _sessionType = sessionType;
    else if ("Stateful".equals(sessionType))
      _sessionType = sessionType;
    else if ("Singleton".equals(sessionType))
      _sessionType = sessionType;
    else
      throw new ConfigException(L.l("'{0}' is an unknown sessionType",
                                    sessionType));
  }
  
  @Override
  public void configure()
  {
    EjbBean<?> ejbBean = getConfig().getBeanConfig(getEjbName());
    
    if (ejbBean == null) {
      if (getEjbClass() == null)
        throw new ConfigException(L.l("'{0}' is an unknown EJB name",
                                      getEjbName()));
      
      ejbBean = createEjbBean(getEjbClass());
      
      if (getEjbName() != null) {
        // ioc/0p65
        ejbBean.setEJBName(getEjbName());
      }
      
      getConfig().setBeanConfig(getEjbName(), ejbBean);
    }
    
    getBuilderProgram().configure(ejbBean);
    
  }
  
  private <T> EjbBean<T> createEjbBean(Class<T> ejbClass)
  {
    AnnotatedType<T> rawAnnType
      = ReflectionAnnotatedFactory.introspectType(ejbClass);
    
    AnnotatedTypeImpl<T> annType = AnnotatedTypeImpl.create(rawAnnType);
    
    String name = getEjbName();
    String description = null;
    String mappedName = null;

    if ("Stateless".equals(_sessionType)) {
      Stateless stateless = new StatelessLiteral(name, mappedName, description);
      annType.addAnnotation(stateless);
      
      return new EjbStatelessBean<T>(getConfig(), rawAnnType, annType, stateless.name());
    }
    else if ("Stateful".equals(_sessionType)) {
      Stateful stateful = new StatefulLiteral(name, mappedName, description);
      annType.addAnnotation(stateful);
      
      return new EjbStatefulBean<T>(getConfig(), rawAnnType, annType, stateful.name());
    }
    else if ("Singleton".equals(_sessionType)) {
      Singleton singleton = new SingletonLiteral(name, mappedName, description);
      annType.addAnnotation(singleton);
      
      return new EjbSingletonBean<T>(getConfig(), rawAnnType, annType, singleton.name());
    }
    
    throw new ConfigException(L.l("'{0}' is an unknown <session-type>",
                                  _sessionType));
  }
}
