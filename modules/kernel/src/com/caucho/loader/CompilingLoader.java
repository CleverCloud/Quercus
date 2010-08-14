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

import com.caucho.config.ConfigException;
import com.caucho.java.CompileClassNotFound;
import com.caucho.java.JavaCompiler;
import com.caucho.make.AlwaysModified;
import com.caucho.make.Make;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class loader that automatically compiles Java.
 */
public class CompilingLoader extends Loader implements Make {
  private static final Logger log
    = Logger.getLogger(CompilingLoader.class.getName());
  private static final L10N L = new L10N(CompilingLoader.class);

  private static final char []INNER_CLASS_SEPARATORS =
    new char[] {'$', '+', '-'};
  
  // classpath
  private String _classPath;

  private String _compiler;
  private String _sourceExt = ".java";
  
  // source directory
  private Path _sourceDir;
  // directory where classes are stored
  private Path _classDir;

  private CodeSource _codeSource;
  
  private ArrayList<String> _args;
  private String _encoding;
  private boolean _requireSource;

  private HashSet<String> _excludedDirectories = new HashSet<String>();

  private long _lastMakeTime;

  private boolean _isBatch = true;

  public CompilingLoader()
  {
    this(Thread.currentThread().getContextClassLoader());
  }
  
  public CompilingLoader(ClassLoader loader)
  {
    super(loader);
    
    _excludedDirectories.add("CVS");
    _excludedDirectories.add(".svn");
  }
  
  /**
   * Creates a new compiling class loader
   *
   * @param classDir generated class directory root
   */
  public CompilingLoader(ClassLoader loader, Path classDir)
  {
    this(loader, classDir, classDir, null, null);
  }

  /**
   * Creates a new compiling class loader
   *
   * @param classDir generated class directory root
   * @param sourceDir Java source directory root
   * @param args Javac arguments
   * @param encoding javac encoding
   */
  public CompilingLoader(ClassLoader loader,
                         Path classDir, Path sourceDir,
                         String args, String encoding)
  {
    this(loader);
    
    if (classDir.getScheme().equals("http")
        || classDir.getScheme().equals("https"))
      throw new RuntimeException("compiling class loader can't be http.  Use compile=false.");

    _sourceDir = sourceDir;
    _classDir = classDir;

    _encoding = encoding;

    // loader.addCodeBasePath(classDir.getFullPath());

      /*
    try {
      if (args != null)
        _args = new Regexp("[\\s,]+").split(args);
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
      */
  }

  /**
   * Create a class loader based on the compiling loader
   *
   * @param path traditional classpath
   *
   * @return the new ClassLoader
   */
  public static DynamicClassLoader create(Path path)
  {
    DynamicClassLoader loader = new DynamicClassLoader(null);

    CompilingLoader compilingLoader = new CompilingLoader(loader, path);
    compilingLoader.init();

    loader.init();

    return loader;
  }

  /**
   * Sets the class path.
   */
  public void setPath(Path path)
  {
    _classDir = path;
    
    if (_sourceDir == null)
      _sourceDir = path;
  }

  /**
   * Gets the class path.
   */
  public Path getPath()
  {
    return _classDir;
  }

  /**
   * Sets the source path.
   */
  public void setSource(Path path)
  {
    _sourceDir = path;
  }

  /**
   * Sets the source extension.
   */
  public void setSourceExtension(String ext)
    throws ConfigException
  {
    if (! ext.startsWith("."))
      throw new ConfigException(L.l("source-extension '{0}' must begin with '.'",
                                    ext));
    
    _sourceExt = ext;
  }

  /**
   * Sets the compiler.
   */
  public void setCompiler(String compiler)
    throws ConfigException
  {
    _compiler = compiler;
  }

  /**
   * Sets the source path.
   */
  public Path getSource()
  {
    return _sourceDir;
  }

  /**
   * Sets the arguments.
   */
  public void setArgs(String arg)
  {
    int i = 0;
    int len = arg.length();

    CharBuffer cb = new CharBuffer();
    
    while (i < len) {
      char ch;
      for (; i < len && Character.isWhitespace(ch = arg.charAt(i)); i++) {
      }

      if (len <= i)
        return;

      cb.clear();

      for (; i < len && ! Character.isWhitespace(ch = arg.charAt(i)); i++)
        cb.append(ch);

      addArg(cb.toString());
    }
  }

  /**
   * Adds an argument.
   */
  public void addArg(String arg)
  {
    if (_args == null)
      _args = new ArrayList<String>();
    
    _args.add(arg);
  }

  /**
   * Sets the encoding.
   */
  public void setEncoding(String encoding)
  {
    _encoding = encoding;
  }

  /**
   * Sets true if source is required.
   */
  public void setRequireSource(boolean requireSource)
  {
    _requireSource = requireSource;
  }

  /**
   * Sets true if compilation should batch as many files as possible.
   */
  public void setBatch(boolean isBatch)
  {
    _isBatch = isBatch;
  }

  /**
   * Initialize.
   */
  @PostConstruct
  @Override
  public void init()
    throws ConfigException
  {
    if (_classDir == null)
      throw new ConfigException(L.l("'path' is a required attribute of <compiling-loader>."));

    try {
      _classDir.mkdirs();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    
    try {
      _codeSource = new CodeSource(new URL(_classDir.getURL()), (Certificate []) null);
    } catch (Exception e) {
      String scheme = _classDir.getScheme();
      
      if (scheme != null && ! scheme.equals("memory"))
        log.log(Level.FINE, e.toString(), e);
    }
    
    super.init();

    getClassLoader().addURL(_classDir);
  }

  /**
   * Creates a new compiling class loader
   *
   * @param classDir generated class directory root
   * @param sourceDir Java source directory root
   * @param args Javac arguments
   * @param encoding javac encoding
   */
  public static DynamicClassLoader create(ClassLoader parent,
                                          Path classDir, Path sourceDir,
                                          String args, String encoding)
  {
    DynamicClassLoader loader = new DynamicClassLoader(parent);
    loader.addLoader(new CompilingLoader(loader, classDir, sourceDir, args, encoding));

    loader.init();
    
    return loader;
  }

  public String getClassPath()
  {
    if (_classPath == null)
      _classPath = getClassLoader().getClassPath();

    return _classPath;
  }

  /**
   * Compiles all changed files in the class directory.
   */
  @Override
  public void make()
    throws IOException, ClassNotFoundException
  {
    synchronized (this) {
      if (Alarm.getCurrentTime() < _lastMakeTime + 2000)
        return;
      
      makeImpl();

      _lastMakeTime = Alarm.getCurrentTime();
    }
  }

  private void makeImpl()
    throws IOException, ClassNotFoundException
  {
    if (_sourceDir.isDirectory() && ! _classDir.isDirectory())
      _classDir.mkdirs();

    String sourcePath = prefixClassPath(getClassPath());
    
    ArrayList<String> files = new ArrayList<String>();
    findAllModifiedClasses("", _sourceDir, _classDir, sourcePath, files);

    if (files.size() == 0)
      return;

    if (_isBatch) {
      String []paths = files.toArray(new String[files.size()]);

      compileBatch(paths, true);
    }
    else {
      while (files.size() > 0) {
        String path = files.remove(0);

        String []paths = new String[] { path };

        compileBatch(paths, true);
      }
    }
  }

  /**
   * Returns the classes which need compilation.
   */
  private void findAllModifiedClasses(String name,
                                      Path sourceDir,
                                      Path classDir,
                                      String sourcePath,
                                      ArrayList<String> sources)
    throws IOException, ClassNotFoundException
  {
    String []list;

    try {
      list = sourceDir.list();
    } catch (IOException e) {
      return;
    }

    for (int i = 0; list != null && i < list.length; i++) {
      if (list[i].startsWith("."))
        continue;

      if (_excludedDirectories.contains(list[i]))
        continue;
      
      Path subSource = sourceDir.lookup(list[i]);

      if (subSource.isDirectory()) {
        findAllModifiedClasses(name + list[i] + "/", subSource,
                               classDir.lookup(list[i]), sourcePath, sources);
      }
      else if (list[i].endsWith(_sourceExt)) {
        int tail = list[i].length() - _sourceExt.length();
        String prefix = list[i].substring(0, tail);
        Path subClass = classDir.lookup(prefix + ".class");

        if (subClass.getLastModified() < subSource.getLastModified()) {
          sources.add(name + list[i]);
        }
      }
    }

    if (! _requireSource)
      return;
    
    try {
      list = classDir.list();
    } catch (IOException e) {
      return;
    }

    for (int i = 0; list != null && i < list.length; i++) {
      if (list[i].startsWith("."))
        continue;

      if (_excludedDirectories.contains(list[i]))
        continue;
      
      Path subClass = classDir.lookup(list[i]);

      if (list[i].endsWith(".class")) {
        String prefix = list[i].substring(0, list[i].length() - 6);
        Path subSource = sourceDir.lookup(prefix + _sourceExt);

        if (! subSource.exists()) {
          String tail = subSource.getTail();
          boolean doRemove = true;

          if (tail.indexOf('$') > 0) {
            String subTail = tail.substring(0, tail.indexOf('$')) + _sourceExt;
            Path subJava = subSource.getParent().lookup(subTail);

            if (subJava.exists())
              doRemove = false;
          }

          if (doRemove) {
            log.finer(L.l("removing obsolete class '{0}'.", subClass.getPath()));

            subClass.remove();
          }
        }
      }
    }
  }

  /**
   * Loads the specified class, compiling if necessary.
   */
  @Override
  protected ClassEntry getClassEntry(String name, String pathName)
    throws ClassNotFoundException
  {
    Path classFile = _classDir.lookup(pathName);
    /*
    Path classDir = classFile.getParent();
    
    if (! classDir.isDirectory())
      return null;
    */
    
    String javaName = name.replace('.', '/') + _sourceExt;
    Path javaFile = _sourceDir.lookup(javaName);

    for (int i = 0; i < INNER_CLASS_SEPARATORS.length; i++) {
      char sep = INNER_CLASS_SEPARATORS[i];
      if (name.indexOf(sep) > 0) {
        String subName = name.substring(0, name.indexOf(sep));
        String subJavaName = subName.replace('.', '/') + _sourceExt;
        Path subJava = _sourceDir.lookup(subJavaName);

        if (subJava.exists()) {
          javaFile = subJava;
        }
      }
    }

    synchronized (this) {
      if (_requireSource && ! javaFile.exists()) {
        boolean doRemove = true;

        if (doRemove) {
          log.finer(L.l("removing obsolete class `{0}'.", classFile.getPath()));

          try {
            classFile.remove();
          } catch (IOException e) {
            log.log(Level.WARNING, e.toString(), e);
          }

          return null;
        }
      }

      if (! classFile.canRead() && ! javaFile.canRead())
        return null;

      return new CompilingClassEntry(this, getClassLoader(),
                                     name, javaFile,
                                     classFile,
                                     getCodeSource(classFile));
    }
  }

  /**
   * Returns the code source for the directory.
   */
  protected CodeSource getCodeSource(Path path)
  {
    return _codeSource;
  }

  /**
   * Checks that the case is okay for the source.
   */
  boolean checkSource(Path sourceDir, String javaName)
  {
    try {
      while (javaName != null && ! javaName.equals("")) {
        int p = javaName.indexOf('/');
        String head;
      
        if (p >= 0) {
          head = javaName.substring(0, p);
          javaName = javaName.substring(p + 1);
        }
        else {
          head = javaName;
          javaName = null;
        }

        String []names = sourceDir.list();
        int i;
        for (i = 0; i < names.length; i++) {
          if (names[i].equals(head))
            break;
        }

        if (i == names.length)
          return false;

        sourceDir = sourceDir.lookup(head);
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
      
      return false;
    }

    return true;
  }

  /**
   * Compile the Java source.  Compile errors are encapsulated in a
   * ClassNotFound wrapper.
   *
   * @param javaSource path to the Java source
   */
  void compileClass(Path javaSource, Path javaClass,
                    String sourcePath, boolean isMake)
    throws ClassNotFoundException
  {
    try {
      JavaCompiler compiler = JavaCompiler.create(getClassLoader());
      compiler.setClassDir(_classDir);
      compiler.setSourceDir(_sourceDir);
      if (_encoding != null)
        compiler.setEncoding(_encoding);
      compiler.setArgs(_args);
      compiler.setCompileParent(! isMake);
      compiler.setSourceExtension(_sourceExt);
      if (_compiler != null)
        compiler.setCompiler(_compiler);

      //LineMap lineMap = new LineMap(javaFile.getNativePath());
      // The context path is obvious from the browser url
      //lineMap.add(name.replace('.', '/') + _sourceExt, 1, 1);

      // Force this into a relative path so different compilers will work
      String prefix = _sourceDir.getPath();
      String full = javaSource.getPath();

      String source;
      if (full.startsWith(prefix)) {
        source = full.substring(prefix.length());
        if (source.startsWith("/"))
          source = source.substring(1);
      }
      else
        source = javaSource.getPath();

      /*
      if (javaSource.canRead() && javaClass.exists())
        javaClass.remove();
      */
          
      compiler.compileIfModified(source, null);
    } catch (Exception e) {
      getClassLoader().addDependency(new Depend(javaSource));

      log.log(Level.FINEST, e.toString(), e);

      // Compile errors are wrapped in a special ClassNotFound class
      // so the server can give a nice error message
      throw new CompileClassNotFound(e);
    }
  }

  /**
   * Compile the Java source.  Compile errors are encapsulated in a
   * ClassNotFound wrapper.
   */
  void compileBatch(String []files, boolean isMake)
    throws ClassNotFoundException
  {
    try {
      JavaCompiler compiler = JavaCompiler.create(getClassLoader());
      compiler.setClassDir(_classDir);
      compiler.setSourceDir(_sourceDir);
      if (_encoding != null)
        compiler.setEncoding(_encoding);
      compiler.setArgs(_args);
      compiler.setCompileParent(! isMake);
      compiler.setSourceExtension(_sourceExt);
      if (_compiler != null)
        compiler.setCompiler(_compiler);

      //LineMap lineMap = new LineMap(javaFile.getNativePath());
      // The context path is obvious from the browser url
      //lineMap.add(name.replace('.', '/') + _sourceExt, 1, 1);
          
      compiler.compileBatch(files);
    } catch (Exception e) {
      getClassLoader().addDependency(AlwaysModified.create());

      // Compile errors are wrapped in a special ClassNotFound class
      // so the server can give a nice error message
      throw new CompileClassNotFound(e);
    }
  }

  /**
   * Returns the path for the given name.
   *
   * @param name the name of the class
   */
  @Override
  public Path getPath(String name)
  {
    Path path = _classDir.lookup(name);

    if (path != null && path.exists())
      return path;

    path = _sourceDir.lookup(name);

    if (path != null && path.exists())
      return path;

    return null;
  }

  /**
   * Adds the classpath we're responsible for to the classpath
   *
   * @param head the overriding classpath
   * @return the new classpath
   */
  @Override
  protected void buildClassPath(ArrayList<String> pathList)
  {
    if (! _classDir.getScheme().equals("file"))
      return;

    try {
      if (! _classDir.isDirectory() && _sourceDir.isDirectory()) {
        try {
          _classDir.mkdirs();
        } catch (IOException e) {
        }
      }

      if (_classDir.isDirectory()) {
        String path = _classDir.getNativePath();

        if (! pathList.contains(path))
          pathList.add(path);
      }
    
      if (! _classDir.equals(_sourceDir)) {
        String path = _sourceDir.getNativePath();

        if (! pathList.contains(path))
          pathList.add(path);
      }
    } catch (java.security.AccessControlException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  protected String prefixClassPath(String tail)
  {
    CharBuffer cb = new CharBuffer();

    if (! _classDir.isDirectory() && _sourceDir.isDirectory()) {
      try {
        _classDir.mkdirs();
      } catch (IOException e) {
      }
    }

    if (_classDir.isDirectory()) {
      if (cb.length() > 0)
        cb.append(CauchoSystem.getPathSeparatorChar());
      cb.append(_classDir.getNativePath());
    }
    
    if (! _classDir.equals(_sourceDir)) {
      if (cb.length() > 0)
        cb.append(CauchoSystem.getPathSeparatorChar());
      cb.append(_sourceDir.getNativePath());
    }

    if (cb.length() > 0)
      cb.append(CauchoSystem.getPathSeparatorChar());
    cb.append(tail);

    return cb.close();
  }

  /*
  @Override
  protected void buildSourcePath(StringBuilder head)
  {
    buildClassPath(head);
  }
  */

  public String toString()
  {
    if (_classDir == null)
      return "CompilingLoader[]";
    else if (_classDir.equals(_sourceDir))
      return "CompilingLoader[src:" + _sourceDir + "]";
    else
      return ("CompilingLoader[src:" + _sourceDir +
              ",class:" + _classDir + "]");
  }
}
