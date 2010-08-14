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

package com.caucho.ant;

import java.io.*;
import java.util.*;

import com.caucho.java.*;
import com.caucho.jsp.*;
import com.caucho.loader.*;
import com.caucho.xml.*;
import com.caucho.vfs.Vfs;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;

/**
 * Command-line tool and ant task to compile jsp files
 */
public class Jspc {
  private File _rootDirectory;
  private Vector _classpath = new Vector();

  /**
   * For ant.
   **/
  public Jspc()
  {
  }

  public void setRootDirectory(File root)
  {
    _rootDirectory = root;
  }

  public void addClasspath(Path path)
  {
    _classpath.add(path);
  }

  /**
   * Executes the ant task.
   **/
  public void execute()
    throws BuildException
  {
    if (_rootDirectory == null)
      throw new BuildException("root-directory is required by jspc");

    /*
    if (_classpath.size() == 0)
      throw new BuildException("classpath is required by jspc");
    */

    ClassLoader loader = JspCompiler.class.getClassLoader();

    String classPath = System.getProperty("java.class.path");
    
    if (loader instanceof AntClassLoader)
      classPath = ((AntClassLoader) loader).getClasspath();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      EnvironmentClassLoader env = EnvironmentClassLoader.create();

      for (String cp : classPath.split("[" + File.pathSeparatorChar + "]")) {
	com.caucho.vfs.Path path = Vfs.lookup(cp);

	env.addRoot(path);
      }

      thread.setContextClassLoader(env);
      
      JspCompiler.main(new String[] {
	"-app-dir", _rootDirectory.getAbsolutePath(),
      });
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new BuildException(e);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
}
