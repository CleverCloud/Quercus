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

package com.caucho.admin;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.config.ConfigException;
import com.caucho.config.types.RawString;
import com.caucho.server.cluster.Server;
import com.caucho.server.dispatch.ServletMapping;
import com.caucho.server.host.HostConfig;
import com.caucho.server.webapp.WebApp;
import com.caucho.server.webapp.WebAppConfig;
import com.caucho.util.L10N;

/**
 * Enables remote administration
 */
@com.caucho.config.Service
public class RemoteAdminService
{
  private static final Logger log
    = Logger.getLogger(RemoteAdminService.class.getName());
  private static final L10N L = new L10N(RemoteAdminService.class);

  private String _hostName = "admin.resin";
  private Server _server;
  private boolean _isAuthenticationRequired = true;

  private WebApp _webApp;

  public void setAuthenticationRequired(boolean isAuthenticationRequired)
  {
    _isAuthenticationRequired = isAuthenticationRequired;
  }

  @PostConstruct
  public void init()
    throws Exception
  {
    _server = Server.getCurrent();

    if (_server == null)
      throw new ConfigException(L.l("<admin:{0}> may only be instantiated in an active server",
                                    getClass().getSimpleName()));

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      HostConfig hostConfig = new HostConfig();
      hostConfig.setHostName(new RawString(_hostName));
      hostConfig.setRootDirectory(new RawString("error:" + _hostName));
      hostConfig.setSkipDefaultConfig(true);
      hostConfig.setRedeployMode("manual");

      WebAppConfig webAppConfig = new WebAppConfig();
      webAppConfig.setId("/");
      webAppConfig.setRootDirectory(new RawString("error:/ROOT"));
      webAppConfig.setSkipDefaultConfig(true);
      webAppConfig.setRedeployMode("manual");

      hostConfig.addPropertyProgram("web-app", webAppConfig);

      // host.addWebApp(webAppConfig);
      
      ServletMapping mapping = new ServletMapping();
      mapping.addURLPattern("/hmtp");
      mapping.setServletClass("com.caucho.remote.HmtpServlet");
      mapping.setInitParam("authentication-required",
                           String.valueOf(_isAuthenticationRequired));
      mapping.setInitParam("admin", "true");
      mapping.init();

      webAppConfig.addPropertyProgram("servlet-mapping", mapping);
      
      _server.addHost(hostConfig);
      
      if (log.isLoggable(Level.FINER))
        log.finer(this + " enabled at http://" + _hostName + "/hmtp");
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  public WebApp getWebApp()
  {
    return _webApp;
  }

  @Override
  public String toString()
  {
    if (_server != null)
      return getClass().getSimpleName() + "[" + _server.getServerId() + "]";
    else
      return getClass().getSimpleName() + "[" + null + "]";
  }
}
