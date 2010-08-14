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

package com.caucho.loader.module;

import com.caucho.util.L10N;

/**
 * A jar artifact in the repository
 */
public class ArtifactDependency
{
  private static final L10N L = new L10N(ArtifactDependency.class);

  private final String _org;
  private final String _module;
  private final String _name;
  private final ArtifactVersionRange _version;

  public ArtifactDependency(String org,
                            String module,
                            String name,
                            ArtifactVersionRange version)
  {
    _org = org;
    if (org == null)
      throw new NullPointerException(L.l("artifact org cannot be null"));
    
    _module = module;
    
    _name = name;
    if (name == null)
      throw new NullPointerException(L.l("artifact name cannot be null"));
    
    _version = version;
  }

  /**
   * Returns the artifact's owning organization (groupId)
   */
  public String getOrg()
  {
    return _org;
  }

  /**
   * Returns the artifact's name
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Returns the artifact's version
   */
  public ArtifactVersionRange getVersion()
  {
    return _version;
  }

  public boolean isSameArtifact(ArtifactDependency dependency)
  {
    if (! getOrg().equals(dependency.getOrg()))
      return false;
    
    if (! getName().equals(dependency.getName()))
      return false;

    return true;
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[org=" + _org
            + ",name=" + _name
            + ",version=" + _version
            + "]");
  }
}
