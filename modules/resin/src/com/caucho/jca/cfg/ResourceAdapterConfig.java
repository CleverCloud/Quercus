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

import java.util.ArrayList;

import com.caucho.config.ConfigException;
import com.caucho.jca.ra.ObjectConfig;
import com.caucho.util.L10N;

/**
 * Configuration for a connector.
 */
public class ResourceAdapterConfig extends ObjectConfig {
  private static final L10N L = new L10N(ResourceAdapterConfig.class);
  
  private Class<?> _adapterClass;

  private ArrayList<ConnectionDefinition> _outboundConnections =
    new ArrayList<ConnectionDefinition>();

  private ArrayList<MessageListenerConfig> _inboundConnections
    = new ArrayList<MessageListenerConfig>();

  private ArrayList<AdminObjectConfig> _resources
    = new ArrayList<AdminObjectConfig>();

  private ConnectionDefinition _connectionDefinition;

  private String _transactionSupport;
  
  public ResourceAdapterConfig()
  {
  }

  /**
   * Sets the resource adapter class
   */
  public void setResourceadapterClass(Class<?> cl)
    throws ConfigException
  {
    _adapterClass = cl;

    setType(cl);
  }

  /**
   * Gets the resource adapter class
   */
  public Class<?> getResourceadapterClass()
  {
    return _adapterClass;
  }

  /**
   * Adds an admin object.
   */
  public void addAdminobject(AdminObjectConfig adminObject)
  {
    _resources.add(adminObject);
  }

  /**
   * Sets the ManagedConnectionFactory class.
   */
  public void setManagedconnectionfactoryClass(Class<?> cl)
    throws ConfigException
  {
    getConnectionDefinition().setManagedconnectionfactoryClass(cl);
  }

  /**
   * Sets the ConnectionFactory interface
   */
  public void setConnectionfactoryInterface(String cl)
  {
  }

  /**
   * Sets the ConnectionFactory impl class
   */
  public void setConnectionfactoryImplClass(String cl)
  {
  }

  /**
   * Sets the Connection interface
   */
  public void setConnectionInterface(String cl)
  {
  }

  /**
   * Sets the Connection impl class
   */
  public void setConnectionImplClass(String cl)
  {
  }

  /**
   * Returns the top connection definition (for backward compatibility).
   */
  private ConnectionDefinition getConnectionDefinition()
    throws ConfigException
  {
    if (_connectionDefinition == null) {
      _connectionDefinition = new ConnectionDefinition();
      _outboundConnections.add(_connectionDefinition);
    }

    return _connectionDefinition;
  }

  /**
   * Adds a new connection definitin.
   */
  void addConnectionDefinition(ConnectionDefinition conn)
    throws ConfigException
  {
    if (getConnectionDefinition(conn.getConnectionFactoryInterface().getName()) != null)
      throw new ConfigException(L.l("'{0}' is a duplicate connection-definition.  The <connectionfactory-interface> must be unique.",
                                    conn.getConnectionFactoryInterface().getName()));
    
    _outboundConnections.add(conn);
  }

  /**
   * Gets the connection definition for the named class.
   */
  public ConnectionDefinition getConnectionDefinition(String type)
  {
    if (type == null && _outboundConnections.size() == 1)
      return _outboundConnections.get(0);
    else if (type == null)
      return null;
    
    for (int i = 0; i < _outboundConnections.size(); i++) {
      ConnectionDefinition cfg = _outboundConnections.get(i);

      Class<?> cl = cfg.getManagedConnectionFactoryClass();
      
      if (cl != null && cl.getName().equals(type))
        return cfg;

      cl = cfg.getConnectionFactoryInterface();
      
      if (cl != null && cl.getName().equals(type))
        return cfg;

      cl = cfg.getConnectionFactoryImpl();
      
      if (cl != null && cl.getName().equals(type))
        return cfg;
    }

    return null;
  }

  /**
   * Adds a new connection definition.
   */
  void addMessageListener(MessageListenerConfig cfg)
    throws ConfigException
  {
    if (getMessageListener(cfg.getMessageListenerType().getName()) != null)
      throw new ConfigException(L.l("'{0}' is a duplicate messagelistener-type.  The <messagelistener-type> must be unique.",
                                    cfg.getMessageListenerType().getName()));
    
    _inboundConnections.add(cfg);
  }

  /**
   * Gets the resource adapter class
   */
  public MessageListenerConfig getMessageListener(String type)
  {
    if (type == null && _inboundConnections.size() == 1)
      return _inboundConnections.get(0);
    else if (type == null)
      return null;
    
    for (int i = 0; i < _inboundConnections.size(); i++) {
      MessageListenerConfig cfg = _inboundConnections.get(i);

      //Class cl = cfg.getMessageListenerType();
      Class<?> cl = cfg.getActivationSpecClass();

      if (cl != null && cl.getName().equals(type))
        return cfg;
    }

    return null;
  }

  /**
   * Adds a new resource
   */
  void addResource(AdminObjectConfig cfg)
  {
    _resources.add(cfg);
  }

  /**
   * Gets the resource adapter class
   */
  public AdminObjectConfig getAdminObject(String type)
  {
    for (int i = 0; i < _resources.size(); i++) {
      AdminObjectConfig cfg = _resources.get(i);

      Class<?> cl = cfg.getAdminObjectClass();

      if (cl != null && cl.getName().equals(type))
        return cfg;

      cl = cfg.getAdminObjectInterface();
      
      if (cl != null && cl.getName().equals(type))
        return cfg;
    }

    return null;
  }

  /**
   * Sets the transaction support.
   */
  public void setTransactionSupport(String xa)
  {
    _transactionSupport = xa;
  }

  /**
   * Gets the transaction support.
   */
  public String getTransactionSupport()
  {
    return _transactionSupport;
  }

  /**
   * Sets the reauthentication support.
   */
  public void setReauthenticationSupport(boolean support)
  {
  }

  /**
   * Creates an authentication mechanism
   */
  public AuthenticationMechanism createAuthenticationMechanism()
  {
    return new AuthenticationMechanism();
  }

  /**
   * Creates a security permission
   */
  public SecurityPermission createSecurityPermission()
  {
    return new SecurityPermission();
  }

  /**
   * Adds an outbound resource adapter.
   */
  public OutboundResourceAdapterConfig createOutboundResourceadapter()
    throws ConfigException
  {
    return new OutboundResourceAdapterConfig(this);
  }

  /**
   * Adds an inbound resource adapter.
   */
  public InboundResourceAdapterConfig createInboundResourceadapter()
    throws ConfigException
  {
    return new InboundResourceAdapterConfig(this);
  }

  public static class AuthenticationMechanism {
    public void setDescription(String description)
    {
    }
    
    public void setAuthenticationMechanismType(String type)
    {
    }
    
    public void setCredentialInterface(String type)
    {
    }
  }

  public static class SecurityPermission {
    public void setDescription(String description)
    {
    }

    public void setSecurityPermissionSpec(String spec)
    {
    }
  }
}
