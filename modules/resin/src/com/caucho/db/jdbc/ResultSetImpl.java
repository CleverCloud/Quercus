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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */
package com.caucho.db.jdbc;

import com.caucho.db.sql.SelectResult;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * The JDBC statement implementation.
 */
public class ResultSetImpl extends AbstractResultSet {
  private StatementImpl _stmt;
  private SelectResult _rs;
  private int _rowNumber;
  
  ResultSetImpl(StatementImpl stmt, SelectResult rs)
  {
    _stmt = stmt;
    _rs = rs;
  }

  public int getRow()
    throws SQLException
  {
    if (_rowNumber < 0)
      throw new SQLException("can't call getRow() after close()");
    
    return _rowNumber;
  }

  public boolean isBeforeFirst()
    throws SQLException
  {
    return _rowNumber == 0;
  }

  public boolean isFirst()
    throws SQLException
  {
    return _rowNumber == 1;
  }

  public java.sql.Statement getStatement()
    throws SQLException
  {
    if (_rowNumber < 0)
      throw new SQLException("can't call getStatement() after close()");
    
    return _stmt;
  }

  public java.sql.ResultSetMetaData getMetaData()
    throws SQLException
  {
    if (_rowNumber < 0)
      throw new SQLException("can't call getMetaData() after close()");
    
    return new ResultSetMetaDataImpl(_rs);
  }

  public boolean wasNull()
    throws SQLException
  {
    return _rs.wasNull();
  }

  /**
   * Goes to the next row, returning true if it exists.
   */
  public boolean next()
    throws SQLException
  {
    if (_rs == null)
      return false;
    else if (_rs.next()) {
      _rowNumber++;
      
      return true;
    }
    else {
      close();

      return false;
    }
  }

  public int findColumn(String columnName)
    throws SQLException
  {
    return _rs.findColumnIndex(columnName);
  }

  /**
   * Returns the boolean value for the column.
   */
  public boolean getBoolean(int columnIndex)
    throws SQLException
  {
    String s = getString(columnIndex);
    
    return s != null && ! s.equals("") && ! s.equals("0") && ! s.equals("n");
  }

  /**
   * Returns the date value for the column.
   */
  public java.sql.Date getDate(int columnIndex)
    throws SQLException
  {
    long date = _rs.getDate(columnIndex - 1);

    if (wasNull())
      return null;
    else
      return new java.sql.Date(date);
  }

  /**
   * Returns the double value for the column.
   */
  public double getDouble(int columnIndex)
    throws SQLException
  {
    return _rs.getDouble(columnIndex - 1);
  }

  /**
   * Returns the integer value for the column.
   */
  public int getInt(int columnIndex)
    throws SQLException
  {
    return _rs.getInt(columnIndex - 1);
  }

  /**
   * Returns the long value for the column.
   */
  public long getLong(int columnIndex)
    throws SQLException
  {
    return _rs.getLong(columnIndex - 1);
  }

  /**
   * Returns the string value for the column.
   */
  public String getString(int columnIndex)
    throws SQLException
  {
    return _rs.getString(columnIndex - 1);
  }

  /**
   * Returns the time value for the column.
   */
  public Time getTime(int columnIndex)
    throws SQLException
  {
    long date = _rs.getDate(columnIndex - 1);

    if (wasNull())
      return null;
    else
      return new java.sql.Time(date);
  }

  public Timestamp getTimestamp(int columnIndex)
    throws SQLException
  {
    long date = _rs.getDate(columnIndex - 1);

    if (wasNull())
      return null;
    else
      return new java.sql.Timestamp(date);
  }

  /**
   * Returns the big-decimal value for the column.
   */
  public BigDecimal getBigDecimal(int columnIndex)
    throws SQLException
  {
    return new BigDecimal(_rs.getString(columnIndex - 1));
  }

  /**
   * Returns the blob value for the column.
   */
  public byte []getBytes(int columnIndex)
    throws SQLException
  {
    return _rs.getBytes(columnIndex - 1);
  }

  /**
   * Returns the blob value for the column.
   */
  public Blob getBlob(int columnIndex)
    throws SQLException
  {
    return _rs.getBlob(columnIndex - 1);
  }

  /**
   * Returns the clob value for the column.
   */
  public Clob getClob(int columnIndex)
    throws SQLException
  {
    return _rs.getClob(columnIndex - 1);
  }

  public void close()
    throws SQLException
  {
    SelectResult result = _rs;
    _rs = null;

    if (result != null)
      result.close();
  }

    public RowId getRowId(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public RowId getRowId(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getHoldability() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isClosed() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNString(int columnIndex, String nString) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNString(String columnLabel, String nString) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public NClob getNClob(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public NClob getNClob(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getNString(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getNString(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
