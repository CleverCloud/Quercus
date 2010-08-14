/**
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.loader;

import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;

/**
 * Class loader specific to loading resources, not classes.
 */
public class ResourceLoader extends Loader {
  // The class directory
  private Path _path;

  /**
   * Null constructor for the resource loader.
   */
  public ResourceLoader()
  {
  }
  
  public ResourceLoader(ClassLoader loader)
  {
    super(loader);
  }

  /**
   * Creates the resource loader with the specified path.
   *
   * @param path specifying the root of the resources
   */
  public ResourceLoader(DynamicClassLoader loader, Path path)
  {
    this(loader);
    
    setPath(path);
  }

  /**
   * Sets the resource directory.
   */
  public void setPath(Path path)
  {
    if (path.getPath().endsWith(".jar")
        || path.getPath().endsWith(".zip")) {
      path = JarPath.create(path);
    }

    _path = path;
  }

  /**
   * Gets the resource path.
   */
  public Path getPath()
  {
    return _path;
  }

  /**
   * Given a class or resource name, returns a patch to that resource.
   *
   * @param name the class or resource name.
   *
   * @return the path representing the class or resource.
   */
  @Override
  public Path getPath(String name)
  {
    if (name.startsWith("/"))
      return _path.lookup("." + name);
    else
      return _path.lookup(name);
  }
  
  /**
   * The class entry is always null.
   */
  @Override
  protected ClassEntry getClassEntry(String name, String pathName)
    throws ClassNotFoundException
  {
    return null;
  }

  /**
   * Returns a printable representation of the loader.
   */
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _path + "]";
  }
}
