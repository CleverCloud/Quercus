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

package com.caucho.boot;

import java.util.ArrayList;
import java.util.HashMap;

import com.caucho.cloud.security.SecurityService;
import com.caucho.config.ConfigException;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.security.AdminAuthenticator;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

public class BootResinConfig implements EnvironmentBean
{
  private static final L10N L = new L10N(BootResinConfig.class);

  private boolean _isWatchdogManagerConfig;
  
  private ArrayList<ContainerProgram> _clusterDefaultList
    = new ArrayList<ContainerProgram>();

  private ArrayList<BootClusterConfig> _clusterList
    = new ArrayList<BootClusterConfig>();
  
  private HashMap<String,WatchdogClient> _watchdogMap
    = new HashMap<String,WatchdogClient>();
  
  private HashMap<String,WatchdogConfig> _serverMap
    = new HashMap<String,WatchdogConfig>();

  private ClassLoader _classLoader;

  private WatchdogArgs _args;
  
  private Path _resinHome;
  private Path _rootDirectory;
  private Path _resinDataDirectory;
  
  private BootManagementConfig _management;
  private String _resinSystemKey;
  
  BootResinConfig(WatchdogArgs args)
  {
    _args = args;
  
    _classLoader = EnvironmentClassLoader.create();
  }
  
  WatchdogArgs getArgs()
  {
    return _args;
  }
  
  public Path getResinHome()
  {
    if (_resinHome != null)
      return _resinHome;
    else
      return _args.getResinHome();
  }
  
  public void setRootDirectory(Path rootDirectory)
  {
    _rootDirectory = rootDirectory;
  }

  public Path getRootDirectory()
  {
    if (_rootDirectory != null)
      return _rootDirectory;
    else
      return _args.getRootDirectory();
  }
  
  public Path getLogDirectory()
  {
    Path logDirectory = _args.getLogDirectory();

    if (logDirectory != null)
      return logDirectory;
    else
      return getRootDirectory().lookup("log");
  }
  
  public void setResinDataDirectory(Path path)
  {
    _resinDataDirectory = path;
  }
  
  public Path getResinDataDirectory()
  {
    if (_resinDataDirectory != null)
      return _resinDataDirectory;
    else
      return getRootDirectory().lookup("resin-data");
  }
  
  public void setResinSystemAuthKey(String digest)
  {
    SecurityService.create().setSignatureSecret(digest);
    _resinSystemKey = digest;
  }
  
  public String getResinSystemAuthKey()
  {
    return _resinSystemKey;
  }

  @Override
  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }

  public void add(AdminAuthenticator auth)
  {
    createManagement().setAdminAuthenticator(auth);
  }

  public BootManagementConfig createManagement()
  {
    if (_management == null)
      _management = new BootManagementConfig();

    return _management;
  }

  /**
   * Adds the management configuration
   */
  public void setManagement(BootManagementConfig management)
  {
    _management = management;
  }

  /**
   * The management configuration
   */
  public BootManagementConfig getManagement()
  {
    return _management;
  }
  
  /**
   * Returns true if there is a <watchdog-manager> config.
   */
  public boolean isWatchdogManagerConfig()
  {
    return _isWatchdogManagerConfig;
  }

  /**
   * Finds a server.
   */
  public WatchdogClient findClient(String id)
  {
    return _watchdogMap.get(id);
  }

  /**
   * Finds a server.
   */
  public void addClient(WatchdogClient client)
  {
    _watchdogMap.put(client.getId(), client);
  }

  /**
   * Finds a server.
   */
  public WatchdogConfig findServer(String id)
  {
    return _serverMap.get(id);
  }

  /**
   * Finds a server.
   */
  public void addServer(WatchdogConfig config)
  {
    _serverMap.put(config.getId(), config);
  }

  /**
   * Finds a server.
   */
  WatchdogClient addDynamicClient(WatchdogArgs args)
  {
    if (! args.isDynamicServer())
      throw new IllegalStateException();

    String clusterId = args.getDynamicCluster();
    String address = args.getDynamicAddress();
    int port = args.getDynamicPort();

    BootClusterConfig cluster = findCluster(clusterId);

    if (cluster == null)
      throw new ConfigException(L.l("'{0}' is an unknown cluster. -dynamic-server must specify an existing cluster",
                                    clusterId));

    if (! cluster.isDynamicServerEnable()) {
      throw new ConfigException(L.l("cluster '{0}' does not have <dynamic-server-enable>. -dynamic-server requires a <dynamic-server-enable> tag.",
                                    clusterId));
    }

    WatchdogConfig config = cluster.createServer();
    config.setId(address + "-" + port);
    config.setDynamic(true);
    config.setAddress(address);
    config.setPort(port);

    cluster.addServer(config);

    addServer(config);

    WatchdogClient client = new WatchdogClient(BootResinConfig.this, config);
    addClient(client);

    return client;
  }

  /**
   * Creates the watchdog-manager config
   */
  public WatchdogManagerConfig createWatchdogManager()
  {
    _isWatchdogManagerConfig = true;
    
    return new WatchdogManagerConfig(this);
  }

  /**
   * Adds a new default to the cluster.
   */
  public void addClusterDefault(ContainerProgram program)
  {
    _clusterDefaultList.add(program);
  }

  public BootClusterConfig createCluster()
  {
    BootClusterConfig cluster = new BootClusterConfig(this);

    for (int i = 0; i < _clusterDefaultList.size(); i++)
      _clusterDefaultList.get(i).configure(cluster);

    _clusterList.add(cluster);
    
    return cluster;
  }

  BootClusterConfig findCluster(String id)
  {
    for (int i = 0; i < _clusterList.size(); i++) {
      BootClusterConfig cluster = _clusterList.get(i);

      if (id.equals(cluster.getId()))
        return cluster;
    }

    return null;
  }
  
  /**
   * Ignore items we can't understand.
   */
  public void addContentProgram(ConfigProgram program)
  {
  }
}
