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

package com.caucho.loader;

import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.make.DependencyContainer;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 */
abstract public class JarListLoader extends Loader implements Dependency {
  private static final Logger log
    = Logger.getLogger(JarListLoader.class.getName());
  
  // list of the jars in the directory
  protected ArrayList<JarEntry> _jarList = new ArrayList<JarEntry>();
  
  // list of dependencies
  private DependencyContainer _dependencyList = new DependencyContainer();

  // Entry map
  private JarMap _pathMap;

  /**
   * Creates a new jar list loader.
   */
  public JarListLoader()
  {
  }
  
  public JarListLoader(ClassLoader loader)
  {
    super(loader);
  }

  /**
   * Sets the owning class loader.
   */
  @Override
  public void setLoader(DynamicClassLoader loader)
  {
    super.setLoader(loader);
 }
  
  /**
   * True if any of the loaded classes have been modified.  If true, the
   * caller should drop the classpath and create a new one.
   */
  public boolean isModified()
  {
    return _dependencyList.isModified();
  }
  
  /**
   * True if any of the loaded classes have been modified.  If true, the
   * caller should drop the classpath and create a new one.
   */
  public boolean logModified(Logger log)
  {
    return _dependencyList.logModified(log);
  }

  /**
   * Validates the loader.
   */
  @Override
  public void validate()
    throws ConfigException
  {
    for (int i = 0; i < _jarList.size(); i++) {
      _jarList.get(i).validate();
    }
  }

  @Override
  public void init()
  {
    super.init();

    for (int i = 0; i < _jarList.size(); i++)
      getClassLoader().addURL(_jarList.get(i).getJarPath());
  }

  protected boolean isJarCacheEnabled()
  {
    DynamicClassLoader loader = getClassLoader();

    if (loader != null)
      return loader.isJarCacheEnabled();
    else
      return false;
  }
  
  protected void addJar(Path jar)
  {
    if (! jar.exists()) {
      log.fine(jar.getTail() + " does not exist"
                  + " (path=" + jar.getNativePath() + ")");
      return;
    }
    else if (! jar.canRead()) {
      log.warning(jar.getTail() + " is unreadable"
                  + " (uid=" + jar.getUser() + " mode="
                  + String.format("%o", jar.getMode())
                  + " path=" + jar.getNativePath() + ")");
      return;
    }
    
    JarPath jarPath = JarPath.create(jar);
    JarEntry jarEntry = new JarEntry(jarPath);

    if (getClassLoader() != null) {
      if (! getClassLoader().addURL(jarPath))
        return;
    }

    // skip duplicates
    if (_jarList.contains(jarEntry))
      return;
    
    _jarList.add(jarEntry);

    _dependencyList.add(new Depend(jarPath));

    if (_pathMap == null && isJarCacheEnabled())
      _pathMap = new JarMap();

    if (_pathMap != null) {
      _pathMap.scan(jar, jarEntry);
    }
  }

  /**
   * Fill data for the class path.  fillClassPath() will add all 
   * .jar and .zip files in the directory list.
   */
  @Override
  protected void buildClassPath(ArrayList<String> pathList)
  {
    for (int i = 0; i < _jarList.size(); i++) {
      JarEntry jarEntry = _jarList.get(i);
      JarPath jar = jarEntry.getJarPath();
      
      String path = jar.getContainer().getNativePath();

      if (! pathList.contains(path))
        pathList.add(path);
    }
  }

  /**
   * Returns the class entry.
   *
   * @param name name of the class
   */
  protected ClassEntry getClassEntry(String name, String pathName)
    throws ClassNotFoundException
  {
    if (_pathMap != null) {
      JarMap.JarList jarEntryList = _pathMap.get(pathName);

      if (jarEntryList != null) {
        JarEntry jarEntry = jarEntryList.getEntry();

        Path filePath = jarEntry.getJarPath().lookup(pathName);

        return createEntry(name, pathName, jarEntry, filePath);
      }
    }
    else {
      // Find the path corresponding to the class
      for (int i = 0; i < _jarList.size(); i++) {
        JarEntry jarEntry = _jarList.get(i);
        Path path = jarEntry.getJarPath();

        Path filePath = path.lookup(pathName);
      
        if (filePath.canRead() && filePath.getLength() > 0) {
          return createEntry(name, pathName, jarEntry, filePath);
        }
      }
    }

    return null;
  }

  private ClassEntry createEntry(String name,
                                 String pathName,
                                 JarEntry jarEntry,
                                 Path filePath)
  {
    String pkg = "";
    int p = pathName.lastIndexOf('/');
    if (p > 0)
      pkg = pathName.substring(0, p + 1);

    ClassEntry entry = new ClassEntry(getClassLoader(), name, filePath,
                                      filePath,
                                      jarEntry.getCodeSource(pathName));

    ClassPackage classPackage = jarEntry.getPackage(pkg);

    entry.setClassPackage(classPackage);

    return entry;
  }
  
  /**
   * Adds resources to the enumeration.
   */
  public void getResources(Vector<URL> vector, String name)
  {
    if (_pathMap != null) {
      JarMap.JarList jarEntryList = _pathMap.get(name);

      for (; jarEntryList != null; jarEntryList = jarEntryList.getNext()) {
        JarEntry jarEntry = jarEntryList.getEntry();
        Path path = jarEntry.getJarPath();

        path = path.lookup(name);

        try {
          URL url = new URL(path.getURL());

          if (! vector.contains(url))
            vector.add(url);
        } catch (Exception e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }
    }
    else {
      for (int i = 0; i < _jarList.size(); i++) {
        JarEntry jarEntry = _jarList.get(i);
        Path path = jarEntry.getJarPath();

        path = path.lookup(name);

        if (path.exists()) {
          try {
            URL url = new URL(path.getURL());

            if (! vector.contains(url))
              vector.add(url);
          } catch (Exception e) {
            log.log(Level.WARNING, e.toString(), e);
          }
        }
      }
    }
  }

  /**
   * Find a given path somewhere in the classpath
   *
   * @param pathName the relative resourceName
   *
   * @return the matching path or null
   */
  public Path getPath(String pathName)
  {
    if (_pathMap != null) {
      JarMap.JarList jarEntryList = _pathMap.get(pathName);

      if (jarEntryList != null) {
        return jarEntryList.getEntry().getJarPath().lookup(pathName);
      }
    }
    else {
      for (int i = 0; i < _jarList.size(); i++) {
        JarEntry jarEntry = _jarList.get(i);
        Path path = jarEntry.getJarPath();

        Path filePath = path.lookup(pathName);

        if (filePath.exists())
          return filePath;
      }
    }

    return null;
  }

  /**
   * Closes the jars.
   */
  protected void clearJars()
  {
    synchronized (this) {
      ArrayList<JarEntry> jars = new ArrayList<JarEntry>(_jarList);
      _jarList.clear();

      if (_pathMap != null)
        _pathMap.clear();
    
      for (int i = 0; i < jars.size(); i++) {
        JarEntry jarEntry = jars.get(i);

        JarPath jarPath = jarEntry.getJarPath();

        jarPath.closeJar();
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _jarList + "]";
  }

  /*
  static class JarList {
    JarEntry _entry;
    JarList _next;

    JarList(JarEntry entry, JarList next)
    {
      _entry = entry;
      _next = next;
    }

    JarEntry getEntry()
    {
      return _entry;
    }

    JarList getNext()
    {
      return _next;
    }
  }
  */
}
