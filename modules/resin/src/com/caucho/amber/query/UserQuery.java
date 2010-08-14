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

package com.caucho.amber.query;

import com.caucho.amber.AmberQuery;
import com.caucho.amber.expr.ArgExpr;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.type.*;
import com.caucho.jdbc.JdbcMetaData;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents the application's view of the query.
 */
public class UserQuery implements AmberQuery {
  private static final int LIMIT_INF = Integer.MAX_VALUE / 2;
  
  private AmberConnection _aConn;
  private AbstractQuery _query;
  private ResultSetImpl _rs;

  private QueryCacheKey _cacheKey;

  private AmberType []_argTypes;
  private Object []_argValues;
  private int _argLength = 0;

  private int _firstResult = 0;
  private int _maxResults = -1;

  private long _cacheMaxAge;

  private boolean _copyOnLoad = true;
  private boolean _loadOnQuery;

  public UserQuery(AbstractQuery query)
  {
    _query = query;

    ArgExpr []argList = query.getArgList();

    _argTypes = new AmberType[argList.length];
    _argValues = new Object[argList.length];

    _argLength = argList.length;

    if (query instanceof SelectQuery) {
      SelectQuery select = (SelectQuery) query;

      if (select.getOffset() >= 0)
        _firstResult = select.getOffset();
      
      if (select.getLimit() >= 0)
        _maxResults = select.getLimit();
    }
  }

  /**
   * Returns the query string.
   */
  public String getQueryString()
  {
    return _query.getQueryString();
  }

  public void init(AmberConnection aConn)
  {
    setSession(aConn);
  }

  public void setSession(AmberConnection aConn)
  {
    _aConn = aConn;
  }

  public AmberConnection getSession()
  {
    return _aConn;
  }

  public AmberConnection getConnection()
  {
    return _aConn;
  }

  /**
   * Sets true for load-on-query.
   */
  public void setLoadOnQuery(boolean loadOnQuery)
  {
    _loadOnQuery = loadOnQuery;
  }

  /**
   * Returns the compiled query.
   */
  public AbstractQuery getQuery()
  {
    return _query;
  }

  /**
   * Returns the arg type array.
   */
  AmberType []getArgTypes()
  {
    return _argTypes;
  }

  /**
   * Returns the arg values
   */
  Object []getArgValues()
  {
    return _argValues;
  }

  /**
   * Returns the arg length
   */
  int getArgLength()
  {
    return _argLength;
  }

  /**
   * Sets the argument with a string
   */
  public void setString(int index, String v)
  {
    _argTypes[index - 1] = StringType.create();
    _argValues[index - 1] = v;
    _argLength = index;
  }

  /**
   * Sets the argument with a byte
   */
  public void setByte(int index, byte v)
  {
    _argTypes[index - 1] = ByteType.create();
    _argValues[index - 1] = new Integer(v);
    _argLength = index;
  }

  /**
   * Sets the argument with a short
   */
  public void setShort(int index, short v)
  {
    _argTypes[index - 1] = ShortType.create();
    _argValues[index - 1] = new Integer(v);
    _argLength = index;
  }

  /**
   * Sets the argument with an int
   */
  public void setInt(int index, int v)
  {
    _argTypes[index - 1] = IntegerType.create();
    _argValues[index - 1] = new Integer(v);
    _argLength = index;
  }

  /**
   * Sets the argument with a string
   */
  public void setLong(int index, long v)
  {
    _argTypes[index - 1] = LongType.create();
    _argValues[index - 1] = new Long(v);
    _argLength = index;
  }

  /**
   * Sets the argument with a double
   */
  public void setDouble(int index, double v)
  {
    _argTypes[index - 1] = DoubleType.create();
    _argValues[index - 1] = new Double(v);
    _argLength = index;
  }

  /**
   * Sets the argument with a double
   */
  public void setFloat(int index, float v)
  {
    _argTypes[index - 1] = FloatType.create();
    _argValues[index - 1] = new Float(v);
    _argLength = index;
  }

  /**
   * Sets the argument with a timestamp
   */
  public void setTimestamp(int index, java.sql.Timestamp v)
  {
    _argTypes[index - 1] = SqlTimestampType.create();
    _argValues[index - 1] = v;
    _argLength = index;
  }

  /**
   * Sets the argument with a date
   */
  public void setDate(int index, java.sql.Date v)
  {
    _argTypes[index - 1] = SqlDateType.create();
    _argValues[index - 1] = v;
    _argLength = index;
  }

  /**
   * Sets the argument with an object.
   */
  public void setObject(int index, Object v)
  {
    _argTypes[index - 1] = ObjectType.create();
    _argValues[index - 1] = v;
    _argLength = index;
  }

  /**
   * Sets the argument with an object and its Amber type.
   */
  public void setObject(int index, Object v, AmberType type)
  {
    _argTypes[index - 1] = type;
    _argValues[index - 1] = v;
    _argLength = index;
  }

  /**
   * Sets the argument with a null
   */
  public void setNull(int index, int v)
  {
    _argTypes[index - 1] = StringType.create();
    _argValues[index - 1] = null;
    _argLength = index;
  }

  /**
   * Sets the first result.
   */
  public void setFirstResult(int index)
  {
    _firstResult = index;
  }

  /**
   * Sets the maximum number of results.
   */
  public void setMaxResults(int index)
  {
    _maxResults = index;
  }

  /**
   * Returns the max results.
   */
  public int getMaxResults()
  {
    return _maxResults;
  }

  /**
   * Executes the query returning a result set.
   */
  public ResultSet executeQuery()
    throws SQLException
  {
    _aConn.flushNoChecks();

    if (_rs == null)
      _rs = new ResultSetImpl();

    SelectQuery query = (SelectQuery) _query;
    
    int firstResult = _firstResult;
    int maxResults = _maxResults;

    _rs.setQuery(query);
    _rs.setSession(_aConn);
    _rs.setFirstResult(firstResult);
    _rs.setMaxResults(maxResults);
    _rs.setRow(0);

    int chunkSize = _aConn.getCacheChunkSize();
    boolean isCacheable;

    if (chunkSize <= _firstResult)
      isCacheable = false;
    else if (_aConn.isActiveTransaction() && ! query.isTableReadOnly())
      isCacheable = false;
    else if (! query.isCacheable())
      isCacheable = false;
    else
      isCacheable = true;

    ResultSetCacheChunk cacheChunk = null;
    ResultSetMetaData metaData = null;

    if (isCacheable) {
      int row = 0;

      cacheChunk = _aConn.getQueryCacheChunk(query.getSQL(), _argValues, row);
      metaData = _aConn.getQueryMetaData();

      if (cacheChunk == null) {
        cacheChunk = fillCache(query);
      }

      // all data returned in chunk
      firstResult = cacheChunk.getRowCount();
      if (cacheChunk.getRowCount() < chunkSize)
        maxResults = 0;
      else if (maxResults < 0)
        maxResults = LIMIT_INF;
      else
        maxResults -= firstResult - _firstResult;

      _rs.setCacheChunk(cacheChunk, metaData);
      _rs.setUserQuery(this);
    }
    else if (maxResults < 0)
      maxResults = LIMIT_INF;

    if (maxResults > 0) {
      ResultSet rs;

      rs = executeQuery(firstResult, maxResults);

      metaData = rs.getMetaData();

      _rs.setResultSet(rs, metaData);
      _rs.setRow(_firstResult);
    }

    _rs.init();

    return _rs;
  }

  private ResultSetCacheChunk fillCache(SelectQuery query)
    throws SQLException
  {
    int chunkSize = _aConn.getCacheChunkSize();
    
    ResultSet rs = executeQuery(0, chunkSize);
      
    ResultSetMetaData metaData = rs.getMetaData();

    _rs.setResultSet(rs, metaData);

    ResultSetCacheChunk cacheChunk = new ResultSetCacheChunk();
    cacheChunk.setQuery(query);

    _rs.fillCacheChunk(cacheChunk);

    _aConn.putQueryCacheChunk(query.getSQL(), _argValues, 0,
                              cacheChunk, metaData);

    return cacheChunk;
  }

  /**
   * Executes the query.
   */
  ResultSet executeQuery(int firstResults, int maxResults)
    throws SQLException
  {
    String sql = _query.getSQL();

    int row = 0;

    if (maxResults > 0 && maxResults < LIMIT_INF) {
      JdbcMetaData metaData = _aConn.getAmberManager().getMetaData();

      // XXX: should limit meta-data as well?
      // jps/1431
      if (metaData.isLimitOffset()) {
        sql = metaData.limit(sql, firstResults, maxResults);
        row = firstResults;
      }
      else
        sql = metaData.limit(sql, 0, firstResults + maxResults);
    }

    PreparedStatement pstmt = _aConn.prepareStatement(sql);
    ArgExpr []args = _query.getArgList();

    if (args.length > 0)
      pstmt.clearParameters();

    for (int i = 0; i < args.length; i++) {
      args[i].setParameter(pstmt, i + 1, _argTypes, _argValues);
    }

    ResultSet rs = pstmt.executeQuery();

    // jpa/1431
    for (int i = row; i < firstResults && rs.next(); i++) {
    }

    return rs;
  }

  /**
   * Executes the query returning a result set.
   */
  public int executeUpdate()
    throws SQLException
  {
    _aConn.flushNoChecks();
    // XXX: sync

    String sql = _query.getSQL();

    PreparedStatement pstmt = _aConn.prepareStatement(sql);
    ArgExpr []args = _query.getArgList();

    if (args.length > 0)
      pstmt.clearParameters();

    for (int i = 0; i < args.length; i++) {
      args[i].setParameter(pstmt, i + 1, _argTypes, _argValues);
    }

    _query.prepare(this, _aConn);

    int count = pstmt.executeUpdate();

    if (count != 0)
      _query.complete(this, _aConn);

    return count;
  }

  /**
   * Sets the cache max age for the query.
   */
  public void setCacheMaxAge(long ms)
  {
    _cacheMaxAge = ms;
  }

  /**
   * Executes the query, returning a list.
   */
  public List<Object> list()
    throws SQLException
  {
    ArrayList<Object> list = new ArrayList<Object>();

    list(list);

    return list;
  }

  /**
   * Executes the query returning the single result.
   */
  public Object getSingleResult()
    throws SQLException
  {
    SelectQuery query = (SelectQuery) _query;
    ResultSet rs = null;

    _aConn.pushDepth();
    try {
      rs = executeQuery();
      if (rs.next())
        return rs.getObject(1);

      return null;
    } catch (SQLException e) {
      throw e;
    } finally {
      _aConn.popDepth();

      if (rs != null)
        rs.close();
    }
  }

  /**
   * Executes the query, filling the list.
   */
  public void list(List<Object> list)
    throws SQLException
  {
    SelectQuery query = (SelectQuery) _query;
    ResultSet rs = null;

    _aConn.pushDepth();
    try {
      rs = executeQuery();

      int tupleCount = query.getResultCount();

      while (rs.next()) {
        if (tupleCount == 1) {
          Object value = rs.getObject(1);

          list.add(value);
        }
        else {
          Object []values = new Object[tupleCount];

          for (int i = 0; i < tupleCount; i++) {
            values[i] = rs.getObject(i + 1);
          }

          list.add(values);
        }
      }
    } catch (SQLException e) {
      throw e;
    } finally {
      _aConn.popDepth();

      if (rs != null)
        rs.close();
    }
  }

  /**
   * Executes the query, filling the map.
   */
  public void list(Map<Object,Object> map,
                   Method methodGetMapKey)
    throws SQLException,
           IllegalAccessException,
           InvocationTargetException
  {
    SelectQuery query = (SelectQuery) _query;

    ResultSet rs = null;

    _aConn.pushDepth();
    try {
      rs = executeQuery();

      int tupleCount = query.getResultCount();

      while (rs.next()) {
        if (tupleCount == 1) {
          Object value = rs.getObject(1);

          com.caucho.amber.entity.Entity entity;
          entity = (com.caucho.amber.entity.Entity) value;

          Object mapKey;
          if (methodGetMapKey == null)
            mapKey = entity.__caucho_getPrimaryKey();
          else
            mapKey = methodGetMapKey.invoke(entity, null);

          map.put(mapKey, value);
        }
        else {
          Object []values = new Object[tupleCount];

          for (int i = 0; i < tupleCount; i++) {
            values[i] = rs.getObject(i + 1);
          }

          // XXX: for now, assume 1st column is the key.
          Object mapKey = values[0];
          map.put(mapKey, values);
        }
      }
    } catch (SQLException e) {
      throw e;
    } finally {
      _aConn.popDepth();

      if (rs != null)
        rs.close();
    }
  }

  public String toString()
  {
    return "UserQuery[" + _query.getQueryString() + "]";
  }
}
