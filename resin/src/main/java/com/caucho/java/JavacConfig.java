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

import com.caucho.loader.EnvironmentLocal;

import javax.annotation.PostConstruct;

public class JavacConfig {
  private static final EnvironmentLocal<JavacConfig> _localJavac =
  new EnvironmentLocal<JavacConfig>();

  private String _compiler = "internal";
  private String _args;
  private String _encoding;
  private int _maxBatch = 64;

  /**
   * Returns the environment configuration.
   */
  public static JavacConfig getLocalConfig()
  {
    JavacConfig config;

    config = _localJavac.get();

    if (config != null)
      return config;
    else
      return new JavacConfig();
  }

  /**
   * Sets the compiler.
   */
  public void setCompiler(String compiler)
  {
    _compiler = compiler;
  }

  /**
   * Gets the compiler.
   */
  public String getCompiler()
  {
    return _compiler;
  }

  /**
   * Sets the args.
   */
  public void setArgs(String args)
  {
    _args = args;
  }

  /**
   * Gets the args.
   */
  public String getArgs()
  {
    return _args;
  }

  /**
   * Sets the encoding.
   */
  public void setEncoding(String encoding)
  {
    _encoding = encoding;
  }

  /**
   * Gets the encoding.
   */
  public String getEncoding()
  {
    return _encoding;
  }

  /**
   * Sets the number of files to batch.
   */
  public void setMaxBatch(int max)
  {
    _maxBatch = max;
  }

  /**
   * Sets the number of files to batch.
   */
  public int getMaxBatch()
  {
    return _maxBatch;
  }

  /**
   * Sets the compiler args (backwards compat)
   */
  public void setCompilerArgs(String args)
  {
    setArgs(args);
  }

  /**
   * Stores self.
   */
  @PostConstruct
  public void init()
  {
    _localJavac.set(this);
  }
}

