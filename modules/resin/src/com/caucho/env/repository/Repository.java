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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.caucho.env.git.GitCommit;
import com.caucho.env.git.GitTree;
import com.caucho.env.git.GitType;
import com.caucho.vfs.Path;

/**
 * The Repository is a collection of archives organized by a tag map. Each
 * archive is equivalent to a .jar file or a directory, consisting of
 * the binary data Blobs, the directory name Tree, and a .git Commit item
 * to track versions.
 * 
 * The tag map is a map of strings to tag entries, where the entry is
 * the sha1 of the .git Commit root of the archive, and metadata.
 */
public interface Repository
{
  //
  // .git file management
  //

  /**
   * Returns true if the file exists.
   */
  public boolean exists(String contentHash);

  /**
   * Returns the GitType of the file.
   */
  public GitType getType(String contentHash);

  /**
   * Returns true if the file is a blob.
   */
  public boolean isBlob(String contentHash);

  /**
   * Returns true if the file is a tree
   */
  public boolean isTree(String contentHash);

  /**
   * Returns true if the file is a commit
   */
  public boolean isCommit(String contentHash);

  /**
   * Adds a stream to the repository where the length is not known.
   * When possible the alternate method with a length should be used because
   * this method requires a copy of the entire stream to calculate the length.
   * 
   * @param is the blob's input stream
   */
  public String addBlob(InputStream is)
    throws IOException;

  /**
   * Adds a stream to the repository where the length is known.
   * 
   * @param is the blob's input stream
   * @param length the blob's length
   */
  public String addBlob(InputStream is, long length)
    throws IOException;

  /**
   * Opens an InputStream to a git blob
   */
  public InputStream openBlob(String blobHash)
    throws IOException;

  /**
   * Writes the contents of a blob to an OutputStream.
   * 
   * @param blobHash the hash of the source blob
   * @param os the OutputStream to write to
   */
  public void writeBlobToStream(String blobHash, OutputStream os);

  /**
   * Adds a git tree to the repository
   */
  public String addTree(GitTree tree)
    throws IOException;

  /**
   * Reads a git tree from the repository
   */
  public GitTree readTree(String treeHash)
    throws IOException;

  /**
   * Adds a git commit entry to the repository
   */
  public String addCommit(GitCommit commit)
    throws IOException;

  /**
   * Reads a git commit entry from the repository
   */
  public GitCommit readCommit(String commitHash)
    throws IOException;

  /**
   * Validates a hash, checking that it and its dependencies exist.
   */
  public boolean validateHash(String contentHash)
    throws IOException;
  
  //
  // Convenience methods
  //

  /**
   * Adds a path to the repository.  If the path is a directory or a
   * jar scheme, adds the contents recursively.
   */
  public String addPath(Path path);

  /**
   * Expands the repository to the filesystem.
   */
  public void expandToPath(String contentHash, Path path);

  //
  // low-level raw .git access
  //
  
  /**
   * Opens a stream to the raw git file.
   */
  public InputStream openRawGitFile(String contentHash)
    throws IOException;

  /**
   * Writes a raw git file
   */
  public void writeRawGitFile(String contentHash, InputStream is)
    throws IOException;
  
  //
  // tag management
  //
  
  /**
   * Updates the repository, checking for any changes across the cluster.
   */
  public void checkForUpdate();
  
  /**
   * Returns the current read-only tag map.
   */
  public Map<String,RepositoryTagEntry> getTagMap();

  /**
   * Convenience method returning the tag's contentHash.
   */
  public String getTagContentHash(String tag);

  /**
   * Adds a tag
   *
   * @param tagName the symbolic tag for the repository
   * @param contentHash the hash for the tag's content, typically a .git tree
   * @param commitMessage user's message for the commit
   * @param commitMetaData additional commit meta-data
   */
  public boolean putTag(String tagName,
                        String contentHash,
                        String commitMessage,
                        Map<String,String> commitMetaData);

  /**
   * Removes a tag
   *
   * @param tagName the symbolic tag for the repository
   * @param user the user adding a tag.
   * @param server the server adding a tag.
   * @param message user's message for the commit
   */
  public boolean removeTag(String tagName,
                           String commitMessage,
                           Map<String,String> commitMetaData);
  
  /**
   * Adds a tag change listener
   */
  public void addListener(String tagName, RepositoryTagListener listener);
  
  /**
   * Adds a tag change listener
   */
  public void removeListener(String tagName, RepositoryTagListener listener);
  
  
  //
  // The repository root hash
  //
  
  /**
   * The Commit .git hash for the repository itself. The hash points
   * to a .git Commit entry for the current repository version.
   * 
   * @return the hash of the .git Commit for the current repository root.
   */
  public String getRepositoryRootHash();
  
  /**
   * The root .git hash for the repository itself. The hash points
   * to a .git Commit entry for the current repository version.
   * 
   * @param rootCommitHash the hash of the new .git Commit for the 
   * repository.
   */
  public void setRepositoryRootHash(String rootCommitHash);
}
