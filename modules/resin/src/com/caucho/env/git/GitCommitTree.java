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

import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/**
 * Tree structure
 */
public class GitCommitTree {
  private GitWorkingTree _root = new GitWorkingTree();

  private HashMap<String,String> _sha1ToPathMap
    = new HashMap<String,String>();

  /**
   * Adds a file to the commit tree
   */
  public String addFile(String treePath, int mode, Path filePath)
    throws IOException
  {
    ReadStream is = filePath.openRead();

    try {
      return addFile(treePath, mode, is, filePath.getLength());
    } finally {
      is.close();
    }
  }

  /**
   * Adds a file to the commit tree
   */
  public String addFile(String path, int mode, InputStream is, long length)
    throws IOException
  {
    String sha1 = calculateBlobDigest(is, length);

    _root.addBlobPath(path, mode, sha1);

    _sha1ToPathMap.put(sha1, path);

    return sha1;
  }

  /**
   * Finds the file path given the sha1
   */
  public String findPath(String sha1)
  {
    return _sha1ToPathMap.get(sha1);
  }

  /**
   * Finds the directory given the path
   */
  public GitWorkingTree findTree(String path)
  {
    return _root.findTreeRec(path);
  }

  /**
   * Commits the tree by calculating the directory hashes
   */
  public String commit()
  {
    return _root.commit(this, "");
  }

  public String getDigest()
  {
    return _root.getDigest();
  }

  void addCommitDir(String sha1, String path)
  {
    if (! path.endsWith("/"))
      path = path + "/";
    
    _sha1ToPathMap.put(sha1, path);
  }

  /**
   * Returns the commit list
   */
  public String []getCommitList()
  {
    String []commitList = new String[_sha1ToPathMap.size()];
    _sha1ToPathMap.keySet().toArray(commitList);
    Arrays.sort(commitList);

    return commitList;
  }

  public static String calculateBlobDigest(InputStream is, long length)
    throws IOException
  {
    TempBuffer tBuf = TempBuffer.allocate();

    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");

      md.update((byte) 'b');
      md.update((byte) 'l');
      md.update((byte) 'o');
      md.update((byte) 'b');
      md.update((byte) ' ');

      String lenString = String.valueOf(length);
      for (int i = 0; i < lenString.length(); i++) {
        md.update((byte) lenString.charAt(i));
      }
      md.update((byte) 0);

      int len;

      byte []buffer = tBuf.getBuffer();
      while ((len = is.read(buffer, 0, buffer.length)) > 0) {
        md.update(buffer, 0, len);
      }

      return Hex.toHex(md.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } finally {
      TempBuffer.free(tBuf);
    }
  }

  public static InputStream writeBlob(InputStream is, long length)
    throws IOException
  {
    TempOutputStream os = new TempOutputStream();
    DeflaterOutputStream out = new DeflaterOutputStream(os);
    TempBuffer tBuf = TempBuffer.allocate();

    try {
      out.write('b');
      out.write('l');
      out.write('o');
      out.write('b');
      out.write(' ');

      String lenString = String.valueOf(length);
      for (int i = 0; i < lenString.length(); i++) {
        out.write(lenString.charAt(i));
      }
      out.write(0);

      int len;

      byte []buffer = tBuf.getBuffer();
      while ((len = is.read(buffer, 0, buffer.length)) > 0) {
        out.write(buffer, 0, len);
      }

      out.close();

      return os.openRead();
    } finally {
      TempBuffer.free(tBuf);
    }
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "[]");
  }
}
