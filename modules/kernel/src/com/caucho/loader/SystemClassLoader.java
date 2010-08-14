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

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.server.util.CauchoSystem;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;
import java.net.URLClassLoader;

/**
 * ClassLoader that initalizes the environment and allows byte code
 * enhancement of classes in the system classpath.
 * <pre>
 * java -Djava.system.class.loader=com.caucho.loader.SystemClassLoader ...
 * </pre>
 * If the system property "system.conf" is defined, it is used as a path
 * to a configuration file that initializes the enviornment.  Relative paths
 * are relative to the current directory (See {@link com.caucho.vfs.Vfs#getPwd()}.
 * <p/>
 * Resources defined in system.conf are available to all classes loaded within the jvm.
 * <pre>
 * java -Dsystem.conf=tests/system.conf -Djava.system.class.loader=com.caucho.loader.SystemClassLoader ...
 * </pre>
 */
public class SystemClassLoader
  extends EnvironmentClassLoader
  implements EnvironmentBean
{
  private AtomicBoolean _isInit = new AtomicBoolean();
  private boolean _hasBootClassPath;

  private Path _libexec;

  /**
   * Creates a new SystemClassLoader.
   */
  public SystemClassLoader(ClassLoader parent)
  {
    super(parent, "system");

    String preScan = System.getProperty("caucho.jar.prescan");
    
    preScan = "false";
    
    if (preScan == null || ! "false".equals(preScan))
      DynamicClassLoader.setJarCacheEnabled(true);

    String smallmem = System.getProperty("caucho.smallmem");
    
    if (smallmem != null && ! "false".equals(smallmem))
      DynamicClassLoader.setJarCacheEnabled(false);
  }

  @Override
  public boolean isJarCacheEnabled()
  {
    return DynamicClassLoader.isJarCacheEnabledDefault();
  }

  @Override
  public ClassLoader getClassLoader()
  {
    return this;
  }

  @Override
  public void init()
  {
    if (_isInit.getAndSet(true))
      return;

    initClasspath();

    super.init();

    String systemConf = System.getProperty("system.conf");

    if (systemConf != null) {
      try {
        Path path = Vfs.lookup(systemConf);

        Config config = new Config();

        config.configure(this, path, getSchema());
      }
      catch (Exception ex) {
        ex.printStackTrace();

        throw new RuntimeException(ex.toString());
      }
    }
  }

  private void initClasspath()
  {
    boolean isValid = false;
    
    try {
      String boot = System.getProperty("sun.boot.class.path");
      if (boot != null) {
        initClasspath(boot);
        _hasBootClassPath = true;

        initExtDirs("java.ext.dirs");
        initExtDirs("java.endorsed.dirs");
      }
    
      initClasspath(System.getProperty("java.class.path"));

      isValid = true;
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (! isValid)
        _hasBootClassPath = false;
    }
  }

  private void initExtDirs(String prop)
    throws IOException
  {
    String extDirPath = System.getProperty(prop);

    if (extDirPath == null)
      return;

    for (String extDir : extDirPath.split(File.pathSeparator, 512)) {
      Path dir = Vfs.lookup(extDir);

      for (String fileName : dir.list()) {
        Path root = dir.lookup(fileName);

        try {
          // #2659
          if (root.isDirectory()
              || root.isFile() && (root.getPath().endsWith(".jar")
                                   || root.getPath().endsWith(".zip"))) {
            addRoot(root);
          }
        } catch (Throwable e) {
          _hasBootClassPath = false;
          e.printStackTrace();
        }
      }
    }
  }

  private void initClasspath(String classpath)
  {
    String[] classpathElements = classpath.split(File.pathSeparator, 512);

    for (String classpathElement : classpathElements) {
      Path root = Vfs.lookup(classpathElement);

      try {
        if (root.exists())
          addRoot(root);
      } catch (Throwable e) {
        _hasBootClassPath = false;
        e.printStackTrace();
      }
    }
  }

  @Override
  protected void initEnvironment()
  {
    // disable for terracotta
  }

  /**
   * Load a class using this class loader
   *
   * @param name the classname to load
   * @param resolve if true, resolve the class
   *
   * @return the loaded classes
   */
  public Class loadClassImpl(String name, boolean resolve)
    throws ClassNotFoundException
  {
    // The JVM has already cached the classes, so we don't need to
    Class<?> cl = findLoadedClass(name);

    if (cl != null) {
      if (resolve)
        resolveClass(cl);
      return cl;
    }

    /*
    // This causes problems with JCE
    if (_hasBootClassPath) {
      String className = name.replace('.', '/') + ".class";

      if (findPath(className) == null)
        return null;
    }
    */

    return super.loadClassImpl(name, resolve);
  }

  protected String getSchema()
  {
    return "com/caucho/loader/system.rnc";
  }
  
  private Path getLibexec()
  {
    if (_libexec == null) {
      if (CauchoSystem.isWindows()) {
        if (CauchoSystem.is64Bit()) {
          _libexec = CauchoSystem.getResinHome().lookup("win64");
        }
        else {
          _libexec = CauchoSystem.getResinHome().lookup("win32");
        }
      }
      else {
        if (CauchoSystem.is64Bit()) {
          _libexec = CauchoSystem.getResinHome().lookup("libexec");
        }
        else {
          _libexec = CauchoSystem.getResinHome().lookup("libexec64");
        }
      }
    }
    
    return _libexec;
  }

  /**
   * Returns the full library path for the name.
   */
  @Override
  public String findLibrary(String name)
  {
    Path path = getLibexec().lookup("lib" + name + ".so");

    if (path.canRead()) {
      return path.getNativePath();
    }
    
    path = getLibexec().lookup("lib" + name + ".jnilib");

    if (path.canRead()) {
      return path.getNativePath();
    }
    
    path = getLibexec().lookup(name + ".dll");

    return super.findLibrary(name);
  }
}


