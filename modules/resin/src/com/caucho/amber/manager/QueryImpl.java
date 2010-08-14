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

package com.caucho.amber.manager;

import com.caucho.amber.AmberException;
import com.caucho.amber.cfg.ColumnResultConfig;
import com.caucho.amber.cfg.EntityResultConfig;
import com.caucho.amber.cfg.FieldResultConfig;
import com.caucho.amber.cfg.SqlResultSetMappingConfig;
import com.caucho.amber.entity.Entity;
import com.caucho.amber.entity.EntityItem;
import com.caucho.amber.expr.AmberExpr;
import com.caucho.amber.expr.ArgExpr;
import com.caucho.amber.expr.LoadEntityExpr;
import com.caucho.amber.query.AbstractQuery;
import com.caucho.amber.query.ResultSetImpl;
import com.caucho.amber.query.SelectQuery;
import com.caucho.amber.query.UserQuery;
import com.caucho.amber.type.CalendarType;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.UtilDateType;
import com.caucho.ejb.EJBExceptionWrapper;
import com.caucho.util.L10N;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.NonUniqueResultException;
import javax.persistence.NoResultException;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The EJB query
 */
public class QueryImpl implements Query {
  private static final L10N L = new L10N(QueryImpl.class);
  private static final Logger log
    = Logger.getLogger(QueryImpl.class.getName());

  private AbstractQuery _query;
  private UserQuery _userQuery;

  private AmberConnection _aConn;
  private int _firstResult;

  private int _currIndex;

  private FlushModeType _flushMode;

  // Constructor queries.
  private Class _primitiveColumns[];

  // Native queries.
  private String _nativeSql;
  private PreparedStatement _nativeStmt;
  private SqlResultSetMappingConfig _sqlResultSetMapping;
  private int _currEntityResult;
  private int _currColumnResult;

  /**
   * Creates a manager instance.
   */
  QueryImpl(AbstractQuery query, AmberConnection aConn)
  {
    _query = query;
    _aConn = aConn;

    _userQuery = new UserQuery(query);
    _userQuery.setSession(_aConn);
  }

  /**
   * Creates a manager instance.
   */
  QueryImpl(AmberConnection aConn)
  {
    _aConn = aConn;
  }

  /**
   * Execute the query and return as a List.
   */
  public List getResultList()
  {
    ResultSet rs = null;

    try {
      // jpa/0y1b
      if (_aConn.isInTransaction())
        _aConn.flush();

      Class constructorClass = null;

      if (isSelectQuery()) {
        if (! isNativeQuery()) {
          SelectQuery selectQuery = (SelectQuery) _query;
          constructorClass = selectQuery.getConstructorClass();
        }
      }
      else
        throw new IllegalStateException(L.l("javax.persistence.Query.getResultList() can only be applied to a SELECT statement"));

      Constructor constructor = null;

      ArrayList results = new ArrayList();

      rs = executeQuery();

      ResultSetMetaData metaData = null;
      int columnCount = -1;

      int n = 0;

      Object row[] = null;

      ArrayList columns = new ArrayList();

      while (rs.next()) {
        Object object = null;

        _currIndex = 1;

        if (n == 0) {
          try {
            metaData = rs.getMetaData();

            if (metaData != null)
              columnCount = metaData.getColumnCount();
          } catch (Exception ex) {
            // Below, we work around if DB is not able
            // to retrieve result set meta data. jpa/0t00
            metaData = null;
          }

          if (columnCount <= 0)
            columnCount = 10000;

          for (int i = 1; i <= columnCount; i++) {

            int columnType = -1;

            try {
              columnType = metaData.getColumnType(i);
            } catch (Exception ex) {
            }

            try {
              if (isNativeQuery()) {
                ArrayList<EntityResultConfig> entityResults
                  = _sqlResultSetMapping.getEntityResults();

                if (_currEntityResult == entityResults.size()) {

                  ArrayList<ColumnResultConfig> columnResults
                    = _sqlResultSetMapping.getColumnResults();

                  if (_currColumnResult == columnResults.size())
                    break;
                }

                object = getInternalNative(rs);
              }
              else {
                object = getInternalObject(rs, columnType);
              }

              columns.add(object);

            } catch (IndexOutOfBoundsException ex1) {
              break;
            }
            // catch (Exception ex2) {
            //  break;
            // }
          }

          n = columns.size();
          row = columns.toArray();

          if (constructorClass != null) {

            _primitiveColumns = new Class[row.length];

            StringBuilder argValues = new StringBuilder();

            try {
              // jpa/11a4, jpa/11a5

              boolean isFirst = true;

              for (int i=0; i < n; i++) {
                if (isFirst)
                  isFirst = false;
                else
                  argValues.append(", ");

                argValues.append(row[i]);
              }

              Constructor ctors[] = constructorClass.getDeclaredConstructors();

              ArrayList<Constructor> validConstructors
                = new ArrayList<Constructor>();

              for (int j=0; j < ctors.length; j++) {

                Class paramTypes[] = ctors[j].getParameterTypes();

                if (paramTypes.length != row.length)
                  continue;

                boolean isValid = true;

                for (int k=0; k < paramTypes.length; k++) {
                  Class columnClass = row[k].getClass();

                  if (! paramTypes[k].isAssignableFrom(columnClass)) {

                    if (! paramTypes[k].isPrimitive()) {
                      isValid = false;
                      break;
                    }

                    Class primitiveType
                      = (Class) columnClass.getDeclaredField("TYPE").get(null);

                    if (paramTypes[k].isAssignableFrom(primitiveType)) {
                      // jpa/11a5
                      _primitiveColumns[k] = primitiveType;
                    }
                    else {
                      isValid = false;
                      break;
                    }
                  }
                }

                if (isValid)
                  validConstructors.add(ctors[j]);
              }

              constructor = validConstructors.get(0);

            } catch (Exception ex) {
              throw error(L.l("Unable to find constructor {0}. Make sure there is a public constructor for the given argument values ({1})", constructorClass.getName(), argValues));
            }
          }
        }
        else {
          row = new Object[n];

          for (int i=0; i < n; i++) {

            int columnType = -1;

            try {
              columnType = metaData.getColumnType(i + 1);
            } catch (Exception ex) {
            }

            if (isNativeQuery())
              row[i] = getInternalNative(rs);
            else
              row[i] = getInternalObject(rs, columnType);
          }
        }

        if (constructor == null) {
          if (n == 1)
            results.add(row[0]);
          else
            results.add(row);
        }
        else {
          try {
            for (int i=0; i < row.length; i++) {
              Class primitiveType = _primitiveColumns[i];

              if (primitiveType == null)
                continue;

              // jpa/11a5
              if (primitiveType == Boolean.TYPE)
                row[i] = ((Boolean) row[i]).booleanValue();
              else if (primitiveType == Byte.TYPE)
                row[i] = ((Byte) row[i]).byteValue();
              else if (primitiveType == Character.TYPE)
                row[i] = ((Character) row[i]).charValue();
              else if (primitiveType == Double.TYPE)
                row[i] = ((Double) row[i]).doubleValue();
              else if (primitiveType == Float.TYPE)
                row[i] = ((Float) row[i]).floatValue();
              else if (primitiveType == Integer.TYPE)
                row[i] = ((Integer) row[i]).intValue();
              else if (primitiveType == Long.TYPE)
                row[i] = ((Long) row[i]).longValue();
              else if (primitiveType == Short.TYPE)
                row[i] = ((Short) row[i]).shortValue();
            }

            object = constructor.newInstance(row);
          } catch (Exception ex) {

            StringBuilder argTypes = new StringBuilder();

            boolean isFirst = true;

            for (int i=0; i < row.length; i++) {

              if (isFirst)
                isFirst = false;
              else
                argTypes.append(", ");

              if (row[i] == null)
                argTypes.append("null");
              else
                argTypes.append(row[i].toString()); // .getClass().getName());
            }

            throw error(L.l("Unable to instantiate {0} with parameters ({1}).", constructorClass.getName(), argTypes));
          }

          results.add(object);
        }
      }

      if (log.isLoggable(Level.FINER)) {
        if (results != null) {
          log.log(Level.FINER, L.l("query result list size: {0}", results.size()));

          java.util.Iterator it = results.iterator();

          while (it.hasNext()) {
            Object o = it.next();

            if (o == null)
              log.log(Level.FINER, L.l("  result entry: null"));
            else
              log.log(Level.FINER, L.l("  result entry: {0}", o.getClass().getName()));
          }
        }
      }

      // jpa/0h19, jpa/1160
      if (! _aConn.isActiveTransaction())
        _aConn.detach();

      return results;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw EJBExceptionWrapper.createRuntime(e);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }
  }

  /**
   * Returns a single result.
   */
  public Object getSingleResult()
  {
    ResultSet rs = null;

    try {
      if (! isSelectQuery())
        throw new IllegalStateException(L.l("javax.persistence.Query.getSingleResult() can only be applied to a SELECT statement"));

      rs = executeQuery();

      Object value = null;

      if (rs.next())
        value = rs.getObject(1);
      else // jpa/1004
        throw new NoResultException("Query returned no results for getSingleResult()");

      // jpa/1005
      if (rs.next())
        throw new NonUniqueResultException("Query returned more than one result for getSingleResult()");

      return value;
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw EJBExceptionWrapper.createRuntime(e);
    }
    finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          log.log(Level.FINE, e.toString(), e);
        }

        // jpa/0h19
        if (! _aConn.isActiveTransaction())
          _aConn.detach();
      }
    }
  }

  /**
   * Execute an update or delete.
   */
  public int executeUpdate()
  {
    try {
      // jpa/1006
      if (isSelectQuery())
        throw new IllegalStateException(L.l("javax.persistence.Query.executeUpdate() cannot be applied to a SELECT statement"));

      if (_flushMode == FlushModeType.AUTO)
        _aConn.flushNoChecks();

      return _userQuery.executeUpdate();
    } catch (Exception e) {
      throw EJBExceptionWrapper.createRuntime(e);
    }
  }

  /**
   * Executes the query returning a result set.
   */
  protected ResultSet executeQuery()
    throws SQLException
  {
    ResultSet rs = null;
    PreparedStatement pstmt = null;

    try {
      if (_flushMode == FlushModeType.AUTO)
        _aConn.flushNoChecks();

      if (_nativeSql == null) {
        // JPA query.
        rs = _userQuery.executeQuery();
      }
      else {
        // Native query stmt is prepared in setNativeSql().
        rs = _nativeStmt.executeQuery();
      }
    }
    catch (SQLException e) {
      if (rs != null)
        rs.close();

      if (pstmt != null)
        _aConn.closeStatement(_nativeSql);

      throw e;
    }

    return rs;
  }

  /**
   * Sets the maximum result returned.
   */
  public Query setMaxResults(int maxResults)
  {
    if (maxResults < 0)
      throw new IllegalArgumentException(L.l("setMaxResults() needs a non-negative argument, '{0}' is not allowed", maxResults));

    _userQuery.setMaxResults(maxResults);

    return this;
  }

  /**
   * Sets the position of the first result.
   */
  public Query setFirstResult(int startPosition)
  {
    if (startPosition < 0)
      throw new IllegalArgumentException("setFirstResult() requires a non-negative argument");

    _userQuery.setFirstResult(startPosition);

    return this;
  }

  /**
   * Sets a hint.
   */
  public Query setHint(String hintName, Object value)
  {
    return this;
  }

  /**
   * Sets a named parameter.
   */
  public Query setParameter(String name, Object value)
  {
    ArrayList<String> mapping = _query.getPreparedMapping();

    int n = mapping.size();

    boolean found = false;

    for (int i=0; i < n; i++) {
      if (mapping.get(i).equals(name)) {
        // jpa/10ee
        ArgExpr args[] = _userQuery.getQuery().getArgList();
        setInternalParameter(args[i], i+1, value);
        found = true;
      }
    }

    if (! found)
      throw new IllegalArgumentException(L.l("Parameter name '{0}' is invalid", name));

    return this;
  }

  /**
   * Sets a date parameter.
   */
  public Query setParameter(String name, Date value, TemporalType type)
  {
    ArrayList<String> mapping = _query.getPreparedMapping();

    int n = mapping.size();

    boolean found = false;

    for (int i=0; i < n; i++) {
      if (mapping.get(i).equals(name)) {
        setParameter(i+1, value, type);
        found = true;
      }
    }

    if (! found)
      throw new IllegalArgumentException(L.l("Parameter name '{0}' is invalid", name));

    return this;
  }

  /**
   * Sets a calendar parameter.
   */
  public Query setParameter(String name, Calendar value, TemporalType type)
  {
    ArrayList<String> mapping = _query.getPreparedMapping();

    int n = mapping.size();

    boolean found = false;

    for (int i=0; i < n; i++) {
      if (mapping.get(i).equals(name)) {
        setParameter(i+1, value, type);
        found = true;
      }
    }

    if (! found)
      throw new IllegalArgumentException(L.l("Parameter name '{0}' is invalid", name));

    return this;
  }

  /**
   * Sets an indexed parameter.
   */
  public Query setParameter(int index, Object value)
  {
    if (_nativeSql == null) {
      // jpa/141h
      ArgExpr arg = checkParameterIndex(index);

      setInternalParameter(arg, index, value);
    }
    else {
      setInternalParameter(_nativeStmt, null, index, value);
    }

    return this;
  }

  /**
   * Sets a date parameter.
   */
  public Query setParameter(int index, Date value, TemporalType type)
  {
    try {

      checkParameterIndex(index);

      if (value == null)
        _userQuery.setNull(index, Types.JAVA_OBJECT);
      else {
        switch (type) {
        case TIME:
          _userQuery.setObject(index, value, UtilDateType.TEMPORAL_TIME_TYPE);
          break;

        case DATE:
          _userQuery.setObject(index, value, UtilDateType.TEMPORAL_DATE_TYPE);
          break;

        default:
          _userQuery.setObject(index, value, UtilDateType.TEMPORAL_TIMESTAMP_TYPE);
        }
      }

      return this;
    } catch (IndexOutOfBoundsException e) {
      throw new IllegalArgumentException(L.l("Parameter index '{0}' is not valid for setParameter()", index));
    }
  }

  /**
   * Sets a calendar parameter.
   */
  public Query setParameter(int index, Calendar value, TemporalType type)
  {
    try {

      checkParameterIndex(index);

      if (value == null)
        _userQuery.setNull(index, Types.JAVA_OBJECT);
      else {
        switch (type) {
        case TIME:
          _userQuery.setObject(index, value, CalendarType.TEMPORAL_TIME_TYPE);
          break;

        case DATE:
          _userQuery.setObject(index, value, CalendarType.TEMPORAL_DATE_TYPE);
          break;

        default:
          _userQuery.setObject(index, value, CalendarType.TEMPORAL_TIMESTAMP_TYPE);
        }
      }

      return this;
    } catch (IndexOutOfBoundsException e) {
      throw new IllegalArgumentException(L.l("Parameter index '{0}' is not valid for setParameter()", index));
    }
  }

  /**
   * Sets the flush mode type.
   */
  public Query setFlushMode(FlushModeType mode)
  {
    _flushMode = mode;

    return this;
  }

  //
  // extensions

  /**
   * Sets an indexed parameter.
   */
  public Query setDouble(int index, double value)
  {
    _userQuery.setDouble(index, value);

    return this;
  }

  /**
   * Sets the sql for native queries.
   */
  protected void setNativeSql(String sql)
    throws SQLException
  {
    _nativeSql = sql;
    _nativeStmt = _aConn.prepareStatement(sql);
  }

  /**
   * Sets the sql result set mapping for native queries.
   */
  protected void setSqlResultSetMapping(SqlResultSetMappingConfig map)
  {
    _sqlResultSetMapping = map;
  }

  /**
   * Returns true for SELECT queries.
   */
  private boolean isSelectQuery()
  {
    if (_query instanceof SelectQuery)
      return true;

    if (isNativeQuery()) {
      String sql = _nativeSql.trim().toUpperCase();

      if (sql.startsWith("SELECT"))
        return true;
    }

    return false;
  }

  /**
   * Returns true for native queries.
   */
  private boolean isNativeQuery()
  {
    return _nativeSql != null;
  }

  /**
   * Creates an error.
   */
  private AmberException error(String msg)
  {
    msg += "\nin \"" + _query.getQueryString() + "\"";

    return new AmberException(msg);
  }

  /**
   * Native queries. Returns the object using the
   * result set mapping.
   */
  private Object getInternalNative(ResultSet rs)
    throws Exception
  {
    // jpa/0y1-

    int oldEntityResult = _currEntityResult;

    ArrayList<EntityResultConfig> entityResults
      = _sqlResultSetMapping.getEntityResults();

    if (oldEntityResult == entityResults.size()) {

      ArrayList<ColumnResultConfig> columnResults
        = _sqlResultSetMapping.getColumnResults();

      int oldColumnResult = _currColumnResult;

      if (_currColumnResult == columnResults.size()) {
        _currColumnResult = 0;
      }

      // jpa/0y19
      if (entityResults.size() == 0
          || oldColumnResult < columnResults.size()) {
        _currColumnResult++;

        if (columnResults.size() > 0) {
          Object object = rs.getObject(_currIndex++);

          return object;
        }
      }

      oldEntityResult = 0;
      _currEntityResult = 0;
    }

    _currEntityResult++;

    EntityResultConfig entityResult = entityResults.get(oldEntityResult);

    String className = entityResult.getEntityClass();

    EntityType entityType = _aConn.getPersistenceUnit().getEntityType(className);

    if (entityType == null)
      throw new IllegalStateException(L.l("Unable to locate entity '{0}' for native query.", className));

    int oldIndex = _currIndex;

    // jpa/0y15 _currIndex++;

    /* jpa/0y14
    EntityItem item = entityType.getHome().findItem(_aConn, rs, oldIndex);

    if (item == null)
      return null;

    Entity entity = item.getEntity();
    */

    int keyLength = entityType.getId().getKeyCount();

    ArrayList<FieldResultConfig> fieldResults
      = entityResult.getFieldResults();

    Entity entity = null;

    int consumed = 0;

    try {
      // jpa/0y14
      entity = (Entity) _aConn.load(className, rs.getObject(oldIndex));

      ArrayList<String> columnNameList = new ArrayList<String>();
      entityType.generateNativeColumnNames(columnNameList);
      String []columnNames = new String[columnNameList.size()];
      columnNameList.toArray(columnNames);
      entity.__caucho_load_native(rs, columnNames);

      // jpa/0y10
      //consumed = entity.__caucho_load(_aConn, rs, oldIndex + keyLength);
    } catch (Exception e) {
      // jpa/0y1a: invalid query.
      throw new IllegalStateException(L.l("Unable to load an entity of class '{0}' using a native query. When mapped to @EntityResult, a native query should select all fields for the corresponding entity in '{1}'", className, _nativeSql));
    }

    // item.setNumberOfLoadingColumns(consumed);

    _currIndex++;

    // jpa/0y12, jpa/0y15
    _currIndex += consumed;

    return entity;
  }

  /**
   * Returns the object using the correct
   * result set getter based on SQL type.
   */
  private Object getInternalObject(ResultSet rs,
                                   int columnType)
    throws Exception
  {
    // jpa/110-, jpa/11a4, and jpa/11z1

    int oldIndex = _currIndex;

    _currIndex++;

    Object object = rs.getObject(oldIndex);

    if (object instanceof Entity) {
      // _currIndex += ((ResultSetImpl) rs).getNumberOfLoadingColumns();

      ArrayList<AmberExpr> resultList
        = ((SelectQuery) _query).getResultList();

      AmberExpr expr = resultList.get(oldIndex-1);

      // jpa/1160
      if (expr instanceof LoadEntityExpr) {
        LoadEntityExpr entityExpr = (LoadEntityExpr) expr;
        joinFetch((ResultSetImpl) rs, entityExpr, (Entity) object);
      }

      return object;
    }

    if (object == null)
      return null;

    switch (columnType) {
    case Types.BIT:
    case Types.BOOLEAN:
      //      try {
      //        object = rs.getInt(oldIndex);
      //      } catch (Exception ex) {
      if (! (object instanceof Boolean))
        object = rs.getBoolean(oldIndex);
      //      }
      break;

    case Types.TINYINT:
      if (! (object instanceof Number))
        object = rs.getByte(oldIndex);
      break;

    case Types.SMALLINT:
      if (! (object instanceof Number))
        object = rs.getShort(oldIndex);
      break;

    case Types.INTEGER:
      if (! (object instanceof Number))
        object = rs.getLong(oldIndex);
      break;

    case Types.DECIMAL:
    case Types.DOUBLE:
    case Types.NUMERIC:
    case Types.REAL:
      if (! (object instanceof Number))
        object = rs.getDouble(oldIndex);
      break;

    case Types.FLOAT:
      if (! (object instanceof Number))
        object = rs.getFloat(oldIndex);
      break;

    // It was fetched with getObject (see top).
    // default:
    //  object = rs.getObject(oldIndex);
    }

    return object;
  }

  private ArgExpr checkParameterIndex(int index)
  {
    // jpa/141h

    ArgExpr args[] = _userQuery.getQuery().getArgList();

    int len = args.length;

    for (int i = 0; i < len; i++) {
      if (args[i].getIndex() == index)
        return args[i];
    }

    throw new IllegalArgumentException(L.l("Parameter index '{0}' is invalid for query {1}", index, _userQuery.getQuery()));
  }

  private void joinFetch(ResultSetImpl rs,
                         LoadEntityExpr entityExpr,
                         Entity entity)
  {
    String property = rs.getJoinFetchMap().get(entityExpr.getExpr());

    EntityType entityType = entity.__caucho_getEntityType();

    Iterator eagerFieldsIterator = null;

    HashSet<String> eagerFieldNames = entityType.getEagerFieldNames();

    if (eagerFieldNames != null)
      eagerFieldsIterator = eagerFieldNames.iterator();

    // XXX: needs to handle field-based access
    if (! entityType.isFieldAccess()) {
      if (property == null)
        if ((eagerFieldsIterator != null) && eagerFieldsIterator.hasNext())
          property = (String) eagerFieldsIterator.next();
    }

    if (property != null) {
      try {
        Class cl = entityType.getInstanceClass();

        do {
          String methodName = "get" +
            Character.toUpperCase(property.charAt(0)) +
            property.substring(1);

          Method method = cl.getDeclaredMethod(methodName, null);

          Object field = method.invoke(entity, null);

          // XXX: for now, invoke the toString() method on
          // the collection to fetch all the objects (join fetch).

          if (field == null) {
            try {
              methodName = "__caucho_item_" + methodName;

              method = cl.getDeclaredMethod(methodName, new Class[] {AmberConnection.class});

              field = method.invoke(entity, _aConn);
            } catch (Exception ex) {
            }
          }

          if (field != null) {
            Class fieldClass = field.getClass();

            method = fieldClass.getMethod("toString", null);

            method.invoke(field, null);
          }

          property = null;

          if ((eagerFieldsIterator != null) && (eagerFieldsIterator.hasNext()))
            property = (String) eagerFieldsIterator.next();
        }
        while (property != null);

      } catch (NoSuchMethodException e) {
        log.log(Level.FINER, e.toString(), e);
      } catch (IllegalAccessException e) {
        log.log(Level.FINER, e.toString(), e);
      } catch (java.lang.reflect.InvocationTargetException e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
  }

  /**
   * Sets an indexed parameter.
   */
  private Query setInternalParameter(ArgExpr arg,
                                     int index,
                                     Object value)
  {
    try {
      if (value == null) {
        _userQuery.setNull(index, java.sql.Types.JAVA_OBJECT);
        return this;
      }

      // jpa/141i
      boolean valueIsNumber = value instanceof Number;

      if (valueIsNumber) {
        // jpa/0w20: type is null.
        if (arg.getType() != null) {
          boolean typeIsNumber = arg.getType().isNumeric();

          if (! typeIsNumber)
            throw new IllegalArgumentException(L.l("Type mismatch for parameter index '{0}'. Value '{1}' for type '{2}' is not valid in query '{3}'", index, value, arg.getType().getClass().getName(), _userQuery.getQuery().getSQL()));
        }
      }

      setInternalParameter(null, _userQuery, index, value);

      return this;
    } catch (IndexOutOfBoundsException e) {
      log.log(Level.FINE, e.toString(), e);

      throw new IllegalArgumentException(L.l("Parameter index '{0}' is not valid for setParameter()", index));
    }
  }

  /**
   * Sets an indexed parameter.
   */
  private static void setInternalParameter(PreparedStatement pstmt,
                                           UserQuery userQuery,
                                           int index,
                                           Object value)
  {
    try {
      if (value instanceof Byte) {
        byte arg = ((Byte) value).byteValue();

        if (pstmt == null)
          userQuery.setByte(index, arg);
        else
          pstmt.setByte(index, arg);
      }
      else if (value instanceof Short) {
        short arg = ((Short) value).shortValue();

        if (pstmt == null)
          userQuery.setShort(index, arg);
        else
          pstmt.setShort(index, arg);
      }
      else if (value instanceof Integer) {
        int arg = ((Integer) value).intValue();

        if (pstmt == null)
          userQuery.setInt(index, arg);
        else
          pstmt.setInt(index, arg);
      }
      else if (value instanceof Long) {
        long arg = ((Long) value).longValue();

        if (pstmt == null)
          userQuery.setLong(index, arg);
        else
          pstmt.setLong(index, arg);
      }
      else if (value instanceof Float) {
        float arg = ((Float) value).floatValue();

        if (pstmt == null)
          userQuery.setFloat(index, arg);
        else
          pstmt.setFloat(index, arg);
      }
      else if (value instanceof Double) { // jpa/141a
        double arg = ((Double) value).doubleValue();

        if (pstmt == null)
          userQuery.setDouble(index, arg);
        else
          pstmt.setDouble(index, arg);
      }
      else if (value instanceof Character) {
        if (pstmt == null)
          userQuery.setString(index, value.toString());
        else
          pstmt.setString(index, value.toString());
      }
      else if (value instanceof Entity) {
        // XXX: needs to handle Compound PK

        Object pk = ((Entity) value).__caucho_getPrimaryKey();

        if (pstmt == null)
          userQuery.setObject(index, pk);
        else
          pstmt.setObject(index, pk);
      }
      else {
        if (pstmt == null)
          userQuery.setObject(index, value);
        else
          pstmt.setObject(index, value);
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      throw new IllegalArgumentException(L.l("Parameter index '{0}' is not valid for setParameter()", index));
    }
  }

  /**
   * The maximum number of results to retrieve.
   *
   * @Since JPA 2.0
   */
  public int getMaxResults()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * The first to retrieve.
   *
   * @Since JPA 2.0
   */
  public int getFirstResult()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the implementation-specific hints
   *
   * @Since JPA 2.0
   */
  public Map getHints()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the supported hints
   *
   * @Since JPA 2.0
   */
  public Set<String> getSupportedHints()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the named parameters as a map
   *
   * @since JPA 2.0
   */
  public Map getNamedParameters()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the positional parameters as a list
   *
   * @since JPA 2.0
   */
  public List getPositionalParameters()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Gets the flush type.
   *
   * @since JPA 2.0
   */
  public FlushModeType getFlushMode()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the lock type.
   *
   * @since JPA 2.0
   */
  public Query setLockMode(LockModeType lockMode)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Gets the lock type.
   *
   * @since JPA 2.0
   */
  public LockModeType getLockMode()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#getParameter(java.lang.String)
   */
  @Override
  public Parameter<?> getParameter(String name)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#getParameter(java.lang.String, java.lang.Class)
   */
  @Override
  public <T> Parameter<T> getParameter(String name, Class<T> type)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#getParameter(int)
   */
  @Override
  public Parameter<?> getParameter(int pos)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#getParameter(int, java.lang.Class)
   */
  @Override
  public <T> Parameter<T> getParameter(int position, Class<T> type)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#getParameterValue(javax.persistence.Parameter)
   */
  @Override
  public <T> T getParameterValue(Parameter<T> param)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#getParameterValue(java.lang.String)
   */
  @Override
  public Object getParameterValue(String name)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#getParameterValue(int)
   */
  @Override
  public Object getParameterValue(int position)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#getParameters()
   */
  @Override
  public Set<Parameter<?>> getParameters()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#isBound(javax.persistence.Parameter)
   */
  @Override
  public boolean isBound(Parameter<?> param)
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#setParameter(javax.persistence.Parameter, java.lang.Object)
   */
  @Override
  public <T> Query setParameter(Parameter<T> param, T value)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#setParameter(javax.persistence.Parameter, java.util.Calendar, javax.persistence.TemporalType)
   */
  @Override
  public Query setParameter(Parameter<Calendar> param, Calendar date,
                            TemporalType type)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#setParameter(javax.persistence.Parameter, java.util.Date, javax.persistence.TemporalType)
   */
  @Override
  public Query setParameter(Parameter<Calendar> param, Date date,
                            TemporalType type)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.Query#unwrap(java.lang.Class)
   */
  @Override
  public <T> T unwrap(Class<T> cl)
  {
    // TODO Auto-generated method stub
    return null;
  }
}
