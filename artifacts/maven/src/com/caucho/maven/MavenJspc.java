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

package com.caucho.maven;

import com.caucho.jsp.*;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

import org.apache.maven.plugin.*;

/**
 * The MavenJspc to precompile .jsp files
 *
 * @goal jspc
 */
public class MavenJspc extends AbstractMojo
{
  private File _rootDirectory;
  private File _config;
  private String _compiler = "javac";

  /**
   * Sets the web-app's root directory
   */
  public void setRootDirectory(File rootDirectory)
  {
    _rootDirectory = rootDirectory;
  }

  public void setConfig(File file) {
    _config = file;
  }

  public void setCompiler(String compiler) {
    _compiler = compiler;
  }

  /**
   * Executes the maven resin:jspc task
   */
  public void execute() throws MojoExecutionException
  {

    List<String> args = new ArrayList<String>();

    args.add("-app-dir");
    args.add(_rootDirectory.getAbsolutePath());

    if (_config != null) {
      args.add("-conf");
      args.add(_config.getAbsolutePath());
    }

    if (_compiler != null) {
      args.add("-compiler");
      args.add(_compiler);
    }

    try {
      JspCompiler.main(args.toArray(new String[args.size()]));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new MojoExecutionException(e.toString(), e);
    }
  }
}
