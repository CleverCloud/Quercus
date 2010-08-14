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
public class GitWorkingTree {
  private Map<String,Entry> _treeMap = new TreeMap<String,Entry>();

  private String _digest;

  public Map<String,Entry> getMap()
  {
    return _treeMap;
  }

  public String getDigest()
  {
    return _digest;
  }

  public void addBlobPath(String path, int mode, String sha1)
  {
    while (path.startsWith("/"))
      path = path.substring(1);

    if ("".equals(path))
      return;
    
    int p = path.indexOf('/');

    if (p > 0) {
      String head = path.substring(0, p);
      String tail = path.substring(p + 1);

      GitWorkingTree tree = getTree(head);

      tree.addBlobPath(tail, mode, sha1);
    }
    else {
      addBlob(path, mode, sha1);
    }
  }

  public GitWorkingTree findTreeRec(String path)
  {
    while (path.startsWith("/"))
      path = path.substring(1);

    if ("".equals(path))
      return this;
    
    int p = path.indexOf('/');

    if (p > 0) {
      String head = path.substring(0, p);
      String tail = path.substring(p + 1);

      GitWorkingTree tree = getTree(head);

      return tree.findTreeRec(tail);
    }
    else {
      return getTree(path);
    }
  }

  public InputStream openFile()
    throws IOException
  {
    TempOutputStream out = new TempOutputStream();

    writeTree(out);

    GitInputStream is = new GitInputStream("tree",
                                           out.getLength(),
                                           out.openRead());

    out = new TempOutputStream();
    DeflaterOutputStream zipOut = new DeflaterOutputStream(out);

    TempBuffer tBuf = TempBuffer.allocate();
    byte []buffer = tBuf.getBuffer();

    int len;

    while ((len = is.read(buffer, 0, buffer.length)) > 0) {
      zipOut.write(buffer, 0, len);
    }

    zipOut.close();

    return out.openRead();
  }

  String commit(GitCommitTree commit, String path)
  {
    for (Entry entry : _treeMap.values()) {
      GitWorkingTree subTree = entry.getSubTree();

      if (subTree != null) {
        String subPath;

        if (! "".equals(path))
          subPath = path + "/" + entry.getName();
        else
          subPath = entry.getName();

        entry.setSha1(subTree.commit(commit, subPath));
      }
    }

    _digest = calculateHash();

    commit.addCommitDir(_digest, path);

    return _digest;
  }

  private String calculateHash()
  {
    try {
      TempOutputStream out = new TempOutputStream();

      writeTree(out);

      MessageDigest md = MessageDigest.getInstance("SHA-1");

      updateString(md, "tree " + out.getLength());
      md.update((byte) 0);

      TempBuffer tBuf = TempBuffer.allocate();
      byte []buffer = tBuf.getBuffer();
      int len;

      InputStream is = out.openRead();

      while ((len = is.read(buffer, 0, buffer.length)) > 0) {
        md.update(buffer, 0, len);
      }

      is.close();
      TempBuffer.free(tBuf);

      return Hex.toHex(md.digest());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void writeTree(OutputStream out)
    throws IOException
  {
    ArrayList<String> keys = new ArrayList<String>(_treeMap.keySet());
    Collections.sort(keys);

    for (String key : keys) {
      Entry entry = _treeMap.get(key);

      String mode = String.format("%o", entry.getMode());

      int len = mode.length();
      for (int i = 0; i < len; i++)
        out.write(mode.charAt(i));

      out.write(' ');
      
      len = key.length();
      for (int i = 0; i < len; i++)
        out.write(key.charAt(i));
      
      out.write(0);

      String sha1 = entry.getSha1();
      byte []hash = Hex.toBytes(sha1);

      out.write(hash, 0, hash.length);
    }
  }

  private void updateString(MessageDigest md, String s)
  {
    int len = s.length();

    for (int i = 0; i < len; i++) {
      md.update((byte) s.charAt(i));
    }
  }
  
  private void addBlob(String name, int mode, String sha1)
  {
    Entry entry = new Entry(name, 0100000 | (mode & 0777), sha1);

    _treeMap.put(name, entry);
  }

  private GitWorkingTree getTree(String name)
  {
    Entry entry = _treeMap.get(name);
    
    if (entry != null)
      return entry.getSubTree();

    GitWorkingTree subTree = new GitWorkingTree();
    
    entry = new Entry(name, subTree);
    _treeMap.put(name, entry);

    return subTree;
  }

  public void toData(OutputStream out)
    throws IOException
  {
    ArrayList<String> keys = new ArrayList<String>(_treeMap.keySet());
    Collections.sort(keys);

    for (String key : keys) {
      Entry entry = _treeMap.get(key);

      String mode = String.format("%o", entry.getMode());

      int len = mode.length();
      for (int i = 0; i < len; i++)
        out.write(mode.charAt(i));

      out.write(' ');
      
      len = key.length();
      for (int i = 0; i < len; i++)
        out.write(key.charAt(i));
      
      out.write(0);

      String sha1 = entry.getSha1();
      byte []hash = Hex.toBytes(sha1);

      out.write(hash, 0, hash.length);
    }
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "[]");
  }

  public class Entry {
    private final String _name;
    private final int _mode;
    private String _sha1;
    private GitWorkingTree _subTree;

    Entry(String name, int mode, String sha1)
    {
      _name = name;
      _mode = mode;
      _sha1 = sha1;
    }

    Entry(String name, GitWorkingTree subTree)
    {
      _name = name;
      _mode = 040000;
      _subTree = subTree;
    }

    public String getName()
    {
      return _name;
    }

    public int getMode()
    {
      return _mode;
    }

    public String getSha1()
    {
      return _sha1;
    }

    public void setSha1(String sha1)
    {
      _sha1 = sha1;
    }

    public GitWorkingTree getSubTree()
    {
      return _subTree;
    }

    public String toString()
    {
      return ("GitTree.Entry[" + _name
              + "," + String.format("%o", _mode)
              + "," + _sha1 + "]");
    }
  }
}
