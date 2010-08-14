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

package com.caucho.server.admin;

import javax.annotation.PostConstruct;

import com.caucho.bam.Broker;
import com.caucho.config.AdminLiteral;
import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.DefaultLiteral;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.types.RawString;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.security.AdminAuthenticator;
import com.caucho.security.Authenticator;
import com.caucho.security.BasicPrincipal;
import com.caucho.security.PasswordUser;
import com.caucho.security.XmlAuthenticator;
import com.caucho.server.cluster.Server;
import com.caucho.server.host.HostConfig;
import com.caucho.server.resin.Resin;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

/**
 * Configuration for management.
 */
@Configurable
public class Management
{
  private static L10N L = new L10N(Management.class);
  public static final String HOST_NAME = "admin.caucho";

  private Resin _resin;
  private Server _server;

  private HostConfig _hostConfig;

  private AdminAuthenticator _auth;
  protected TransactionManager _transactionManager;

  private Lifecycle _lifecycle = new Lifecycle();

  public Management()
  {
    _resin = Resin.getCurrent();
  }

  public Management(Resin resin)
  {
    _resin = resin;
  }

  public void setResin(Resin resin)
  {
    _resin = resin;
  }

  public void setServer(Server server)
  {
    _server = server;
  }

  public String getServerId()
  {
    return Resin.getCurrent().getServerId();
  }

  /**
   * @Deprecated
   */
  public void setPath(Path path)
  {
    // _resin.setAdminPath(path);
  }

  public XmlAuthenticator.User createUser()
  {
    if (_auth == null)
      _auth = new AdminAuthenticator();

    return _auth.createUser();
  }

  /**
   * Adds a user
   */
  public void addUser(XmlAuthenticator.User user)
  {
    _auth.addUser(user);
  }

  /**
   * Returns the management cookie.
   */
  public String getRemoteCookie()
  {
    if (_auth != null)
      return _auth.getHash();
    else
      return null;
  }

  /**
   * Returns the admin broker
   */
  public Broker getAdminBroker()
  {
    if (_server != null)
      return _server.getBroker();
    else
      return null;
  }

  /**
   * Create and configure the j2ee deploy service.
   */
  public Object createDeployService()
  {
    return createService("com.caucho.server.admin.DeployService");
  }
  /**
   * Create and configure the jmx service.
   */
  public Object createJmxService()
  {
    return createService("com.caucho.admin.JmxService");
  }

  /**
   * Create and configure the persistent logger.
   */
  public Object createLogService()
  {
    return createService("com.caucho.admin.LogService");
  }

  /**
   * Creates the remote service
   */
  public Object createRemoteService()
  {
    return createService("com.caucho.admin.RemoteAdminService");
  }

  /**
   * Create and configure the stat service
   */
  public Object createStatService()
  {
    return createService("com.caucho.admin.StatService");
  }

  /**
   * Create and configure the stat service
   */
  public Object createPing()
  {
    return createService("com.caucho.server.admin.PingThread");
  }

  /**
   * Create and configure the stat service
   */
  public Object createXaLogService()
  {
    return createService("com.caucho.admin.XaLogService");
  }

  /**
   * backwards compat
   */
  @Deprecated
  public void setManagementPath(Path managementPath)
  {
    setPath(managementPath);
  }

  /**
   * backwards compat
   */
  @Deprecated
  public TransactionManager createTransactionManager()
    throws ConfigException
  {
    if (_transactionManager == null)
      _transactionManager = new TransactionManager(this);

    return _transactionManager;
  }

  @PostConstruct
  public void init()
  {
    try {
      if (! _lifecycle.toInit())
        return;

      if (_auth != null) {
        _auth.init();

        InjectManager cdiManager = InjectManager.create();
        BeanBuilder<?> factory = cdiManager.createBeanFactory(Authenticator.class);
        factory.type(Authenticator.class);
        factory.type(AdminAuthenticator.class);
        
        factory.qualifier(DefaultLiteral.DEFAULT);
        factory.qualifier(new AdminLiteral());

        cdiManager.addBean(factory.singleton(_auth));
      }

      if (_transactionManager != null)
        _transactionManager.start();
    } catch (Exception e) {
      e.printStackTrace();

      throw ConfigException.create(e);
    }
  }

  /**
   * Starts the management server
   */
  public void start(Server server)
  {
    /*
    if (_deployService != null)
      _deployService.start();
    */
  }

  public HostConfig getHostConfig()
  {
    if (_hostConfig == null) {
      HostConfig hostConfig = new HostConfig();
      hostConfig.setId(HOST_NAME);
      /*
      if (_path != null) {
        hostConfig.setRootDirectory(new RawString(_path.getFullPath() + "/bogus-admin"));
      }
      else
        hostConfig.setRootDirectory(new RawString("/bogus-admin"));
      */
      hostConfig.setRootDirectory(new RawString("/bogus-admin"));

      hostConfig.setSkipDefaultConfig(true);

      hostConfig.init();

      try {
        if (_server == null)
          _server = _resin.getServer();

        if (_server != null)
          _server.addHost(hostConfig);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw ConfigException.create(e);
      }

      _hostConfig = hostConfig;
    }

    return _hostConfig;
  }

  public double getCpuLoad()
  {
    return 0;
  }

  public void dumpThreads()
  {
  }

  private Object createService(String className)
  {
    int p = className.lastIndexOf('.');
    String shortName = className.substring(p + 1);
    
    try {
      Class<?> cl = Class.forName(className);
      
      return cl.newInstance();
    } catch (Exception e) {
      throw ConfigException.create(L.l("{0} is an unavailable service because it requires Resin Professional.\n  {1}",
                                       shortName, e.toString()),
                                   e);
    }
  }

  public void destroy()
  {
    TransactionManager transactionManager = _transactionManager;
    _transactionManager = null;

    if (transactionManager != null)
      transactionManager.destroy();
  }

  public static class User {
    private String _name;
    private String _password;
    private boolean _isDisabled;

    public void setName(String name)
    {
      _name = name;
    }

    public String getName()
    {
      return _name;
    }

    public void setPassword(String password)
    {
      _password = password;
    }

    public String getPassword()
    {
      return _password;
    }

    public void setDisable(boolean isDisabled)
    {
      _isDisabled = isDisabled;
    }

    public boolean isDisable()
    {
      return _isDisabled;
    }

    PasswordUser getPasswordUser()
    {
      if (_name == null)
        throw new ConfigException(L.l("management <user> requires a 'name' attribute"));

      boolean isAnonymous = false;

      return new PasswordUser(new BasicPrincipal(_name),
                              _password.toCharArray(),
                              _isDisabled, isAnonymous,
                              new String[] { "resin-admin" });
    }
  }
}
