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

package com.caucho.server.cluster;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.cloud.network.ClusterServer;
import com.caucho.config.ConfigException;
import com.caucho.config.SchemaBean;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.management.server.ClusterMXBean;
import com.caucho.server.distcache.DistributedCacheManager;
import com.caucho.server.resin.Resin;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

/**
 * Defines a set of clustered servers.
 */
// cluster/6615 - <cluster> is not an EnvironmentBean
abstract public class Cluster
  implements EnvironmentListener, SchemaBean
{
  private static final L10N L = new L10N(Cluster.class);
  private static final Logger log = Logger.getLogger(Cluster.class.getName());

  // private static final int DECODE[];

  private String _id = "";

  private Resin _resin;

  //private EnvironmentClassLoader _classLoader;
  //private Path _rootDirectory;

  private ClusterAdmin _admin;

  private ArrayList<ContainerProgram> _serverDefaultList
    = new ArrayList<ContainerProgram>();

  private ContainerProgram _serverProgram
    = new ContainerProgram();

  private final Lifecycle _lifecycle = new Lifecycle();

  protected Cluster(Resin resin)
  {
    if (resin == null)
      throw new NullPointerException(L.l("resin argument is required"));

    _resin = resin;

    //_classLoader = EnvironmentClassLoader.create("cluster:??");

    //Environment.addEnvironmentListener(this, resin.getClassLoader());

    // Config.setProperty("cluster", new Var(), _classLoader);

    //_rootDirectory = Vfs.getPwd();
  }

  /**
   * Sets the cluster id.
   */
  public void setId(String id)
  {
    if (id == null)
      throw new NullPointerException();

    _id = id;

    // _classLoader.setId("cluster:" + _id);
  }

  /**
   * Gets the cluster id.
   */
  public String getId()
  {
    return _id;
  }

  /**
   * Returns the owning resin container.
   */
  public Resin getResin()
  {
    return _resin;
  }

  /**
   * Returns the server
   */
  public Server getServer()
  {
    return getResin().getServer();
  }

  /**
   * Returns the relax schema.
   */
  public String getSchema()
  {
    return "com/caucho/server/resin/cluster.rnc";
  }

  /**
   * Enables dynamic servers
   */
  public void setDynamicServerEnable(boolean isEnable)
  {
    log.warning(L.l("{0}: dynamic-server-enable requires Resin Professional",
                    this));
  }

  /**
   * Enables dynamic servers
   */
  public boolean isDynamicServerEnable()
  {
    return false;
  }

  /**
   * Returns the version
   */
  public long getVersion()
  {
    return 0;
  }

  /**
   * Returns the admin.
   */
  public ClusterMXBean getAdmin()
  {
    return _admin;
  }

  /**
   * Returns the list of pods for the cluster
   */
  abstract public ClusterPod []getPodList();

  /**
   * Finds the first server with the given server-id.
   */
  public ClusterServer findServer(String id)
  {
    for (ClusterPod pod : getPodList()) {
      ClusterServer server = pod.findServer(id);

      if (server != null)
        return server;
    }

    return null;
  }

  /**
   * Finds the first server with the given server-id.
   */
  public ClusterServer findServer(int podIndex,
                                  int index)
  {
    for (ClusterPod pod : getPodList()) {
      if (pod.getIndex() == podIndex) {
        for (ClusterServer server : pod.getServerList()) {
          if (server.getIndex() == index)
            return server;
        }

        return null;
      }
    }

    return null;
  }

  /**
   * Finds the first server with the given server-id.
   */
  public ClusterPod findPod(int podIndex)
  {
    for (ClusterPod pod : getPodList()) {
      if (pod.getIndex() == podIndex) {
        return pod;
      }
    }

    return null;
  }

  /**
   * Finds the first server with the given server-id.
   */
  public ClusterServer findServerByPrefix(String prefix)
  {
    for (ClusterPod pod : getPodList()) {
      ClusterServer server = pod.findServerByPrefix(prefix);

      if (server != null)
        return server;
    }

    return null;
  }

  /**
   * Finds the first server with the given server-id.
   */
  public ClusterServer findServer(String address, int port)
  {
    for (ClusterPod pod : getPodList()) {
      ClusterServer server = pod.findServer(address, port);

      if (server != null)
        return server;
    }

    return null;
  }

  /**
   * Adds a new server to the cluster.
   */
  public void addServerDefault(ContainerProgram program)
  {
    _serverDefaultList.add(program);
  }

  /**
   * Adds a new pod to the cluster
   */
  public ClusterPod createPod()
  {
    throw new UnsupportedOperationException(L.l("<pod> requires Resin Professional"));
  }

  /**
   * Adds a new server to the cluster during configuration.
   */
  abstract public ClusterServer createServer();

  /**
   * Adds a new server to the cluster during configuration.
   */
  public void addServer(ClusterServer server)
  {
  }

  /**
   * Configure the default values for the server
   */
  protected void configureServerDefault(ClusterServer server)
  {
    for (int i = 0; i < _serverDefaultList.size(); i++)
      _serverDefaultList.get(i).configure(server);
  }

  /**
   * Adds a new server to the cluster.
   */
  public void addDynamicServer(String serverId, String address, int port)
    throws ConfigException
  {
    throw new UnsupportedOperationException(L.l("{0}: dynamic servers require Resin Professional", this));
  }

  protected void setSelfServer(ClusterServer server)
  {
  }

  /**
   * Adds a new server to the cluster.
   */
  public void removeDynamicServer(ClusterServer server)
    throws ConfigException
  {
    throw new UnsupportedOperationException(L.l("{0}: dynamic servers require Resin Professional", this));
  }

  /**
   * Returns the distributed cache manager.
   */
  public DistributedCacheManager getDistributedCacheManager()
  {
    return getResin().getServer().getDistributedCacheManager();
  }

  /**
   * Adds a program.
   */
  public void addContentProgram(ConfigProgram program)
  {
    // server/4322 - resin:import and resin:if must execute for cluster
    _serverProgram.addProgram(program);
  }

  //
  // lifecycle
  //

  /**
   * Returns true if the cluster is active
   */
  public boolean isActive()
  {
    return _lifecycle.isActive();
  }

  /**
   * Initializes the cluster.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    _lifecycle.toInit();

    _admin = new ClusterAdmin(this);
    _admin.register();

    for (ClusterPod pod : getPodList()) {
      pod.init();
    }
  }

  /**
   * Start the cluster.
   */
  public void start()
    throws ConfigException
  {
    _lifecycle.toActive();

    for (ClusterPod pod : getPodList()) {
      pod.start();
    }
  }

  //
  // persistent store support
  //

  /**
   * Handles the case where a class loader has completed initialization
   */
  public void classLoaderInit(DynamicClassLoader loader)
  {
  }

  /**
   * Handles the case where a class loader is dropped.
   */
  public void classLoaderDestroy(DynamicClassLoader loader)
  {
  }

  /**
   * Start any work in notifying other members in the cluster
   * that the server is active.
   */
  public void startRemote()
  {
  }

  /**
   * Handles the case where the environment is configured (after init).
   */
  public void environmentConfigure(EnvironmentClassLoader loader)
  {
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentBind(EnvironmentClassLoader loader)
  {
  }

 /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
  }

  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
    try {
      close();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Closes the cluster.
   */
  public void close()
  {
    if (! _lifecycle.toDestroy())
      return;

    for (ClusterPod pod : getPodList()) {
      try {
        if (pod != null)
          pod.close();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }

  /**
   * EL variables
   */
  public class ClusterVar {
    /**
     * Returns the resin.id
     */
    public String getId()
    {
      return _id;
    }

    /**
     * Returns the root directory.
     *
     * @return root directory
     */
    public Path getRoot()
    {
      return Server.getCurrent().getRootDirectory();
    }

    /**
     * Returns the root directory.
     *
     * @return root directory
     */
    public Path getRootDir()
    {
      return getRoot();
    }

    /**
     * Returns the root directory.
     *
     * @return root directory
     */
    public Path getRootDirectory()
    {
      return getRoot();
    }
  }
}
