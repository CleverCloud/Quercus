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
 * @author Sam
 */

package com.caucho.jca.ra;

import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.ResourceDeployMXBean;
import com.caucho.vfs.Path;

public class ResourceDeployAdmin
  extends AbstractManagedObject
  implements ResourceDeployMXBean
{
  private final ResourceDeploy _resourceDeploy;

  public ResourceDeployAdmin(ResourceDeploy resourceDeploy)
  {
    _resourceDeploy = resourceDeploy;
  }

  protected ResourceDeploy getResourceDeploy()
  {
    return _resourceDeploy;
  }

  void register()
  {
    registerSelf();
  }

  void unregister()
  {
    registerSelf();
  }

  public String getName()
  {
    Path containerRootDirectory = getResourceDeploy().getContainerRootDirectory();

    Path archiveDirectory = getResourceDeploy().getArchiveDirectory();

    if (containerRootDirectory == null)
      return archiveDirectory.getNativePath();
    else
      return containerRootDirectory.lookupRelativeNativePath(archiveDirectory);
  }

  public long getDependencyCheckInterval()
  {
    return getResourceDeploy().getDependencyCheckInterval();
  }

  public String getArchiveDirectory()
  {
    return getResourceDeploy().getArchiveDirectory().getNativePath();
  }

  public String getArchivePath(String name)
  {
    return getResourceDeploy().getArchivePath(name).getNativePath();
  }

  public String getExtension()
  {
    return getResourceDeploy().getExtension();
  }

  public String getExpandDirectory()
  {
    return getResourceDeploy().getExpandDirectory().getNativePath();
  }

  public String getExpandPrefix()
  {
    return getResourceDeploy().getExpandPrefix();
  }

  public String getExpandSuffix()
  {
    return getResourceDeploy().getExpandSuffix();
  }

  public String getExpandPath(String name)
  {
    Path path = getResourceDeploy().getExpandPath(name);

    return path == null ? null : path.getNativePath();
  }

  public String getRedeployMode()
  {
    return getResourceDeploy().getRedeployMode();
  }

  public String getStartupMode()
  {
    return getResourceDeploy().getStartupMode();
  }

  public boolean isModified()
  {
    return getResourceDeploy().isModified();
  }

  public String getState()
  {
    return getResourceDeploy().getState();
  }

  public void start()
  {
    getResourceDeploy().start();
  }

  public Throwable getConfigException()
  {
    return getResourceDeploy().getConfigException();
  }

  public void stop()
  {
    getResourceDeploy().stop();
  }

  public void update()
  {
    getResourceDeploy().update();
  }

  public String[] getNames()
  {
    return getResourceDeploy().getNames();
  }

  public void deploy(String name)
  {
    getResourceDeploy().start(name);
  }

  public void start(String name)
  {
    getResourceDeploy().start(name);
  }

  public Throwable getConfigException(String moduleID)
  {
    return getResourceDeploy().getConfigException(moduleID);
  }

  public void stop(String name)
  {
    getResourceDeploy().stop(name);
  }

  public void undeploy(String name)
  {
    getResourceDeploy().undeploy(name);
  }
}
