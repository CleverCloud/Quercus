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

package com.caucho.jca.ra;

import com.caucho.jca.cfg.ResourceAdapterConfig;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.L10N;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Manages the resource archives.
 */
public class ResourceArchiveManager {
  static final L10N L = new L10N(ResourceArchiveManager.class);
  static final Logger log
    = Logger.getLogger(ResourceArchiveManager.class.getName());

  private static final EnvironmentLocal<ResourceArchiveManager> _localManager
    = new EnvironmentLocal<ResourceArchiveManager>();

  private ArrayList<ResourceArchive> _resources
    = new ArrayList<ResourceArchive>();

  /**
   * Creates the application.
   */
  private ResourceArchiveManager()
  {
  }

  /**
   * Adds a new resource.
   */
  static void addResourceArchive(ResourceArchive rar)
  {
    ResourceArchiveManager raManager = _localManager.getLevel();

    if (raManager == null) {
      raManager = new ResourceArchiveManager();
      _localManager.set(raManager);
    }

    raManager._resources.add(rar);
  }

  /**
   * Adds a new resource.
   */
  static void removeResourceArchive(String name)
  {
    ResourceArchiveManager raManager = _localManager.getLevel();

    if (raManager == null)
      return;

    ResourceArchive rar = raManager.getResourceArchive(name);

    if (rar != null) {
      raManager._resources.remove(rar);

      rar.destroy();
    }
  }

  /**
   * Finds a resource.
   */
  public static ResourceArchive findResourceArchive(String name)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        EnvironmentClassLoader envLoader = (EnvironmentClassLoader) loader;

        ResourceArchiveManager manager = _localManager.getLevel(envLoader);

        if (manager != null) {
          ResourceArchive ra = manager.getResourceArchive(name);

          if (ra != null)
            return ra;
        }
      }
    }

    return null;
  }

  /**
   * Returns the resource archive in the manager.
   */
  private ResourceArchive getResourceArchive(String type)
  {
    for (int i = 0; i < _resources.size(); i++) {
      ResourceArchive ra = _resources.get(i);

      if (type.equals(ra.getDisplayName()))
        return ra;
    }
    
    for (int i = 0; i < _resources.size(); i++) {
      ResourceArchive ra = _resources.get(i);

      ResourceAdapterConfig raConfig =  ra.getResourceAdapter();
      if (raConfig == null)
        continue;

      Class resourceAdapterClass = raConfig.getResourceadapterClass();
      if (resourceAdapterClass != null
          && type.equals(resourceAdapterClass.getName()))
        return ra;
    }
    
    for (int i = 0; i < _resources.size(); i++) {
      ResourceArchive ra = _resources.get(i);
      if (ra.getConnectionDefinition(type) != null)
        return ra;
    }
    
    for (int i = 0; i < _resources.size(); i++) {
      ResourceArchive ra = _resources.get(i);
      
      if (ra.getMessageListener(type) != null)
        return ra;
    }

    return null;
  }

  /**
   * Returns the resource archive in the manager.
   */
  private ResourceArchive getResourceArchiveByType(Class type)
  {
    for (int i = 0; i < _resources.size(); i++) {
      ResourceArchive ra = _resources.get(i);

      ResourceAdapterConfig raConfig =  ra.getResourceAdapter();
      if (raConfig == null)
        continue;

      Class resourceAdapterClass = raConfig.getResourceadapterClass();

      if (type.equals(resourceAdapterClass))
        return ra;
    }
    
    for (int i = 0; i < _resources.size(); i++) {
      ResourceArchive ra = _resources.get(i);
      if (ra.getConnectionDefinition(type.getName()) != null)
        return ra;
    }
    
    for (int i = 0; i < _resources.size(); i++) {
      ResourceArchive ra = _resources.get(i);
      
      if (ra.getMessageListener(type.getName()) != null)
        return ra;
    }

    return null;
  }
}
