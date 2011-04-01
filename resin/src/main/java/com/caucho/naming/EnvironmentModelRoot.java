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

package com.caucho.naming;

import javax.naming.NamingException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.caucho.loader.*;

/**
 * Environment based model for JNDI.
 */
public class EnvironmentModelRoot
{
  private static final EnvironmentLocal<EnvironmentModelRoot> _local
    = new EnvironmentLocal<EnvironmentModelRoot>();

  private final ClassLoader _loader;
  
  private ConcurrentHashMap<String,EnvironmentModel> _map
    = new ConcurrentHashMap<String,EnvironmentModel>();

  /**
   * Creates a new instance of the memory model.
   */
  private EnvironmentModelRoot(ClassLoader loader)
  {
    for (;
               loader != null && ! (loader instanceof EnvironmentClassLoader);
               loader = loader.getParent()) {
    }
    
    _loader = loader;

    _map.put("", new EnvironmentModel(this, ""));
  }

  /**
   * Return the local model if it exists.
   */
  public static EnvironmentModelRoot getCurrent()
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
     
    return getCurrent(loader);
  }

  /**
   * Return the local model if it exists.
   */
  public static EnvironmentModelRoot getCurrent(ClassLoader loader)
  {
    return _local.get(loader);
  }

  /**
   * Create the local model if it exists.
   */
  public static EnvironmentModelRoot create()
  {
    return create(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Create the local model if it exists.
   */
  public static EnvironmentModelRoot create(ClassLoader loader)
  {
    synchronized (_local) {
      EnvironmentModelRoot root = _local.getLevel(loader);

      if (root == null) {
        root = new EnvironmentModelRoot(loader);
        _local.set(root, loader);
      }

      return root;
    }
  }

  protected ClassLoader getClassLoader()
  {
    return _loader;
  }

  /**
   * Returns a specific node.
   */
  public EnvironmentModel get(String path)
  {
    return _map.get(path);
  }

  /**
   * Set a specific node.
   */
  public EnvironmentModel put(String path, EnvironmentModel value)
  {
    return _map.put(path, value);
  }

  /**
   * Set a specific node.
   */
  public EnvironmentModel remove(String path)
  {
    return _map.remove(path);
  }
}
