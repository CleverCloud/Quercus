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

package com.caucho.env.git;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import com.caucho.java.WorkDir;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * Tree structure from a jar
 */
public class GitCommitJar {
  private static final Logger log
    = Logger.getLogger(GitCommitJar.class.getName());
  
  private GitCommitTree _commit = new GitCommitTree();

  private Path _jar;
  
  private Path _tempJar;

  public GitCommitJar(Path jar)
    throws IOException
  {
    init(jar);
  }

  public GitCommitJar(InputStream is)
    throws IOException
  {
    Path dir = WorkDir.getLocalWorkDir();

    Path path = dir.createTempFile("git", "tmp");

    try {
      WriteStream os = path.openWrite();
      os.writeStream(is);
      os.close();

      init(path);

      _tempJar = path;
    } catch (IOException e) {
      path.remove();
    }
  }

  private void init(Path jar)
    throws IOException
  {
    _jar = jar;

    HashMap<String,Long> lengthMap = new HashMap<String,Long>();

    fillLengthMap(lengthMap, jar);

    ReadStream is = jar.openRead();

    fillCommit(lengthMap, is);

    _commit.commit();
  }

  private void fillCommit(HashMap<String,Long> lengthMap, InputStream is)
    throws IOException
  {
    try {
      ZipInputStream zin = new ZipInputStream(is);

      ZipEntry entry;
      
      while ((entry = zin.getNextEntry()) != null) {
        String path = entry.getName();
        long length = entry.getSize();

        if (entry.isDirectory())
          continue;

        Long lengthValue = lengthMap.get(path);

        if (lengthValue != null)
          length = lengthValue;

        _commit.addFile(path, 0664, zin, length);
      }
    } finally {
      is.close();
    }
  }

  public String []getCommitList()
  {
    return _commit.getCommitList();
  }

  public String getDigest()
  {
    return _commit.getDigest();
  }

  public String findPath(String sha1)
  {
    return _commit.findPath(sha1);
  }

  private void fillLengthMap(HashMap<String,Long> lengthMap, Path jar)
    throws IOException
  {
    ReadStream is = jar.openRead();
    try {
      ZipInputStream zin = new ZipInputStream(is);

      ZipEntry entry;
      
      while ((entry = zin.getNextEntry()) != null) {
        String path = entry.getName();
        long length = entry.getSize();

        if (entry.isDirectory())
          continue;

        if (length < 0) {
          length = 0;

          while (zin.read() >= 0) {
            length++;
          }
        }

        lengthMap.put(path, length);
      }
    } finally {
      is.close();
    }
  }

  public long getLength(String sha1)
    throws IOException
  {
    InputStream is = openFile(sha1);
    long length = 0;

    while (is.read() >= 0) {
      length++;
    }

    return length;
  }

  public InputStream openFile(String sha1)
    throws IOException
  {
    String path = _commit.findPath(sha1);

    if (path.endsWith("/")) {
      GitWorkingTree tree = _commit.findTree(path);

      return tree.openFile();
    }
    else {
      ZipFile file = new ZipFile(_jar.getNativePath());

      try {
        ZipEntry entry = file.getEntry(path);
        InputStream is = file.getInputStream(entry);

        try {
          return GitCommitTree.writeBlob(is, entry.getSize());
        } finally {
          is.close();
        }
      } finally {
        file.close();
      }
    }
  }

  public void close()
  {
    if (_tempJar != null) {
      try {
        _tempJar.remove();
      } catch (IOException e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "[]");
  }
}
