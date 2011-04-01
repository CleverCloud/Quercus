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

import java.util.ArrayList;
import java.util.Collections;

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;

/**
 * A jar artifact in the repository
 */
public class ArtifactRepository
{
  private static final EnvironmentLocal<ArtifactRepository> _local
    = new EnvironmentLocal<ArtifactRepository>();

  private ArtifactRepository _parent;
  private EnvironmentClassLoader _loader;

  private ArrayList<ArtifactResolver> _resolverList
    = new ArrayList<ArtifactResolver>();

  private ArtifactRepository(EnvironmentClassLoader loader)
  {
    _loader = loader;

    if (loader != null) {
      EnvironmentClassLoader parentLoader
        = Environment.getEnvironmentClassLoader(loader.getParent());

      if (parentLoader != null && parentLoader != loader)
        _parent = create(parentLoader);
    }
  }

  public static ArtifactRepository create()
  {
    return create(Thread.currentThread().getContextClassLoader());
  }

  public static ArtifactRepository create(ClassLoader loader)
  {
    synchronized (_local) {
      ArtifactRepository repository = _local.getLevel(loader);

      if (repository == null) {
        ClassLoader parentLoader = null;
        
        if (loader != null)
          parentLoader = loader.getParent();
        
        EnvironmentClassLoader envLoader
          = Environment.getEnvironmentClassLoader(parentLoader);

        repository = new ArtifactRepository(envLoader);

        _local.set(repository);
      }

      return repository;
    }
  }

  public static ArtifactRepository getCurrent()
  {
    return _local.get();
  }

  public void addResolver(ArtifactResolver resolver)
  {
    _resolverList.add(resolver);
  }

  public ArrayList<Artifact> resolve(ArtifactDependency dependency)
  {
    ArrayList<ArtifactDependency> peers = new ArrayList<ArtifactDependency>();

    return resolve(dependency, peers);
  }

  public ArrayList<Artifact> resolve(ArtifactDependency dependency,
                                     ArtifactDependency []peerDependencyList)
  {
    ArrayList<ArtifactDependency> peers = new ArrayList<ArtifactDependency>();

    for (ArtifactDependency peer : peerDependencyList)
      peers.add(peer);

    return resolve(dependency, peers);
  }

  public ArrayList<Artifact> resolve(ArtifactDependency dependency,
                                     ArrayList<ArtifactDependency> peerDependencyList)
  {
    ArrayList<Artifact> artifactList = new ArrayList<Artifact>();
    resolve(artifactList, dependency);

    ArrayList<ArtifactDependency> peerDeps
      = resolvePeer(dependency, peerDependencyList);
    
    ArrayList<Artifact> filteredArtifactList = new ArrayList<Artifact>();

    for (Artifact artifact : artifactList) {
      if (isValid(artifact, peerDeps)) {
        filteredArtifactList.add(artifact);
      }
    }

    if (filteredArtifactList.size() > 0)
      artifactList = filteredArtifactList;

    Collections.sort(artifactList);
    Collections.reverse(artifactList);

    return artifactList;
  }

  private boolean isValid(Artifact artifact,
                          ArrayList<ArtifactDependency> dependencyList)
  {
    for (ArtifactDependency dep : dependencyList) {
      if (! artifact.isMatch(dep))
        return false;
    }

    return true;
  }

  protected void resolve(ArrayList<Artifact> artifactList,
                         ArtifactDependency dependency)
  {
    if (_parent != null)
      _parent.resolve(artifactList, dependency);

    for (ArtifactResolver resolver : _resolverList)
      resolver.resolve(artifactList, dependency);
  }

  protected ArrayList<ArtifactDependency>
    resolvePeer(ArtifactDependency dependency,
                ArrayList<ArtifactDependency> peerList)
  {
    ArrayList<ArtifactDependency> commonDeps
      = new ArrayList<ArtifactDependency>();
    
    for (ArtifactDependency peer : peerList) {
      if (peer == dependency)
        continue;

      ArrayList<Artifact> peerArtifacts = resolve(peer);

      for (Artifact peerArtifact : peerArtifacts) {
        for (ArtifactDependency peerDependency
               : peerArtifact.getDependencies()) {
          if (peerDependency.isSameArtifact(dependency)) {
            commonDeps.add(peerDependency);
          }
        }
      }
    }

    return commonDeps;
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "[" + _loader + "]");
  }
}
