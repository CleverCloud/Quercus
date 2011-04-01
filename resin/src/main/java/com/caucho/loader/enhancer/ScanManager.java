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

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.logging.*;
import java.util.zip.*;

import com.caucho.bytecode.ByteCodeClassMatcher;
import com.caucho.bytecode.ByteCodeClassScanner;
import com.caucho.inject.Module;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.*;

/**
 * Interface for a scan manager
 */
@Module
public class ScanManager {
  private static final Logger log
    = Logger.getLogger(ScanManager.class.getName());
  
  private final ScanListener []_listeners;

  public ScanManager(ArrayList<ScanListener> listeners)
  {
    _listeners = new ScanListener[listeners.size()];
    
    listeners.toArray(_listeners);
  }

  public void scan(EnvironmentClassLoader loader, URL url, String packageRoot)
  {
    // #3576
    scan(loader, Vfs.lookup(url), packageRoot);
  }
  
  public void scan(EnvironmentClassLoader loader, 
                   Path root,
                   String packageRoot)
  {
    if (root.getPath().endsWith(".jar") && ! (root instanceof JarPath)) {
      root = JarPath.create(root);
    }
    
    ScanListener []listeners = new ScanListener[_listeners.length];

    boolean hasListener = false;
    for (int i = 0; i < _listeners.length; i++) {
      if (_listeners[i].isRootScannable(root, packageRoot)) {
        listeners[i] = _listeners[i];
        hasListener = true;
      }
    }

    if (! hasListener) {
      return;
    }
    
    ByteCodeClassScanner scanner = new ByteCodeClassScanner();
    
    String packagePath = null;
    
    if (packageRoot != null)
      packagePath = packageRoot.replace('.', '/');

    if (root instanceof JarPath) {
      JarPath jarRoot = (JarPath) root;
      Path jar = jarRoot.getContainer();
      
      JarByteCodeMatcher matcher
        = new JarByteCodeMatcher(loader, root, packageRoot, listeners);
    
      scanForJarClasses(jar, packageRoot,
                        scanner, matcher);
    }
    else {
      PathByteCodeMatcher matcher
        = new PathByteCodeMatcher(loader, root, packageRoot, listeners);
      
      Path scanRoot = root;
      
      if (packagePath != null)
        scanRoot = scanRoot.lookup(packagePath);
      
      scanForClasses(root, scanRoot, scanner, matcher);
    }
  }

  private void scanForClasses(Path root,
                              Path path,
                              ByteCodeClassScanner classScanner,
                              PathByteCodeMatcher matcher)
  {
    try {
      if (path.isDirectory()) {
        for (String name : path.list()) {
          if (name.indexOf(':') >= 0) {
            continue;
          }
          
          scanForClasses(root, path.lookup(name), classScanner, matcher);
        }

        return;
      }

      if (! path.getPath().endsWith(".class"))
        return;

      matcher.init(root, path);

      ReadStream is = path.openRead();
      
      try {
        classScanner.init(path.getPath(), is, matcher);

        classScanner.scan();
      } finally {
        is.close();
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  private void scanForJarClasses(Path path,
                                 String packagePath,
                                 ByteCodeClassScanner classScanner,
                                 JarByteCodeMatcher matcher)
  {
    ZipFile zipFile = null;

    try {
      zipFile = new ZipFile(path.getNativePath());

      Enumeration<? extends ZipEntry> e = zipFile.entries();

      while (e.hasMoreElements()) {
        ZipEntry entry = e.nextElement();

        String entryName = entry.getName();
        if (! entryName.endsWith(".class"))
          continue;

        if (packagePath != null && ! entryName.startsWith(packagePath))
          continue;

        matcher.init();

        ReadStream is = Vfs.openRead(zipFile.getInputStream(entry));
        try {
          classScanner.init(entryName, is, matcher);

          classScanner.scan();
        } finally {
          is.close();
        }
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      try {
        if (zipFile != null)
          zipFile.close();
      } catch (Exception e) {
      }
    }
  }

  static class JarByteCodeMatcher extends ScanByteCodeMatcher {
    JarByteCodeMatcher(EnvironmentClassLoader loader,
                       Path root,
                       String packageName,
                       ScanListener []listeners)
    {
      super(loader, root, packageName, listeners);
    }
  }

  static class PathByteCodeMatcher extends ScanByteCodeMatcher {
    private Path _root;
    private Path _path;

    PathByteCodeMatcher(EnvironmentClassLoader loader,
                        Path root,
                        String packageName,
                        ScanListener []listeners)
    {
      super(loader, root, packageName, listeners);
    }

    void init(Path root, Path path)
    {
      super.init();

      _root = root;
      _path = path;
    }

    String getClassName()
    {
      String rootName = _root.getFullPath();
      String name = _path.getFullPath();
      
      int p = name.lastIndexOf('.');

      String className = name.substring(rootName.length(), p);

      return className.replace('/', '.');
    }
  }
    
  abstract static class ScanByteCodeMatcher implements ByteCodeClassMatcher {
    private Path _root;
    private String _packageRoot;
    
    private final ScanListener []_listeners;
    private final ScanListener []_currentListeners;
    private final ScanClass []_currentClasses;

    ScanByteCodeMatcher(EnvironmentClassLoader loader,
                        Path root,
                        String packageRoot,
                        ScanListener []listeners)
    {
      _root = root;
      _packageRoot = packageRoot;
      
      _listeners = listeners;
      _currentListeners = new ScanListener[listeners.length];
      _currentClasses = new ScanClass[listeners.length];
    }

    void init()
    {
      for (int i = 0; i < _listeners.length; i++) {
        _currentListeners[i] = _listeners[i];
        _currentClasses[i] = null;
      }
    }
    
    /**
     * Returns true if the annotation class is a match.
     */
    @Override
    public boolean scanClass(String className, int modifiers)
    {
      int activeCount = 0;

      for (int i = _listeners.length - 1; i >= 0; i--) {
        ScanListener listener = _currentListeners[i];

        if (listener == null)
          continue;

        ScanClass scanClass = listener.scanClass(_root, _packageRoot, 
                                                 className, modifiers);

        if (scanClass != null) {
          activeCount++;
          _currentClasses[i] = scanClass;
        }
        else {
          _currentListeners[i] = null;
        }
      }

      return activeCount > 0;
    }

    @Override
    public void addInterface(char[] buffer, int offset, int length)
    {
      for (ScanClass scanClass : _currentClasses) {
        if (scanClass != null) {
          scanClass.addInterface(buffer, offset, length);
        }
      }
    }

    @Override
    public void addSuperClass(char[] buffer, int offset, int length)
    {
      for (ScanClass scanClass : _currentClasses) {
        if (scanClass != null) {
          scanClass.addSuperClass(buffer, offset, length);
        }
      }
    }

    @Override
    public void addClassAnnotation(char[] buffer, int offset, int length)
    {
      for (ScanClass scanClass : _currentClasses) {
        if (scanClass != null) {
          scanClass.addClassAnnotation(buffer, offset, length);
        }
      }
    }

    @Override
    public void addPoolString(char[] buffer, int offset, int length)
    {
      for (ScanClass scanClass : _currentClasses) {
        if (scanClass != null) {
          scanClass.addPoolString(buffer, offset, length);
        }
      }
    }

    @Override
    public void finishScan()
    {
      for (ScanClass scanClass : _currentClasses) {
        if (scanClass != null) {
          scanClass.finishScan();
        }
      }
    }
 
    /**
     * Returns true if the annotation class is a match.
     */
    public boolean isAnnotationMatch(CharBuffer annotationClassName)
    {
      int activeCount = 0;

      for (int i = _listeners.length - 1; i >= 0; i--) {
        ScanListener listener = _currentListeners[i];

        if (listener == null)
          continue;

        if (listener.isScanMatchAnnotation(annotationClassName)) {
          _currentListeners[i] = null;
        }
        else
          activeCount++;
      }

      return activeCount == 0;
    }
  }
}
