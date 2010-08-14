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

package com.caucho.server.webapp;

import com.caucho.server.deploy.DeployContainer;
import com.caucho.server.deploy.DeployGenerator;
import com.caucho.server.e_app.EarDeployController;
import com.caucho.server.e_app.EarDeployGenerator;

import java.util.logging.Logger;

/**
 * The generator for the ear deploy
 */
public class WebAppEarDeployGenerator extends DeployGenerator<WebAppController> {
  private static final Logger log
    = Logger.getLogger(WebAppEarDeployGenerator.class.getName());

  private WebAppContainer _container;
  
  private String _urlPrefix = "";

  private ClassLoader _parentLoader;
  
  private DeployContainer<EarDeployController> _earContainer;

  private EarDeployGenerator _earDeploy;

  /**
   * Creates the new host deploy.
   */
  public WebAppEarDeployGenerator(DeployContainer<WebAppController> deployContainer,
                                  WebAppContainer container,
                                  EarDeployGenerator earDeploy)
    throws Exception
  {
    super(deployContainer);
    
    setContainer(container);

    _earDeploy = earDeploy;
    _earContainer = earDeploy.getDeployContainer();
  }

  /**
   * Gets the webApp container.
   */
  public WebAppContainer getContainer()
  {
    return _container;
  }

  /**
   * Sets the webApp container.
   */
  public void setContainer(WebAppContainer container)
  {
    _container = container;

    if (_parentLoader == null)
      _parentLoader = container.getClassLoader();
  }

  /**
   * Sets the parent loader.
   */
  public void setParentClassLoader(ClassLoader loader)
  {
    _parentLoader = loader;
  }

  /**
   * Sets the url prefix.
   */
  public void setURLPrefix(String prefix)
  {
    while (prefix.endsWith("/")) {
      prefix = prefix.substring(0, prefix.length() - 1);
    }
    
    _urlPrefix = prefix;
  }

  /**
   * Gets the url prefix.
   */
  public String getURLPrefix()
  {
    return _urlPrefix;
  }

  /**
   * Returns the log.
   */
  protected Logger getLog()
  {
    return log;
  }

  /**
   * Starts the deployment.
   */
  @Override
  protected void startImpl()
  {
    super.startImpl();

    _earContainer.start();
  }

  /**
   * Return true if modified.
   */
  public boolean isModified()
  {
    return _earContainer.isModified();
  }

  /**
   * Redeploys if modified.
   */
  public void update()
  {
    _earContainer.update();
  }
  
  /**
   * Returns the current array of webApp entries.
   */
  @Override
  public WebAppController generateController(String name)
  {
    for (EarDeployController earController : _earContainer.getControllers()) {
      WebAppController webAppController;

      webAppController = earController.findWebAppController(name);

      if (webAppController != null)
        return webAppController;
    }

    return null;
  }
  
  /**
   * Destroy the deployment.
   */
  @Override
  protected void stopImpl()
  {
    _earContainer.stop();

    super.stopImpl();
  }
  
  /**
   * Destroy the deployment.
   */
  @Override
  protected void destroyImpl()
  {
    _earContainer.destroy();

    super.destroyImpl();
  }

  public String toString()
  {
    return "WebAppEarDeployGenerator[" + _earDeploy + "]";
  }
}
