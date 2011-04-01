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

package com.caucho.loader;

import com.caucho.vfs.Path;

import javax.annotation.PostConstruct;

/**
 * Class for configuration.
 */
public class ClasspathConfig {
  private Path _classDir;
  private Path _source;
  private boolean _compile = true;
  private boolean _isLibraryDir = false;
  
  /**
   * Sets the id.
   */
  public void setId(Path id)
  {
    _classDir = id;
  }
  
  /**
   * Sets the source.
   */
  public void setSource(Path source)
  {
    _source = source;
  }
  
  /**
   * Sets true if compilation is allowed.
   */
  public void setCompile(boolean compile)
  {
    _compile = compile;
  }
  
  /**
   * Sets true if it's a jar library direcotyr
   */
  public void setLibraryDir(boolean libraryDir)
  {
    _isLibraryDir = libraryDir;
  }

  /**
   * Initialize
   */
  @PostConstruct
  public void init()
  {
    DynamicClassLoader classLoader;
    classLoader = (DynamicClassLoader) Thread.currentThread().getContextClassLoader();
    
    Loader loader = null;

    if (_isLibraryDir)
      loader = new DirectoryLoader(classLoader, _classDir);
    else if (! _compile)
      loader = new SimpleLoader(classLoader, _classDir);
    else if (_classDir.getPath().endsWith(".jar") ||
             _classDir.getPath().endsWith(".zip"))
      loader = new SimpleLoader(classLoader, _classDir);
    else if (_source != null)
      loader = new CompilingLoader(classLoader, _classDir, _source, null, null);
    else
      loader = new CompilingLoader(classLoader, _classDir);
    
    loader.init();

    classLoader.addLoader(loader);

    classLoader.init();
  }

  public String toString()
  {
    return "ClasspathConfig[" + _classDir + "]";
  }
}


