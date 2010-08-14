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

package com.caucho.server.embed;

import java.io.IOException;
import java.io.InputStream;

import com.caucho.cloud.topology.CloudCluster;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.types.RawString;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.resin.HttpEmbed;
import com.caucho.server.cluster.Server;
import com.caucho.server.host.Host;
import com.caucho.server.host.HostConfig;
import com.caucho.server.resin.Resin;
import com.caucho.server.webapp.WebAppConfig;

/**
 * Embeddable version of the Resin server.
 */
public class ResinEmbed
{
  private static final String EMBED_CONF
    = "com/caucho/server/embed/resin-embed.xml";
  
  private Resin _resin = Resin.create();
  private Host _host;
  private Server _server;
  
  private int _httpPort = -1;

  private Lifecycle _lifecycle = new Lifecycle();
  
  /**
   * Creates a new resin server.
   */
  public ResinEmbed()
  {
    InputStream is = null;
    try {
      Config config = new Config();
      
      is = _resin.getClassLoader().getResourceAsStream(EMBED_CONF);

      config.configure(_resin, is);
    } catch (Exception e) {
      throw ConfigException.create(e);
    } finally {
      try {
        is.close();
      } catch (IOException e) {
      }
    }
  }

  public void setHttpPort(int port)
  {
    _httpPort = port;
  }

  public void addWebApp(String contextPath,
                        String rootDirectory)
  {
    try {
      start();

      WebAppConfig config = new WebAppConfig();
      config.setContextPath(contextPath);
      config.setRootDirectory(new RawString(rootDirectory));

      _host.addWebApp(config);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public void start()
  {
    if (! _lifecycle.toActive())
      return;
      
    try {
      _resin.start();
      _server = _resin.getServer();
      
      if (_httpPort >= 0) {
        HttpEmbed httpEmbed = new HttpEmbed(_httpPort);
        httpEmbed.bindTo(_server);
      }
      
      HostConfig hostConfig = new HostConfig();
      _server.addHost(hostConfig);
      _host = _server.getHost("", 0);
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw ConfigException.create(e);
    }
  }

  public void stop()
  {
    if (! _lifecycle.toStop())
      return;
      
    try {
      _resin.stop();
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw ConfigException.create(e);
    }
  }

  public void destroy()
  {
    if (! _lifecycle.toDestroy())
      return;
      
    try {
      _resin.destroy();
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw ConfigException.create(e);
    }
  }

  protected void finalize()
    throws Throwable
  {
    super.finalize();
    
    _resin.destroy();
  }
}
