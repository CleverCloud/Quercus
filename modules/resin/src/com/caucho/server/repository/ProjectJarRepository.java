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

package com.caucho.server.repository;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.config.Config;
import com.caucho.config.Service;
import com.caucho.config.Unbound;
import com.caucho.loader.JarMap;
import com.caucho.loader.maven.MavenProject;
import com.caucho.loader.module.Artifact;
import com.caucho.loader.module.ArtifactDependency;
import com.caucho.loader.module.ArtifactRepository;
import com.caucho.loader.module.ArtifactResolver;
import com.caucho.util.L10N;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;

/**
 * The directory holding jars
 */
@Service
@Unbound
public class ProjectJarRepository implements ArtifactResolver
{
  private static final L10N L = new L10N(ProjectJarRepository.class);

  private static final Logger log
    = Logger.getLogger(ProjectJarRepository.class.getName());

  private Path _path;

  private ArrayList<Entry> _entryList = new ArrayList<Entry>();

  public void setPath(Path path)
  {
    _path = path;
  }

  @PostConstruct
  public void init()
  {
    update();

    ArtifactRepository repository = ArtifactRepository.create();

    repository.addResolver(this);
  }

  public void resolve(ArrayList<Artifact> artifactList,
                      ArtifactDependency dependency)
  {
    for (Entry entry : _entryList) {
      Artifact artifact = entry.getArtifact();

      if (artifact != null && artifact.isMatch(dependency))
        artifactList.add(artifact);
    }
  }

  protected void update()
  {
    ArrayList<Path> jarList = getJarList();

    for (Path jarPath : jarList) {
      if (! jarPath.canRead()) {
        log.warning(L.l("{0}: '{1}' is an unreadable repository jar",
                        this, jarPath));
        continue;
      }

      Entry entry = readJar(jarPath);

      _entryList.add(entry);
    }
  }

  private Entry readJar(Path jarPath)
  {
    JarMap jarMap = new JarMap();

    jarMap.scan(jarPath);

    Iterator<String> keyIter = jarMap.keys();
    while (keyIter.hasNext()) {
      String key = keyIter.next();

      if (! key.endsWith("/pom.xml"))
        continue;

      String lowerKey = key.toLowerCase();

      if (! lowerKey.startsWith("meta-inf/maven/"))
        continue;

      Artifact artifact = readPom(jarPath, key);

      if (artifact != null)
        return new Entry(jarPath, artifact, jarMap);
    }

    return new Entry(jarPath, null, null);
  }

  private Artifact readPom(Path jarPath, String key)
  {
    JarPath jar = JarPath.create(jarPath);

    try {
      MavenProject project = new MavenProject();

      new Config().configure(project, jar.lookup(key));

      Artifact artifact = project.toArtifact(jarPath);

      return artifact;
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  protected ArrayList<Path> getJarList()
  {
    ArrayList<Path> jarList = new ArrayList<Path>();

    try {
      for (String name : _path.list()) {
        if (name.endsWith(".jar")) {
          Path jar = _path.lookup(name);

          jarList.add(jar);
        }
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return jarList;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _path + "]";
  }

  static class Entry {
    private Path _path;
    private Artifact _artifact;

    Entry(Path path, Artifact artifact, JarMap jarMap)
    {
      _path = path;
      _artifact = artifact;
      new SoftReference<JarMap>(jarMap);
    }

    Artifact getArtifact()
    {
      return _artifact;
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _path + "," + _artifact + "]";
    }
  }
}
