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

package com.caucho.jca.ra;

import com.caucho.config.ConfigException;
import com.caucho.jca.cfg.ResourceConfig;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.server.deploy.DeployController;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * The generator for the resource-deploy
 */
public class ResourceDeploy {
  private static final L10N L = new L10N(ResourceDeploy.class);

  private final ResourceDeployAdmin _admin = new ResourceDeployAdmin(this);

  private ClassLoader _classLoader;
  
  private Path _containerRootDirectory;

  private Path _rarDir;
  private Path _rarExpandDir;

  private String _expandPrefix = "";

  private HashSet<String> _rarNames = new HashSet<String>();

  private volatile boolean _isInit;

  public ResourceDeploy()
  {
    _classLoader = Thread.currentThread().getContextClassLoader();
    
    setExpandPrefix("_rar_");

    _containerRootDirectory = Vfs.getPwd();
  }

  Path getContainerRootDirectory()
  {
    return _containerRootDirectory;
  }

  /**
   * Gets the rar directory.
   */
  public Path getPath()
  {
    return _rarDir;
  }

  /**
   * Sets the rar directory.
   */
  public void setPath(Path path)
  {
    _rarDir = path;
  }

  public Path getArchiveDirectory()
  {
    return getPath();
  }

  public Path getArchivePath(String name)
  {
    return getArchiveDirectory().lookup(name + getExtension());
  }

  /**
   * Returns the combination of prefix, name, and suffix used for expanded
   * archives.
   *
   * @return
   */
  protected String getExpandName(String name)
  {
    return getExpandPrefix() + name + getExpandSuffix();
  }


  /**
   * Sets the war expand dir to check for new applications.
   */
  public void setExpandPath(Path path)
  {
    _rarExpandDir = path;
  }

  /**
   * @deprecated use {@link @getExpandDirectory}
   */
  public Path getExpandPath()
  {
    return getExpandDirectory();
  }

  /**
   * Gets the rar expand directory.
   */
  public Path getExpandDirectory()
  {
    if (_rarExpandDir != null)
      return _rarExpandDir;
    else
      return _rarDir;
  }

  /**
   * Returns the location of an expanded archive, or null if no archive with
   * the passed name is deployed.
   *
   * @param name a name, without an extension
   */
  public Path getExpandPath(String name)
  {
    if (!isDeployedKey(name))
      return null;

    return getExpandDirectory().lookup(getExpandName(name));
  }

  private boolean isDeployedKey(String name)
  {
    return _rarNames.contains(name);
  }

  /**
   * Returns the expand prefix.
   */
  public String getExpandPrefix()
  {
    return _expandPrefix;
  }

  /**
   * Sets the expand prefix.
   */
  public void setExpandPrefix(String prefix)
  {
    _expandPrefix = prefix;
  }

  public boolean isModified()
  {
    try {
      return ! _rarNames.equals(getRarNames());
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Initialize the resource-deploy.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    synchronized (this) {
      if (_isInit)
        return;
      _isInit = true;
    }
    
    if (getPath() == null)
      throw new ConfigException(L.l("resource-deploy requires a path attribute"));
    deployResources();

    _admin.register();
  }

  private void deployResources()
  {
    HashSet<String> oldNames = _rarNames;

    try {
      _rarNames = getRarNames();
      
      for (String oldName : oldNames) {
        if (! _rarNames.contains(oldName))
          undeployResource(oldName);
      }
      
      for (String name : _rarNames) {
        if (oldNames.contains(name))
          continue;

        ResourceArchive rar;

        rar = new ResourceArchive();
        rar.setRarPath(getPath().lookup(name + ".rar"));
        rar.setRootDirectory(getExpandPath().lookup(getExpandPrefix() + name));

        Path oldPwd = Vfs.getPwd();

        try {
          Vfs.setPwd(rar.getRootDirectory());

          for (ResourceConfig config : ResourceDefault.getDefaultList()) {
            config.getBuilderProgram().configure(rar);
          }
        } finally {
          Vfs.setPwd(oldPwd);
        }

        rar.init();

        ResourceArchiveManager.addResourceArchive(rar);
      }
    } catch (ConfigException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  private void undeployResource(String name)
  {
    ResourceArchiveManager.removeResourceArchive(name);
  }

  /**
   * Return the war-expansion directories which were added.
   */
  private HashSet<String> getRarNames()
    throws IOException
  {
    HashSet<String> rarNames = new HashSet<String>();

    Path rarDir = getPath();
    Path rarExpandDir = getExpandPath();

    if (rarDir == null || rarExpandDir == null)
      return rarNames;

    String []rarDirList = rarDir.list();

    // collect all the new wars
    loop:
    for (int i = 0; i < rarDirList.length; i++) {
      String rarName = rarDirList[i];
      String appName;

      if (! rarName.endsWith(".rar"))
        continue;

      Path path = rarDir.lookup(rarName);

      if (! path.canRead())
        continue;

      appName = rarName.substring(0, rarName.length() - 4);

      if (CauchoSystem.isCaseInsensitive())
        appName = appName.toLowerCase();

      rarNames.add(appName);
    }
    
    String []rarExpandList = rarExpandDir.list();
    ArrayList<String> newNames = new ArrayList<String>();

    // collect all the new rar expand directories
    loop:
    for (int i = 0; i < rarExpandList.length; i++) {
      String rarDirName = rarExpandList[i];

      if (! rarDirName.startsWith(getExpandPrefix()))
        continue;

      if (CauchoSystem.isCaseInsensitive())
        rarDirName = rarDirName.toLowerCase();

      Path path = rarExpandDir.lookup(rarDirName);

      if (! path.isDirectory() || ! rarDirName.startsWith(getExpandPrefix()))
        continue;
      
      String appName = rarDirName.substring(getExpandPrefix().length());

      if (! newNames.contains(appName))
        newNames.add(appName);

      rarNames.add(appName);
    }

    return rarNames;
  }

  public String getExtension()
  {
    return ".rar";
  }

  public String getExpandSuffix()
  {
    return "";
  }

  public long getDependencyCheckInterval()
  {
    return -1;
  }

  public String getStartupMode()
  {
    return DeployController.STARTUP_AUTOMATIC;
  }

  public String getRedeployMode()
  {
    return DeployController.REDEPLOY_MANUAL;
  }

  public String getState()
  {
    if (!_isInit)
      return Lifecycle.getStateName(Lifecycle.IS_NEW);
    else
      return Lifecycle.getStateName(Lifecycle.IS_ACTIVE);
  }

  public void start()
  {
    // XXX:
  }

  public Throwable getConfigException()
  {
    // XXX:
    return null;
  }

  public void stop()
  {
    // XXX:
  }

  public void update()
  {
    // XXX:
  }

  public String[] getNames()
  {
    // XXX:
    return new String[0];
  }

  public void stop(String name)
  {
    // XXX:
  }

  public void start(String name)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_classLoader);
      
      deployResources();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  public Throwable getConfigException(String moduleID)
  {
    // XXX:
    return null;
  }

  public void undeploy(String name)
  {
    // XXX:
  }

}
