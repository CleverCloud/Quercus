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

package com.caucho.jca.cfg;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.jca.ra.ObjectConfig;
import com.caucho.util.L10N;

import javax.resource.spi.ManagedConnectionFactory;

/**
 * Configuration for a connector.
 */
public class ConnectionDefinition extends ObjectConfig {
  private static final L10N L = new L10N(ConnectionDefinition.class);

  private Class _managedConnectionFactoryClass;
  private Class _connectionFactoryInterface;
  private Class _connectionFactoryImplClass;
  private Class _connectionInterface;
  private String _connectionImplClass;
  
  public void setManagedconnectionfactoryClass(Class cl)
    throws ConfigException
  {
    Config.checkCanInstantiate(cl);

    if (! ManagedConnectionFactory.class.isAssignableFrom(cl))
      throw new ConfigException(L.l("`{0}' must implement ManagedConnectionFactory.  managedconnectionfactory-class must implement javax.resource.spi.ManagedConnectionFactory."));
      
    _managedConnectionFactoryClass = cl;

    setType(cl);
  }

  public Class getManagedConnectionFactoryClass()
  {
    return _managedConnectionFactoryClass;
  }

  /**
   * Sets the application-view for the connection factory.
   */
  public void setConnectionfactoryInterface(Class cl)
    throws ConfigException
  {
    _connectionFactoryInterface = cl;
  }

  /**
   * Returns the application-view for the connection factory.
   */
  public Class getConnectionFactoryInterface()
  {
    return _connectionFactoryInterface;
  }
    
  /**
   * Sets the connection factory implementation class.
   */
  public void setConnectionfactoryImplClass(Class cl)
    throws ConfigException
  {
    _connectionFactoryImplClass = cl;
  }

  /**
   * Returns the application-view for the connection factory.
   */
  public Class getConnectionFactoryImpl()
  {
    return _connectionFactoryImplClass;
  }
    
  /**
   * Sets the application-view for the connection
   */
  public void setConnectionInterface(Class cl)
    throws ConfigException
  {
    _connectionInterface = cl;
  }
    
  /**
   * Returns the application-view for the connection
   */
  public Class getConnectionInterface()
  {
    return _connectionInterface;
  }
    
  /**
   * Sets the connection implementation class.
   */
  public void setConnectionImplClass(String cl)
    throws ConfigException
  {
    _connectionImplClass = cl;
  }
}
