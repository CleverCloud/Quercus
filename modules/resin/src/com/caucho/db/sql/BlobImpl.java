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
package com.caucho.db.sql;

import com.caucho.db.blob.BlobInputStream;
import com.caucho.db.block.BlockStore;
import com.caucho.util.L10N;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;

/**
 * The JDBC blob implementation.
 */
public class BlobImpl implements java.sql.Blob {
  private static final L10N L = new L10N(BlobImpl.class);

  private BlockStore _store;
  private byte []_inode = new byte[128];
    
  BlobImpl()
  {
  }

  void setStore(BlockStore store)
  {
    _store = store;
  }

  byte []getInode()
  {
    return _inode;
  }

  /**
   * Returns the blob as a stream.
   */
  public InputStream getBinaryStream()
    throws SQLException
  {
    return new BlobInputStream(_store, _inode, 0);
  }

  /**
   * Returns a subset of the bytes.
   */
  public byte []getBytes(long pos, int length)
    throws SQLException
  {
    try {
      // XXX: performance
      ByteArrayOutputStream bos = new ByteArrayOutputStream();

      InputStream is = getBinaryStream();

      if (pos > 1)
        is.skip(pos - 1);

      for (int i = 0; i < length; i++) {
        int ch = is.read();

        if (ch < 0)
          break;

        bos.write(ch);
      }

      is.close();

      return bos.toByteArray();
    } catch (IOException e) {
      throw new SQLException(e);
    }
  }

  /**
   * Returns the length of the blob
   */
  public long length()
    throws SQLException
  {
    return BlobInputStream.readLong(_inode, 0);
  }

  /**
   * Returns the position in the blob where the pattern starts.
   */
  public long position(Blob pattern, long start)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the position in the blob where the pattern starts.
   */
  public long position(byte []pattern, long start)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a stream to write to the blob.
   */
  public OutputStream setBinaryStream(long pos)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets a subset of bytes.
   */
  public int setBytes(long pos, byte []bytes)
    throws SQLException
  {
    return setBytes(pos, bytes, 0, bytes.length);
  }

  /**
   * Sets a subset of bytes.
   */
  public int setBytes(long pos, byte []bytes, int offset, int length)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Truncates the blob
   */
  public void truncate(long length)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return "BlobImpl[]";
  }

    public void free() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public InputStream getBinaryStream(long pos, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
