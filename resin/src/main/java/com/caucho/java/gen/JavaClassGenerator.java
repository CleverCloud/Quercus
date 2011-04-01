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

package com.caucho.java.gen;

import com.caucho.java.JavaCompiler;
import com.caucho.java.JavaWriter;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.SimpleLoader;
import com.caucho.loader.enhancer.EnhancingClassLoader;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.MergePath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for generating Java classes.
 */
public class JavaClassGenerator {
  private static final L10N L = new L10N(JavaClassGenerator.class);
  private static final Logger log
    = Logger.getLogger(JavaClassGenerator.class.getName());
  
  // Parent class loader
  private ClassLoader _parentLoader;
  // class loader
  private ClassLoader _loader;

  private String _encoding;
  
  // The search path
  private Path _searchPath;

  // The work directory
  private Path _workPath;

  private ArrayList<String> _pendingFiles = new ArrayList<String>();

  private String _initMethod = "_caucho_init";
  private String _isModifiedMethod = "_caucho_is_modified";

  /**
   * Sets the full generated class.
   */
  public static String cleanClassName(String className)
  {
    StringBuilder cb = new StringBuilder();
    
    for (int i = 0; i < className.length(); i++) {
      char ch = className.charAt(i);

      if (ch == '.' || ch == '/')
        cb.append('.');
      else if (Character.isJavaIdentifierPart(ch))
        cb.append(ch);
      else
        cb.append('_');
    }

    return cb.toString();
  }

  /**
   * Returns the default merge path
   */
  public static Path getDefaultSearchPath()
  {
    MergePath mergePath = new MergePath();
    
    mergePath.addMergePath(Vfs.lookup());
    mergePath.addClassPath();

    return mergePath;
  }
  
  /**
   * Sets the search path.
   */
  public void setSearchPath(Path path)
  {
    _searchPath = path;
  }

  /**
   * Returns the assigned search path.
   */
  public Path getSearchPath()
  {
    return _searchPath;
  }
  
  public void setEncoding(String encoding)
  {
    _encoding = encoding;
  }
  
  /**
   * Sets the parent class loader.
   *
   * @param loader parent class loader
   */
  public void setParentLoader(ClassLoader loader)
  {
    if (loader instanceof EnhancingClassLoader) {
      EnhancingClassLoader enhancingLoader = (EnhancingClassLoader) loader;
    }
    
    _parentLoader = loader;
  }
  
  /**
   * Sets the class loader.
   *
   * @param loader parent class loader
   */
  public void setLoader(ClassLoader loader)
  {
    _loader = loader;
  }
  
  /**
   * Gets the class loader.
   */
  public ClassLoader getClassLoader()
  {
    return _loader;
    /*
    if (_loader == null) {
      _loader = SimpleLoader.create(getParentLoader(),
                                    getWorkDir());
    }
    
    return _loader;
    */
  }

  /**
   * Sets the parent class loader.
   *
   * @return the parent class loader
   */
  public ClassLoader getParentLoader()
  {
    if (_parentLoader == null)
      _parentLoader = Thread.currentThread().getContextClassLoader();
    
    return _parentLoader;
  }

  /**
   * Sets the parent class loader.
   *
   * @return the parent class loader
   */
  public ClassLoader getPreloadLoader()
  {
    return Thread.currentThread().getContextClassLoader();

      /*
      if (_preloadLoader instanceof EnhancingClassLoader) {
        EnhancingClassLoader enhancingLoader;
        enhancingLoader = (EnhancingClassLoader) _preloadLoader;
        _preloadLoader = enhancingLoader.getRawLoader();
      }
      */
  }

  /**
   * Sets the work path for the generated class.
   */
  public void setWorkDir(Path workPath)
  {
    _workPath = workPath;
  }
  
  /**
   * Returns the class dir for the generated class.
   */
  public Path getWorkDir()
  {
    if (_workPath == null)
      return CauchoSystem.getWorkPath();
    else
      return _workPath;
  }

  /**
   * Try to preload the class.
   *
   * @return true if the preloaded class is still valid.
   */
  public Class<?> preload(String fullClassName)
  {
    try {
      Class<?> cl = loadClass(fullClassName, true);

      if (cl != null) {
        // force validation of the class
        Constructor<?> []ctors = cl.getConstructors();
      }

      return cl;
    } catch (ClassNotFoundException e) {
      log.log(Level.FINEST, e.toString(), e);
      
      return null;
    } catch (ClassFormatError e) {
      log.log(Level.FINEST, e.toString(), e);
      
      return null;
    }
  }

  /**
   * Try to preload the class.
   *
   * @return true if the preloaded class is still valid.
   */
  public Class<?> load(String fullClassName)
  {
    try {
      Class<?> cl = loadClass(fullClassName, false);

      if (cl != null) {
        // force validation of the class
        Constructor<?> []ctors = cl.getConstructors();
      }

      return cl;
    } catch (ClassNotFoundException e) {
      log.log(Level.FINEST, e.toString(), e);
      
      return null;
    } catch (ClassFormatError e) {
      log.log(Level.FINEST, e.toString(), e);
      
      return null;
    }
  }

  public Path getClassFilePath(String className)
  {
    String classPathName = className.replace('.', '/') + ".class";
    
    return getWorkDir().lookup(classPathName);
  }

  /**
   * Call to generate the java source.
   */
  public void generate(GenClass javaClass)
    throws Exception
  {
    String className = javaClass.getFullClassName();
    String javaPathName = className.replace('.', '/') + ".java";
    String classPathName = className.replace('.', '/') + ".class";

    Path javaPath = getWorkDir().lookup(javaPathName);
    Path classPath = getWorkDir().lookup(classPathName);

    try {
      classPath.remove();
    } catch (IOException e) {
    }

    javaPath.getParent().mkdirs();

    WriteStream os = javaPath.openWrite();
    try {
      if (_encoding != null)
        os.setEncoding(_encoding);
      else
        os.setEncoding("JAVA");
      
      JavaWriter out = new JavaWriter(os);

      javaClass.generate(out);
    } finally {
      os.close();
    }

    _pendingFiles.add(javaPathName);
  }

  public void addPendingFile(String javaPath)
  {
    _pendingFiles.add(javaPath);
  }

  /**
   * Compiles the Java code
   */
  public Class compile(String fullClassName)
    throws Exception
  {
    compileJava(fullClassName);
    
    return loadClass(fullClassName, false);
  }
  
  /**
   * Compiles the class.
   */
  public void compileJava(String fullClassName)
    throws IOException, ClassNotFoundException
  {
    JavaCompiler compiler = JavaCompiler.create(getPreloadLoader());

    compiler.setClassLoader(getPreloadLoader());
    compiler.setClassDir(getWorkDir());

    if (_encoding != null)
      compiler.setEncoding(_encoding);

    compiler.compile(fullClassName.replace('.', '/') + ".java", null);
  }

  /**
   * Returns the pending Java files.
   */
  public String []getPendingFiles()
  {
    String []files = new String[_pendingFiles.size()];
    _pendingFiles.toArray(files);
    _pendingFiles.clear();

    return files;
  }

  public void addPendingFiles(String []files)
  {
    for (String file : files) {
      _pendingFiles.add(file);
    }
  }
  
  /**
   * Compiles the pending files
   */
  public void compilePendingJava()
    throws IOException, ClassNotFoundException
  {
    JavaCompiler compiler = JavaCompiler.create(getPreloadLoader());

    compiler.setClassLoader(getPreloadLoader());
    compiler.setClassDir(getWorkDir());

    if (_encoding != null)
      compiler.setEncoding(_encoding);

    String []files = new String[_pendingFiles.size()];
    _pendingFiles.toArray(files);
    _pendingFiles.clear();

    compiler.compileBatch(files);
  }

  /**
   * Loads the generated class.  If any class dependencies have
   * changed, return null.
   */
  public Class<?> loadClass(String fullClassName)
    throws ClassNotFoundException
  {
    return loadClass(fullClassName, false);
  }

  /**
   * Checks if the preload exists
   */
  public boolean preloadExists(String fullClassName)
  {
    Path workDir = getWorkDir();

    String classFile = fullClassName.replace('.', '/') + ".class";

    return workDir.lookup(classFile).exists();
  }

  /**
   * Loads the generated class.  If any class dependencies have
   * changed, return null.
   */
  public Class<?> loadClass(String fullClassName, boolean preload)
    throws ClassNotFoundException
  {
    DynamicClassLoader preloadLoader = null;
    
    try {
      ClassLoader loader;

      if (preload) {
        preloadLoader = SimpleLoader.create(getPreloadLoader(),
                                            getWorkDir(),
                                            fullClassName);
        // needed for cases like Amber enhancing
        preloadLoader.setServletHack(true);
        loader = preloadLoader;
      }
      else {
        // XXX: because of automatic instantiation, might cause trouble
        loader = getClassLoader();      

        if (loader == null) {
          loader = SimpleLoader.create(getParentLoader(),
                                       getWorkDir(),
                                       fullClassName);
        }
      }

      Class<?> cl = Class.forName(fullClassName, false, loader);

      if (cl == null)
        return null;
      
      if (! preload)
        return cl;

      if (isModified(cl)) {
        return null;
      }

      if (_loader != null)
        loader = _loader;
      else {
        loader = SimpleLoader.create(getParentLoader(),
                                     getWorkDir(),
                                     fullClassName);
      }

      cl = Class.forName(fullClassName, false, loader);

      return cl;
    } catch (RuntimeException e) {
      if (! preload)
        throw e;
      else {
        log.log(Level.FINE, e.toString(), e);
        
        return null;
      }
    } catch (Error e) {
      if (! preload)
        throw e;
      else {
        log.log(Level.FINE, e.toString(), e);
      
        return null;
      }
    } catch (ClassNotFoundException e) {
      if (! preload)
        throw e;
      else
        return null;
    } finally {
      if (preloadLoader != null)
        preloadLoader.destroy();
    }
  }

  /**
   * Loads the generated class into the parent loader.
   */
  public Class<?> preloadClassParentLoader(String fullClassName,
                                           Class<?> parentClass)
  {
    try {
      Class<?> cl = loadClassParentLoader(fullClassName, parentClass);
      
      if (! isModified(cl))
        return cl;
      else
        return null;
    } catch (ClassNotFoundException e) {
      log.log(Level.ALL, e.toString(), e);
      
      return null;
    }
  }

  /**
   * Loads the generated class into the parent loader.
   */
  public Class<?> loadClassParentLoader(String fullClassName,
                                        Class<?> parentClass)
      throws ClassNotFoundException
  {
    ClassLoader parentLoader = parentClass.getClassLoader();
    
    if (! (parentLoader instanceof DynamicClassLoader)) {
      throw new IllegalStateException(parentClass + " must belong to a Resin class loader " + parentLoader);
    }
    
    DynamicClassLoader dynParentLoader
      = (DynamicClassLoader) parentLoader;

    SimpleLoader simpleLoader
      = new SimpleLoader(dynParentLoader, getWorkDir(), fullClassName);
    simpleLoader.init();

    try {
      return Class.forName(fullClassName, false, dynParentLoader);
    } finally {
      // XXX: parentLoader.removeLoader(simpleLoader);
    }
  }

  /**
   * Returns true if the class is modified.
   */
  public boolean isModified(Class<?> cl)
  {
    Path searchPath = getSearchPath();
      
    if (searchPath == null)
      searchPath = getDefaultSearchPath();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    try {
      if (_parentLoader != null)
        thread.setContextClassLoader(_parentLoader);

      Method method = cl.getMethod(_initMethod, new Class[] { Path.class });
      method.invoke(null, new Object[] { searchPath });

      method = cl.getMethod(_isModifiedMethod, new Class[0]);
      Boolean value = (Boolean) method.invoke(null, new Object[] {});

      return value.booleanValue();
    } catch (NoSuchMethodException e) {
      return false;
    } catch (Throwable e) {
      log.warning(e.toString());
      
      return true;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
}
