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

package com.caucho.loader.maven;

import com.caucho.config.ConfigException;
import com.caucho.config.program.ConfigProgram;
import com.caucho.loader.module.Artifact;
import com.caucho.loader.module.ArtifactDependency;
import com.caucho.loader.module.ArtifactVersion;
import com.caucho.loader.module.ArtifactVersionRange;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import java.util.ArrayList;
import javax.annotation.PostConstruct;

/**
 * Parsed maven project
 */
public class MavenProject
{
  private static final L10N L = new L10N(MavenProject.class);
  
  private String _groupId;
  private String _artifactId;
  private ArtifactVersion _version;

  private Parent _parent;

  private ArrayList<ArtifactDependency> _dependencyList
    = new ArrayList<ArtifactDependency>();

  /**
   * Sets the groupId for the project
   */
  public void setGroupId(String groupId)
  {
    _groupId = groupId;
  }

  /**
   * Sets the artifactId for the project
   */
  public void setArtifactId(String artifactId)
  {
    _artifactId = artifactId;
  }

  /**
   * Sets the version for the project
   */
  public void setVersion(String version)
  {
    _version = ArtifactVersion.create(version);
  }

  public void addParent(Parent parent)
  {
    _parent = parent;
  }

  /**
   * Creates the dependencies section.
   */
  public Dependencies createDependencies()
  {
    return new Dependencies();
  }
  
  /**
   * Add BuilderProgram ignores unknown tags
   */
  public void addBuilderProgram(ConfigProgram program)
  {
  }

  void addArtifactDependency(ArtifactDependency dependency)
  {
    _dependencyList.add(dependency);
  }

  @PostConstruct
  public void init()
  {
    if (_groupId == null && _parent != null) {
      _groupId = _parent.getGroupId();
    }

    if (_groupId == null)
      throw new ConfigException(L.l("<groupId> is a required attribute of Maven <project>"));

    if (_artifactId == null)
      throw new ConfigException(L.l("<artifactId> is a required attribute of Maven <project>"));

    if (_version == null && _parent != null) {
      _version = _parent.getVersion();
    }

    if (_version == null)
      throw new ConfigException(L.l("<version> is a required attribute of Maven <project>"));
  }

  /**
   * Returns the Artifact corresponding to the project.
   */
  public Artifact toArtifact(Path path)
  {
    ArtifactDependency parent = null;

    if (_parent != null) {
      ArtifactVersion parentVersion = _parent.getVersion();
      
      ArtifactVersionRange parentRange
        = new ArtifactVersionRange(parentVersion, true,
                                   parentVersion, true);

      parent = new ArtifactDependency(_parent.getGroupId(),
                                      null,
                                      _parent.getArtifactId(),
                                      parentRange);
    }
    
    return new Artifact(path, _groupId, null, _artifactId, _version,
                        parent, _dependencyList);
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[group=" + _groupId
            + ",artifact=" + _artifactId
            + ",version=" + _version
            + "]");
  }

  public static class Parent {
    private String _groupId;
    private String _artifactId;
    private ArtifactVersion _version;

    /**
     * Sets the groupId for the project
     */
    public void setGroupId(String groupId)
    {
      _groupId = groupId;
    }

    public String getGroupId()
    {
      return _groupId;
    }

    /**
     * Sets the artifactId for the project
     */
    public void setArtifactId(String artifactId)
    {
      _artifactId = artifactId;
    }

    public String getArtifactId()
    {
      return _artifactId;
    }

    /**
     * Sets the version for the project
     */
    public void setVersion(String version)
    {
      _version = ArtifactVersion.create(version);
    }

    public ArtifactVersion getVersion()
    {
      return _version;
    }
    
    public void addBuilderProgram(ConfigProgram program)
    {
    }

    @PostConstruct
    public void init()
    {
      if (_groupId == null)
        throw new ConfigException(L.l("<groupId> is a required attribute of <parent>"));
      
      if (_artifactId == null)
        throw new ConfigException(L.l("<artifactId> is a required attribute of <parent>"));
      
      if (_version == null)
        throw new ConfigException(L.l("<version> is a required attribute of <parent>"));
    }
  }

  public class Dependencies {
    public Dependency createDependency()
    {
      return new Dependency();
    }
    
    public void addBuilderProgram(ConfigProgram program)
    {
    }
  }

  public class Dependency {
    private String _groupId;
    private String _artifactId;
    private ArtifactVersionRange _version;
    
    public void setGroupId(String groupId)
    {
      _groupId = groupId;
    }
    
    public void setArtifactId(String artifactId)
    {
      _artifactId = artifactId;
    }
    
    public void setVersion(String version)
    {
      _version = ArtifactVersionRange.create(version);
    }
    
    public void addBuilderProgram(ConfigProgram program)
    {
    }
    
    @PostConstruct
    public void init()
    {
      if (_groupId == null)
        throw new ConfigException(L.l("<groupId> is required in a <dependency> in a Maven pom.xml file"));
      
      if (_artifactId == null)
        throw new ConfigException(L.l("<artifactId> is required in a <dependency> in a Maven pom.xml file"));

      ArtifactDependency dependency
        = new ArtifactDependency(_groupId, null, _artifactId, _version);

      addArtifactDependency(dependency);
    }
  }
}
