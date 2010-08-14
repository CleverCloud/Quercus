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

package com.caucho.java;

import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.IOExceptionWrapper;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Compiles Groovy source, returning the loaded class.
 */
public class GroovyCompiler extends AbstractJavaCompiler {
  protected static final Logger log 
    = Logger.getLogger(GroovyCompiler.class.getName());

  private final static String GROOVY_COMPILER =
    "org.codehaus.groovy.tools.FileSystemCompiler";

  private static Class _groovyCompilerClass;
  private static Method _setOutputDir;
  private static Method _setClasspath;
  private static Method _compile;
  
  String _userPrefix;
  
  public GroovyCompiler(JavaCompiler compiler)
  {
    super(compiler);
  }

  /**
   * Compile the configured file.
   *
   * @param path the path to the java source.
   * @param lineMap mapping from the generated source to the original files.
   */
  protected void compileInt(String []paths, LineMap lineMap)
    throws IOException
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    if (_groovyCompilerClass == null) {
      try {
        _groovyCompilerClass = Class.forName(GROOVY_COMPILER);
        _setClasspath =
          _groovyCompilerClass.getMethod("setClasspath",
                                         new Class[] { String.class });

        _setOutputDir =
          _groovyCompilerClass.getMethod("setOutputDir",
                                         new Class[] { String.class });

        _compile =
          _groovyCompilerClass.getMethod("compile",
                                         new Class[] { String[].class });

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    Object compiler;
    try {
      compiler = _groovyCompilerClass.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    try {
      String sourceExt = _compiler.getSourceExtension();

      String path = paths[0];
      int tail = path.length() - sourceExt.length();
      String className = path.substring(0, tail);
      Path classFile = _compiler.getClassDir().lookup(className + ".class");

      String cp = normalizeClassPath(_compiler.getClassPath(), false);

      _setClasspath.invoke(compiler, new Object[] { cp });

      String dest = normalizePath(_compiler.getClassDirName(), false);

      _setOutputDir.invoke(compiler, new Object[] { dest });

      ArrayList<String> argList = new ArrayList<String>();
      for (int i = 0; i < paths.length; i++) {
        Path javaPath = _compiler.getSourceDir().lookup(paths[i]);
        argList.add(javaPath.getNativePath());
      }

      String []files = new String[argList.size()];

      argList.toArray(files);

      _compile.invoke(compiler, new Object[] {files});
    } catch (Exception e) {
      e.printStackTrace();
      throw new IOExceptionWrapper(e);
    }
  }

  /**
   * Converts any relative classpath references to the full path.
   */
  String normalizeClassPath(String classPath, boolean generateRelative)
  {
    char sep = CauchoSystem.getPathSeparatorChar();
    int head = 0;
    int tail = 0;

    CharBuffer cb = CharBuffer.allocate();

    while (head < classPath.length()) {
      tail = classPath.indexOf(sep, head);
      if (tail < 0)
        tail = classPath.length();

      if (tail > head) {
        String segment = classPath.substring(head, tail);

        if (cb.length() != 0)
          cb.append(sep);
      
        cb.append(normalizePath(segment, generateRelative));
      }

      head = tail + 1;
    }

    return cb.close();
  }
  /**
   * Normalizes a path.
   */
  String normalizePath(String segment, boolean generateRelative)
  {
    if (_userPrefix == null) {
      Path userPath = Vfs.lookup(CauchoSystem.getUserDir());
      char sep = CauchoSystem.getFileSeparatorChar();
      _userPrefix = userPath.getNativePath();
      
      if (_userPrefix.length() == 0 ||
          _userPrefix.charAt(_userPrefix.length() - 1) != sep) {
        _userPrefix = _userPrefix + sep;
      }
    }
    
    Path path = Vfs.lookup(segment);
    String nativePath = path.getNativePath();

    if (! generateRelative)
      return nativePath;

    if (nativePath.startsWith(_userPrefix))
      nativePath = nativePath.substring(_userPrefix.length());

    if (nativePath.equals(""))
      return ".";
    else
      return nativePath;
  }
}
