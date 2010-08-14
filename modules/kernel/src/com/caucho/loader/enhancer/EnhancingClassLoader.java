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

package com.caucho.loader.enhancer;

import com.caucho.inject.Module;
import com.caucho.java.WorkDir;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.vfs.Path;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 *
 * <p>DynamicClassLoaders can be chained creating one virtual class loader.
 * From the perspective of the JDK, it's all one classloader.  Internally,
 * the class loader chain searches like a classpath.
 */
@Module
public class EnhancingClassLoader extends EnvironmentClassLoader {
  private Path _workPath;

  /**
   * Creates a new environment class loader.
   */
  public EnhancingClassLoader()
  {
    this(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Creates a new environment class loader.
   */
  public EnhancingClassLoader(ClassLoader parent)
  {
    super(parent, null);
  }

  /**
   * Gets the work path.
   */
  public Path getWorkPath()
  {
    if (_workPath != null)
      return _workPath;
    else
      return WorkDir.getLocalWorkDir(this);
  }

  /**
   * Sets the work path.
   */
  public void setWorkPath(Path workPath)
  {
    _workPath = workPath;
  }

  /**
   * Gets the work path.
   */
  public final Path getPreWorkPath()
  {
    return getWorkPath().lookup("pre-enhance");
  }

  /**
   * Gets the work path.
   */
  public final Path getPostWorkPath()
  {
    return getWorkPath().lookup("post-enhance");
  }

  /**
   * Initialize the loader.
   */
  public void init()
  {
    super.init();

    if (getTransformerList() == null
        && EnhancerManager.getLocalEnhancer(this) != null) {
      EnhancerManager.create(this);
    }
  }

  public String toString()
  {
    if (getId() != null)
      return "EnhancingClassLoader[" + getId() + "]";
    else
      return "EnhancingClassLoader" + getLoaders();
  }
}
