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
import com.caucho.config.ConfigELContext;
import com.caucho.config.ConfigException;
import com.caucho.config.types.RawString;
import com.caucho.el.EL;
import com.caucho.el.MapVariableResolver;
import com.caucho.server.deploy.DeployContainer;
import com.caucho.server.deploy.ExpandDeployGenerator;
import com.caucho.vfs.Path;

import javax.el.ELContext;
import javax.el.ELResolver;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The generator for the host deploy
 */
public class HostExpandDeployGenerator
  extends ExpandDeployGenerator<HostController>
{
  private static final Logger log
    = Logger.getLogger(HostExpandDeployGenerator.class.getName());

  private final HostExpandDeployGeneratorAdmin _admin
    = new HostExpandDeployGeneratorAdmin(this);

  private HostContainer _container;

  private ArrayList<HostConfig> _hostDefaults = new ArrayList<HostConfig>();

  private String _hostName;

  /**
   * Creates the new host deploy.
   */
  public HostExpandDeployGenerator(DeployContainer<HostController> container,
                                   HostContainer hostContainer)
  {
    super(container, hostContainer.getRootDirectory());
    
    _container = hostContainer;
  }

  /**
   * Gets the host container.
   */
  public HostContainer getContainer()
  {
    return _container;
  }

  /**
   * Sets the host name.
   */
  public void setHostName(RawString name)
  {
    _hostName = name.getValue();
  }

  /**
   * Gets the host name.
   */
  public String getHostName()
  {
    return _hostName;
  }

  /**
   * Sets true for a lazy-init.
   */
  public void setLazyInit(boolean lazyInit)
    throws ConfigException
  {
    log.config("lazy-init is deprecated.  Use <startup>lazy</startup> instead.");
    if (lazyInit)
      setStartupMode("lazy");
    else
      setStartupMode("automatic");
  }

  /**
   * Adds a default.
   */
  public void addHostDefault(HostConfig config)
  {
    _hostDefaults.add(config);
  }

  @Override
  protected void initImpl()
    throws ConfigException
  {
    super.initImpl();
  }

  @Override
  protected void startImpl()
    throws ConfigException
  {
    super.startImpl();

    _admin.register();
  }

  /**
   * Returns the log.
   */
  protected Logger getLog()
  {
    return log;
  }

  /**
   * Returns the current array of application entries.
   */
  public HostController createController(String name)
  {
    // server/13g3
    if (name.equals(""))
      return null;
    
    /*
    if (! isDeployedKey(name))
      return null;
    */
    
    Path rootDirectory = getExpandDirectory().lookup("./" + name);

    String hostName = name;

    if ("default".equals(hostName))
      hostName = "";

    HostController controller
      = new HostController(hostName, rootDirectory, _container);

    Path jarPath = getArchiveDirectory().lookup("./" + name + ".jar");
    controller.setArchivePath(jarPath);
    
    if (rootDirectory.isDirectory()
        && ! isValidDirectory(rootDirectory, name))
      return null;
    else if (! rootDirectory.isDirectory()
             && ! jarPath.isFile())
      return null;

    try {
      String hostNamePattern = getHostName();

      if (hostNamePattern != null) {
        ELContext parentEnv = Config.getEnvironment();
        ELResolver resolver
          = new MapVariableResolver(controller.getVariableMap());

        ELContext env = new ConfigELContext(resolver);

        controller.setHostName(EL.evalString(hostNamePattern, env));
      }
      else
        controller.setHostName(hostName);

      controller.addDepend(jarPath);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
      
      controller.setConfigException(e);
    }

    return controller;
  }

  
  /**
   * Adds configuration to the current controller
   */
  protected HostController mergeController(HostController controller,
                                           String key)
  {
    try {
      Path expandDirectory = getExpandDirectory();
      Path rootDirectory = controller.getRootDirectory();

      if (! expandDirectory.equals(rootDirectory.getParent()))
        return controller;

      controller = super.mergeController(controller, key);
      
      controller.setStartupMode(getStartupMode());
    
      for (int i = 0; i < _hostDefaults.size(); i++)
        controller.addConfigDefault(_hostDefaults.get(i));
    } catch (ConfigException e) {
      log.warning(e.toString());
      log.log(Level.FINER, e.toString(), e);
      
      controller.setConfigException(e);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
      
      controller.setConfigException(e);
    }

    return controller;
  }

  @Override
  protected void destroyImpl()
  {
    _admin.unregister();

    super.destroyImpl();
  }

  public boolean equals(Object o)
  {
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    HostExpandDeployGenerator deploy = (HostExpandDeployGenerator) o;

    Path expandPath = getExpandDirectory();
    Path deployExpandPath = deploy.getExpandDirectory();
    if (expandPath != deployExpandPath &&
        (expandPath == null || ! expandPath.equals(deployExpandPath)))
      return false;

    return true;
  }

  public String toString()
  {
    return "HostExpandDeployGenerator[" + getExpandDirectory() + "]";
  }
}
