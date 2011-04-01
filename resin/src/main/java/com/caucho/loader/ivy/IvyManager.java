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

package com.caucho.loader.ivy;

import java.util.*;
import java.util.logging.*;

import com.caucho.config.*;
import com.caucho.vfs.*;

/**
 * Class loader which uses an ivy dependency
 */
public class IvyManager {
  private static final Logger log
    = Logger.getLogger(IvyManager.class.getName());
  
  private static final String SCHEMA = "com/caucho/loader/ivy/ivy.rnc";

  private ArrayList<IvyCache> _cacheList = new ArrayList<IvyCache>();
  
  private Path _ivyFile;

  private HashMap<IvyModuleKey,String[]> _versionMap
    = new HashMap<IvyModuleKey,String[]>();

  private ArrayList<IvyModule> _moduleList = new ArrayList<IvyModule>();
  
  private ArrayList<IvyDependency> _dependencyList
    = new ArrayList<IvyDependency>();

  private boolean _isModified;
  
  private ArrayList<Path> _jarList = new ArrayList<Path>();

  public IvyManager()
  {
  }

  public IvyCache createCache()
  {
    IvyCache cache = new IvyCache(this);
    
    _cacheList.add(cache);

    return cache;
  }

  void init()
  {
    if (_cacheList.size() == 0) {
      IvyCache cache = new IvyCache(this);
      cache.init();
      
      _cacheList.add(cache);
    }
  }

  public void setIvyFile(Path ivyFile)
  {
    _ivyFile = ivyFile;
  }

  public IvyModule configureIvyFile(Path ivyFile)
  {
    IvyModule module = findIvyModule(ivyFile);

    if (module != null)
      return module;

    module = new IvyModule(this);
    module.setIvyPath(ivyFile);

    Config config = new Config();
    config.configure(module, ivyFile, SCHEMA);

    _moduleList.add(module);
    _isModified = true;

    return module;
  }

  public IvyModule findIvyModule(Path ivyFile)
  {
    for (IvyModule module : _moduleList) {
      if (ivyFile.equals(module.getIvyPath()))
        return module;
    }

    return null;
  }

  public ArrayList<Path> resolve()
  {
    while (_isModified) {
      _isModified = false;
      
      ArrayList<IvyModule> list = new ArrayList<IvyModule>(_moduleList);
    
      for (IvyModule module : list) {
        for (IvyDependency depend : module.getDependencyList()) {
          resolve(depend);
        }
      }

      ArrayList<IvyDependency> depList
        = new ArrayList<IvyDependency>(_dependencyList);
      for (IvyDependency dependency : depList) {
        for (IvyCache cache : _cacheList) {
          Path path = dependency.resolve(cache);

          if (path != null && path.canRead()) {
            addJar(path);
            break;
          }
        }
      }
    }

    return _jarList;
  }

  void resolve(IvyDependency dependency)
  {
    dependency = mergeDependency(dependency);
    
    Path path = null;

    for (IvyCache cache : _cacheList) {
      cache.resolveIvy(dependency);
    }
  }

  private IvyDependency mergeDependency(IvyDependency dependency)
  {
    for (int i = 0; i < _dependencyList.size(); i++) {
      IvyDependency dep = _dependencyList.get(i);
      IvyDependency merge = dep.merge(dependency);

      if (merge != null) {
        _dependencyList.set(i, merge);
        return merge;
      }
    }

    IvyModuleKey key = new IvyModuleKey(dependency.getOrg(),
                                        dependency.getName());

    String []versions = getVersions(dependency);

    dependency.setVersions(versions);
    
    _isModified = true;
    _dependencyList.add(dependency);

    return dependency;
  }

  private String []getVersions(IvyDependency dependency)
  {
    ArrayList<String> versions = new ArrayList<String>();

    for (IvyCache cache : _cacheList) {
      cache.resolveVersions(versions, dependency);
    }

    String []versionArray = new String[versions.size()];

    versions.toArray(versionArray);
    
    return versionArray;
  }

  void addJar(Path path)
  {
    if (! _jarList.contains(path)) {
      _jarList.add(path);
    }
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  static class IvyModuleKey {
    private String _org;
    private String _name;

    IvyModuleKey(String org, String name)
    {
      _org = org;
      _name = name;
    }

    public int hashCode()
    {
      return _org.hashCode() * 6551 + _name.hashCode();
    }

    public boolean equals(Object o)
    {
      if (this == o)
        return true;
      else if (! (o instanceof IvyModuleKey))
        return false;

      IvyModuleKey key = (IvyModuleKey) o;

      return _org.equals(key._org) && _name.equals(key._name);
    }
  }
}
