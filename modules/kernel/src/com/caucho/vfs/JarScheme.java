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

import java.util.Map;

/**
 * JarScheme implements the lookup of the jar scheme.
 */
public class JarScheme extends FilesystemPath {
  JarScheme(String path)
  {
    super(null, "/", "/");
  }

  /**
   * Lookup the path, handling windows weirdness
   */
  protected Path schemeWalk(String userPath,
                            Map<String,Object> attributes,
                            String filePath,
                            int offset)
  {
    int p = filePath.indexOf('!', offset);
    String backingPath;
    String jarPath;

    if (p > 0) {
      backingPath = filePath.substring(offset, p);
      jarPath = filePath.substring(p + 1);
    }
    else {
      backingPath = filePath.substring(offset);
      jarPath = "";
    }

    Path backing = Vfs.lookup(backingPath);

    return JarPath.create(backing).lookup(jarPath);
  }

  public Path fsWalk(String userPath,
                        Map<String,Object> attributes,
                        String path)
  {
    return schemeWalk(userPath, attributes, path, 0);
  }

  public String getScheme()
  {
    return "jar";
  }
}
