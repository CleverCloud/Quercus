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

package com.caucho.java;

import com.caucho.loader.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.*;

/**
 * Experimental internal compiler API using JDK 1.6 JavaCompiler.
 *
 * Based on initial tests, the performance difference does not seem
 * to be large.
 */
public class InternalCompiler2 extends AbstractJavaCompiler {
  private static final Logger log
    = Logger.getLogger(InternalCompiler2.class.getName());

  private static final List<JavaFileObject> NULL_FILE_LIST
    = Collections.emptyList();

  private static final EnvironmentLocal<FileCache> _fileCacheLocal
    = new EnvironmentLocal<FileCache>();

  private static final FreeList<JavaFileManager> _freeSystemManager
    = new FreeList<JavaFileManager>(4);


  public InternalCompiler2(JavaCompiler compiler)
  {
    super(compiler);
  }

  protected void compileInt(String []path, LineMap lineMap)
    throws IOException, JavaCompileException
  {
    executeInt(path, lineMap);
  }

  protected Path getClassDir()
  {
    return _compiler.getClassDir();
  }

  /**
   * Compiles the names files.
   */
  private void executeInt(String []path, LineMap lineMap)
    throws JavaCompileException, IOException
  {
    MemoryStream tempStream = new MemoryStream();
    WriteStream error = new WriteStream(tempStream);

    try {
      // String parent = javaPath.getParent().getNativePath();

      ArrayList<String> optionList = new ArrayList<String>();
      optionList.add("-d");
      optionList.add(_compiler.getClassDirName());
      if (_compiler.getEncoding() != null) {
        String encoding = Encoding.getJavaName(_compiler.getEncoding());
        if (encoding != null && ! encoding.equals("ISO8859_1")) {
          optionList.add("-encoding");
          optionList.add(_compiler.getEncoding());
        }
      }

      optionList.add("-classpath");
      optionList.add(_compiler.getClassPath());

      ArrayList<String> options = _compiler.getArgs();
      if (options != null)
        optionList.addAll(options);

      ArrayList<String> classes = null;

      ArrayList<JavaFileObject> files = new ArrayList<JavaFileObject>();
      for (int i = 0; i < path.length; i++) {
        Path javaPath = _compiler.getSourceDir().lookup(path[i]);
        files.add(createJavaFileObject(javaPath));
      }

      if (log.isLoggable(Level.FINER)) {
        logJavac(optionList);
      }

      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
        // thread.setContextClassLoader(env);

        try {
          javax.tools.JavaCompiler compiler
            = ToolProvider.getSystemJavaCompiler();

          DiagnosticListener<? super JavaFileObject> diagnosticListener
            = new DiagnosticListenerImpl<JavaFileObject>();

          JavaFileManager parentFileManager
            = buildFileManager(compiler, oldLoader, diagnosticListener);

          JavaFileManager fileManager
            = new CauchoFileManager(parentFileManager);

          javax.tools.JavaCompiler.CompilationTask value;

          value = compiler.getTask(error.getPrintWriter(),
                                   fileManager,
                                   diagnosticListener,
                                   options,
                                   classes,
                                   files);

          Boolean result = value.call();
        } catch (Exception e) {
          throw new JavaCompileException(e);
        }

        error.close();
        tempStream.close();
      } finally {
        thread.setContextClassLoader(oldLoader);
      }

      int status = 0;

      ReadStream read = tempStream.openReadAndSaveBuffer();
      JavacErrorParser parser = new JavacErrorParser(this, path[0], _compiler.getEncoding());

      String errors = parser.parseErrors((InputStream) read, lineMap);
      read.close();

      if (errors != null)
        errors = errors.trim();

      if (log.isLoggable(Level.FINE)) {
        read = tempStream.openReadAndSaveBuffer();
        CharBuffer cb = new CharBuffer();
        int ch;
        while ((ch = read.read()) >= 0) {
          cb.append((char) ch);
        }
        read.close();

        log.fine(cb.toString());
      }
      else if (status == 0 && errors != null && ! errors.equals("")) {
        final String msg = errors;

        new com.caucho.loader.ClassLoaderContext(_compiler.getClassLoader()) {
          public void run()
          {
            log.warning(msg);
          }
        };
      }
      
      if (status != 0)
        throw new JavaCompileException(errors);
    } finally {
      tempStream.destroy();
    }
  }

  private void logJavac(ArrayList<String> optionList)
  {
    CharBuffer msg = new CharBuffer();
    msg.append("javac(int)");
    for (int i = 0; i < optionList.size(); i++) {
      if (optionList.get(i).equals("-classpath")
          && ! log.isLoggable(Level.FINEST)) {
        i++;
        continue;
      }

      msg.append(" ");
      msg.append(optionList.get(i));
    }

    log.finer(msg.toString());
  }

  private JavaFileObject createJavaFileObject(Path path)
  {
    return new PathFileObject(path, JavaFileObject.Kind.SOURCE);
  }

  private JavaFileManager buildFileManager(javax.tools.JavaCompiler compiler,
                                           ClassLoader loader,
                                           DiagnosticListener listener)
  {
    if (loader instanceof DynamicClassLoader) {
      JavaFileManager parent = buildFileManager(compiler,
                                                loader.getParent(),
                                                listener);

      return new EnvironmentFileManager(parent, (DynamicClassLoader) loader);
    }
    else {
      JavaFileManager standardFileManager = _freeSystemManager.allocate();

      if (standardFileManager == null) {
        standardFileManager
          = compiler.getStandardFileManager(listener, null, null);
      }

      return new CacheFileManager(standardFileManager, loader);
    }
  }

  static class PathFileObject extends SimpleJavaFileObject {
    private final Path _path;

    PathFileObject(Path path, JavaFileObject.Kind kind)
    {
      super(createURI(path), kind);

      _path = path;

      System.out.println("PFO: " + toUri());
    }

    private static URI createURI(Path path)
    {
      try {
        return new URI(path.getURL());
      } catch (Exception e) {
        throw new JavaCompileException(e);
      }
    }

    public String getName()
    {
      return _path.getFullPath();
    }

    public long getLastModified()
    {
      return _path.getLastModified();
    }

    public boolean delete()
    {
      try {
        return _path.remove();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public CharSequence getCharContent(boolean ignoreEncodingErrors)
      throws IOException
    {
      StringBuilder sb = new StringBuilder();
      ReadStream is = _path.openRead();
      try {
        char []buffer = new char[1024];
        int len;

        while ((len = is.read(buffer, 0, buffer.length)) > 0) {
          sb.append(buffer, 0, len);
        }

        return sb;
      } finally {
        is.close();
      }
    }

    public InputStream openInputStream()
      throws IOException
    {
      return _path.openRead();
    }

    public OutputStream openOutputStream()
      throws IOException
    {
      _path.getParent().mkdirs();

      return _path.openWrite();
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _path + "]";
    }
  }

  static class DiagnosticListenerImpl<S> implements DiagnosticListener<S> {
    public void report(Diagnostic<? extends S> diagnostic)
    {
      System.out.println("ERROR: " + diagnostic);
    }
  }

  class DelegatingFileManager implements JavaFileManager {
    protected JavaFileManager _parent;

    DelegatingFileManager(JavaFileManager parent)
    {
      _parent = parent;
    }

    public ClassLoader getClassLoader(JavaFileManager.Location location)
    {
      return _parent.getClassLoader(location);
    }

    public FileObject getFileForInput(JavaFileManager.Location location,
                                      String packageName,
                                      String relativeName)
      throws IOException
    {
      FileObject result
        = _parent.getFileForInput(location, packageName, relativeName);

      return result;
    }

    public FileObject getFileForOutput(JavaFileManager.Location location,
                                       String packageName,
                                       String relativeName,
                                       FileObject sibling)
      throws IOException
    {
      FileObject result = _parent.getFileForOutput(location,
                                                   packageName, relativeName,
                                                   sibling);

      return result;
    }

    public JavaFileObject getJavaFileForInput(JavaFileManager.Location location,
                                              String className,
                                              JavaFileObject.Kind kind)
      throws IOException
    {
      return _parent.getJavaFileForInput(location, className, kind);
    }

    public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location,
                                               String className,
                                               JavaFileObject.Kind kind,
                                               FileObject sibling)
      throws IOException
    {
      JavaFileObject result
        = _parent.getJavaFileForOutput(location, className, kind, sibling);

      return result;
    }

    public int isSupportedOption(String option)
    {
      return _parent.isSupportedOption(option);
    }

    public boolean handleOption(String current, Iterator<String> remaining)
    {
      return _parent.handleOption(current, remaining);
    }

    public boolean hasLocation(JavaFileManager.Location location)
    {
      return _parent.hasLocation(location);
    }

    public String inferBinaryName(JavaFileManager.Location location,
                                  JavaFileObject file)
    {
      return _parent.inferBinaryName(location, file);
    }

    public boolean isSameFile(FileObject a, FileObject b)
    {
      return _parent.isSameFile(a, b);
    }

    public Iterable<JavaFileObject> list(JavaFileManager.Location location,
                                         String packageName,
                                         Set<JavaFileObject.Kind> kinds,
                                         boolean recurse)
      throws IOException
    {
      Iterable<JavaFileObject> result
        = _parent.list(location, packageName, kinds, recurse);

      // System.out.println("LIST: " + result);

      return result;
    }

    public void flush()
      throws IOException
    {
      _parent.flush();
    }

    public void close()
      throws IOException
    {
      _parent.close();
    }
  }

  class CauchoFileManager extends DelegatingFileManager {
    CauchoFileManager(JavaFileManager parent)
    {
      super(parent);
    }

    public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location,
                                               String className,
                                               JavaFileObject.Kind kind,
                                               FileObject sibling)
      throws IOException
    {
      Path path;

      switch (kind) {
      case CLASS:
        {
          String name = className.replace('.', '/') + ".class";
          path = getClassDir().lookup(name);
          return new PathFileObject(path, kind);
        }
      }

      return super.getJavaFileForOutput(location, className, kind, sibling);
    }
  }

  class EnvironmentFileManager extends DelegatingFileManager {
    private final DynamicClassLoader _loader;

    EnvironmentFileManager(JavaFileManager parent,
                           DynamicClassLoader loader)
    {
      super(parent);

      _loader = loader;
    }
  }

  class CacheFileManager extends DelegatingFileManager {
    private final FileCache _fileCache;

    CacheFileManager(JavaFileManager parent, ClassLoader loader)
    {
      super(parent);

      FileCache fileCache = _fileCacheLocal.getLevel(loader);
      if (fileCache == null) {
        fileCache = new FileCache();
        _fileCacheLocal.set(fileCache, loader);
      }

      _fileCache = fileCache;
    }

    public int isSupportedOption(String option)
    {
      return 0;
    }

    public boolean handleOption(String current, Iterator<String> remaining)
    {
      return false;
    }

    public Iterable<JavaFileObject> list(JavaFileManager.Location location,
                                         String packageName,
                                         Set<JavaFileObject.Kind> kinds,
                                         boolean recurse)
      throws IOException
    {
      ListKey key = new ListKey(location, packageName, kinds, recurse);

      Iterable<JavaFileObject> result = _fileCache.getList(key);

      if (result == null) {
        result = super.list(location, packageName, kinds, recurse);

        if (result == null)
          result = NULL_FILE_LIST;

        _fileCache.putList(key, result);
      }
      else {
      }

      if (result == NULL_FILE_LIST)
        return null;
      else
        return result;
    }

    public void close()
    {
      JavaFileManager parent = _parent;
      _parent = null;

      if (parent != null)
        _freeSystemManager.free(parent);
    }
  }

  static class ListKey {
    private final JavaFileManager.Location _location;
    private final String _packageName;
    private final Set<JavaFileObject.Kind> _kinds;
    private final boolean _recurse;

    ListKey(JavaFileManager.Location location,
            String packageName,
            Set<JavaFileObject.Kind> kinds,
            boolean recurse)
    {
      _location = location;
      _packageName = packageName;
      _kinds = kinds;
      _recurse = recurse;
    }

    @Override
    public int hashCode()
    {
      return _packageName.hashCode() * 65521 + _kinds.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
      if (o == null || o.getClass() != getClass())
        return false;

      ListKey key = (ListKey) o;

      return (_packageName.equals(key._packageName)
              && _kinds.equals(key._kinds)
              && _location.equals(key._location)
              && _recurse ==  key._recurse);
    }
  }

  static class FileCache {
    private final LruCache<ListKey,Iterable<JavaFileObject>> _listCache
      = new  LruCache<ListKey,Iterable<JavaFileObject>>(1024);

    Iterable<JavaFileObject> getList(ListKey key)
    {
      return _listCache.get(key);
    }

    void putList(ListKey key, Iterable<JavaFileObject> list)
    {
      _listCache.put(key, list);
    }
  }
}
