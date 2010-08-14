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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.query;

import com.caucho.amber.expr.AmberExpr;
import com.caucho.util.Alarm;

import java.lang.ref.SoftReference;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * The JDBC statement implementation.
 */
public class ResultSetCacheChunk {
  public static final int CACHE_CHUNK_SIZE = 64;

  private SelectQuery _query;

  private ArrayList<FromItem> _fromList;
  private ArrayList<AmberExpr> _resultList;

  private int _startRow;

  private final ArrayList<Object[]> _results = new ArrayList<Object[]>();

  private SoftReference<ResultSetCacheChunk> _next;
  private boolean _isLast;

  private long _expireTime;

  public ResultSetCacheChunk()
  {
  }

  public ResultSetCacheChunk(ResultSetCacheChunk prev)
  {
    _startRow = prev._startRow + CACHE_CHUNK_SIZE;
    _query = prev._query;
    _fromList = prev._fromList;
    _resultList = prev._resultList;

    _expireTime = prev._expireTime;
  }

  /**
   * Sets the query.
   */
  public void setQuery(SelectQuery query)
  {
    _query = query;

    _fromList = query.getFromList();
    _resultList = query.getResultList();

    _expireTime = Alarm.getCurrentTime() + query.getCacheMaxAge();
  }

  /**
   * Gets the query.
   */
  public SelectQuery getQuery()
  {
    return _query;
  }

  /**
   * Returns the expire time.
   */
  public long getExpireTime()
  {
    return _expireTime;
  }

  /**
   * Return true if the chunk is still valid.
   */
  public boolean isValid()
  {
    return Alarm.getCurrentTime() <= _expireTime;
  }

  /**
   * Invalidates the chunk.
   */
  public void invalidate()
  {
    _expireTime = 0;
    _next = null;
  }

  /**
   * Invalidates the chunk based on a table and key.
   */
  public boolean invalidate(String table, Object key)
  {
    if (getQuery().invalidateTable(table)) {
      invalidate();

      return true;
    }
    else
      return false;
  }

  /**
   * Returns the number of rows.
   */
  public int getRowCount()
  {
    return _startRow + _results.size();
  }

  /**
   * Adds a new row.
   */
  public void newRow()
  {
    _results.add(new Object[_resultList.size()]);
  }

  /**
   * Sets a row value.
   */
  public void setValue(int row, int column, Object value)
  {
    _results.get(row % CACHE_CHUNK_SIZE)[column] = value;
  }

  /**
   * Gets the next chunk
   */
  public ResultSetCacheChunk getNext()
  {
    SoftReference<ResultSetCacheChunk> nextRef = _next;

    if (nextRef != null)
      return nextRef.get();
    else
      return null;
  }

  /**
   * Gets the next chunk
   */
  public void setNext(ResultSetCacheChunk next)
  {
    _next = new SoftReference<ResultSetCacheChunk>(next);
  }

  /**
   * Sets true for the last.
   */
  public void setLast(boolean isLast)
  {
    _isLast = isLast;
  }

  /**
   * True for the last.
   */
  public boolean isLast()
  {
    return _isLast;
  }

  /**
   * Returns true if the last column read was null.
   */
  public boolean isNull(int row, int column)
  {
    return getObject(row, column) == null;
  }

  /**
   * Returns the boolean value for the column.
   */
  public boolean getBoolean(int row, int column)
    throws SQLException
  {
    Object object = getObject(row, column);

    if (object instanceof Boolean)
      return ((Boolean) object).booleanValue();
    else if (object instanceof Number)
      return ((Number) object).intValue() != 0;
    else if (object instanceof String) {
      String s = (String) object;

      return s.startsWith("t") || s.startsWith("y");
    }
    else
      return object != null;
  }

  /**
   * Returns the byte value for the column.
   */
  public byte getByte(int row, int column)
    throws SQLException
  {
    Object object = getObject(row, column);

    if (object instanceof Byte)
      return ((Byte) object).byteValue();
    else if (object instanceof String)
      return Byte.parseByte((String) object);
    else if (object == null)
      return 0;
    else
      return Byte.parseByte(String.valueOf(object));
  }

  /**
   * Returns the int value for the column.
   */
  public int getInt(int row, int column)
    throws SQLException
  {
    Object object = getObject(row, column);

    if (object instanceof Number)
      return ((Number) object).intValue();
    else if (object instanceof String)
      return Integer.parseInt((String) object);
    else if (object == null)
      return 0;
    else
      return Integer.parseInt(String.valueOf(object));
  }

  /**
   * Returns the short value for the column.
   */
  public short getShort(int row, int column)
    throws SQLException
  {
    Object object = getObject(row, column);

    if (object instanceof Number)
      return ((Number) object).shortValue();
    else if (object instanceof String)
      return Short.parseShort((String) object);
    else if (object == null)
      return 0;
    else
      return Short.parseShort(String.valueOf(object));
  }

  /**
   * Returns the long value for the column.
   */
  public long getLong(int row, int column)
    throws SQLException
  {
    Object object = getObject(row, column);

    if (object instanceof Number)
      return ((Number) object).longValue();
    else if (object instanceof String)
      return Long.parseLong((String) object);
    else if (object instanceof java.sql.Date)
      return ((java.sql.Date) object).getTime();
    else if (object == null)
      return 0;
    else
      return Long.parseLong(String.valueOf(object));
  }

  /**
   * Returns the double value for the column.
   */
  public double getDouble(int row, int column)
    throws SQLException
  {
    Object object = getObject(row, column);

    if (object instanceof Number)
      return ((Number) object).doubleValue();
    else if (object instanceof String)
      return Double.parseDouble((String) object);
    else if (object == null)
      return 0;
    else
      return Double.parseDouble(String.valueOf(object));
  }

  /**
   * Returns the float value for the column.
   */
  public float getFloat(int row, int column)
    throws SQLException
  {
    Object object = getObject(row, column);

    if (object instanceof Number)
      return ((Number) object).floatValue();
    else if (object instanceof String)
      return Float.parseFloat((String) object);
    else if (object == null)
      return 0;
    else
      return Float.parseFloat(String.valueOf(object));
  }

  /**
   * Returns the string value for the column.
   */
  public String getString(int row, int column)
    throws SQLException
  {
    Object object = getObject(row, column);

    if (object instanceof String)
      return (String) object;
    else if (object == null)
      return null;
    else
      return String.valueOf(object);
  }

  /**
   * Returns the value.
   */
  public final Object getObject(int row, int column)
  {
    return _results.get(row % CACHE_CHUNK_SIZE)[column];
  }
}
