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

package com.caucho.env.repository;

import com.caucho.env.git.*;
import com.caucho.inject.Module;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

import java.io.*;
import java.util.*;

/**
 * Map of the current tags.
 */
@Module
public class RepositoryTagMap
{
  private static final L10N L = new L10N(RepositoryTagMap.class);

  private final String _commitHash;
  private final GitCommit _commit;

  private final long _sequence;

  private final GitTree _tree;

  private final Map<String,RepositoryTagEntry> _tagMap;

  public RepositoryTagMap()
  {
    _commitHash = null;
    _commit = null;
    _sequence = 0;
    _tagMap
      = Collections.unmodifiableMap(new HashMap<String,RepositoryTagEntry>());

    _tree = null;
  }

  public RepositoryTagMap(AbstractRepository repository,
                          String commitHash)
    throws IOException
  {
    _commitHash = commitHash;

    // force loading and validation from backend
    repository.validateHash(commitHash);

    _commit = repository.readCommit(commitHash);

    _sequence = Long.parseLong(_commit.get("sequence"));

    _tree = repository.readTree(_commit.getTree());

    _tagMap = readTagMap(repository, _tree.getHash("tags"));
  }

  public RepositoryTagMap(AbstractRepository repository,
                          RepositoryTagMap parent,
                          Map<String,RepositoryTagEntry> tagMap)
    throws IOException
  {
    _tagMap = Collections.unmodifiableMap(tagMap);

    _sequence = parent.getSequence() + 1;

    TempStream os = new TempStream();
    WriteStream out = new WriteStream(os);

    writeTagMap(out);
    out.close();

    String tagHash;

    InputStream is = os.getInputStream();

    try {
      tagHash = repository.addBlob(is);
    } finally {
      is.close();
    }

    _tree = new GitTree();

    _tree.addBlob("tags", 0775, tagHash);

    for (String key : tagMap.keySet()) {
      RepositoryTagEntry entry = tagMap.get(key);

      String sha1 = entry.getTagEntryHash();
      String root = entry.getRoot();

      _tree.addBlob(sha1, 0644, sha1);

      GitType type = repository.getType(root);

      if (type == GitType.BLOB)
        _tree.addBlob(root, 0644, root);
      else if (type == GitType.TREE)
        _tree.addDir(root, root);
      else
        throw new IllegalStateException(L.l("'{0}' has an unknown type {1}",
                                            root, type));
    }

    String treeHash = repository.addTree(_tree);

    _commit = new GitCommit();
    _commit.setTree(treeHash);
    _commit.put("sequence", String.valueOf(parent.getSequence() + 1));

    _commitHash = repository.addCommit(_commit);
  }

  /**
   * Returns the commit hash value of the tag map itself
   */
  public String getCommitHash()
  {
    return _commitHash;
  }

  /**
   * Returns the sequence
   */
  public long getSequence()
  {
    return _sequence;
  }

  /**
   * Returns the deployment's tag map.
   */
  public Map<String,RepositoryTagEntry> getTagMap()
  {
    return _tagMap;
  }

  private Map<String,RepositoryTagEntry> readTagMap(AbstractRepository repository,
                                                String sha1)
    throws IOException
  {
    TreeMap<String,RepositoryTagEntry> map
      = new TreeMap<String,RepositoryTagEntry>();

    InputStream is = repository.openBlob(sha1);
    try {
      ReadStream in = Vfs.openRead(is);

      String tag;

      while ((tag = in.readLine()) != null) {
        String entrySha1 = in.readLine();

        RepositoryTagEntry entry = new RepositoryTagEntry(repository, entrySha1);

        map.put(tag, entry);
      }
    } finally {
      is.close();
    }

    return Collections.unmodifiableMap(map);
  }

  private void writeTagMap(WriteStream out)
    throws IOException
  {
    for (Map.Entry<String,RepositoryTagEntry> entry : _tagMap.entrySet()) {
      out.println(entry.getKey());
      out.println(entry.getValue().getTagEntryHash());
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[seq=" + _sequence + "," + _commitHash + "]";
  }
}