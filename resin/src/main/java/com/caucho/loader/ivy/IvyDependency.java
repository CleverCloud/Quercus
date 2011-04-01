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

import java.util.logging.*;
import javax.annotation.*;

import com.caucho.config.program.ConfigProgram;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * IvyDependency configuration
 */
public class IvyDependency {
  private String _artifact;
  private String _org;
  private String _name;
  private String _rev = "latest";

  private String []_versions;

  /**
   * The artifact name
   */
  public void setArtifact(String artifact)
  {
    _artifact = artifact;
  }

  /**
   * The artifact name
   */
  public String getArtifact()
  {
    return _artifact;
  }

  /**
   * The owning organization
   */
  public void setOrg(String org)
  {
    _org = org;
  }

  /**
   * The owning organization
   */
  public String getOrg()
  {
    return _org;
  }

  /**
   * The name of the module
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * The name of the module
   */
  public String getName()
  {
    return _name;
  }

  /**
   * The version of the artifact
   */
  public void setRev(String rev)
  {
    _rev = rev;
  }

  /**
   * The version of the artifact
   */
  public String getRev()
  {
    return _rev;
  }

  /**
   * Sets the versions
   */
  public void setVersions(String []versions)
  {
    _versions = versions;
  }
  
  /**
   * Ignore unknown properties
   */
  public void addBuilderProgram(ConfigProgram program)
  {
  }

  public boolean isMatch(IvyDependency dep)
  {
    return (_org.equals(dep._org)
            && _name.equals(dep._name));
  }

  public Path resolve(IvyCache cache)
  {
    Path path = cache.resolve(this);

    if (path != null)
      return path;

    if (_versions == null)
      return null;

    Path bestPath = null;
    String bestVersion = null;
    
    for (String version : _versions) {
      path = cache.resolve(this, version);

      if (path == null)
        continue;
      else if (bestPath == null) {
        bestPath = path;
        bestVersion = version;
      }
      else if (new IvyRevision(bestVersion).compareTo(new IvyRevision(version)) < 0) {
        bestPath = path;
        bestVersion = version;
      }
      
    }

    return bestPath;
  }

  public IvyDependency merge(IvyDependency dep)
  {
    if (equals(dep))
      return this;
    else if (! isMatch(dep))
      return null;

    return new IvyMergeDependency(this, dep);
  }

  @Override
  public int hashCode()
  {
    int hash = 37;

    hash = hash * 65521 + _org.hashCode();
    hash = hash * 65521 + _name.hashCode();
    hash = hash * 65521 + _rev.hashCode();

    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof IvyDependency))
      return false;

    IvyDependency dep = (IvyDependency) o;

    return (_org.equals(dep._org)
            && _name.equals(dep._name)
            && _rev.equals(dep._rev));
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _org
            + "," + _name
            + "," + _rev + "]");
  }
}
