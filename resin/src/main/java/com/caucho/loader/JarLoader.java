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

package com.caucho.loader;

import com.caucho.config.ConfigException;
import com.caucho.make.DependencyContainer;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Dependency;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 */
public class JarLoader extends JarListLoader implements Dependency {
  private static final Logger log
    = Logger.getLogger(JarLoader.class.getName());

  /**
   * Creates a new directory loader.
   */
  public JarLoader()
  {
  }

  /**
   * Creates a new directory loader.
   */
  public JarLoader(ClassLoader loader)
  {
    super(loader);
  }

  /**
   * Sets the owning class loader.
   */
  @Override
  public void setLoader(DynamicClassLoader loader)
  {
    super.setLoader(loader);
  }

  @Override
  public void init()
  {
    super.init();

    for (int i = 0; i < _jarList.size(); i++) {
      getClassLoader().addURL(_jarList.get(i).getJarPath());
    }
  }

  public Path getCodePath()
  {
    return null;
  }
}
