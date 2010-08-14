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

import com.caucho.config.ConfigException;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;

import com.caucho.util.L10N;
import java.util.ArrayList;

public class BootClusterConfig {
  private static final L10N L = new L10N(BootClusterConfig.class);
  
  private BootResinConfig _resin;
  
  private String _id = "";
  private boolean _isDynamicServerEnable;
  private ArrayList<ContainerProgram> _serverDefaultList
    = new ArrayList<ContainerProgram>();

  BootClusterConfig(BootResinConfig resin)
  {
    _resin = resin;
  }

  public void setId(String id)
  {
    _id = id;
  }

  public String getId()
  {
    return _id;
  }

  public void setDynamicServerEnable(boolean isEnabled)
  {
    _isDynamicServerEnable = isEnabled;
  }

  public boolean isDynamicServerEnable()
  {
    return _isDynamicServerEnable;
  }

  /**
   * Adds a new server to the cluster.
   */
  public void addServerDefault(ContainerProgram program)
  {
    _serverDefaultList.add(program);
  }

  public void addManagement(BootManagementConfig management)
  {
    _resin.setManagement(management);
  }

  public WatchdogConfig createServer()
  {
    WatchdogConfig config
      = new WatchdogConfig(_resin.getArgs(), _resin.getRootDirectory());

    for (int i = 0; i < _serverDefaultList.size(); i++)
      _serverDefaultList.get(i).configure(config);

    return config;
  }

  public void addServer(WatchdogConfig config)
    throws ConfigException
  {
    if (_resin.isWatchdogManagerConfig())
      return;
      
    if (_resin.findClient(config.getId()) != null)
      throw new ConfigException(L.l("<server id='{0}'> is a duplicate server.  servers must have unique ids.",
                                    config.getId()));
      
    _resin.addServer(config);
    _resin.addClient(new WatchdogClient(_resin, config));
  }
  
  /**
   * Ignore items we can't understand.
   */
  public void addContentProgram(ConfigProgram program)
  {
  }
}
