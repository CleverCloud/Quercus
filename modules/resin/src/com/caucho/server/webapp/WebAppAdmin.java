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

package com.caucho.server.webapp;

import java.util.Date;
import java.util.Map;

import com.caucho.management.server.HostMXBean;
import com.caucho.management.server.SessionManagerMXBean;
import com.caucho.management.server.WebAppMXBean;
import com.caucho.server.deploy.DeployControllerAdmin;
import com.caucho.server.host.Host;
import com.caucho.util.L10N;

/**
 * The admin implementation for a web-app.
 */
public class WebAppAdmin extends DeployControllerAdmin<WebAppController>
  implements WebAppMXBean
{
  private static final L10N L = new L10N(WebAppAdmin.class);

  public WebAppAdmin(WebAppController controller)
  {
    super(controller);
  }

  //
  // Hierarchy attributes
  //

  /**
   * Returns the owning host
   */
  public HostMXBean getHost()
  {
    Host host = getController().getHost();

    if (host != null)
      return host.getAdmin();
    else
      return null;
  }

  public SessionManagerMXBean getSessionManager()
  {
    WebApp app = getWebApp();

    if (app == null)
      return null;

    return app.getSessionManager().getAdmin();
  }

  //
  // Configuration attribute
  // 

  /**
   * Returns the context path
   */
  public String getContextPath()
  {
    return getController().getContextPath();
  }

  /**
   * Returns the web-app version number
   */
  public String getVersion()
  {
    return getController().getVersion();
  }

  /**
   * Returns the manifest attributes
   */
  public Map<String,String> getManifestAttributes()
  {
    return getController().getManifestAttributes();
  }

  //
  // error statistics
  //

  public long getStatus500CountTotal()
  {
    return getWebApp().getStatus500CountTotal();
  }

  public Date getStatus500LastTime()
  {
    long lastTime = getWebApp().getStatus500LastTime();

    if (lastTime > 0)
      return new Date(lastTime);
    else
      return null;
  }

  //
  // statistics
  //
  
  public int getRequestCount()
  {
    return getWebApp().getRequestCount();
  }

  public long getRequestCountTotal()
  {
    return getController().getLifetimeConnectionCount();
  }

  public long getRequestTimeTotal()
  {
    return getController().getLifetimeConnectionTime();
  }

  public long getRequestReadBytesTotal()
  {
    return getController().getLifetimeReadBytes();
  }

  public long getRequestWriteBytesTotal()
  {
    return getController().getLifetimeWriteBytes();
  }

  public long getClientDisconnectCountTotal()
  {
    return getController().getLifetimeClientDisconnectCount();
  }

  /**
   * Returns the active webApp.
   */
  protected WebApp getWebApp()
  {
    return getController().getWebApp();
  }

}
