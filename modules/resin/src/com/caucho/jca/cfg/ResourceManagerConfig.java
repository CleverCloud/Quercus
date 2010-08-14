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

package com.caucho.jca.cfg;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.jca.ra.ResourceManagerImpl;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import javax.annotation.PostConstruct;
import javax.resource.spi.ResourceAdapter;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Configuration for the resource manager.
 */
public class ResourceManagerConfig {
  private static final L10N L = new L10N(ResourceManagerConfig.class);

  private Path _configDirectory;

  private ArrayList<ConnectorConfig> _connList =
    new ArrayList<ConnectorConfig>();
  
  public ResourceManagerConfig()
    throws ConfigException
  {
    ResourceManagerImpl.createLocalManager();
  }

  /**
   * Sets the configuration directory.
   */
  public void setConfigDirectory(Path path)
  {
    _configDirectory = path;
  }

  /**
   * Returns the matching connector.
   */
  public ConnectorConfig getConnector(String adapterClass)
  {
    for (int i = 0; i < _connList.size(); i++) {
      ConnectorConfig conn = _connList.get(i);
      com.caucho.jca.cfg.ResourceAdapterConfig ra = conn.getResourceAdapter();

      if (ra.getResourceadapterClass() != null &&
          adapterClass.equals(ra.getResourceadapterClass().getName()))
        return conn;
    }

    return null;
  }

  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_configDirectory == null)
      throw new ConfigException(L.l("resource-manager requires a config-directory"));
    
    try {
      Path path = _configDirectory;
      String []list = path.list();

      for (int i = 0; i < list.length; i++) {
        String name = list[i];

        if (! name.endsWith(".ra"))
          continue;

        InputStream is = path.lookup(name).openRead();
        try {
          ConnectorConfig conn = new ConnectorConfig();

          new Config().configure(conn, is, "com/caucho/jca/jca.rnc");

          _connList.add(conn);
        } finally {
          is.close();
        }
      }
    } catch (ConfigException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }

    for (int i = 0; i < _connList.size(); i++) {
      ConnectorConfig conn = _connList.get(i);

      initResource(conn);
    }
  }

  private void initResource(ConnectorConfig conn)
    throws ConfigException
  {
    ResourceAdapterConfig raCfg = conn.getResourceAdapter();
    
    try {
      Class<?> raClass = raCfg.getResourceadapterClass();

      ResourceAdapter ra = (ResourceAdapter) raClass.newInstance();

      java.lang.reflect.Method init = raClass.getMethod("init", new Class[0]);

      if (init != null)
        init.invoke(ra, (Object []) null);

      ResourceManagerImpl.addResource(ra);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}

