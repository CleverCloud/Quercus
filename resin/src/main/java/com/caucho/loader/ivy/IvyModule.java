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

import com.caucho.config.program.ConfigProgram;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * IvyModule configuration
 */
public class IvyModule {
  private IvyManager _manager;
  
  private String _version;

  private IvyInfo _info;

  private ArrayList<IvyDependency> _dependencies
    = new ArrayList<IvyDependency>();

  private Path _ivyPath;
  private Path _artifactPath;

  IvyModule(IvyManager manager)
  {
    _manager = manager;
  }

  /**
   * Resolves the module
   */
  public void resolve()
  {
    for (IvyDependency dependency : _dependencies) {
      _manager.resolve(dependency);
    }
  }

  /**
   * The path to the ivy configuration file.
   */
  public Path getIvyPath()
  {
    return _ivyPath;
  }

  /**
   * The path to the ivy configuration file.
   */
  public void setIvyPath(Path ivyPath)
  {
    _ivyPath = ivyPath;
  }

  /**
   * The path to the artifact
   */
  public Path getArtifactPath()
  {
    return _artifactPath;
  }

  /**
   * The path to the artifact
   */
  public void setArtifactPath(Path artifactPath)
  {
    _artifactPath = artifactPath;
  }

  //
  // config stuff
  //
  
  /**
   * Adds dependencies
   */
  public Dependencies createDependencies()
  {
    return new Dependencies();
  }

  public ArrayList<IvyDependency> getDependencyList()
  {
    return _dependencies;
  }
  
  public void setVersion(String version)
  {
    _version = version;
  }

  public IvyInfo createInfo()
  {
    if (_info == null)
      _info = new IvyInfo(_manager);

    return _info;
  }

  public boolean isMatch(String org, String module)
  {
    return _info.isMatch(org, module);
  }

  public IvyInfo getInfo()
  {
    return _info;
  }

  /**
   * Ignore unknown properties
   */
  public void addBuilderProgram(ConfigProgram program)
  {
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _info + "]";
  }
  
  class Dependencies {
    public void addDependency(IvyDependency dependency)
    {
      _dependencies.add(dependency);
    }
    
    public void addBuilderProgram(ConfigProgram program)
    {
    }
  }

}
