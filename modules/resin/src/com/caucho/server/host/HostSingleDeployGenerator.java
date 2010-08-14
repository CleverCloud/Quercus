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

package com.caucho.server.host;

import com.caucho.config.Config;
import com.caucho.server.deploy.DeployContainer;
import com.caucho.server.deploy.DeployGenerator;

import java.util.Set;
import java.util.logging.Logger;

/**
 * The generator for the host deploy
 */
public class HostSingleDeployGenerator
  extends DeployGenerator<HostController> {
  private static final Logger log
    = Logger.getLogger(HostSingleDeployGenerator.class.getName());

  private HostContainer _container;

  private HostConfig _config;

  private HostController _controller;

  /**
   * Creates the new host deploy.
   */
  public HostSingleDeployGenerator(DeployContainer<HostController> container)
  {
    super(container);
  }

  /**
   * Creates the new host deploy.
   */
  public HostSingleDeployGenerator(DeployContainer<HostController> container,
                                   HostContainer hostContainer,
                                   HostConfig config)
  {
    super(container);
    
    _container = hostContainer;

    _config = config;

    init();
  }

  /**
   * Gets the host container.
   */
  public HostContainer getContainer()
  {
    return _container;
  }

  /**
   * Gets the parent loader.
   */
  public ClassLoader getParentClassLoader()
  {
    return _container.getClassLoader();
  }

  /**
   * Returns the log.
   */
  protected Logger getLog()
  {
    return log;
  }

  /**
   * Initializes the entry.
   */
  @Override
  public void initImpl()
  {
    super.initImpl();

    String hostName = null;
    String id = null;
    
    String rawId = _config.getId();
    String rawHostName = _config.getHostName();

    if (rawId != null) {
      id = Config.evalString(rawId);

      if (id.equals("*"))  // server/1f20
        id = "";
    }

    if (rawHostName != null) {
      hostName = Config.evalString(rawHostName);

      if (rawHostName.equals("*"))  // server/1f20
        hostName = "";
    }

    if (hostName != null) {
      _controller = new HostController(hostName, _config, _container, null);

      if (id != null)
        _controller.addHostAlias(id);
    }
    else if (id != null)
      _controller = new HostController(id, _config, _container, null);
    else
      _controller = new HostController("", _config, _container, null);
  }

  /**
   * Returns the deployed keys.
   */
  protected void fillDeployedKeys(Set<String> keys)
  {
    keys.add(_controller.getName());
  }
  
  /**
   * Returns the current array of application entries.
   */
  public HostController generateController(String name)
  {
    if (_controller.isNameMatch(name)) {
      return new HostController(_controller.getName(), _config,
                                _container, null);
    }
    else
      return null;
  }
  
  /**
   * Merges the controllers.
   */
  public HostController mergeController(HostController controller,
                                        String name)
  {
    // if directory matches, merge the two controllers.  The
    // last controller has priority.
    if (controller.getRootDirectory().equals(_controller.getRootDirectory())) {
      // controller.setDynamicDeploy(false);
      
      return controller.merge(_controller);
    }
    else if (! _controller.isNameMatch(name)) {
      // else if the names don't match, return the new controller
      
      return controller;
    }
    else {
      // otherwise, the single deploy overrides
      // server/10v9
      return _controller;
    }
  }

  public Throwable getConfigException()
  {
    Throwable configException =  super.getConfigException();

    if (configException == null && _controller != null)
      configException = _controller.getConfigException();

    return configException;
  }

  public String toString()
  {
    if (_config == null)
      return "HostSingleDeployGenerator[]";
    else
      return "HostSingleDeployGenerator[" + _config.getHostName() + "]";
  }
}
