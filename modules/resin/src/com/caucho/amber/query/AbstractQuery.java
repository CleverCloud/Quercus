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
import com.caucho.amber.expr.ArgExpr;
import com.caucho.amber.expr.EmbeddedExpr;
import com.caucho.amber.expr.JoinExpr;
import com.caucho.amber.expr.ManyToOneJoinExpr;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.EntityType;
import com.caucho.jdbc.JdbcMetaData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Represents an amber query
 */
abstract public class AbstractQuery {
  private String _sql;

  AmberExpr _where;
  AmberExpr _having;

  protected ArrayList<FromItem> _fromList = new ArrayList<FromItem>();

  // jpa/0w22
  // SELECT p.startMonth FROM TestBean o JOIN o.period p
  // p is an alias to o.period (o.period is @Embedded)
  // "p" -> "o.period"
  protected HashMap<String, EmbeddedExpr> _embeddedAliases
    = new HashMap<String, EmbeddedExpr>();

  private ArgExpr []_argList;

  // Map named parameters to JDBC ?,?,?.
  // Ex: INSERT INTO test VALUES(:testId, :testName) is mapped as [0]->"testId", [1]->"testName"
  //     INSERT INTO test VALUES(:testName, :testName) is mapped as [0]->"testName", [1]->"testName"
  // XXX: HashMap<String, ArrayList<Long>> would probably be an overkill.
  //
  private ArrayList<String> _preparedMapping = new ArrayList<String>();

  private JdbcMetaData _metaData;

  // jpa/1231
  private boolean _hasSubQuery;


  AbstractQuery(String sql, JdbcMetaData metaData)
  {
    _sql = sql;
    _metaData = metaData;
  }

  /**
   * Returns the query string.
   */
  public String getQueryString()
  {
    return _sql;
  }

  /**
   * Adds an embedded alias.
   */
  public void addEmbeddedAlias(String alias,
                               EmbeddedExpr expr)
  {
    _embeddedAliases.put(alias, expr);
  }

  /**
   * Gets the embedded aliases.
   */
  public HashMap<String, EmbeddedExpr> getEmbeddedAliases()
  {
    return _embeddedAliases;
  }

  /**
   * Sets the from list.
   */
  public FromItem createFromItem(AmberTable table,
                                 String name)
  {
    return createFromItem(null, table, name);
  }

  /**
   * Sets the from list.
   */
  public FromItem createFromItem(EntityType entityType,
                                 AmberTable table,
                                 String name)
  {
    FromItem item = new FromItem(entityType, table,
                                 name, _fromList.size());

    item.setQuery(this);

    _fromList.add(item);

    return item;
  }

  /**
   * Creates a dependent from item
   */
  public FromItem createDependentFromItem(FromItem parent,
                                          LinkColumns link,
                                          String name)
  {
    for (int i = 0; i < _fromList.size(); i++) {
      JoinExpr join = _fromList.get(i).getJoinExpr();

      if (join != null && join.isDependent(parent, link))
        return _fromList.get(i);
    }

    FromItem item = createFromItem(null, link.getSourceTable(), name);

    JoinExpr join = new ManyToOneJoinExpr(link, item, parent);

    item.setJoinExpr(join);

    return item;
  }

  /**
   * Returns the from list.
   */
  public ArrayList<FromItem> getFromList()
  {
    return _fromList;
  }

  /**
   * Gets the parent query.
   */
  public AbstractQuery getParentQuery()
  {
    return null;
  }

  /**
   * Returns the prepared mapping.
   */
  public ArrayList<String> getPreparedMapping()
  {
    return _preparedMapping;
  }

  /**
   * Returns the SQL.
   */
  public abstract String getSQL();

  /**
   * initializes the query.
   */
  void init()
    throws QueryParseException
  {
    if (_where instanceof AndExpr) {
      AndExpr and = (AndExpr) _where;

      ArrayList<AmberExpr> components = and.getComponents();

      for (int i = components.size() - 1; i >= 0; i--) {
        AmberExpr component = components.get(i);

        if (component instanceof JoinExpr) {
          JoinExpr link = (JoinExpr) component;

          if (link.bindToFromItem()) {
            components.remove(i);
          }
        }
      }

      _where = and.getSingle();
    }

    if (_having instanceof AndExpr) {
      AndExpr and = (AndExpr) _having;

      ArrayList<AmberExpr> components = and.getComponents();

      for (int i = components.size() - 1; i >= 0; i--) {
        AmberExpr component = components.get(i);

        if (component instanceof JoinExpr) {
          JoinExpr link = (JoinExpr) component;

          if (link.bindToFromItem()) {
            components.remove(i);
          }
        }
      }

      _having = and.getSingle();
    }

    // Rolls up unused from items from the left to the right.
    // It's not necessary to roll up the rightmost items because
    // they're only created if they're actually needed
    for (int i = 0; i < _fromList.size(); i++) {
      FromItem item = _fromList.get(i);

      JoinExpr join = item.getJoinExpr();

      if (join == null)
        continue;

      // XXX: jpa/1173, jpa/1178
      // if (getParentQuery() != null)
      //   break;

      FromItem joinParent = join.getJoinParent();
      FromItem joinTarget = join.getJoinTarget();

      boolean isTarget = item == joinTarget;

      if (joinParent == null) {
      }
      else if (joinParent.getJoinExpr() == null
               && joinParent == joinTarget
               && ! usesFromData(joinParent)) {
        _fromList.remove(joinParent);

        replaceJoin(join);

        // XXX:
        item.setJoinExpr(null);
        //item.setOuterJoin(false);
        i = -1;

        AmberExpr joinWhere = join.getWhere();

        if (joinWhere != null)
          _where = AndExpr.create(_where, joinWhere);
      }
      else if (item == joinTarget
               && ! isJoinParent(item)
               && ! usesFromData(item)) {

        boolean isManyToOne = false;
        boolean isManyToMany = false;

        if (join instanceof ManyToOneJoinExpr) {
          // jpa/0h1c
          isManyToOne = true;

          // jpa/1144
          ManyToOneJoinExpr manyToOneJoinExpr;
          manyToOneJoinExpr = (ManyToOneJoinExpr) join;
          isManyToMany = manyToOneJoinExpr.isManyToMany();
        }

        // ejb/06u0, jpa/1144, jpa/0h1c, jpa/114g
        if (isManyToMany || (isManyToOne && ! item.isInnerJoin())) {
          // ejb/06u0 || isFromInnerJoin(item)))) {

          // Optimization for common children query:
          // SELECT o FROM TestBean o WHERE o.parent.id=?
          // jpa/0h1k
          // jpa/114g as negative exists test

          // jpa/0h1m
          if (i + 1 < _fromList.size()) {
            FromItem subItem = _fromList.get(i + 1);

            JoinExpr nextJoin = subItem.getJoinExpr();

            if (nextJoin != null
                && nextJoin instanceof ManyToOneJoinExpr) {
              continue;
            }
          }

          _fromList.remove(item);

          replaceJoin(join);

          i = -1;

          AmberExpr joinWhere = join.getWhere();

          if (joinWhere != null)
            _where = AndExpr.create(_where, joinWhere);
        }
      }
    }

    for (int i = 0; i < _fromList.size(); i++) {
      FromItem item = _fromList.get(i);

      if (item.isInnerJoin())
        continue;

      if (item.getJoinExpr() == null)
        continue;

      boolean isFromInner = isFromInnerJoin(item);

      item.setOuterJoin(! isFromInner);
    }
  }

  boolean isJoinParent(FromItem item)
  {
    for (int i = 0; i < _fromList.size(); i++) {
      FromItem subItem = _fromList.get(i);

      if (subItem.getJoinExpr() != null &&
          subItem.getJoinExpr().getJoinParent() == item) {
        return true;
      }
    }

    return false;
  }

  boolean isFromInnerJoin(FromItem item)
  {
    return usesFrom(item, AmberExpr.IS_INNER_JOIN);
  }

  boolean usesFromData(FromItem item)
  {
    return usesFrom(item, AmberExpr.USES_DATA);
  }

  /**
   * Returns true if this query has a subquery.
   */
  public boolean hasSubQuery()
  {
    return _hasSubQuery;
  }

  /**
   * Sets true if this query has a subquery.
   */
  public void setHasSubQuery(boolean hasSubQuery)
  {
    _hasSubQuery = hasSubQuery;
  }

  /**
   * Returns true if the item must have at least one entry in the database.
   */
  public boolean exists(FromItem item)
  {
    if (_where != null && _where.exists(item)) {
      return true;
    }

    return false;
  }

  /**
   * Returns true if the from item is used by the query.
   */
  public boolean usesFrom(FromItem item, int type)
  {
    // jpa/1201
    if (_where != null && _where.usesFrom(item, type)) {
      return true;
    }

    return false;
  }

  void replaceJoin(JoinExpr join)
  {
    if (_where != null) {
      _where = _where.replaceJoin(join);
    }
  }

  /**
   * Sets the arg list.
   */
  boolean setArgList(ArgExpr []argList)
  {
    _argList = argList;

    int n = argList.length;

    if (n > 0) {

      if (argList[0].getName() != null) {

        for (int i=0; i < n; i++) {

          String name = argList[i].getName();

          if (name == null) {
            _preparedMapping = null;
            return false;
          }

          _preparedMapping.add(name);
        }
      }
    }

    return true;
  }

  /**
   * Returns the arg list.
   */
  public ArgExpr []getArgList()
  {
    return _argList;
  }

  /**
   * Generates update
   */
  void registerUpdates(CachedQuery query)
  {
    for (int i = 0; i < _fromList.size(); i++) {
      FromItem item = _fromList.get(i);

      AmberEntityHome home = item.getEntityHome();

      CacheUpdate update = new TableCacheUpdate(query);

      home.addUpdate(update);
    }
  }

  /**
   * Returns the expire time.
   */
  public long getCacheMaxAge()
  {
    return -1;
  }

  /**
   * Prepares before any update.
   */
  public void prepare(UserQuery userQuery, AmberConnection aConn)
    throws SQLException
  {
  }

  /**
   * Any post-sql completion
   */
  public void complete(UserQuery userQuery, AmberConnection aConn)
    throws SQLException
  {
  }

  /**
   * Returns the jdbc meta data, if available.
   */
  JdbcMetaData getMetaData()
  {
    return _metaData;
  }
}
