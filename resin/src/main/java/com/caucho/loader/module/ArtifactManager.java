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

import java.net.URL;
import java.util.ArrayList;

import com.caucho.config.ConfigException;
import com.caucho.inject.Module;
import com.caucho.loader.EnvironmentApply;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.util.L10N;

/**
 * A jar artifact in the repository
 */
@Module
public class ArtifactManager
{
  private static final L10N L = new L10N(ArtifactManager.class);

  private EnvironmentClassLoader _loader;

  private ArrayList<ArtifactDependency> _dependencyList
    = new ArrayList<ArtifactDependency>();

  private ArrayList<ArtifactDependency> _pendingList
    = new ArrayList<ArtifactDependency>();

  private ArrayList<Artifact> _artifactList
    = new ArrayList<Artifact>();

  private ArrayList<Entry> _entryList
    = new ArrayList<Entry>();

  private ArrayList<ArtifactClassLoader> _loaderList
    = new ArrayList<ArtifactClassLoader>();

  public ArtifactManager(EnvironmentClassLoader loader)
  {
    _loader = loader;
  }

  public void addDependency(ArtifactDependency dependency)
  {
    ArtifactRepository repository = ArtifactRepository.getCurrent();

    if (repository == null) {
      throw new ConfigException(L.l("Artifact dependency org='{0}', name='{1}' is not valid because no artifact repositories have been defined",
                                    dependency.getOrg(),
                                    dependency.getName()));
    }

    ArrayList<Artifact> artifactList = repository.resolve(dependency);

    if (artifactList == null || artifactList.size() == 0) {
      ArtifactDependency plainDependency
        = new ArtifactDependency(dependency.getOrg(),
                                 null,
                                 dependency.getName(),
                                 null);

      artifactList = repository.resolve(plainDependency);
      
      throw new ConfigException(L.l("Artifact dependency '{0}', org='{1}', version={2} does not match any jars in the repository.  Available artifacts:{3}",
                                    dependency.getName(),
                                    dependency.getOrg(),
                                    dependency.getVersion().toDebugString(),
                                    toArtifactList(artifactList)));
    }

    _dependencyList.add(dependency);
    
    _pendingList.add(dependency);
  }

  private String toArtifactList(ArrayList<Artifact> artifactList)
  {
    StringBuilder sb = new StringBuilder();

    for (Artifact artifact : artifactList) {
      sb.append("\n  ").append(artifact.getName());
      sb.append(", org=").append(artifact.getOrg());

      if (artifact.getVersion() != null)
        sb.append(", version=").append(artifact.getVersion().toDebugString());
    }

    return sb.toString();
  }

  public void start()
  {
    resolve();
  }

  public Class findImportClass(String name)
  {
    resolve();

    for (int i = 0; i < _entryList.size(); i++) {
      Entry entry = _entryList.get(i);

      try {
        Class cl = entry.getLoader().findClassImpl(name);

        if (cl != null)
          return cl;
      } catch (ClassNotFoundException e) {
      }
    }
    
    return null;
  }

  public URL getImportResource(String name)
  {
    resolve();
    
    return null;
  }

  public void buildImportClassPath(ArrayList<String> cp)
  {
    resolve();

    for (Entry entry : _entryList) {
      entry.getLoader().buildClassPathImpl(cp);
    }
  }

  public void applyVisibleModules(EnvironmentApply apply)
  {
    resolve();
    
    for (Entry entry : _entryList) {
      apply.apply(entry.getLoader());
    }
  }

  private void resolve()
  {
    synchronized (this) {
      if (_pendingList.size() == 0)
        return;
      
      ArrayList<ArtifactDependency> pendingList
        = new ArrayList<ArtifactDependency>(_pendingList);

      ArtifactRepository repository = ArtifactRepository.getCurrent();

      ArrayList<Artifact> newArtifactList = new ArrayList<Artifact>();
      
      for (ArtifactDependency depend : pendingList) {
        ArrayList<Artifact> artifacts
          = repository.resolve(depend, _dependencyList);

        Artifact artifact = artifacts.get(0);

        _artifactList.add(artifact);
        newArtifactList.add(artifact);
      }

      for (Artifact artifact : newArtifactList) {
        ArrayList<Artifact> createList = new ArrayList<Artifact>();

        ArtifactClassLoader loader
          = createLoader(_loader.getParent(), artifact,
                         _artifactList, createList);

        _entryList.add(new Entry(artifact, loader));
      }
    }

    // XXX: timing
    for (Entry entry : _entryList) {
      entry.getLoader().start();
    }
  }

  private ArtifactClassLoader createLoader(ClassLoader parent,
                                           Artifact artifact,
                                           ArrayList<Artifact> artifactList,
                                           ArrayList<Artifact> createList)
  {
    if (createList.contains(artifact)) {
      throw new ConfigException(L.l("Dependency loop detected at {0} while creating artifact {1}",
                                    artifact,
                                    createList.get(0)));
    }

    createList.add(artifact);
    
    ArtifactRepository repository = ArtifactRepository.getCurrent();
    
    ArrayList<ArtifactClassLoader> importList
      = new ArrayList<ArtifactClassLoader>();

    ArrayList<Artifact> subArtifactList
      = new ArrayList<Artifact>();

    ArrayList<ArtifactDependency> dependencies
      = getArtifactDependencies(artifact);
      
    for (ArtifactDependency depend : dependencies) {
      ArrayList<Artifact> artifacts = repository.resolve(depend, dependencies);

      if (artifacts.size() == 0) {
        throw new ConfigException(L.l("Dependency org={0}, name={1} does not have any matching artifacts",
                                      depend.getOrg(),
                                      depend.getName()));
      }
      
      Artifact subArtifact = findArtifact(artifacts.get(0),
                                          artifactList);

      subArtifactList.add(subArtifact);
    }

    for (Artifact subArtifact : subArtifactList) {
      importList.add(createLoader(parent, subArtifact,
                                  subArtifactList, createList));
    }
    
    createList.remove(artifact);

    
    ArtifactClassLoader loader = findLoader(artifact);

    if (loader == null) {
      loader = new ArtifactClassLoader(_loader.getParent(),
                                       artifact,
                                       importList);

      _loaderList.add(loader);
    }

    return loader;
  }

  private ArrayList<ArtifactDependency>
    getArtifactDependencies(Artifact artifact)
  {
    ArrayList<ArtifactDependency> dependencies
      = new ArrayList<ArtifactDependency>();

    ArtifactDependency parentDep = artifact.getParent();

    if (parentDep != null) {
      ArtifactRepository repository = ArtifactRepository.getCurrent();
      
      ArrayList<Artifact> parentList = repository.resolve(parentDep);

      if (parentList == null || parentList.size() == 0)
        throw new ConfigException(L.l("maven parent {0} cannot be resolved",
                                      parentDep));

      Artifact parent = parentList.get(0);

      dependencies.addAll(getArtifactDependencies(parent));
    }

    for (ArtifactDependency dep : artifact.getDependencies()) {
      dependencies.add(dep);
    }

    return dependencies;
  }

  private Artifact findArtifact(Artifact newArtifact,
                                ArrayList<Artifact> artifactList)
  {
    for (Artifact artifact : artifactList) {
      if (newArtifact.isSameArtifact(artifact))
        return artifact;
    }

    return newArtifact;
  }
  
  private ArtifactClassLoader findLoader(Artifact artifact)
  {
    for (ArtifactClassLoader loader : _loaderList) {
      if (loader.getArtifact().equals(artifact))
        return loader;
    }

    return null;
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "[" + _loader + "]");
  }

  static class Entry {
    private Artifact _artifact;
    private ArtifactClassLoader _loader;

    Entry(Artifact artifact,
          ArtifactClassLoader loader)
    {
      _artifact = artifact;
      _loader = loader;
    }

    ArtifactClassLoader getLoader()
    {
      return _loader;
    }
  }
}
