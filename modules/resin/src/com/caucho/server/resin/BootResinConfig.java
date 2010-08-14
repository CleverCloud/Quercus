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

package com.caucho.server.resin;

import java.util.ArrayList;

import com.caucho.cloud.topology.CloudSystem;
import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.SchemaBean;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * The Resin class represents the top-level container for Resin.
 * It exactly matches the &lt;resin> tag in the resin.xml
 */
public class BootResinConfig implements SchemaBean
{
  private Resin _resin;

  private ContainerProgram _resinProgram
    = new ContainerProgram();

  private ArrayList<ConfigProgram> _clusterDefaults
    = new ArrayList<ConfigProgram>();

  private ArrayList<BootClusterConfig> _clusters
    = new ArrayList<BootClusterConfig>();

  /**
   * Creates a new resin server.
   */
  public BootResinConfig(Resin resin)
  {
    _resin = resin;
  }
  
  public CloudSystem getCloudSystem()
  {
    return _resin.getCloudSystem();
  }

  /**
   * Returns the relax schema.
   */
  @Override
  public String getSchema()
  {
    return "com/caucho/server/resin/resin.rnc";
  }
  
  /**
   * Returns the configured root directory.
   */
  public Path getRootDirectory()
  {
    return _resin.getRootDirectory();
  }
  
  /**
   * Sets the configured root directory
   */
  public void setRootDirectory(Path rootDirectory)
  {
    _resin.setRootDirectory(rootDirectory);
    
    Vfs.setPwd(rootDirectory);
  }
  
  /**
   * Adds a <cluster-default> for default cluster configuration.
   */
  @Configurable
  public void addClusterDefault(ContainerProgram program)
  {
    _clusterDefaults.add(program);
  }

  @Configurable
  public BootClusterConfig createCluster()
    throws ConfigException
  {
    BootClusterConfig cluster = new BootClusterConfig(this);
    
    _clusters.add(cluster);

    for (int i = 0; i < _clusterDefaults.size(); i++)
      _clusterDefaults.get(i).configure(cluster);

    return cluster;
  }

  public BootClusterConfig findCluster(String id)
  {
    for (BootClusterConfig cluster : _clusters) {
      if (id.equals(cluster.getId())) {
        return cluster;
      }
    }
    
    return null;
  }

  public ArrayList<BootClusterConfig> getClusterList()
  {
    return _clusters;
  }
  
  BootServerConfig findServer(String id)
  {
    for (BootClusterConfig cluster : getClusterList()) {
      for (BootServerConfig server : cluster.getServerList()) {
        if (id.equals(server.getId()))
          return server;
      }
    }
    
    return null;
  }
  
  public void addContentProgram(ConfigProgram program)
  {
    _resinProgram.addProgram(program);
  }
  
  public ConfigProgram getProgram()
  {
    return _resinProgram;
  }
  
  void configureServers()
  {
    /*
    for (BootClusterConfig cluster : getClusterList()) {
      for (BootServerConfig server : cluster.getServerList()) {
        server.configureServer();
      }
    }
    */
  }
}
