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

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.types.FileSetType;
import com.caucho.config.types.PathPatternType;
import com.caucho.vfs.Path;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 */
@Configurable
public class LibraryLoader extends JarListLoader {
  // Configured path.
  private Path _path;
  
  private FileSetType _fileSet;

  // list of the matching paths
  private ArrayList<Path> _pathList = new ArrayList<Path>();

  // list of the matching paths
  private ArrayList<Path> _newPathList = new ArrayList<Path>();

  /**
   * Creates a new directory loader.
   */
  public LibraryLoader()
  {
  }
  
  public LibraryLoader(ClassLoader loader)
  {
    super(loader);
  }

  /**
   * Creates a new directory loader.
   */
  public LibraryLoader(ClassLoader loader, Path path)
  {
    this(loader);
    
    _path = path;

    try {
      init();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * The library loader's path.
   */
  public void setPath(Path path)
  {
    _path = path;
  }

  /**
   * The library loader's path.
   */
  public Path getPath()
  {
    return _path;
  }

  /**
   * Sets a file set.
   */
  public void setFileset(FileSetType fileSet)
  {
    _fileSet = fileSet;
  }

  /**
   * Create a new class loader
   *
   * @param parent parent class loader
   * @param dir directories which can handle dynamic jar addition
   */
  public static DynamicClassLoader create(ClassLoader parent, Path path)
  {
    DynamicClassLoader loader = new DynamicClassLoader(parent);

    LibraryLoader dirLoader = new LibraryLoader(loader, path);
    dirLoader.init();

    loader.init();
    
    return loader;
  }

  /**
   * Initialize
   */
  @PostConstruct
  @Override
  public void init()
    throws ConfigException
  {
    super.init();
    
    try {
      if (_fileSet != null) {
      }
      else if (_path.getPath().endsWith(".jar")
               || _path.getPath().endsWith(".zip")) {
        _fileSet = new FileSetType();
        _fileSet.setDir(_path.getParent());
        _fileSet.addInclude(new PathPatternType(_path.getTail()));
      }
      else {
        _fileSet = new FileSetType();
        _fileSet.setDir(_path);
        _fileSet.addInclude(new PathPatternType("*.jar"));
        _fileSet.addInclude(new PathPatternType("*.zip"));
      }

      fillJars();
    } catch (ConfigException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  
  /**
   * True if any of the loaded classes have been modified.  If true, the
   * caller should drop the classpath and create a new one.
   */
  @Override
  public boolean isModified()
  {
    _newPathList.clear();

    _fileSet.getPaths(_newPathList);

    return ! _newPathList.equals(_pathList);
  }
  
  /**
   * True if the classes in the directory have changed.
   */
  @Override
  public boolean logModified(Logger log)
  {
    if (isModified()) {
      log.info(_path.getNativePath() + " has modified jar files");
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
    _jarList.clear();

    _fileSet.getPaths(_pathList);

    for (int i = 0; i < _pathList.size(); i++) {
      Path jar = _pathList.get(i);

      addJar(jar);
    }
  }

  public Path getCodePath()
  {
    return _fileSet.getDir();
  }

  /**
   * Destroys the loader, closing the jars.
   */
  @Override
  protected void destroy()
  {
    clearJars();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _fileSet + "]";
  }
}
