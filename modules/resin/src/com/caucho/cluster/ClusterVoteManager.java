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

package com.caucho.cluster;

import com.caucho.cloud.network.ClusterServer;
import com.caucho.config.Configurable;
import com.caucho.config.ConfigException;
import com.caucho.loader.Environment;
import com.caucho.server.cluster.Server;
import com.caucho.server.distlock.AbstractVoteManager;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

/**
 * Distributed vote manager.
 */

@ApplicationScoped
@Configurable  
public class ClusterVoteManager
{
  private static final L10N L = new L10N(ClusterVoteManager.class);
  
  private AbstractVoteManager _manager;

  private String _name;
  private String _guid;

  private boolean _isInit;
  
  public ClusterVoteManager()
  {
  }
  
  public ClusterVoteManager(String name)
  {
    setName(name);
    init();
  }
  
  /**
   * Returns the name of the lock.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Assigns the name of the lock.
   * A name is mandatory and must be unique among open locks.
   */
  @Configurable
  public void setName(String name)
  {
    _name = name;
  }

  public void setGuid(String guid)
  {
    _guid = guid;
  }

  //
  // Vote API
  //

  /**
   * Selects a cluster owner
   */
  public ClusterServer electServer(String name)
  {
    return _manager.electServer(_guid, name);
  }

  /**
   * Initialize the lock.
   */
  @PostConstruct
  public void init()
  {
    synchronized (this) {
      if (_isInit)
        return;

      _isInit = true;

      initServer();

      initName(_name);
    }
  }

  private void initName(String name)
    throws ConfigException
  {
    if (_name == null || _name.length() == 0)
      throw new ConfigException(L.l("'{0}' requires a name because each vote manager must be identified uniquely.", getClass().getSimpleName()));

    // HashSet<String> cacheNameSet = getLocalCacheNameSet();
    String contextId = Environment.getEnvironmentName();

    if (_guid == null)
      _guid = contextId + ":" + _name;

    /*
    _config.setGuid(_guid);

    if (! cacheNameSet.contains(_guid))
      cacheNameSet.add(_guid);
    else
      throw new ConfigException(L.l(
                                    "'{0}' is an invalid Cache name because it's already used by another cache.",
                                    _name));
    */
  }

  private void initServer()
    throws ConfigException
  {
    Server server = Server.getCurrent();
    
    if (server == null)
      throw new ConfigException(L.l("'{0}' cannot be initialized because it is not in a Resin environment",
                                    getClass().getSimpleName()));

    _manager = server.getDistributedVoteManager();

    if (_manager == null)
      throw new IllegalStateException(L.l("distributed vote manager not available"));
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _guid + "]";
  }
}
