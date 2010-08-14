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

package com.caucho.quercus.mysql;

import com.caucho.quercus.lib.db.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;
import javax.sql.*;

/**
 * Special Quercus Mysql connection.
 */
public class MysqlResultImpl extends AbstractResultSet
  implements QuercusResultSet
{
  private static final Logger log
    = Logger.getLogger(MysqlResultImpl.class.getName());
  private static final L10N L = new L10N(MysqlResultImpl.class);

  private MysqlStatementImpl _stmt;
  private boolean _isResultSet;
  private int _updateCount;
  private long _insertId;

  private MysqlResultSetMetaData _resultMetaData
    = new MysqlResultSetMetaData();

  private int _columnCount;
  private ArrayList<MysqlColumn> _columns;

  private boolean _isRowAvailable;
  private TempOutputStream _resultData;
  private char []_charBuffer;

  MysqlResultImpl(MysqlStatementImpl stmt)
  {
    _stmt = stmt;

    _columns = _resultMetaData.getColumns();
  }

  public boolean isResultSet()
  {
    return _isResultSet;
  }

  public void setResultSet(boolean isResultSet)
  {
    _isResultSet = isResultSet;
  }

  public int getUpdateCount()
  {
    return _updateCount;
  }

  public void setUpdateCount(int count)
  {
    _updateCount = count;
  }

  public long getInsertId()
  {
    return _insertId;
  }

  public void setInsertId(long id)
  {
    _insertId = id;
  }

  public void setColumnCount(int count)
  {
    _columnCount = count;

    _resultMetaData.setColumnCount(count);
    _columns = _resultMetaData.getColumns();
  }

  public int getColumnCount()
  {
    return _columnCount;
  }

  public MysqlColumn getColumn(int index)
  {
    return _columns.get(index);
  }

  public java.sql.Statement getStatement()
    throws SQLException
  {
    return _stmt;
  }

  public java.sql.ResultSetMetaData getMetaData()
    throws SQLException
  {
    return _resultMetaData;
  }

  public void setRowAvailable(boolean isRowAvailable)
  {
    _isRowAvailable = isRowAvailable;
  }

  public boolean next()
    throws SQLException
  {
    if (_isRowAvailable) {
      _isRowAvailable = _stmt.getConnection().readRow(this);

      if (! _isRowAvailable) {
        if (_resultData != null)
          _resultData.destroy();
      }
    }


    return _isRowAvailable;
  }

  public TempOutputStream getResultStream()
  {
    if (_resultData == null)
      _resultData = new TempOutputStream();
    else
      _resultData.destroy();

    return _resultData;
  }

  public boolean wasNull()
    throws SQLException
  {
    return false;
  }

  public int findColumn(String columnName)
    throws SQLException
  {
    for (int i = 0; i < _columnCount; i++) {
      MysqlColumn column = _columns.get(i);

      if (column.getName().equals(columnName))
        return i + 1;
    }

    return -1;
  }

  public String getString(int columnIndex)
    throws SQLException
  {
    if (columnIndex < 1 || _columnCount < columnIndex)
      throw new SQLException(L.l("{0} is an invalid column [1-{1}]",
                                 columnIndex, _columnCount));

    MysqlColumn column = _columns.get(columnIndex - 1);

    int offset = column.getRowOffset();
    int length = column.getRowLength();

    if (length < 0)
      return null;

    if (_charBuffer == null || _charBuffer.length < length)
      _charBuffer = new char[length];

    _resultData.readAll(offset, _charBuffer, 0, length);

    return new String(_charBuffer, 0, length);
  }

  public int getStringLength(int columnIndex)
    throws SQLException
  {
    if (columnIndex < 1 || _columnCount < columnIndex)
      throw new SQLException(L.l("{0} is an invalid column [1-{1}]",
                                 columnIndex, _columnCount));

    MysqlColumn column = _columns.get(columnIndex - 1);

    return column.getRowLength();
  }

  public void getString(int columnIndex, byte []buffer, int offset)
    throws SQLException
  {
    if (columnIndex < 1 || _columnCount < columnIndex)
      throw new SQLException(L.l("{0} is an invalid column [1-{1}]",
                                 columnIndex, _columnCount));

    MysqlColumn column = _columns.get(columnIndex - 1);

    int columnOffset = column.getRowOffset();
    int columnLength = column.getRowLength();

    // assert(length == columnLength);

    _resultData.readAll(columnOffset, buffer, offset, columnLength);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
