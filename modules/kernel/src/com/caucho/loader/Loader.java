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

import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.config.ConfigException;
import com.caucho.vfs.Path;


/**
 * Loads resources.
 */
abstract public class Loader {
  protected static final Logger log
    = Logger.getLogger(Loader.class.getName());
  
  private DynamicClassLoader _loader;

  protected Loader()
  {
    this(Thread.currentThread().getContextClassLoader());
  }
  
  protected Loader(ClassLoader loader)
  {
    if (! (loader instanceof DynamicClassLoader)) {
      // XXX: no L10N for initialization reasons
      
      throw new IllegalStateException("'" + loader + "' must be created in a DynamicClassLoader context");
    }
    
    _loader = (DynamicClassLoader) loader;
  }
  /**
   * Sets the owning class loader.
   */
  public void setLoader(DynamicClassLoader loader)
  {
    _loader = loader;
  }

  /**
   * Gets the owning class loader.
   */
  public DynamicClassLoader getClassLoader()
  {
    return _loader;
  }

  /**
   * Validates the loader.
   */
  public void validate()
    throws ConfigException
  {
  }
  
  /**
   * Initialize the loader
   */
  @PostConstruct
  public void init()
  {
    if (_loader != null)
      _loader.addLoader(this);
  }

  /**
   * Loads the class directly, e.g. from OSGi
   */
  protected Class<?> loadClass(String name)
  {
    return null;
  }
  
  /**
   * Returns the class entry.
   *
   * @param name name of the class
   */
  protected ClassEntry getClassEntry(String name, String pathName)
    throws ClassNotFoundException
  {
    // Find the path corresponding to the class
    Path path = getPath(pathName);

    if (path != null && path.getLength() > 0) {
      return new ClassEntry(_loader, name, path, path,
                            getCodeSource(path));
    }
    else
      return null;
  }
  
  /**
   * Returns the resource
   *
   * @param name name of the resource
   */
  public URL getResource(String name)
  {
    Path path;

    path = getPath(name);

    if (path != null && path.exists()) {
      try {
        return new URL(path.getURL());
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }

    return null;
  }
  
  /**
   * Returns the resource
   *
   * @param name name of the resource
   */
  public void getResources(Vector<URL> resources, String name)
  {
    Path path;

    path = getPath(name);

    if (path != null && path.canRead()) {
      try {
        resources.add(new URL(path.getURL()));
      } catch (Exception e) {
      }
    }
  }
  
  /**
   * Opens the stream to the resource.
   *
   * @param name name of the resource
   */
  public InputStream getResourceAsStream(String name)
  {
    Path path;

    path = getPath(name);

    if (path != null && path.canRead()) {
      try {
        return path.openRead();
      } catch (Exception e) {
      }
    }

    return null;
  }
  
  /**
   * Returns a path for the given name.
   */
  public Path getPath(String name)
  {
    return null;
  }
  
  /**
   * Returns the code source for the path.
   */
  protected CodeSource getCodeSource(Path path)
  {
    try {
      return new CodeSource(new URL(path.getURL()),
                            (Certificate []) path.getCertificates());
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }
  
  /**
   * Adds the sourcepath of this loader.
   */
  protected void buildClassPath(ArrayList<String> pathList)
  {
  }
  
  /**
   * Adds the sourcepath of this loader.
   */
  protected void buildSourcePath(ArrayList<String> pathList)
  {
    buildClassPath(pathList);
  }

  /**
   * Destroys the loader.
   */
  protected void destroy()
  {
  }
}
