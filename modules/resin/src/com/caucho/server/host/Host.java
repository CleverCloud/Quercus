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

package com.caucho.server.host;

import com.caucho.bam.*;
import com.caucho.cloud.topology.CloudCluster;
import com.caucho.config.ConfigException;
import com.caucho.config.SchemaBean;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.SingletonBean;
import com.caucho.hemp.broker.*;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.make.AlwaysModified;
import com.caucho.management.server.HostMXBean;
import com.caucho.network.listen.SocketLinkListener;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.Server;
import com.caucho.server.deploy.EnvironmentDeployInstance;
import com.caucho.server.dispatch.DispatchServer;
import com.caucho.server.dispatch.ExceptionFilterChain;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.resin.*;
import com.caucho.server.webapp.WebAppContainer;
import com.caucho.util.L10N;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.Path;

import java.net.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;

/**
 * Resin's virtual host implementation.
 */
public class Host extends WebAppContainer
  implements EnvironmentBean, Dependency, SchemaBean,
             EnvironmentDeployInstance
{
  private static final Logger log = Logger.getLogger(Host.class.getName());
  private static final L10N L = new L10N(Host.class);

  private static EnvironmentLocal<Host> _hostLocal
    = new EnvironmentLocal<Host>("caucho.host");

  private HostContainer _parent;

  // The Host entry
  private HostController _controller;

  // The canonical host name.  The host name may include the port.
  private String _hostName = "";
  // The canonical URL
  private String _url;

  private String _serverName = "";
  private int _serverPort = 0;

  // The secure host
  private String _secureHostName;

  private boolean _isDefaultHost;

  // Alises
  private ArrayList<String> _aliasList = new ArrayList<String>();

  private HempBroker _bamBroker;

  private Throwable _configException;

  private boolean _isRootDirSet;
  private boolean _isDocDirSet;

  private String _configETag = null;

  /**
   * Creates the webApp with its environment loader.
   */
  public Host(HostContainer parent, HostController controller, String hostName)
  {
    super(EnvironmentClassLoader.create("host:" + controller.getName()),
          new Lifecycle(log, "Host[" + hostName + "]", Level.INFO));

    try {
      _controller = controller;

      setParent(parent);
      setHostName(hostName);

      _hostLocal.set(this, getClassLoader());
    } catch (Exception e) {
      _configException = e;
    }
  }

  /**
   * Returns the local host.
   */
  public static Host getLocal()
  {
    return _hostLocal.get();
  }

  /**
   * Sets the canonical host name.
   */
  private void setHostName(String name)
    throws ConfigException
  {
    _hostName = name;

    if (name.equals(""))
      _isDefaultHost = true;

    addHostAlias(name);

    // getEnvironmentClassLoader().setId("host:" + name);

    // _jmxContext.put("J2EEServer", name);

    int p = name.indexOf("://");

    if (p >= 0)
      name = name.substring(p + 3);

    _serverName = name;

    p = name.lastIndexOf(':');
    if (p > 0) {
      _serverName = name.substring(0, p);

      boolean isPort = true;
      int port = 0;
      for (p++; p < name.length(); p++) {
        char ch = name.charAt(p);

        if ('0' <= ch && ch <= '9')
          port = 10 * port + ch - '0';
        else
          isPort = false;
      }

      if (isPort)
        _serverPort = port;
    }
  }

  /**
   * Returns the entry name
   */
  public String getName()
  {
    return _controller.getName();
  }

  /**
   * Returns the canonical host name.  The canonical host name may include
   * the port.
   */
  public String getHostName()
  {
    return _hostName;
  }

  /**
   * Returns the host (as an webApp container)
   */
  public Host getHost()
  {
    return this;
  }

  /**
   * Returns the secure host name.  Used for redirects.
   */
  public String getSecureHostName()
  {
    return _secureHostName;
  }

  /**
   * Sets the secure host name.  Used for redirects.
   */
  public void setSecureHostName(String secureHostName)
  {
    _secureHostName = secureHostName;
  }

  /**
   * Returns the bam broker.
   */
  public Broker getBamBroker()
  {
    return _bamBroker;
  }

  /**
   * Returns the relax schema.
   */
  public String getSchema()
  {
    return "com/caucho/server/host/host.rnc";
  }

  /**
   * Returns the URL for the container.
   */
  public String getId()
  {
    if (_url != null && ! "".equals(_url))
      return _url;
    else if (_hostName == null
             || _hostName.equals("")) {
      return getURL();
    }
    else if (_hostName.startsWith("http:")
             || _hostName.startsWith("https:"))
      return _hostName;
    else if (_hostName.equals(""))
      return "http://default";
    else
      return "http://" + _hostName;
  }

  /**
   * Returns the URL for the container.
   */
  public String getURL()
  {
    if (_url != null && ! "".equals(_url))
      return _url;
    else if (_hostName == null
             || _hostName.equals("")
             || _hostName.equals("default")) {
      Server server = getServer();

      if (server == null)
        return "http://localhost";

      for (SocketLinkListener port : server.getPorts()) {
        if ("http".equals(port.getProtocolName())) {
          String address = port.getAddress();

          if (address == null || address.equals(""))
            address = "localhost";

          return "http://" + address + ":" + port.getPort();
        }
      }

      for (SocketLinkListener port : server.getPorts()) {
        if ("https".equals(port.getProtocolName())) {
          String address = port.getAddress();
          if (address == null || address.equals(""))
            address = "localhost";

          return "https://" + address + ":" + port.getPort();
        }
      }

      return "http://localhost";
    }
    else if (_hostName.startsWith("http:")
             || _hostName.startsWith("https:"))
      return _hostName;
    else if (_hostName.equals("") || _hostName.equals("default"))
      return "http://localhost";
    else
      return "http://" + _hostName;
  }

  /**
   * Adds an alias.
   */
  public void addHostAlias(String name)
  {
    name = name.toLowerCase();

    if (! _aliasList.contains(name))
      _aliasList.add(name);

    if (name.equals("") || name.equals("*"))
      _isDefaultHost = true;


    _controller.addExtHostAlias(name);
  }

  /**
   * Gets the alias list.
   */
  public ArrayList<String> getAliasList()
  {
    return _aliasList;
  }

  /**
   * Adds an alias.
   */
  public void addHostAliasRegexp(String name)
  {
    name = name.trim();

    Pattern pattern = Pattern.compile(name, Pattern.CASE_INSENSITIVE);

    _controller.addExtHostAliasRegexp(pattern);
  }

  /**
   * Returns true if matches the default host.
   */
  public boolean isDefaultHost()
  {
    return _isDefaultHost;
  }

  /**
   * Sets the parent container.
   */
  private void setParent(HostContainer parent)
  {
    _parent = parent;

    setDispatchServer(parent.getDispatchServer());

    if (! _isRootDirSet) {
      setRootDirectory(parent.getRootDirectory());
      _isRootDirSet = false;
    }
  }

  /**
   * Gets the environment class loader.
   */
  public EnvironmentClassLoader getEnvironmentClassLoader()
  {
    return (EnvironmentClassLoader) getClassLoader();
  }

  /**
   * Sets the root dir.
   */
  public void setRootDirectory(Path rootDir)
  {
    super.setRootDirectory(rootDir);
    _isRootDirSet = true;

    if (! _isDocDirSet) {
      setDocumentDirectory(rootDir);
      _isDocDirSet = false;
    }
  }

  /**
   * Sets the doc dir.
   */
  public void setDocumentDirectory(Path docDir)
  {
    super.setDocumentDirectory(docDir);
    _isDocDirSet = true;
  }

  /**
   * Sets the config exception.
   */
  public void setConfigException(Throwable e)
  {
    if (e != null) {
      _configException = e;
      getEnvironmentClassLoader().addDependency(AlwaysModified.create());

      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Gets the config exception.
   */
  public Throwable getConfigException()
  {
    return _configException;
  }

  /**
   * Returns the owning server.
   */
  public Server getServer()
  {
    if (_parent != null) {
      DispatchServer server = _parent.getDispatchServer();

      if (server instanceof Server)
        return (Server) server;
    }

    return null;
  }

  /**
   * Returns the current cluster.
   */
  public CloudCluster getCluster()
  {
    Server server = getServer();

    if (server != null)
      return server.getCluster();
    else
      return null;
  }

  /**
   * Returns the config etag.
   */
  public String getConfigETag()
  {
    return _configETag;
  }

  /**
   * Returns the config etag.
   */
  public void setConfigETag(String etag)
  {
    _configETag = etag;
  }

  /**
   * Returns the admin.
   */
  public HostMXBean getAdmin()
  {
    return _controller.getAdmin();
  }

  /**
   * Initialization before configuration
   */
  public void preConfigInit()
  {
  }

  /**
   * Starts the host.
   */
  protected void startImpl()
  {
    if (getURL().equals("") && _parent != null) {
      _url = _parent.getURL();
    }

    EnvironmentClassLoader loader;
    loader = getEnvironmentClassLoader();

    // server/1al2
    // loader.setId("host:" + getURL());

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(loader);

      initBam();

      // ioc/04010
      // loader needs to start first, so Host managed beans will be
      // initialized before the webappd
      loader.start();

      super.startImpl();

      if (_parent != null)
        _parent.clearCache();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private void initBam()
  {
    if (Resin.getCurrent() == null)
      return;

    String hostName = _hostName;

    if ("".equals(hostName)) {
      try {
        hostName = InetAddress.getLocalHost().getCanonicalHostName();
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }

    HempBrokerManager brokerManager = HempBrokerManager.getCurrent();

    _bamBroker = new HempBroker(brokerManager, hostName);

    if (brokerManager != null)
      brokerManager.addBroker(hostName, _bamBroker);

    for (String alias : _aliasList) {
      _bamBroker.addAlias(alias);

      if (brokerManager != null)
        brokerManager.addBroker(alias, _bamBroker);
    }

    InjectManager webBeans = InjectManager.getCurrent();

    webBeans.addBean(webBeans.createBeanFactory(Broker.class)
                     .name("bamBroker").singleton(_bamBroker));

    webBeans.addExtension(_bamBroker);

    // XXX: webBeans.addRegistrationListener(new BamRegisterListener());
  }

  /**
   * Clears the cache
   */
  public void clearCache()
  {
    super.clearCache();

    setConfigETag(null);
  }

  /**
   * Builds the invocation for the host.
   */
  public Invocation buildInvocation(Invocation invocation)
    throws ConfigException
  {
    invocation.setHostName(_serverName);
    invocation.setPort(_serverPort);

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());

      if (_configException == null)
        return super.buildInvocation(invocation);
      else {
        invocation.setFilterChain(new ExceptionFilterChain(_configException));
        invocation.setDependency(AlwaysModified.create());

        return invocation;
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Returns true if the host is modified.
   */
  public boolean isModified()
  {
    return (isDestroyed() || getEnvironmentClassLoader().isModified());
  }

  /**
   * Returns true if the host is modified.
   */
  public boolean isModifiedNow()
  {
    return (isDestroyed() || getEnvironmentClassLoader().isModifiedNow());
  }

  /**
   * Log the reason for modification.
   */
  public boolean logModified(Logger log)
  {
    if (isDestroyed())
      return true;
    else
      return getEnvironmentClassLoader().logModified(log);
  }

  /**
   * Returns true if the host deploy was an error
   */
  public boolean isDeployError()
  {
    return _configException != null;
  }

  /**
   * Returns true if the host is idle
   */
  public boolean isDeployIdle()
  {
    return false;
  }

  /**
   * Stops the host.
   */
  public boolean stop()
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      EnvironmentClassLoader envLoader = getEnvironmentClassLoader();
      thread.setContextClassLoader(envLoader);

      if (! _lifecycle.toStopping())
        return false;

      super.stop();

      if (_bamBroker != null)
        _bamBroker.close();

      envLoader.stop();

      return true;
    } finally {
      _lifecycle.toStop();

      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Closes the host.
   */
  public void destroy()
  {
    stop();

    if (isDestroyed())
      return;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    EnvironmentClassLoader classLoader = getEnvironmentClassLoader();

    thread.setContextClassLoader(classLoader);

    try {
      super.destroy();
    } finally {
      thread.setContextClassLoader(oldLoader);

      classLoader.destroy();
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getHostName() + "]";
  }
}
