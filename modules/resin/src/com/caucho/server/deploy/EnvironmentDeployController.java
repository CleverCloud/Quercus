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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELException;
import javax.management.ObjectName;

import com.caucho.config.ConfigException;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.types.PathBuilder;
import com.caucho.jmx.Jmx;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * A deploy controller for an environment.
 */
abstract public class
  EnvironmentDeployController<I extends EnvironmentDeployInstance,
                                        C extends DeployConfig>
  extends ExpandDeployController<I>
  implements EnvironmentListener
{
  private static final L10N L = new L10N(EnvironmentDeployController.class);
  private static final Logger log
    = Logger.getLogger(EnvironmentDeployController.class.getName());

  // The JMX identity
  private LinkedHashMap<String,String> _jmxContext;

  private Object _mbean;

  private ObjectName _objectName;

  // The default configurations
  private ArrayList<C> _configDefaults =  new ArrayList<C>();

  // The primary configuration
  private C _config;
  private DeployConfig _prologue;

  // The variable mapping
  private HashMap<String,Object> _variableMap = new HashMap<String,Object>();

  // Config exception passed in from parent, e.g. .ear
  private Throwable _configException;

  public EnvironmentDeployController()
  {
    this("", (Path) null);
  }

  public EnvironmentDeployController(C config)
  {
    this(config.getId(), config.calculateRootDirectory());

    setConfig(config);
  }

  public EnvironmentDeployController(String id, C config)
  {
    this(id, config.calculateRootDirectory());

    setConfig(config);
  }

  public EnvironmentDeployController(String id, Path rootDirectory)
  {
    super(id, null, rootDirectory);

    _jmxContext = Jmx.copyContextProperties(getParentClassLoader());
  }

  /**
   * Sets the primary configuration.
   */
  public void setConfig(C config)
  {
    if (config == null)
      return;

    if (_config != null && ! _configDefaults.contains(_config))
      addConfigDefault(_config);

    addConfigController(config);

    _config = config;

    if (_prologue == null)
      setPrologue(config.getPrologue());
  }

  /**
   * Gets the primary configuration
   */
  public C getConfig()
  {
    return _config;
  }

  /**
   * Gets the prologue configuration
   */
  public DeployConfig getPrologue()
  {
    return _prologue;
  }

  /**
   * Sets the prologue configuration
   */
  public void setPrologue(DeployConfig prologue)
  {
    _prologue = prologue;
  }

  /**
   * Returns the error message
   */
  public String getErrorMessage()
  {
    Throwable exn = getConfigException();

    if (exn instanceof ConfigException) {
      exn.printStackTrace();
      return exn.getMessage();
    }
    else if (exn != null) {
      exn.printStackTrace();
      return exn.toString();
    }
    else
      return null;
  }

  /**
   * Returns the configure exception.
   */
  @Override
  public Throwable getConfigException()
  {
    Throwable configException = super.getConfigException();

    if (configException == null)
      configException = _configException;

    if (configException == null) {
      DeployInstance deploy = getDeployInstance();

      if (deploy != null)
        configException = deploy.getConfigException();
    }

    return configException;
  }

  /**
   * Adds a default config.
   */
  public void addConfigDefault(C config)
  {
    if (! _configDefaults.contains(config)) {
      _configDefaults.add(config);

      addConfigController(config);
    }
  }

  private void addConfigController(C config)
  {
    if (config.getStartupMode() != null)
      setStartupMode(config.getStartupMode());

    if (config.getRedeployCheckInterval() != null)
      setRedeployCheckInterval(config.getRedeployCheckInterval());

    if (config.getRedeployMode() != null)
      setRedeployMode(config.getRedeployMode());

    if (config.getExpandCleanupFileset() != null)
      setExpandCleanupFileSet(config.getExpandCleanupFileset());
  }

  /**
   * Returns the path variable map.
   */
  public HashMap<String,Object> getVariableMap()
  {
    return _variableMap;
  }

  /**
   * Returns the mbean.
   */
  public Object getMBean()
  {
    return _mbean;
  }

  /**
   * Returns the object name.
   */
  public ObjectName getObjectName()
  {
    return _objectName;
  }

  /**
   * Sets a parent config exception (e.g. from a .ear)
   */
  public void setConfigException(Throwable e)
  {
    _configException = e;
    
    if (getDeployInstance() != null)
      getDeployInstance().setConfigException(e);
  }

  /**
   * Initialize the controller.
   */
  @Override
  protected void initEnd()
  {
    super.initEnd();

    if (getDeployAdmin() != null)
      getDeployAdmin().register();
  }

  /**
   * Returns true if the entry matches.
   */
  @Override
  public boolean isNameMatch(String url)
  {
    return url.equals(getId());
  }

  /**
   * Merges with the old controller.
   */
  @Override
  protected void mergeController(DeployController oldControllerV)
  {
    super.mergeController(oldControllerV);

    EnvironmentDeployController<I,C> oldController;
    oldController = (EnvironmentDeployController) oldControllerV;
    // setId(oldController.getId());

    ArrayList<C> configDefaults = new ArrayList<C>();

    if (getPrologue() == null)
      setPrologue(oldController.getPrologue());
    else if (oldController.getPrologue() != null) {
      configDefaults.add(0, (C) getPrologue()); // XXX: must be first

      setPrologue(oldController.getPrologue());
    }

    configDefaults.addAll(oldController._configDefaults);
    
    if (getConfig() == null)
      setConfig(oldController.getConfig());
    else if (oldController.getConfig() != null) {
      configDefaults.add(getConfig());

      setConfig(oldController.getConfig());
    }

    for (C config : _configDefaults) {
      if (! configDefaults.contains(config))
        configDefaults.add(config);
    }
    
    _configDefaults = configDefaults;

    mergeStartupMode(oldController.getStartupMode());

    mergeRedeployCheckInterval(oldController.getRedeployCheckInterval());

    mergeRedeployMode(oldController.getRedeployMode());
  }

  /**
   * Returns the application object.
   */
  @Override
  public boolean destroy()
  {
    if (! super.destroy())
      return false;

    Environment.removeEnvironmentListener(this, getParentClassLoader());

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getParentClassLoader());
      
      if (getDeployAdmin() != null)
        getDeployAdmin().unregister();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    return true;
  }

  /**
   * Configures the instance.
   */
  @Override
  protected void configureInstance(I instance)
    throws Throwable
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      ClassLoader classLoader = instance.getClassLoader();

      thread.setContextClassLoader(classLoader);

      log.fine(instance + " initializing");

      // set from external error, like .ear
      instance.setConfigException(_configException);

      extendJMXContext(_jmxContext);
      Jmx.setContextProperties(_jmxContext, classLoader);
      
      configureInstanceVariables(instance);

      ArrayList<DeployConfig> initList = new ArrayList<DeployConfig>();

      if (getPrologue() != null)
        initList.add(getPrologue());

      fillInitList(initList);

      thread.setContextClassLoader(instance.getClassLoader());
      Vfs.setPwd(getRootDirectory());

      addDependencies();

      instance.preConfigInit();

      for (DeployConfig config : initList) {
        ConfigProgram program = config.getBuilderProgram();

        if (program != null)
          program.configure(instance);
      }

      instance.init();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  protected void extendJMXContext(Map<String,String> context)
  {
    // _jmxContext.put(getMBeanTypeName(), getMBeanId());
  }

  protected void fillInitList(ArrayList<DeployConfig> initList)
  {
    boolean isSkipDefault = _config != null && _config.isSkipDefaultConfig();

    if (isSkipDefault) {
      initList.clear();
    }
    else {
      for (DeployConfig config : _configDefaults) {
        DeployConfig prologue = config.getPrologue();

        if (prologue != null)
          initList.add(prologue);
      }
    
      initList.addAll(_configDefaults);
    }

    if (_config != null && ! initList.contains(_config))
      initList.add(_config);
  }

  protected void configureInstanceVariables(I instance)
    throws Throwable
  {
    Path rootDirectory = getRootDirectory();

    if (rootDirectory == null)
      throw new NullPointerException("Null root directory");

    if (! rootDirectory.isFile()) {
    }
    else if (rootDirectory.getPath().endsWith(".jar") ||
             rootDirectory.getPath().endsWith(".war")) {
      throw new ConfigException(L.l("root-directory `{0}' must specify a directory.  It may not be a .jar or .war.",
                                    rootDirectory.getPath()));
    }
    else
      throw new ConfigException(L.l("root-directory `{0}' may not be a file.  root-directory must specify a directory.",
                                    rootDirectory.getPath()));
    Vfs.setPwd(rootDirectory);

    if (log.isLoggable(Level.FINE))
      log.fine(instance + " root-directory=" + rootDirectory);

    instance.setRootDirectory(rootDirectory);
  }

  @Override
  public Path getArchivePath()
  {
    Path path = super.getArchivePath();

    if (path != null)
      return path;

    if (_config != null) {
      String pathString = _config.getArchivePath();

      if (pathString != null) {
        try {
          path = PathBuilder.lookupPath(pathString);
        } catch (ELException e) {
          throw new RuntimeException(e);
        }
      }

      setArchivePath(path);
    }

    return path;
  }

  /**
   * Handles config phase.
   */
  public void environmentConfigure(EnvironmentClassLoader loader)
  {
  }

  /**
   * Handles bind phase.
   */
  public void environmentBind(EnvironmentClassLoader loader)
  {
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
    try {
      start();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
    stop();
  }

  /**
   * Returns a printable view.
   */
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "$" + System.identityHashCode(this) + "[" + getId() + "]";
  }
}
