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
import com.caucho.config.program.ContainerProgram;

import com.caucho.util.L10N;
import java.util.ArrayList;

class WatchdogManagerConfig {
  private static final L10N L = new L10N(WatchdogManagerConfig.class);
  
  private BootResinConfig _resin;
  
  private ArrayList<ContainerProgram> _watchdogDefaultList
    = new ArrayList<ContainerProgram>();

  WatchdogManagerConfig(BootResinConfig resin)
  {
    _resin = resin;
  }

  public void setWatchdogPort(int watchdogPort)
  {
    if (_resin.getArgs().getWatchdogPort() == 0)
      _resin.getArgs().setWatchdogPort(watchdogPort);
  }
    
  /**
   * Adds a new server to the cluster.
   */
  public void addWatchdogDefault(ContainerProgram program)
  {
    _watchdogDefaultList.add(program);
  }

  public WatchdogConfig createWatchdog()
  {
    WatchdogConfig config
      = new WatchdogConfig(_resin.getArgs(), _resin.getRootDirectory());

    for (int i = 0; i < _watchdogDefaultList.size(); i++)
      _watchdogDefaultList.get(i).configure(config);

    return config;
  }

  public void addWatchdog(WatchdogConfig config)
    throws ConfigException
  {
    if (_resin.findClient(config.getId()) != null)
      throw new ConfigException(L.l("<server id='{0}'> is a duplicate server.  servers must have unique ids.",
                                    config.getId()));
      
    _resin.addClient(new WatchdogClient(_resin, config));
  }
}
