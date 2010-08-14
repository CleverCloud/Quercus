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

package com.caucho.jca.ra;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.Names;
import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.CurrentLiteral;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.type.TypeFactory;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.naming.Jndi;
import com.caucho.transaction.ConnectionPool;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.resource.spi.*;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration for the <connector> pattern.
 */
public class ConnectorConfig implements EnvironmentListener {
  private static L10N L = new L10N(ConnectorConfig.class);
  private static Logger log
    = Logger.getLogger(ConnectorConfig.class.getName());

  private static int _idGen;

  private String _url;
  private String _name;
  private String _jndiName;
  private String _type;

  private ResourceArchive _rar;

  private ResourceAdapterConfig _resourceAdapter = new ResourceAdapterConfig();
  private ContainerProgram _init;

  private ArrayList<ConnectionFactory> _outboundList
    = new ArrayList<ConnectionFactory>();

  private ArrayList<ConnectionListener> _inboundList
    = new ArrayList<ConnectionListener>();

  private ArrayList<ConnectorBean> _beanList
    = new ArrayList<ConnectorBean>();

  private ResourceAdapter _ra;
  private boolean _isInitRA;

  private final Lifecycle _lifecycle = new Lifecycle();

  /**
   * Sets the name
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Sets the name
   */
  public void setJndiName(String name)
  {
    _jndiName = name;
  }

  /**
   * Gets the name
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the type of the connector.
   */
  public void setType(String type)
    throws Exception
  {
    setClass(type);
  }

  /**
   * Sets the type of the connector using known types.
   */
  public void setURI(String url)
    throws Exception
  {
    TypeFactory factory = TypeFactory.create();
    
    Class type = factory.getDriverClassByUrl(ResourceAdapter.class, url);

    setClass(type.getName());

    ContainerProgram program = factory.getUrlProgram(url);

    if (program == null) {
    }
    else if (_init == null)
      _init = program;
    else
      _init.addProgram(program);
  }

  /**
   * Sets the type of the connector.
   */
  public void setClass(String type)
    throws Exception
  {
    _type = type;

    _rar = ResourceArchiveManager.findResourceArchive(_type);

    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    if (_rar != null) {
      ObjectConfig raConfig = _rar.getResourceAdapter();

      if (raConfig.getType() != null)
        _ra = (ResourceAdapter) raConfig.instantiate();
    }
    else {
      try {
        Class raClass = Class.forName(_type, false, loader);

        _ra = (ResourceAdapter) raClass.newInstance();
      } catch (Exception e) {
        throw new ConfigException(L.l("'{0}' is not a known connector.  The type must match the resource adaptor or managed connection factory of one of the installed *.rar files or specify a ResourceAdapter implementation.",
                                      _type),
                                  e);
      }
    }
  }

  /**
   * Gets the type
   */
  public String getType()
  {
    return _type;
  }

  public void addInit(ContainerProgram init)
  {
    _init = init;
  }

  /**
   * Configures the resource adapter.
   */
  public ResourceAdapterConfig createResourceAdapter()
    throws ConfigException
  {
    if (_ra == null)
      throw new ConfigException(L.l("'{0}' may not have a <resource-adapter> section.  Old-style connectors must use <connection-factory>, but not <resource-adapter>.",
                                    _type));
    return _resourceAdapter;
  }

  /**
   * Sets the configured resource adapter.
   */
  public void setResourceAdapter(ResourceAdapterConfig raConfig)
    throws Exception
  {
  }

  /**
   * Configures a connection-factory
   */
  public ConnectionFactory createConnectionFactory()
    throws Exception
  {
    initRA();
    
    return new ConnectionFactory();
  }

  /**
   * Configures a connection-factory
   */
  public void addConnectionFactory(ConnectionFactory factory)
    throws Exception
  {
    _outboundList.add(factory);
  }

  /**
   * Configures a connection-listener
   */
  public ConnectionListener createMessageListener()
    throws Exception
  {
    initRA();
    
    return new ConnectionListener();
  }

  /**
   * Adds the configured connection-listener
   */
  public void addMessageListener(ConnectionListener listener)
    throws Throwable
  {
    _inboundList.add(listener);
    
    String listenerType = listener.getType();

    if (_ra == null)
      throw new ConfigException(L.l("message-listener requires a resource-adapter."));

    ActivationSpec activationSpec;
    
    if (_rar != null) {
      ObjectConfig objectCfg = _rar.getMessageListener(listenerType);

      if (objectCfg == null)
        throw new ConfigException(L.l("'{0}' is an unknown type of <connection-listener> for '{1}'.  The connector has no matching inbound connection-listener.",
                                      listenerType,
                                      _type));

      activationSpec = (ActivationSpec) objectCfg.instantiate();
    }
    else {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Class listenerClass = null;
      
      try {
        listenerClass = Class.forName(listenerType, false, loader);
      } catch (Throwable e) {
        throw new ConfigException(L.l("'{0}' is not a known listener.  The type must match the activation spec for an inbound connection of one of the installed *.rar files or specify an ActivationSpec implementation.",
                                      listenerType), e);
      }
      
      activationSpec = (ActivationSpec) listenerClass.newInstance();
    }
      
    if (listener.getInit() != null)
      listener.getInit().configure(activationSpec);
    /*
      TypeBuilderFactory.init(activationSpec);
    */

    activationSpec.setResourceAdapter(_ra);
    // activationSpec.validate();

    EndpointFactory endpointFactoryCfg = listener.getEndpointFactory();
      
    if (endpointFactoryCfg == null)
      throw new ConfigException(L.l("connection-listener needs endpoint factory."));

    Class endpointClass = endpointFactoryCfg.getType();

    MessageEndpointFactory endpointFactory;
    endpointFactory = (MessageEndpointFactory) endpointClass.newInstance();

    if (endpointFactoryCfg.getInit() != null)
      endpointFactoryCfg.getInit().configure(endpointFactory);

    Config.init(endpointFactory);

    listener.setEndpoint(endpointFactory);
    listener.setActivation(activationSpec);
  }

  /**
   * Configures a connection-resource
   */
  public ConnectorBean createBean()
  {
    return new ConnectorBean();
  }

  /**
   * Configures a connection-resource
   */
  public ConnectorBean createResource()
  {
    return createBean();
  }

  /**
   * Initialize the resource.
   */
  @PostConstruct
  public void init()
    throws Exception
  {
    if (_type == null)
      throw new ConfigException(L.l("<connector> requires a <type>."));

    if (_name == null)
      _name = _jndiName;

    if (_name == null && _rar != null)
      _name = _rar.getDisplayName() + "-" + _idGen++;

    if (_ra == null)
      throw new ConfigException(L.l("<connector> does not have a resource adapter."));
    
    if (_resourceAdapter.getInit() != null)
      _resourceAdapter.getInit().configure(_ra);

    if (_init != null)
      _init.configure(_ra);

    ResourceManagerImpl.addResource(_ra);

    InjectManager beanManager = InjectManager.create();

    BeanBuilder factory = beanManager.createBeanFactory(_ra.getClass());
    
    if (_resourceAdapter.getName() != null) {
      Jndi.bindDeepShort(_resourceAdapter.getName(), _ra);

      beanManager.addBean(factory.name(_resourceAdapter.getName())
                          .singleton(_ra));
    }
    else {
      beanManager.addBean(factory.name(_name).singleton(_ra));
    }

    // create a default outbound factory
    if (_outboundList.size() == 0 && _jndiName != null && _rar != null) {
      ObjectConfig factoryConfig = _rar.getConnectionDefinition(null);

      if (factoryConfig != null) {
        ConnectionFactory connFactory = createConnectionFactory();
        connFactory.setJndiName(_jndiName);
        connFactory.init();

        addConnectionFactory(connFactory);
      }
    }

    /*
    if (close != null && ! (obj instanceof ResourceAdapter))
      Environment.addEnvironmentListener(new CloseListener(obj, close));
    */
    initRA();
    
    Environment.addEnvironmentListener(this);
    
    start();

    log.fine("Connector[" + _type + "] active");
  }

  /**
   * Start the resource.
   */
  public void start()
    throws Exception
  {
    if (! _lifecycle.toActive())
      return;
    
    initRA();
    
    for (int i = 0; i < _outboundList.size(); i++) {
      ConnectionFactory factory = _outboundList.get(i);

      factory.start();
    }
    
    for (int i = 0; i < _inboundList.size(); i++) {
      ConnectionListener listener = _inboundList.get(i);
      
      _ra.endpointActivation(listener.getEndpoint(),
                             listener.getActivation());
    }
  }

  /**
   * Initializes the ra.
   */
  public void initRA()
    throws Exception
  {
    /*
    if (! _isInitRA) {
      _isInitRA = true;
      
      if (_ra != null) {
        // TypeBuilderFactory.init(_ra);
        ResourceManagerImpl.addResource(_ra);
      }
    }
    */
  }

  /**
   * Stops the connector.
   */
  public void stop()
  {
    if (! _lifecycle.toStop())
      return;

    for (int i = 0; i < _inboundList.size(); i++) {
      ConnectionListener listener = _inboundList.get(i);

      MessageEndpointFactory endpointFactory = listener.getEndpoint();
      ActivationSpec activation = listener.getActivation();

      if (_ra != null)
        _ra.endpointDeactivation(endpointFactory, activation);
    }
  }
  
  /**
   * Handles the configure phase.
   */
  public void environmentConfigure(EnvironmentClassLoader loader)
  {
  }
  
  /**
   * Handles the bind phase.
   */
  public void environmentBind(EnvironmentClassLoader loader)
  {
  }
  
  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
    try {
      start();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
    stop();
  }

  @Override
  public String toString()
  {
    return "ConnectorResource[" + _name + "]";
  }

  public class ResourceAdapterConfig {
    private String _name;
    private ContainerProgram _init;

    public void setJndiName(String name)
    {
      _name = name;
    }

    public String getName()
    {
      return _name;
    }
    
    public void setInit(ContainerProgram init)
    {
      _init = init;
    }

    public ContainerProgram getInit()
    {
      return _init;
    }
  }

  public class ConnectionFactory {
    private String _name;
    private String _type;
    private ManagedConnectionFactory _factory;
    private boolean _localTransactionOptimization = true;
    private boolean _shareable = true;
    private ContainerProgram _init;

    public void setJndiName(String name)
    {
      _name = name;
    }

    public void setName(String name)
    {
      _name = name;
    }

    public String getName()
    {
      return _name;
    }

    public void setType(String type)
      throws Exception
    {
      setClass(type);
    }

    public void setClass(String type)
      throws Exception
    {
      _type = type;

      if (_rar != null) {
        ObjectConfig factoryConfig = _rar.getConnectionDefinition(type);

        if (factoryConfig == null)
          throw new ConfigException(L.l("'{0}' is an unknown type of <connection-factory> for '{1}'.  The connector has no matching outbound connection-factory.",
                                        type,
                                        ConnectorConfig.this._type));

        _factory = (ManagedConnectionFactory) factoryConfig.instantiate();
      }
      else if (type != null) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class factoryClass = null;

        try {
          factoryClass = Class.forName(type, false, loader);
        } catch (Exception e) {
          throw new ConfigException(L.l("'{0}' is not a known connection factory.  The type must match the resource adaptor or managed connection factory of one of the installed *.rar files or specify a ManagedConnectionFactory implementation.",
                                        type));
        }

        if (! ManagedConnectionFactory.class.isAssignableFrom(factoryClass)) {
          throw new ConfigException(L.l("'{0}' does not implement javax.resource.spi.ManagedConnectionFactory.  <connection-factory> classes must implement ManagedConnectionFactory.",
                                        factoryClass.getName()));
        }

        _factory = (ManagedConnectionFactory) factoryClass.newInstance();
      }
      
    }

    public String getType()
    {
      return _type;
    }

    /**
     * Enables the local transaction optimization.
     */
    public void setLocalTransactionOptimization(boolean enable)
    {
      _localTransactionOptimization = enable;
    }

    /**
     * Enables the local transaction optimization.
     */
    public boolean getLocalTransactionOptimization()
    {
      return _localTransactionOptimization;
    }

    /**
     * Enables the shareable property
     */
    public void setShareable(boolean enable)
    {
      _shareable = enable;
    }

    /**
     * Enables the shareable property
     */
    public boolean getShareable()
    {
      return _shareable;
    }

    public ManagedConnectionFactory getFactory()
    {
      return _factory;
    }
    
    public void setInit(ContainerProgram init)
    {
      _init = init;
    }

    public ContainerProgram getInit()
    {
      return _init;
    }

    @PostConstruct
    public void init()
      throws Exception
    {
      if (_factory == null && _rar != null) {
        ObjectConfig factoryConfig = _rar.getConnectionDefinition(null);

        _factory = (ManagedConnectionFactory) factoryConfig.instantiate();
      }
      
      if (_factory == null)
        throw new ConfigException(L.l("connection-factory requires a valid type."));
    }

  /**
   * Configures a connection-factory
   */
  public void start()
    throws Exception
    {
      ManagedConnectionFactory managedFactory = getFactory();
    
      if (getInit() != null)
        getInit().configure(managedFactory);

      if (_ra != null
          && managedFactory instanceof ResourceAdapterAssociation) {
        ((ResourceAdapterAssociation) managedFactory).setResourceAdapter(_ra);
      }

      ResourceManagerImpl rm = ResourceManagerImpl.createLocalManager();

      ConnectionPool cm = rm.createConnectionPool();

      if (_name != null)
        cm.setName(_name);

      if (_rar != null) {
        String trans = _rar.getTransactionSupport();

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

      cm.setLocalTransactionOptimization(getLocalTransactionOptimization());
      cm.setShareable(getShareable());

      Object connectionFactory = cm.init(managedFactory);
      cm.start();

      InjectManager manager = InjectManager.create();
      BeanBuilder factory
        = manager.createBeanFactory(connectionFactory.getClass());
      
      if (getName() != null) {
        Jndi.bindDeepShort(getName(), connectionFactory);

        factory.name(getName());
        // server/30b4
        factory.qualifier(Names.create(getName()));
        factory.qualifier(CurrentLiteral.CURRENT);
      }
      
      manager.addBean(factory.singleton(connectionFactory));
    }
  }

  public class ConnectionListener {
    private String _name;
    private String _type;
    private ContainerProgram _init;
    private EndpointFactory _endpointFactory;

    private MessageEndpointFactory _endpoint;
    private ActivationSpec _activation;

    public void setJndiName(String name)
    {
      _name = name;
    }

    public String getName()
    {
      return _name;
    }

    public void setType(String type)
    {
      _type = type;
    }

    public String getType()
    {
      return _type;
    }
    
    public void setInit(ContainerProgram init)
    {
      _init = init;
    }

    public ContainerProgram getInit()
    {
      return _init;
    }
    
    public EndpointFactory getEndpointFactory()
    {
      return _endpointFactory;
    }
    
    public EndpointFactory createEndpointFactory()
    {
      _endpointFactory = new EndpointFactory();
      
      return _endpointFactory;
    }

    @PostConstruct
    public void init()
      throws ConfigException
    {
      if (_endpointFactory == null)
        throw new ConfigException(L.l("connection-listener needs an endpoint-factory"));
    }

    public void setEndpoint(MessageEndpointFactory endpoint)
    {
      _endpoint = endpoint;
    }

    public MessageEndpointFactory getEndpoint()
    {
      return _endpoint;
    }

    public void setActivation(ActivationSpec activation)
    {
      _activation = activation;
    }

    public ActivationSpec getActivation()
    {
      return _activation;
    }
  }

  public class EndpointFactory {
    private String _name;
    private Class _type;
    private ContainerProgram _init;

    public void setJndiName(String name)
    {
      _name = name;
    }

    public String getName()
    {
      return _name;
    }

    public void setType(Class type)
      throws ConfigException
    {
      _type = type;

      Config.checkCanInstantiate(type);
    }

    public Class getType()
    {
      return _type;
    }
    
    public void setInit(ContainerProgram init)
    {
      _init = init;
    }

    public ContainerProgram getInit()
    {
      return _init;
    }
  }

  public class ConnectorBean {
    private String _name;
    private String _type;
    private ContainerProgram _init;

    private ObjectConfig _objectConfig;
    private Object _object;

    public void setJndiName(String name)
    {
      _name = name;
    }

    public void setName(String name)
    {
      _name = name;
    }

    public String getName()
    {
      return _name;
    }

    public void setType(String type)
      throws Exception
    {
      setClass(type);
    }

    public void setClass(String type)
      throws Exception
    {
      _type = type;

      Object resourceObject = null;

      if (_rar != null) {
        _objectConfig = _rar.getAdminObject(type);

        if (_objectConfig == null)
          throw new ConfigException(L.l("'{0}' may not have a <resource> section.  The connector has no matching <adminobject> defined.",
                                        _type));

        _object = _objectConfig.instantiate();
      }
      else {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
          Class resourceClass = Class.forName(type, false, loader);

          _object = resourceClass.newInstance();
        } catch (Exception e) {
          throw new ConfigException(L.l("'{0}' is not a known resource.  The type must match the adminobject of one of the installed *.rar files.",
                                        _type), e);
        }
      
      }
    }

    public String getType()
    {
      return _type;
    }
    
    public void setInit(ContainerProgram init)
    {
      _init = init;
    }

    public ContainerProgram getInit()
    {
      return _init;
    }

    public Object getObject()
    {
      return _object;
    }

    @PostConstruct
    public void init()
      throws Exception
    {
      if (_object == null)
        throw new ConfigException(L.l("<class> must be set for a bean."));
      
      Object resourceObject = getObject();
    
      if (getInit() != null)
        getInit().configure(resourceObject);

      if (_ra != null && resourceObject instanceof ResourceAdapterAssociation)
        ((ResourceAdapterAssociation) resourceObject).setResourceAdapter(_ra);

      InjectManager manager = InjectManager.create();

      BeanBuilder factory
        = manager.createBeanFactory(resourceObject.getClass());
      
      if (getName() != null) {
        Jndi.bindDeepShort(getName(), resourceObject);

        manager.addBean(factory.name(getName()).singleton(resourceObject));
      }
      else
        manager.addBean(factory.singleton(resourceObject));
    }
  }
}

