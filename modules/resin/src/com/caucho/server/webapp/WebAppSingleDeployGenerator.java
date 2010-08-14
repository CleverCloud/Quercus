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

import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.types.PathBuilder;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentListener;
import com.caucho.server.deploy.DeployContainer;
import com.caucho.server.deploy.DeployGenerator;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

/**
 * The generator for the web-app deploy
 */
public class WebAppSingleDeployGenerator
  extends DeployGenerator<WebAppController>
  implements EnvironmentListener
{
  private static final L10N L = new L10N(WebAppSingleDeployGenerator.class);
  private static final Logger log
    = Logger.getLogger(WebAppSingleDeployGenerator.class.getName());

  private WebAppContainer _container;
  
  private WebAppController _parentWebApp;

  private String _urlPrefix = "";

  private Path _archivePath;
  private Path _rootDirectory;

  private ArrayList<WebAppConfig> _defaultList = new ArrayList<WebAppConfig>();
  private WebAppConfig _config;

  private ClassLoader _parentLoader;

  private WebAppController _controller;

  /**
   * Creates the new host deploy.
   */
  public WebAppSingleDeployGenerator(DeployContainer<WebAppController> deployContainer)
  {
    super(deployContainer);
  }

  /**
   * Creates the new web-app deploy.
   */
  public WebAppSingleDeployGenerator(DeployContainer<WebAppController> deployContainer,
                                     WebAppContainer container,
                                     WebAppConfig config)
  {
    super(deployContainer);
    
    setContainer(container);

    String contextPath = config.getContextPath();

    if (contextPath.equals("/"))
      contextPath = "";
    
    setURLPrefix(config.getContextPath());

    _config = config;
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
   * Sets the parent webApp.
   */
  public void setParentWebApp(WebAppController parent)
  {
    _parentWebApp = parent;
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
    if (! prefix.startsWith("/"))
      prefix = "/" + prefix;
    
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
   * Sets the root directory.
   */
  public void setRootDirectory(Path rootDirectory)
  {
    _rootDirectory = rootDirectory;
  }

  /**
   * Adds a default.
   */
  public void addWebAppDefault(WebAppConfig config)
  {
    _defaultList.add(config);
  }

  /**
   * Returns the log.
   */
  protected Logger getLog()
  {
    return log;
  }

  /**
   * Initializes the controller.
   */
  @Override
  protected void initImpl()
  {
    super.initImpl();

    if (_controller != null)
      return;

    String rootDir = _config.getRootDirectory();
    String archivePath = _config.getArchivePath();

    if (archivePath != null) {
      _archivePath = PathBuilder.lookupPath(archivePath, null,
                                            _container.getRootDirectory());
    }

    if (_rootDirectory == null) {
      if (rootDir != null) {
      }
      else if (_archivePath != null
               && (_urlPrefix.equals("/") || _urlPrefix.equals(""))
               && _container != null) {
        log.warning(L.l("web-app's root-directory '{0}' must be outside of the '{1}' root-directory when using 'archive-path",
                        _rootDirectory, _container));

        rootDir = "./ROOT";
      }
      else
        rootDir = "./" + _urlPrefix;
      
      _rootDirectory = PathBuilder.lookupPath(rootDir, null,
                                              _container.getDocumentDirectory());
    }
    
    _controller = new WebAppController(_urlPrefix, _urlPrefix,
                                       _rootDirectory, _container);

    _controller.setArchivePath(_archivePath);

    if (_archivePath != null)
      _controller.addDepend(_archivePath);
    
    _controller.setParentWebApp(_parentWebApp);

    for (WebAppConfig config : _defaultList)
      _controller.addConfigDefault(config);
    
    // server/1h13 vs server/2e00
    _controller.setConfig(_config);
    // _controller.addConfigDefault(_config);

    _controller.setPrologue(_config.getPrologue());

    _controller.setStartupPriority(_config.getStartupPriority());

    _controller.setSourceType("single");

    Environment.addEnvironmentListener(this, _parentLoader);
  }

  /**
   * Returns the deployed keys.
   */
  protected void fillDeployedKeys(Set<String> keys)
  {
    keys.add(_controller.getContextPath());
  }
  
  /**
   * Creates a controller given the name
   */
  public WebAppController generateController(String name)
  {
    if (name.equals(_controller.getContextPath())) {
      WebAppController webApp;
      
      webApp = new WebAppController(_urlPrefix, _urlPrefix,
                                    _rootDirectory, _container);

      webApp.setArchivePath(_controller.getArchivePath());

      return webApp;
    }
    else
      return null;
  }
  
  /**
   * Merges the controllers.
   */
  public WebAppController mergeController(WebAppController controller,
                                          String name)
  {
    // if directory matches, merge the two controllers.  The
    // last controller has priority.
    if (controller.getRootDirectory().equals(_controller.getRootDirectory())) {
      // server/1h10
      controller.setContextPath(_controller.getContextPath());
      
      controller.setDynamicDeploy(false);
      
      return controller.merge(_controller);
    }
    // else if the names don't match, return the new controller
    else if (! _controller.isNameMatch(name))
      return controller;
    // otherwise, the single deploy overrides
    else
      return _controller;
  }

  /**
   * Initialize the deployment.
   */
  public void deploy()
  {
    try {
      init();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  public Throwable getConfigException()
  {
    Throwable configException =   super.getConfigException();

    if (configException == null && _controller != null)
      configException = _controller.getConfigException();

    return configException;
  }

  /**
   * Destroy the deployment.
   */
  @Override
  protected void destroyImpl()
  {
    Environment.removeEnvironmentListener(this, _parentLoader);

    _container.removeWebAppDeploy(this);

    super.destroyImpl();
  }
  
  public String toString()
  {
    return "WebAppSingleDeployGenerator[" + _urlPrefix + "]";
  }
}
