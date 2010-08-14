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

package com.caucho.resin;

import com.caucho.resin.deploy.*;
import com.caucho.server.cluster.*;

/**
 * Enables the local deployment service at /resin.deploy
 *
 * <code><pre>
 * ResinEmbed resin = new ResinEmbed();
 *
 * WebAppLocalDeployEmbed webApp = new WebAppLocalDeployEmbed();
 *
 * resin.addWebApp(webApp);
 * </pre></code>
 */
public class WebAppLocalDeployEmbed extends WebAppEmbed
{
  public static final String LOCAL_DEPLOY_CONTEXT_PATH
    = "/resin:local-deploy";

  private final ServletMappingEmbed _deploy;
  
  public WebAppLocalDeployEmbed()
  {
    super.setContextPath(LOCAL_DEPLOY_CONTEXT_PATH);

    _deploy = new ServletMappingEmbed();
    _deploy.setServletClass(LocalDeployServlet.class.getName());
    _deploy.setServletName(LocalDeployServlet.class.getName());
    _deploy.setUrlPattern("/");
    _deploy.addProperty("enable", true);
    _deploy.addProperty("role", "*");

    addServletMapping(_deploy);
  }

  /**
   * The context-path can't be set
   */
  @Override
  public void setContextPath(String contextPath)
  {
  }

  /**
   * The root directory can't be set
   */
  @Override
  public void setRootDirectory(String rootDirectory)
  {
  }

  /**
   * Sets the deploy role.
   */
  public void setRole(String role)
  {
    _deploy.addProperty("role", role);
  }
}
