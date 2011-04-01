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

package com.caucho.loader.ivy;

import com.caucho.config.ConfigException;
import com.caucho.config.types.FileSetType;
import com.caucho.config.types.PathPatternType;
import com.caucho.loader.*;
import com.caucho.make.DependencyContainer;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.util.zip.*;

/**
 * Class loader which uses an ivy dependency
 */
public class IvyLoader extends JarListLoader {
  private static final Logger log
    = Logger.getLogger(IvyLoader.class.getName());

  private IvyManager _manager = new IvyManager();
  
  // Configured ivy-file.
  private Path _ivyFile;

  // list of the dependencies
  private ArrayList<IvyDependency> _dependencyList
    = new ArrayList<IvyDependency>();
  
  // list of the matching paths
  private ArrayList<Path> _pathList = new ArrayList<Path>();

  /**
   * Creates a new ivy loader.
   */
  public IvyLoader()
  {
  }

  /**
   * The ivy loader's configuration path.
   */
  public void setIvyFile(Path path)
  {
    _ivyFile = path;
  }

  /**
   * The ivy loader's path.
   */
  public Path getIvyFile()
  {
    return _ivyFile;
  }

  /**
   * Sets the ivy cache
   */
  public IvyCache createCache()
  {
    return _manager.createCache();
  }

  /**
   * Adds an ivy dependency
   */
  public void addDependency(IvyDependency dependency)
  {
    _dependencyList.add(dependency);
  }

  /**
   * Initialize
   */
  @PostConstruct
  @Override
  public void init()
    throws ConfigException
  {
    _manager.init();
    
    if (_ivyFile != null && _ivyFile.canRead()) {
      _manager.configureIvyFile(_ivyFile);
    }

    for (IvyDependency dependency : _dependencyList) {
      _manager.resolve(dependency);
    }

    for (Path path : _manager.resolve()) {
      if (log.isLoggable(Level.FINE))
        log.fine("ivy-loader add " + path);
      
      addJar(path);
    }
    
    super.init();
  }
  
  /**
   * True if any of the loaded classes have been modified.  If true, the
   * caller should drop the classpath and create a new one.
   */
  public boolean isModified()
  {
    return false;
  }
  
  /**
   * True if the classes in the directory have changed.
   */
  public boolean logModified(Logger log)
  {
    if (isModified()) {
      log.info(_ivyFile.getNativePath() + " has modified jar files");
      return true;
    }
    else
      return false;
  }

  /**
   * Find all the jars in this directory and add them to jarList.
   */
  private void fillJars()
  {
    _pathList.clear();
  }

  /*
  public Path getCodePath()
  {
    return _fileSet.getDir();
  }
  */

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _ivyFile + "]";
  }
}
