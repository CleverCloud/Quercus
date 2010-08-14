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

package com.caucho.loader.module;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * ClassLoader for an artifact.
 */
public class ArtifactClassLoader extends EnvironmentClassLoader
{
  private Artifact _artifact;

  private ArtifactClassLoader []_imports;

  /**
   * Creates a new ArtifactClassLoader
   */
  ArtifactClassLoader(ClassLoader parent,
                      Artifact artifact,
                      ArrayList<ArtifactClassLoader> importList)
  {
    super(parent, createId(artifact));

    _artifact = artifact;

    addJar(artifact.getPath());

    _imports = new ArtifactClassLoader[importList.size()];
    importList.toArray(_imports);

    init();
  }

  private static String createId(Artifact artifact)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("artifact:org=").append(artifact.getOrg());
    sb.append(",name=").append(artifact.getName());

    if (artifact.getVersion() != null)
      sb.append(",version=").append(artifact.getVersion().toDebugString());

    return sb.toString();
  }

  Artifact getArtifact()
  {
    return _artifact;
  }

  /**
   * Returns any import class, e.g. from an artifact
   */
  @Override
  protected Class findImportClass(String name)
  {
    for (ArtifactClassLoader loader : _imports) {
      try {
        Class cl = loader.findClassImpl(name);

        if (cl != null)
          return cl;
      } catch (ClassNotFoundException e) {
      }
    }

    return null;
  }

  protected void buildClassPathImpl(ArrayList<String> cp)
  {
    String path = _artifact.getPath().getNativePath();

    if (! cp.contains(path))
      cp.add(path);
  }
}


