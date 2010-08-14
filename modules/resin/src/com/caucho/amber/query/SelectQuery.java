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

import com.caucho.amber.entity.AmberEntityHome;
import com.caucho.amber.expr.AmberExpr;
import com.caucho.amber.expr.AndExpr;
import com.caucho.amber.expr.JoinExpr;
import com.caucho.amber.expr.KeyColumnExpr;
import com.caucho.amber.expr.LoadEntityExpr;
import com.caucho.amber.expr.ManyToOneJoinExpr;
import com.caucho.amber.table.AmberColumn;
import com.caucho.amber.type.EntityType;
import com.caucho.amber.type.SubEntityType;
import com.caucho.amber.type.AmberType;
import com.caucho.jdbc.JdbcMetaData;
import com.caucho.util.CharBuffer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;


/**
 * Represents an Amber select query
 */
public class SelectQuery extends AbstractQuery {
  private AbstractQuery _parentQuery;

  private boolean _isDistinct;

  private ArrayList<AmberExpr> _resultList;

  private ArrayList<AmberExpr> _orderList;
  private ArrayList<Boolean> _ascList;

  private ArrayList<AmberExpr> _groupList;

  private int _offset = -1;
  private int _limit = -1;

  private Map<AmberExpr, String> _joinFetchMap;

  private String _sql;

  // SELECT NEW
  private Class _constructorClass;

  private boolean _isTableReadOnly = false;
  private long _cacheTimeout = -1;

  private boolean _hasFrom = true;

  SelectQuery(String query, JdbcMetaData metaData)
  {
    super(query, metaData);
  }

  /**
   * Gets the (join) fetch map.
   */
  Map<AmberExpr, String> getJoinFetchMap()
  {
    return _joinFetchMap;
  }

  /**
   * Sets the constructor class for SELECT NEW.
   */
  void setConstructorClass(Class cl)
  {
    _constructorClass = cl;
  }

  /**
   * Gets the constructor class for SELECT NEW.
   */
  public Class getConstructorClass()
  {
    return _constructorClass;
  }

  /**
   * Sets whether the query has a FROM clause or not.
   */
  void setHasFrom(boolean hasFrom)
  {
    // The spec. is not clear about the FROM clause for
    // Current_Date/Time/Timestamp functions.

    _hasFrom = hasFrom;
  }

  /**
   * Sets the parent query.
   */
  void setParentQuery(AbstractQuery parent)
  {
    _parentQuery = parent;

    // jpa/0g40
    if (parent != null) {
      // jpa/1231
      parent.setHasSubQuery(true);
    }
  }

  /**
   * Gets the parent query.
   */
  public AbstractQuery getParentQuery()
  {
    return _parentQuery;
  }

  /**
   * Sets true if distinct.
   */
  void setDistinct(boolean isDistinct)
  {
    _isDistinct = isDistinct;
  }

  /**
   * Sets the result list.
   */
  void setResultList(ArrayList<AmberExpr> resultList)
  {
    _resultList = resultList;
  }

  /**
   * Returns the result list.
   */
  public ArrayList<AmberExpr> getResultList()
  {
    return _resultList;
  }

  /**
   * Returns the result type.
   */
  int getResultCount()
  {
    return _resultList.size();
  }

  /**
   * Returns the result type.
   */
  AmberType getResultType(int index)
  {
    AmberExpr expr = _resultList.get(index);

    return expr.getType();
  }

  /**
   * Sets the having expression
   */
  void setHaving(AmberExpr expr)
  {
    _having = expr;
  }

  /**
   * Sets the where expression
   */
  void setWhere(AmberExpr expr)
  {
    _where = expr;
  }

  /**
   * Sets the group by list.
   */
  void setGroupList(ArrayList<AmberExpr> groupList)
  {
    _groupList = groupList;
  }

  /**
   * Sets the (join) fetch map.
   */
  void setJoinFetchMap(Map<AmberExpr, String> joinFetchMap)
  {
    _joinFetchMap = joinFetchMap;
  }

  /**
   * Sets the order by list.
   */
  void setOrderList(ArrayList<AmberExpr> orderList,
                    ArrayList<Boolean> ascList)
  {
    _orderList = orderList;
    _ascList = ascList;
  }

  /**
   * Returns the id load sql
   */
  public String getSQL()
  {
    return _sql;
  }

  /**
   * Returns the expire time.
   */
  public long getCacheMaxAge()
  {
    return _cacheTimeout;
  }

  /**
   * Returns true for cacheable queries.
   */
  public boolean isCacheable()
  {
    return 100L <= _cacheTimeout;
  }

  /**
   * Are the tables read-only
   */
  public boolean isTableReadOnly()
  {
    return _isTableReadOnly;
  }

  /**
   * Sets the OFFSET value.
   */
  public void setOffset(int offset)
  {
    _offset = offset;
  }

  /**
   * Gets the OFFSET value.
   */
  public int getOffset()
  {
    return _offset;
  }

  /**
   * Sets the LIMIT value.
   */
  public void setLimit(int limit)
  {
    _limit = limit;
  }

  /**
   * Gets the LIMIT value.
   */
  public int getLimit()
  {
    return _limit;
  }

  /**
   * initializes the query.
   */
  void init()
    throws QueryParseException
  {
    super.init();

    _cacheTimeout = Long.MAX_VALUE / 2;
    _isTableReadOnly = true;
    for (FromItem item : _fromList) {
      EntityType type = item.getTableType();

      if (type != null) {
        long timeout = type.getCacheTimeout();

        if (timeout < _cacheTimeout)
          _cacheTimeout = timeout;

        if (! type.isReadOnly())
          _isTableReadOnly = false;
      }
      else {
        // XXX: kills the cache?
        _isTableReadOnly = false;
      }
    }

    _sql = generateLoadSQL();
  }

  /**
   * Returns true if the item must have at least one entry in the database.
   */
  public boolean exists(FromItem item)
  {
    // jpa/0h1b vs jpa/114g
    if (_where != null && _where.exists(item)) {
      return true;
    }

    if (_orderList != null) {
      for (AmberExpr orderBy : _orderList) {
        // jpa/1110
        if (orderBy instanceof KeyColumnExpr
            && orderBy.usesFrom(item, AmberExpr.IS_INNER_JOIN, false))
          return true;
      }
    }

    if (_groupList != null) {
      for (AmberExpr groupBy : _groupList) {
        if (groupBy instanceof KeyColumnExpr
            && groupBy.usesFrom(item, AmberExpr.IS_INNER_JOIN, false))
          return true;
      }
    }

    if (_having != null && _having.exists(item))
      return true;

    return false;
  }

  /**
   * Returns true if the from item is used by the query.
   */
  public boolean usesFrom(FromItem item, int type)
  {
    for (int j = 0; j < _resultList.size(); j++) {
      AmberExpr result = _resultList.get(j);

      if (result.usesFrom(item, type)) {
        return true;
      }
    }

    if (_where != null && _where.usesFrom(item, type)) {
      return true;
    }

    if (_orderList != null) {
      for (int j = 0; j < _orderList.size(); j++) {
        AmberExpr order = _orderList.get(j);

        if (order.usesFrom(item, type)) {
          return true;
        }
      }
    }

    // jpa/1123
    if (_groupList != null) {
      for (int j = 0; j < _groupList.size(); j++) {
        AmberExpr group = _groupList.get(j);

        // jpa/1123 if (group.usesFrom(item, type)) {
        if (group.usesFrom(item, AmberExpr.IS_INNER_JOIN)) {
          return true;
        }
      }

      if (_having != null && _having.usesFrom(item, type))
        return true;
    }

    return false;
  }

  void replaceJoin(JoinExpr join)
  {
    for (int i = 0; i < _resultList.size(); i++) {
      AmberExpr result = _resultList.get(i);

      _resultList.set(i, result.replaceJoin(join));
    }

    if (_where != null) {
      _where = _where.replaceJoin(join);
    }

    if (_orderList != null) {
      for (int i = 0; i < _orderList.size(); i++) {
        AmberExpr order = _orderList.get(i);

        _orderList.set(i, order.replaceJoin(join));
      }
    }
  }

  public String generateLoadSQL()
  {
    return generateLoadSQL(true);
  }

  /**
   * Generates the load SQL.
   *
   * @param fullSelect true if the load entity expressions
   *                   should be fully loaded for all entity
   *                   fields. Otherwise, only the entity id
   *                   will be loaded: select o.id from ...
   *                   It is implemented to optimize the SQL
   *                   and allow for databases that only
   *                   support single columns in subqueries.
   *                   Derby is an example. An additional
   *                   condition to generate only the o.id
   *                   is the absence of group by. If there
   *                   is a group by the full select will
   *                   always be generated.
   *
   *                   See also com.caucho.amber.expr.ExistsExpr
   *
   * @return the load SQL.
   */
  public String generateLoadSQL(boolean fullSelect)
  {
    CharBuffer cb = CharBuffer.allocate();

    cb.append("select ");

    if (_isDistinct)
      cb.append(" distinct ");

    for (int i = 0; i < _resultList.size(); i++) {
      if (i != 0)
        cb.append(", ");

      AmberExpr expr = _resultList.get(i);

      if (_groupList == null && expr instanceof LoadEntityExpr)
        ((LoadEntityExpr) expr).generateSelect(cb, fullSelect);
      else
        expr.generateSelect(cb);
    }

    if (_hasFrom)
      cb.append(" from ");

    // jpa/114f: reorder from list for left outer join
    for (int i = 1; i < _fromList.size(); i++) {
      FromItem item = _fromList.get(i);

      if (item.isOuterJoin()) {
        JoinExpr join = item.getJoinExpr();

        if (join == null)
          continue;

        FromItem parent = join.getJoinParent();

        int index = _fromList.indexOf(parent);

        if (index < 0)
          continue;

        _fromList.remove(i);

        if (index < i)
          index++;

        _fromList.add(index, item);
      }
    }

    boolean hasJoinExpr = false;
    boolean isFirst = true;
    for (int i = 0; i < _fromList.size(); i++) {
      FromItem item = _fromList.get(i);

      // jpa/1178
      if (getParentQuery() != null) {
        ArrayList<FromItem> fromList = getParentQuery().getFromList();
        if (fromList != null) {
          if (fromList.contains(item)) {
            hasJoinExpr = true;
            continue;
          }
        }
      }

      if (isFirst) {
        isFirst = false;
      }
      else {
        if (item.isOuterJoin())
          cb.append(" left outer join ");
        else {
          cb.append(", ");

          if (item.getJoinExpr() != null)
            hasJoinExpr = true;
        }
      }

      cb.append(item.getTable().getName());
      cb.append(" ");
      cb.append(item.getName());

      if (item.getJoinExpr() != null && item.isOuterJoin()) {
        cb.append(" on ");
        item.getJoinExpr().generateJoin(cb);
      }

      EntityType entityType = item.getEntityType();

      // jpa/0l44, jpa/0l12
      /* XXX: jpa/0l47 move this to LoadExpr.generateSelect
      if (entityType != null) {
        AmberColumn discriminator = entityType.getDiscriminator();

        if (entityType instanceof SubEntityType &&
            discriminator != null) {
          // jpa/0l4b
          // XXX: needs to use parser.createTableName()
          FromItem discriminatorItem
            = new FromItem((EntityType) entityType,
                           discriminator.getTable(),
                           item.getName() + "_disc",
                           ++i);

          discriminatorItem.setQuery(this);

          _fromList.add(i, discriminatorItem);

          cb.append(", ");
          cb.append(discriminator.getTable().getName());
          cb.append(' ');
          cb.append(discriminatorItem.getName());
        }
      }
      */
    }

    // jpa/0l12
    // if (hasJoinExpr || _where != null) {

    boolean hasExpr = false;

    for (int i = 0; i < _fromList.size(); i++) {
      FromItem item = _fromList.get(i);

      AmberExpr expr = item.getJoinExpr();

      if (expr != null && ! item.isOuterJoin()) {
        if (hasExpr)
          cb.append(" and ");
        else {
          cb.append(" where ");
          hasExpr = true;
        }

        expr.generateJoin(cb);
      }

      EntityType entityType = item.getEntityType();

      // jpa/0l44
      if (entityType != null) {
        AmberColumn discriminator = entityType.getDiscriminator();

        // jpa/0l43
        if (entityType instanceof SubEntityType &&
            discriminator != null) {
          // jpa/0l12, jpa/0l4b

          if (item.getTable() == discriminator.getTable()) {
            if (hasExpr)
              cb.append(" and ");
            else {
              cb.append(" where ");
              hasExpr = true;
            }

            cb.append("(" + item.getName() + "." + discriminator.getName() + " = ");
            cb.append("'" + entityType.getDiscriminatorValue() + "')");
          }
        }
      }
    }

    if (_where != null) {
      if (hasExpr)
        cb.append(" and ");
      else {
        cb.append(" where ");
        hasExpr = true;
      }

      _where.generateWhere(cb);
    }

    if (_groupList != null) {
      cb.append(" group by ");

      for (int i = 0; i < _groupList.size(); i++) {
        if (i != 0)
          cb.append(", ");

        _groupList.get(i).generateSelect(cb);
      }
    }

    if (_having != null) {
      hasExpr = false;

      cb.append(" having ");

      /*
      for (int i = 0; i < _fromList.size(); i++) {
        FromItem item = _fromList.get(i);
        AmberExpr expr = item.getJoinExpr();

        if (expr != null && ! item.isOuterJoin()) {
          if (hasExpr)
            cb.append(" and ");
          hasExpr = true;

          expr.generateJoin(cb);
        }
      }
      */

      if (_having != null) {
        if (hasExpr)
          cb.append(" and ");
        hasExpr = true;

        _having.generateHaving(cb);
      }
    }

    if (_orderList != null) {
      cb.append(" order by ");

      for (int i = 0; i < _orderList.size(); i++) {
        if (i != 0)
          cb.append(", ");

        _orderList.get(i).generateSelect(cb);

        if (Boolean.FALSE.equals(_ascList.get(i)))
          cb.append(" desc");
      }
    }

    return cb.toString();
  }

  /**
   * Returns true if modifying the given table modifies a cached query.
   */
  public boolean invalidateTable(String table)
  {
    for (int i = _fromList.size() - 1; i >= 0; i--) {
      FromItem from = _fromList.get(i);

      if (table.equals(from.getTable().getName()))
        return true;
    }

    return false;
  }

  /**
   * Debug view.
   */
  public String toString()
  {
    return "SelectQuery[" + getQueryString() + "]";
  }
}
