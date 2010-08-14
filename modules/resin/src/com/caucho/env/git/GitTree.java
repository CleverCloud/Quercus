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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.caucho.util.Hex;

/**
 * Tree structure
 */
public class GitTree {
  private HashMap<String,Entry> _treeMap = new LinkedHashMap<String,Entry>();

  public void addEntry(String name, int mode, String sha1)
  {
    _treeMap.put(name, new Entry(name, mode, sha1));
  }

  public HashMap<String,Entry> getMap()
  {
    return _treeMap;
  }

  public String getHash(String name)
  {
    Entry entry = getMap().get(name);

    if (entry != null)
      return entry.getSha1();
    else
      return null;
  }

  public void addBlob(String name, int mode, String sha1)
  {
    Entry entry = new Entry(name, 0100000 | (mode & 0777), sha1);

    _treeMap.put(name, entry);
  }

  public void addDir(String name, String sha1)
  {
    Entry entry = new Entry(name, 040000, sha1);

    _treeMap.put(name, entry);
  }

  public Collection<Entry> entries()
  {
    return _treeMap.values();
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
    private final String _sha1;

    Entry(String name, int mode, String sha1)
    {
      _name = name;
      _mode = mode;
      _sha1 = sha1;
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

    public boolean isDir()
    {
      return (_mode & 0777000) == 0040000;
    }

    public boolean isFile()
    {
      return (_mode & 0777000) == 0100000;
    }

    public String toString()
    {
      return ("GitTree.Entry[" + _name
              + "," + String.format("%o", _mode)
              + "," + _sha1 + "]");
    }
  }
}
