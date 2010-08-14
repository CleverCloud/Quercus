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

package com.caucho.server.deploy;

import com.caucho.management.server.ArchiveDeployMXBean;
import com.caucho.vfs.Path;

abstract public class ExpandDeployGeneratorAdmin<C extends ExpandDeployGenerator>
  extends DeployGeneratorAdmin<C>
  implements ArchiveDeployMXBean
{
  public ExpandDeployGeneratorAdmin(C expandDeployGenerator)
  {
    super(expandDeployGenerator);
  }

  public String getName()
  {
    Path containerRootDirectory = getDeployGenerator().getContainerRootDirectory();

    Path archiveDirectory = getDeployGenerator().getArchiveDirectory();

    if (containerRootDirectory == null)
      return archiveDirectory.getNativePath();
    else
      return containerRootDirectory.lookupRelativeNativePath(archiveDirectory);
  }

  public long getDependencyCheckInterval()
  {
    return getDeployGenerator().getDependencyCheckInterval();
  }

  public String getArchiveDirectory()
  {
    return getDeployGenerator().getArchiveDirectory().getNativePath();
  }

  public String getArchivePath(String name)
  {
    return getDeployGenerator().getArchivePath(name).getNativePath();
  }

  public String getExpandDirectory()
  {
    return getDeployGenerator().getExpandDirectory().getNativePath();
  }

  public String getExpandPrefix()
  {
    return getDeployGenerator().getExpandPrefix();
  }

  public String getExpandPath(String name)
  {
    Path path =  getDeployGenerator().getExpandPath(name);

    return path == null ? null : path.getNativePath();
  }

  public String getExpandSuffix()
  {
    return getDeployGenerator().getExpandSuffix();
  }

  public String getExtension()
  {
    return getDeployGenerator().getExtension();
  }

  public String[] getNames()
  {
    return getDeployGenerator().getNames();
  }

  public void deploy(String name)
  {
    getDeployGenerator().deploy(name);
  }

  public void start(String name)
  {
    getDeployGenerator().start(name);
  }

  public void stop(String name)
  {
    getDeployGenerator().stop(name);
  }

  public void undeploy(String name)
  {
    getDeployGenerator().undeploy(name);
  }

  public Throwable getConfigException(String name)
  {
    return getDeployGenerator().getConfigException(name);
  }
}
