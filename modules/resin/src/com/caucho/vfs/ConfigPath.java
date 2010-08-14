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

package com.caucho.vfs;

import com.caucho.loader.EnvironmentLocal;

import java.util.Map;

/**
 * ConfigPath implements remote configuration scheme.
 */
public class ConfigPath extends Path {
  private static final EnvironmentLocal<RemotePwd> _remotePath
    = new EnvironmentLocal<RemotePwd>();

  /**
   * Creates the path (for the scheme)
   */
  ConfigPath()
  {
    super(SchemeMap.getNullSchemeMap());
  }

  /**
   * Sets the remote.
   */
  public static void setRemote(Path remotePath)
  {
    _remotePath.set(new RemotePwd(remotePath, Vfs.lookup()));
  }

  /**
   * Path-specific lookup.  Path implementations will override this.
   *
   * @param userPath the user's lookup() path.
   * @param newAttributes the attributes for the new path.
   * @param newPath the lookup() path
   * @param offset offset into newPath to start lookup.
   *
   * @return the found path
   */
  protected Path schemeWalk(String userPath,
                            Map<String,Object> newAttributes,
                            String newPath, int offset)
  {
    throw new UnsupportedOperationException();
    /*
    Path path = Vfs.lookup();

    path = path.schemeWalk(userPath, newAttributes, newPath, offset);

    RemotePwd remotePwd = _remotePath.get();

    if (remotePwd == null)
      return path;

    Path configPwd = remotePwd.getPwd();
    Path remotePath = remotePwd.getRemote();

    String pathName = path.getFullPath();
    String configName = configPwd.getFullPath();

    if (pathName.startsWith(configName))
      pathName = pathName.substring(configName.length());

    return remotePath.schemeWalk(userPath, newAttributes, pathName, 0);
    */
  }

  /**
   * Returns the scheme.
   */
  public String getScheme()
  {
    Path path = Vfs.lookup();

    return path.getScheme();
  }

  /**
   * Returns the path.
   */
  public String getPath()
  {
    Path path = Vfs.lookup();

    return path.getPath();
  }

  static class RemotePwd {
    Path _remote;
    Path _pwd;

    RemotePwd(Path remote, Path pwd)
    {
      _remote = remote;
      _pwd = pwd;
    }

    Path getRemote()
    {
      return _remote;
    }

    Path getPwd()
    {
      return _pwd;
    }
  }
}
