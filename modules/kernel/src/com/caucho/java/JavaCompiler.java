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

import com.caucho.bytecode.ByteCodeParser;
import com.caucho.bytecode.JavaClass;
import com.caucho.bytecode.SourceDebugExtensionAttribute;
import com.caucho.config.*;
import com.caucho.env.thread.ThreadPool;
import com.caucho.i18n.CharacterEncoding;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Loader;
import com.caucho.make.Make;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Compiles Java source, returning the loaded class.
 */
public class JavaCompiler {
  static final L10N L = new L10N(JavaCompiler.class);
  static final Logger log
    = Logger.getLogger(JavaCompiler.class.getName());

  private static final Object LOCK = new Object();

  // Parent class loader.  Used to grab the classpath.
  private ClassLoader _loader;

  // Executable name of the compiler
  private String _compiler;

  private String _sourceExt = ".java";

  private Path _classDir;
  private Path _sourceDir;

  private boolean _compileParent = true;

  private String _extraClassPath;
  private String _classPath;

  protected String _charEncoding;
  protected ArrayList<String> _args;

  private int _maxBatch = 64;
  protected long _maxCompileTime = 120 * 1000L;

  private JavaCompiler()
  {
    String encoding = CharacterEncoding.getLocalEncoding();
    String javaEncoding = Encoding.getJavaName(encoding);

    if (javaEncoding == null || javaEncoding.equals("ISO8859_1"))
      javaEncoding = null;
    
    _charEncoding = javaEncoding;
  }

  /**
   * Creates a new compiler.
   */
  public static JavaCompiler create()
  {
    return create(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Creates a new compiler.
   *
   * @param loader the parent class loader for the compiler.
   */
  public static JavaCompiler create(ClassLoader loader)
  {
    JavacConfig config = JavacConfig.getLocalConfig();

    String javac = config.getCompiler();

    JavaCompiler javaCompiler = new JavaCompiler();

    if (loader == null)
      loader = Thread.currentThread().getContextClassLoader();

    javaCompiler.setClassLoader(loader);
    javaCompiler.setCompiler(javac);
    javaCompiler.setArgs(config.getArgs());
    javaCompiler.setEncoding(config.getEncoding());
    javaCompiler.setMaxBatch(config.getMaxBatch());

    return javaCompiler;
  }

  /**
   * Sets the class loader used to load the compiled class and to grab
   * the classpath from.
   */
  public void setClassLoader(ClassLoader loader)
  {
    _loader = loader;
  }

  /**
   * Sets the class loader used to load the compiled class and to grab
   * the classpath from.
   */
  public ClassLoader getClassLoader()
  {
    return _loader;
  }

  /**
   * Sets the compiler name, e.g. jikes.
   */
  public void setCompiler(String compiler)
  {
    _compiler = compiler;
  }

  /**
   * Gets the compiler name, e.g. jikes.
   */
  public String getCompiler()
  {
    if (_compiler == null)
      _compiler = "javac";

    return _compiler;
  }

  /**
   * Sets the directory where compiled classes go.
   *
   * @param path representing the class dir.
   */
  public void setClassDir(Path path)
  {
    try {
      path.mkdirs();
    } catch (IOException e) {
    }

    _classDir = path;
  }

  /**
   * Returns the directory where compiled classes go.
   */
  Path getClassDir()
  {
    if (_classDir != null)
      return _classDir;
    else
      return CauchoSystem.getWorkPath();
  }

  /**
   * Returns the path to the class directory.
   */
  String getClassDirName()
  {
    return getClassDir().getNativePath();
  }

  /**
   * Sets the directory the java source comes from.
   *
   * @param path representing the source dir.
   */
  public void setSourceDir(Path path)
  {
    _sourceDir = path;
  }

  /**
   * Returns the directory where compiled classes go.
   */
  public Path getSourceDir()
  {
    if (_sourceDir != null)
      return _sourceDir;
    else
      return getClassDir();
  }

  /**
   * Returns the path to the class directory.
   */
  String getSourceDirName()
  {
    return getSourceDir().getNativePath();
  }

  /**
   * Sets the source extension.
   */
  public void setSourceExtension(String ext)
  {
    _sourceExt = ext;
  }

  /**
   * Gets the source extension.
   */
  public String getSourceExtension()
  {
    return _sourceExt;
  }

  /**
   * Sets the class path for compilation.  Normally, the classpath from
   * the class loader will be sufficient.
   */
  public void setClassPath(String classPath)
  {
    _classPath = classPath;
  }

  /**
   * Sets an extra class path for compilation.
   */
  public void setExtraClassPath(String classPath)
  {
    _extraClassPath = classPath;
  }

  public void setCompileParent(boolean compileParent)
  {
    _compileParent = compileParent;
  }

  /**
   * Returns the classpath.
   */
  public String getClassPath()
  {
    String rawClassPath = buildClassPath();

    if (true)
      return rawClassPath;

    char sep = CauchoSystem.getPathSeparatorChar();
    String []splitClassPath = rawClassPath.split("[" + sep + "]");

    String javaHome = System.getProperty("java.home");

    Path pwd = Vfs.lookup(System.getProperty("user.dir"));

    ArrayList<String> cleanClassPath = new ArrayList<String>();
    StringBuilder sb = new StringBuilder();

    for (String pathName : splitClassPath) {
      Path path = pwd.lookup(pathName);
      pathName = path.getNativePath();

      if (! pathName.startsWith(javaHome)
          && ! cleanClassPath.contains(pathName)) {
        cleanClassPath.add(pathName);
        if (sb.length() > 0)
          sb.append(sep);
        sb.append(pathName);
      }
    }

    return sb.toString();
  }


  /**
   * Returns the classpath for the compiler.
   */
  private String buildClassPath()
  {
    String classPath = null;//_classPath;

    if (classPath != null)
      return classPath;

    if (classPath == null && _loader instanceof DynamicClassLoader) {
      classPath = ((DynamicClassLoader) _loader).getClassPath();
    }
    else { // if (true || _loader instanceof URLClassLoader) {
      StringBuilder sb = new StringBuilder();
      sb.append(CauchoSystem.getClassPath());

      buildClassPath(sb, _loader);

      classPath = sb.toString();
    }
    //else if (classPath == null)
      //classPath = CauchoSystem.getClassPath();

    String srcDirName = getSourceDirName();
    String classDirName = getClassDirName();

    char sep = CauchoSystem.getPathSeparatorChar();

    if (_extraClassPath != null)
      classPath = classPath + sep + _extraClassPath;

    // Adding the srcDir lets javac and jikes find source files
    if (! srcDirName.equals(classDirName))
      classPath = srcDirName + sep + classPath;
    classPath = classDirName + sep + classPath;

    return classPath;
  }

  private static void buildClassPath(StringBuilder sb, ClassLoader loader)
  {
    ClassLoader parent = loader.getParent();

    if (parent != null)
      buildClassPath(sb, parent);

    if (loader instanceof URLClassLoader) {
      for (URL url : ((URLClassLoader) loader).getURLs()) {
        if (sb.length() > 0)
          sb.append(CauchoSystem.getPathSeparatorChar());

        String urlString = url.toString();
        if (urlString.startsWith("file:"))
          urlString = urlString.substring("file:".length());

        // https://issues.apache.org/bugzilla/show_bug.cgi?id=47053
        // Tomcat's WebAppClassLoader.getURLs() returns paths with spaces
        // replaced by %20
        if (Path.isWindows() && urlString.contains("%20")) {
          urlString = urlString.replace("%20", " ");
        }

        sb.append(urlString);
      }
    }
  }

  /**
   * Sets any additional arguments for the compiler.
   */
  public void setArgs(String argString)
  {
    try {
      if (argString != null) {
        String []args = Pattern.compile("[\\s,]+").split(argString);

        _args = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
          if (! args[i].equals(""))
            _args.add(args[i]);
        }
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Returns the ArrayList of arguments.
   */
  public ArrayList<String> getArgs()
  {
    return _args;
  }

  /**
   * Sets the Java encoding for the compiler.
   */
  public void setEncoding(String encoding)
  {
    _charEncoding = encoding;

    String javaEncoding = Encoding.getJavaName(encoding);

    if ("ISO8859_1".equals(javaEncoding))
      _charEncoding = null;
  }

  /**
   * Returns the encoding.
   */
  public String getEncoding()
  {
    return _charEncoding;
  }

  /**
   * Returns the maximum time allowed for an external compilation.
   */
  public long getMaxCompileTime()
  {
    return _maxCompileTime;
  }

  /**
   * Sets the maximum time allowed for an external compilation.
   */
  public void setMaxCompileTime(long maxCompileTime)
  {
    _maxCompileTime = maxCompileTime;
  }

  /**
   * Sets the maximum time allowed for an external compilation.
   */
  public void setMaxBatch(int maxBatch)
  {
    _maxBatch = maxBatch;
  }

  /**
   * Mangles the path into a valid Java class name.
   */
  public static String mangleName(String name)
  {
    boolean toLower = CauchoSystem.isCaseInsensitive();

    CharBuffer cb = new CharBuffer();
    cb.append("_");

    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);

      if (ch == '/' || ch == CauchoSystem.getPathSeparatorChar()) {
        if (i == 0) {
        }
        else if (cb.charAt(cb.length() - 1) != '.' &&
                 (i + 1 < name.length() && name.charAt(i + 1) != '/'))
          cb.append("._");
      }
      else if (ch == '.')
        cb.append("__");
      else if (ch == '_')
        cb.append("_0");
      else if (Character.isJavaIdentifierPart(ch))
        cb.append(toLower ? Character.toLowerCase(ch) : ch);
      else if (ch <= 256)
        cb.append("_2" + encodeHex(ch >> 4) + encodeHex(ch));
      else
        cb.append("_4" + encodeHex(ch >> 12) + encodeHex(ch >> 8) +
                  encodeHex(ch >> 4) + encodeHex(ch));
    }

    if (cb.length() == 0)
      cb.append("_z");

    return cb.toString();
  }

  private static char encodeHex(int i)
  {
    i &= 0xf;

    if (i < 10)
      return (char) (i + '0');
    else
      return (char) (i - 10 + 'a');
  }

  public void setArgs(ArrayList<String> args)
  {
    if (args == null)
      return;
    if (_args == null)
      _args = new ArrayList<String>();

    _args.addAll(args);
  }

  /**
   * Compiles the class.  className is a fully qualified Java class, e.g.
   * work.jsp.Test
   *
   * @param fileName Java source name -- in VFS format
   * @param lineMap mapping from generated class back to the source class
   *
   * @return compiled class
   */
  public void compile(String fileName, LineMap lineMap)
    throws IOException, ClassNotFoundException
  {
    compile(fileName, lineMap, false);
  }

  /**
   * Compiles the class.  className is a fully qualified Java class, e.g.
   * work.jsp.Test
   *
   * @param fileName Java source name -- in VFS format
   * @param lineMap mapping from generated class back to the source class
   *
   * @return compiled class
   */
  public void compileIfModified(String fileName, LineMap lineMap)
    throws IOException, ClassNotFoundException
  {
    compile(fileName, lineMap, true);
  }

  /**
   * Compiles the class.  className is a fully qualified Java class, e.g.
   * work.jsp.Test
   *
   * @param fileName Java source name -- in VFS format
   * @param lineMap mapping from generated class back to the source class
   * @param ifModified compile only if the *.java is modified
   *
   * @return compiled class
   */
  public void compile(String fileName, LineMap lineMap,
                      boolean ifModified)
    throws IOException, ClassNotFoundException
  {
    if (_compileParent) {
      try {
        if (_loader instanceof Make)
          ((Make) _loader).make();
      } catch (RuntimeException e) {
        throw e;
      } catch (ClassNotFoundException e) {
        throw e;
      } catch (IOException e) {
        throw e;
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }

    int p = fileName.lastIndexOf('.');
    String path = fileName.substring(0, p);
    String javaName = path + _sourceExt;
    Path javaPath = getSourceDir().lookup(javaName);

    String className = path + ".class";
    Path classPath = getClassDir().lookup(className);

    synchronized (LOCK) {
      if (ifModified
          && javaPath.getLastModified() <= classPath.getLastModified())
        return;

      if (javaPath.canRead() && classPath.exists())
        classPath.remove();

      compileInt(new String[] { fileName }, lineMap);

      // XXX: This is needed for some regressions to pass,
      // basically the timing wouldn't work if the classpath time
      // was selected by the compiler
      // server/141d, server/10k0
      // classPath.setLastModified(javaPath.getLastModified());
    }
  }

  /**
   * Compiles a batch list of classes.
   *
   * @return compiled class
   */
  public void compileBatch(String []files)
    throws IOException, ClassNotFoundException
  {
    if (_compileParent) {
      try {
        if (_loader instanceof Make)
          ((Make) _loader).make();
      } catch (Exception e) {
        throw new IOExceptionWrapper(e);
      }
    }

    if (files.length == 0)
      return;

    // only batch a number of files at a time

    int batchCount = _maxBatch;
    if (batchCount < 0)
      batchCount = Integer.MAX_VALUE / 2;
    else if (batchCount == 0)
      batchCount = 1;

    IOException exn = null;

    ArrayList<String> uniqueFiles = new ArrayList<String>();
    for (int i = 0; i < files.length; i++) {
      if (! uniqueFiles.contains(files[i]))
        uniqueFiles.add(files[i]);
    }
    files = new String[uniqueFiles.size()];
    uniqueFiles.toArray(files);

    synchronized (LOCK) {
      for (int i = 0; i < files.length; i += batchCount) {
        int len = files.length - i;

        if (batchCount < len)
          len = batchCount;

        String []batchFiles = new String[len];

        System.arraycopy(files, i, batchFiles, 0, len);

        Arrays.sort(batchFiles);

        try {
          compileInt(batchFiles, null);
        } catch (IOException e) {
          if (exn == null)
            exn = e;
          else
            log.log(Level.WARNING, e.toString(), e);
        }
      }
    }

    if (exn != null)
      throw exn;
  }

  protected void compileInt(String []path, LineMap lineMap)
    throws IOException, JavaCompileException
  {
    AbstractJavaCompiler compiler;

    for (int i = 0; i < path.length; i++)
      log.config("Compiling " + path[i]);

    if (_compiler.equals("internal"))
      compiler = new InternalCompiler(this);
    else if (_compiler.equals("internal2"))
      compiler = new InternalCompiler2(this);
    else if (_compiler.equals("eclipse"))
      compiler = new EclipseCompiler(this);
    else if (_compiler.equals("groovyc"))
      compiler = new GroovyCompiler(this);
    else
      compiler = new ExternalCompiler(this);

    compiler.setPath(path);
    compiler.setLineMap(lineMap);

    // the compiler may not be well-behaved enough to use the ThreadPool
    ThreadPool.getCurrent().schedule(compiler);

    compiler.waitForComplete(_maxCompileTime);

    if (! compiler.isDone()) {
      log.warning("compilation timed out");
      // thread.interrupt();
      compiler.abort();
    }

    Throwable exn = compiler.getException();

    if (exn == null) {
    }
    else if (exn instanceof IOException)
      throw (IOException) exn;
    else if (exn instanceof JavaCompileException)
      throw (JavaCompileException) exn;
    else if (exn instanceof RuntimeException)
      throw (RuntimeException) exn;
    else if (exn instanceof Error)
      throw (Error) exn;
    else
      throw new IOExceptionWrapper(exn);

    for (int i = 0; i < path.length; i++) {
      Path javaPath = getSourceDir().lookup(path[i]);

      if (! path[i].endsWith(".java"))
        continue;

      String className = path[i].substring(0, path[i].length() - 5) + ".class";
      Path classPath = getClassDir().lookup(className);
      Path smapPath = getSourceDir().lookup(path[i] + ".smap");

      if (classPath.canRead() && smapPath.canRead())
        mergeSmap(classPath, smapPath);
    }
  }

  public void mergeSmap(Path classPath, Path smapPath)
  {
    try {
      if (smapPath.getLength() >= 65536) {
        log.warning(".smap for " + classPath.getTail() + " is too large (" + smapPath.getLength() + " bytes)");
        return;
      }

      log.fine("merging .smap for " + classPath.getTail());

      ByteCodeParser parser = new ByteCodeParser();
      JavaClass javaClass;

      ReadStream is = classPath.openRead();
      try {
        javaClass = parser.parse(is);
      } finally {
        is.close();
      }

      CharBuffer smap = new CharBuffer();

      is = smapPath.openRead();
      try {
        int ch;

        while ((ch = is.read()) >= 0) {
          smap.append((char) ch);
        }
      } finally {
        is.close();
      }

      SourceDebugExtensionAttribute attr;

      attr = new SourceDebugExtensionAttribute(smap.toString());

      javaClass.addAttribute(attr);

      WriteStream os = classPath.openWrite();
      try {
        javaClass.write(os);
      } finally {
        os.close();
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
}
