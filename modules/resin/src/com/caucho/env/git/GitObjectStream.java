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
import java.util.HashMap;
import java.util.zip.*;

/**
 * Top-level class for a repository
 */
public class GitObjectStream extends InputStream {
  private static final HashMap<String,GitType> _gitTypeMap
    = new HashMap<String,GitType>();
  
  private ReadStream _rawStream;
  private InflaterInputStream _is;

  private GitType _type;
  private long _length;
  
  public GitObjectStream(Path path)
    throws IOException
  {
    _rawStream = path.openRead();
    _is = new InflaterInputStream(_rawStream);

    int ch;

    StringBuilder type = new StringBuilder();

    while ((ch = _is.read()) >= 0 && ch != ' ') {
      type.append((char) ch);
    }

    _type = _gitTypeMap.get(type.toString());

    long length = 0;
    while ((ch = _is.read()) >= 0 && '0' <= ch && ch <= '9') {
      length = length * 10 + ch - '0';
    }

    _length = length;

    // skip to null
    for (; ch > 0; ch = _is.read()) {
    }
  }

  public GitType getType()
  {
    return _type;
  }

  public GitCommit parseCommit()
    throws IOException
  {
    GitCommit commit = new GitCommit();

    int ch;
    while ((ch = _is.read()) >= 0) {
      StringBuilder keyBuilder = new StringBuilder();

      for (; ch >= 0 && ch != ' ' && ch != '\n'; ch = _is.read()) {
        keyBuilder.append((char) ch);
      }
      String key = keyBuilder.toString();

      if (key.length() == 0) {
        StringBuilder msg = new StringBuilder();

        for (ch = _is.read(); ch >= 0; ch = _is.read()) {
          msg.append((char) ch);

          commit.setMessage(msg.toString());
        }

        break;
      }

      for (; ch == ' '; ch = _is.read()) {
      }

      StringBuilder value = new StringBuilder();
      for (; ch >= 0 && ch != '\n'; ch = _is.read()) {
        value.append((char) ch);
      }

      if ("tree".equals(key))
        commit.setTree(value.toString());
      if ("parent".equals(key))
        commit.setParent(value.toString());
      else
        commit.put(key, value.toString());
    }

    return commit;
  }

  public GitTree parseTree()
    throws IOException
  {
    GitTree tree = new GitTree();

    int ch;
    while ((ch = _is.read()) >= 0) {
      int mode = 0;

      for (; '0' <= ch && ch <= '7'; ch = _is.read()) {
        mode = mode * 8 + ch - '0';
      }

      for (; ch == ' '; ch = _is.read()) {
      }

      StringBuilder nameBuffer = new StringBuilder();
      for (; ch > 0; ch = _is.read()) {
        nameBuffer.append((char) ch);
      }
      String name = nameBuffer.toString();

      byte []sha1 = new byte[20];
      for (int i = 0; i < sha1.length; i++) {
        sha1[i] = (byte) _is.read();
      }

      tree.addEntry(name, mode, Hex.toHex(sha1));
    }

    return tree;
  }
  
  public InputStream getInputStream()
  {
    return this;
  }

  public int read()
    throws IOException
  {
    return _is.read();
  }

  public int read(byte []buffer, int offset, int length)
    throws IOException
  {
    return _is.read(buffer, offset, length);
  }

  public void close()
  {
    ReadStream in = _rawStream;
    _rawStream = null;

    if (in != null)
      in.close();

    InputStream is = _is;
    _is = null;

    try {
      if (is != null)
        is.close();
    } catch (IOException e) {
    }
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[type=" + _type
            + ",length=" + _length + "]");
  }

  static {
    _gitTypeMap.put("blob", GitType.BLOB);
    _gitTypeMap.put("tree", GitType.TREE);
    _gitTypeMap.put("commit", GitType.COMMIT);
  }
}
