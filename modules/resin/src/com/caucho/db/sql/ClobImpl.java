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
import java.io.Reader;
import java.io.Writer;
import java.sql.Clob;
import java.sql.SQLException;

/**
 * The JDBC clob implementation.
 */
public class ClobImpl implements java.sql.Clob {
  private static final L10N L = new L10N(ClobImpl.class);

  private BlockStore _store;
  private byte []_inode = new byte[128];
    
  ClobImpl()
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
   * Returns the clob as a stream.
   */
  public InputStream getAsciiStream()
    throws SQLException
  {
    // XXX: lie, since this is utf8
    return new BlobInputStream(_store, _inode, 0);
  }

  /**
   * Returns a subset of the bytes.
   */
  public Reader getCharacterStream()
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a copy of the specified substring.
   */
  public String getSubString(long pos, int length)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the length of the clob
   */
  public long length()
    throws SQLException
  {
    return BlobInputStream.readLong(_inode, 0);
  }

  /**
   * Returns the position in the clob where the pattern starts.
   */
  public long position(Clob pattern, long start)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the position in the clob where the pattern starts.
   */
  public long position(String pattern, long start)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a stream to write to the clob.
   */
  public OutputStream setAsciiStream(long pos)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a stream to write to the clob.
   */
  public Writer setCharacterStream(long pos)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets a subset of bytes.
   */
  public int setString(long pos, String string)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets a subset of bytes.
   */
  public int setString(long pos, String string, int offset, int len)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Truncates the clob
   */
  public void truncate(long length)
    throws SQLException
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return "ClobImpl[]";
  }

    public void free() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Reader getCharacterStream(long pos, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
