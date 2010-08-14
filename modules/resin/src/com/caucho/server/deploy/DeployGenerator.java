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

package com.caucho.server.deploy;

import com.caucho.config.ConfigException;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.util.L10N;
import com.caucho.vfs.Dependency;

import javax.annotation.PostConstruct;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The generator for the deploy
 */
abstract public class DeployGenerator<E extends DeployController<?>>
  implements Dependency, EnvironmentListener
{
  private static final Logger log
    = Logger.getLogger(DeployGenerator.class.getName());
  private static final L10N L = new L10N(DeployGenerator.class);

  // The owning deployment container
  private DeployContainer<E> _container;

  private ClassLoader _parentClassLoader;

  private String _startupMode = DeployController.STARTUP_AUTOMATIC;
  private String _redeployMode = DeployController.REDEPLOY_AUTOMATIC;

  private Throwable _configException;

  private final Lifecycle _lifecycle = new Lifecycle(getLog());

  /**
   * Creates the deploy.
   */
  public DeployGenerator(DeployContainer<E> container)
  {
    _parentClassLoader = Thread.currentThread().getContextClassLoader();
    _container = container;

    _lifecycle.setName(toString());
    _lifecycle.setLevel(Level.FINEST);
  }

  /**
   * Returns the deploy container.
   */
  public DeployContainer<E> getDeployContainer()
  {
    return _container;
  }

  /**
   * Returns the parent class loader.
   */
  public ClassLoader getParentClassLoader()
  {
    return _parentClassLoader;
  }

  /**
   * Sets the startup mode.
   */
  public void setStartupMode(String mode)
    throws ConfigException
  {
    _startupMode = DeployController.toStartupCode(mode);
  }

  /**
   * Gets the startup mode.
   */
  public String getStartupMode()
    throws ConfigException
  {
    return _startupMode;
  }

  /**
   * Sets the redeploy mode.
   */
  public void setRedeployMode(String mode)
    throws ConfigException
  {
    _redeployMode = DeployController.toRedeployCode(mode);
  }

  /**
   * Gets the redeploy mode.
   */
  public String getRedeployMode()
    throws ConfigException
  {
    return _redeployMode;
  }

  @PostConstruct
  final public void init()
    throws ConfigException
  {
    try {
      initImpl();
    }
    catch (RuntimeException ex) {
      _configException = ex;
      throw ex;
    }

    _lifecycle.setName(toString());
  }

  /**
   * Derived class implementation of init
   */
  protected void initImpl()
  {
  }

  /**
   * Returns true if the deployment has modified.
   */
  public boolean isModified()
  {
    return false;
  }

  /**
   * Returns true if the deployment has modified.
   */
  public boolean logModified(Logger log)
  {
    return false;
  }

  public String getState()
  {
    return _lifecycle.getStateName();
  } 

  /**
   * Starts the deployment.
   */
  final public void start()
  {
    try {
      init();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    if (! _lifecycle.toActive())
      return;

    startImpl();
  }

  public boolean isActive()
  {
    return _lifecycle.isActive();
  }

  public boolean isDestroyed()
  {
    return _lifecycle.isDestroyed();
  }

  /**
   * Derived class implentation of start.
   */
  protected void startImpl()
  {
    Environment.addEnvironmentListener(this);
  }

  /**
   * lazy-start
   */
  public void request()
  {
  }

  /**
   * Forces an update.
   */
  public void update()
  {
  }

  /**
   * Returns the deployed keys.
   */
  protected void fillDeployedKeys(Set<String> keys)
  {
  }

  /**
   * Generates the controller.
   */
  protected E generateController(String key)
  {
    return null;
  }

  /**
   * Merges the entry with other matching entries, returning the
   * new entry.
   */
  protected E mergeController(E controller, String key)
  {
    return controller;
  }

  /**
   * Returns the log.
   */
  protected Logger getLog()
  {
    return log;
  }

  /**
   * Stops the deploy
   */
  final public void stop()
  {
    if (!_lifecycle.toStop())
      return;

    stopImpl();
  }

  /**
   * Derived class implentation of stop.
   */
  protected void stopImpl()
  {
  }

  public Throwable getConfigException()
  {
    return _configException;
  }

  /**
   * Closes the deploy
   */
  final public void destroy()
  {
    try {
      stop();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    if (!_lifecycle.toDestroy())
      return;

    destroyImpl();
  }

  /**
   * Derived class implentation of destroy.
   */
  protected void destroyImpl()
  {
    _container.remove(this);
  }

  /**
   * Handles the case where the environment is configuring (after init).
   */
  public void environmentConfigure(EnvironmentClassLoader loader)
  {
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentBind(EnvironmentClassLoader loader)
  {
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
    start();
  }

  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
    destroy();
  }

  public String toString()
  {
    String name = getClass().getName();
    int p = name.lastIndexOf('.');
    if (p > 0)
      name = name.substring(p + 1);

    return name + "[]";
  }

}
