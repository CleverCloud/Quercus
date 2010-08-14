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

package com.caucho.amber;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Represents an Amber query
 */
public interface AmberQuery {
  /**
   * Initialize with the connection.
   */
  public void init(com.caucho.amber.manager.AmberConnection aConn);

  /**
   * Returns the query string.
   */
  public String getQueryString();

  /**
   * Sets the argument with a string
   */
  public void setString(int index, String v);

  /**
   * Sets the argument with a byte
   */
  public void setByte(int index, byte v);

  /**
   * Sets the argument with a short
   */
  public void setShort(int index, short v);

  /**
   * Sets the argument with an integer
   */
  public void setInt(int index, int v);

  /**
   * Sets the argument with a long
   */
  public void setLong(int index, long v);

  /**
   * Sets the argument with a double
   */
  public void setDouble(int index, double v);

  /**
   * Sets the argument with a double
   */
  public void setFloat(int index, float v);

  /**
   * Sets the argument with a timestamp
   */
  public void setTimestamp(int index, java.sql.Timestamp v);

  /**
   * Sets the argument with a date
   */
  public void setDate(int index, java.sql.Date v);

  /**
   * Sets the argument with an object.
   */
  public void setObject(int index, Object v);

  /**
   * Sets the argument with an null.
   */
  public void setNull(int index, int type);

  /**
   * Sets the first result.
   */
  public void setFirstResult(int index);

  /**
   * Sets the maximum number of results.
   */
  public void setMaxResults(int index);

  /**
   * Executes the query returning a result set.
   */
  public ResultSet executeQuery()
    throws SQLException;

  /**
   * Executes the query as an update, returning the rows changed.
   */
  public int executeUpdate()
    throws SQLException;

  /**
   * Sets the cache max age.
   */
  public void setCacheMaxAge(long ms);

  /**
   * Execute the query, returning a single value
   */
  public Object getSingleResult()
    throws SQLException;

  /**
   * Execute the query, returning a list.
   */
  public List<Object> list()
    throws SQLException;

  /**
   * Execute the query, filling a list.
   */
  public void list(List<Object> list)
    throws SQLException;

  /**
   * Execute the query, filling a map.
   */
  public void list(Map<Object, Object> map,
                   Method methodGetMapKey)
    throws SQLException,
           IllegalAccessException,
           InvocationTargetException;

  /**
   * Sets the load on query.
   */
  public void setLoadOnQuery(boolean isLoad);
}
