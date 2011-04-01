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
import javax.annotation.*;

import com.caucho.config.*;
import com.caucho.vfs.*;

/**
 * IvyCache configuration
 */
public class IvyCache {
  private static final Logger log
    = Logger.getLogger(IvyCache.class.getName());

  private static final String IVY_PATTERN
    = "[organisation]/[module]/ivy-[revision].xml";

  private static final String ARTIFACT_PATTERN
    = "[organisation]/[module]/[type]s/[artifact]-[revision].[ext]";

  private final IvyManager _manager;
  
  private Path _defaultCacheDir;
  private Path _repositoryCacheDir;

  private IvyPattern _artifactPattern;
  private IvyPattern _ivyPattern;

  IvyCache(IvyManager manager)
  {
    _manager = manager;
  }

  /**
   * Sets the main ivy cache directory
   */
  public void setDefaultCacheDir(Path dir)
  {
    _defaultCacheDir = dir;
  }

  /**
   * Finds a dependency in the cache
   */
  public Path resolve(IvyDependency dependency)
  {
    return resolve(dependency, dependency.getRev());
  }
    
  /**
   * Finds a dependency in the cache
   */
  public Path resolve(IvyDependency dependency, String rev)
  {
    String org = dependency.getOrg();
    String name = dependency.getName();
    String artifact = dependency.getArtifact();

    if (artifact == null)
      artifact = name;

    HashMap<String,String> props = new HashMap<String,String>();
    props.put("organisation", org);
    props.put("module", name);
    props.put("artifact", artifact);
    props.put("revision", rev);
    props.put("type", "jar");
    props.put("ext", "jar");

    String pathName = _artifactPattern.resolve(props);

    Path path = _repositoryCacheDir.lookup(pathName);

    if (path.canRead())
      return path;
    else
      return null;
  }

  /**
   * Finds a dependency in the cache
   */
  public IvyModule resolveIvy(IvyDependency dependency)
  {
    String org = dependency.getOrg();
    String name = dependency.getName();
    String artifact = dependency.getArtifact();
    String rev = dependency.getRev();

    if (artifact == null)
      artifact = name;

    HashMap<String,String> props = new HashMap<String,String>();
    props.put("organisation", org);
    props.put("module", name);
    props.put("artifact", artifact);

    if (rev != null)
      props.put("revision", rev);
    
    props.put("type", "ivy");
    props.put("ext", "xml");

    String pathName = _ivyPattern.resolve(props);

    Path path = _repositoryCacheDir.lookup(pathName);

    if (path.canRead())
      return _manager.configureIvyFile(path);
    else
      return null;
  }

  /**
   * Finds a dependency in the cache
   */
  public void resolveVersions(ArrayList<String> versions,
                               IvyDependency dependency)
  {
    String org = dependency.getOrg();
    String name = dependency.getName();
    String artifact = dependency.getArtifact();
    String rev = dependency.getRev();

    if (artifact == null)
      artifact = name;

    HashMap<String,String> props = new HashMap<String,String>();
    props.put("organisation", org);
    props.put("module", name);
    props.put("artifact", artifact);

    if (rev != null)
      props.put("revision", rev);
    
    props.put("type", "ivy");
    props.put("ext", "xml");

    String pathName = _ivyPattern.resolveRevisionPath(props);

    int revIndex = pathName.indexOf("[revision]");
    if (revIndex < 0)
      return;
    
    int tail = pathName.indexOf('/', revIndex);

    if (tail > 0)
      pathName = pathName.substring(0, tail);

    int head = pathName.lastIndexOf('/');
    String segment;
    
    if (head > 0) {
      segment = pathName.substring(head + 1);
      pathName = pathName.substring(0, head);
    }
    else {
      pathName = ".";
      segment = pathName;
    }

    revIndex = segment.indexOf("[revision]");
    String prefix = segment.substring(0, revIndex);
    String suffix = segment.substring(revIndex + "[revision]".length());

    Path path = _repositoryCacheDir.lookup(pathName);
    
    try {
      for (String item : path.list()) {
        if (item.startsWith(prefix) && item.endsWith(suffix)) {
          int len = item.length() - suffix.length();

          String revName = item.substring(prefix.length(), len);

          if (! versions.contains(revName))
            versions.add(revName);
        }
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  @PostConstruct
  public void init()
  {
    if (_defaultCacheDir == null) {
      Path userDir = Vfs.lookup(System.getProperty("user.home"));
      _defaultCacheDir = userDir.lookup(".ivy2/cache");
      
      log.fine("ivy-loader using " + _defaultCacheDir);
    }

    if (_repositoryCacheDir == null)
      _repositoryCacheDir = _defaultCacheDir;

    if (_artifactPattern == null)
      _artifactPattern = new IvyPattern(ARTIFACT_PATTERN);

    if (_ivyPattern == null)
      _ivyPattern = new IvyPattern(IVY_PATTERN);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _defaultCacheDir + "]";
  }
}
