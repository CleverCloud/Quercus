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

package com.caucho.server.e_app;

import com.caucho.server.webapp.WebAppConfig;

import java.util.*;

/**
 * Configuration for the application web module
 */
public class WebModule {
  private String _webUri;
  private String _contextRoot;

  private ArrayList<WebAppConfig> _webAppConfig
    = new ArrayList<WebAppConfig>();;

  /**
   * Sets the location to the .war file.
   */
  public void setWebURI(String webUri)
  {
    _webUri = webUri;
  }

  /**
   * Returns the web uri.
   */
  public String getWebURI()
  {
    return _webUri;
  }

  /**
   * Sets the context-root
   */
  public void setContextRoot(String contextRoot)
  {
    _contextRoot = contextRoot;
  }

  /**
   * Gets the context-root
   */
  public String getContextRoot()
  {
    return _contextRoot;
  }

  /**
   * Customization of web-app.
   */
  public void setWebApp(WebAppConfig config)
  {
    _webAppConfig.add(config);
  }

  /**
   * Customization of web-app.
   */
  public ArrayList<WebAppConfig> getWebAppList()
  {
    return _webAppConfig;
  }

  public void addWebAppList(ArrayList<WebAppConfig> list)
  {
    _webAppConfig.addAll(list);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _webUri + "," + _contextRoot + "]";
  }
}
