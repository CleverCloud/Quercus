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

import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.Encoding;
import com.caucho.vfs.IOExceptionWrapper;
import com.caucho.vfs.MemoryStream;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * Compiles Java source, returning the loaded class.
 */
public class EclipseCompiler extends AbstractJavaCompiler {
  private static boolean _hasCompiler; // already tested for compiler

  private static final String COMPILER
    = "org.eclipse.jdt.internal.compiler.batch.Main";
  
  Process _process;
  String _userPrefix;
  
  boolean _isDead;
  
  public EclipseCompiler(JavaCompiler compiler)
  {
    super(compiler);
  }

  protected void compileInt(String []path, LineMap lineMap)
    throws IOException, JavaCompileException
  {
    if (! _hasCompiler) {
      try {
        Class.forName(COMPILER, false,
                      Thread.currentThread().getContextClassLoader());

        _hasCompiler = true;
      } catch (Exception e) {
        e.printStackTrace();
        throw new JavaCompileException(L.l("Resin can't load org.eclipse.jdt.core.JDTCompilerAdapter.  Usually this means that eclipse-compiler.jar is missing from the classpath.  You can either add eclipse-compiler.jar to the classpath or change the compiler with <java compiler='javac'/>.\n\n{0}", String.valueOf(e)));
      }
    }

    executeInt(path, lineMap);
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

      ArrayList<String> argList = new ArrayList<String>();
      /* This isn't needed since srcDirName is in the classpath
      if ("1.2".compareTo(System.getProperty("java.version")) <= 0) {
        argList.add("-sourcepath");
        argList.add(srcDirName);
      }
      */
      argList.add("-d");
      argList.add(_compiler.getClassDirName());
      if (_compiler.getEncoding() != null) {
        String encoding = Encoding.getJavaName(_compiler.getEncoding());
        if (encoding != null && ! encoding.equals("ISO8859_1")) {
          argList.add("-encoding");
          argList.add(_compiler.getEncoding());
        }
      }
      argList.add("-classpath");
      argList.add(_compiler.getClassPath());
      ArrayList<String> args = _compiler.getArgs();
      if (args != null)
        argList.addAll(args);

      for (int i = 0; i < path.length; i++) {
        Path javaPath = _compiler.getSourceDir().lookup(path[i]);
        argList.add(javaPath.getNativePath());
      }

      if (log.isLoggable(Level.FINE)) {
        CharBuffer msg = CharBuffer.allocate();
        msg.append("javac(int)");
        for (int i = 0; i < argList.size(); i++) {
          msg.append(" ");
          msg.append(argList.get(i));
        }
        log.fine(msg.close());
      }

      String []argArray = argList.toArray(new String[argList.size()]);

      int status = -1;
      
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();
      try {
        EnvironmentClassLoader env;
        env = EnvironmentClassLoader.create(ClassLoader.getSystemClassLoader());
        thread.setContextClassLoader(env);

        try {
          Class cl = Class.forName(COMPILER, false, env);
          Constructor xtor = cl.getConstructor(new Class[] { PrintWriter.class, PrintWriter.class, boolean.class });

          Object value = xtor.newInstance(error.getPrintWriter(), error.getPrintWriter(), Boolean.FALSE);

          Method compile = cl.getMethod("compile", new Class[] { String[].class });

          Object result = compile.invoke(value, new Object[] { argArray });

          status = Boolean.TRUE.equals(result) ? 0 : -1;
        } catch (ClassNotFoundException e) {
          throw new JavaCompileException(L.l("Can't find internal Java compiler.  Either configure an external compiler with <javac> or use a JDK which contains a Java compiler."));
        } catch (NoSuchMethodException e) {
          throw new JavaCompileException(e);
        } catch (InstantiationException e) {
          throw new JavaCompileException(e);
        } catch (IllegalAccessException e) {
          throw new JavaCompileException(e);
        } catch (InvocationTargetException e) {
          throw new IOExceptionWrapper(e);
        }
      
        error.close();
        tempStream.close();
      } finally {
        thread.setContextClassLoader(oldLoader);
      }

      ReadStream read = tempStream.openReadAndSaveBuffer();
      JavacErrorParser parser = new JavacErrorParser();

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
      /* XXX: why should warnings be sent as warning?
      else if (status == 0 && errors != null && ! errors.equals("")) {
        final String msg = errors;

        new com.caucho.loader.ClassLoaderContext(_compiler.getClassLoader()) {
          public void run()
          {
            log.warning(msg);
          }
        };
      }
      */

      if (status != 0)
        throw new JavaCompileException(errors);
    } finally {
      tempStream.destroy();
    }
  }
}
