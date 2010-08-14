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

package com.caucho.amber.table;

import com.caucho.amber.entity.AmberCompletion;
import com.caucho.amber.entity.Entity;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.type.EntityType;
import com.caucho.ejb.EJBExceptionWrapper;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Represents a many-to-one link from one table to another.
 */
public class LinkColumns {
  private static final L10N L = new L10N(LinkColumns.class);
  private static final Logger log = Log.open(LinkColumns.class);

  private static final int NO_CASCADE_DELETE = 0;
  private static final int SOURCE_CASCADE_DELETE = 1;
  private static final int TARGET_CASCADE_DELETE = 2;

  private AmberTable _sourceTable;
  private AmberTable _targetTable;

  private ArrayList<ForeignColumn> _columns;

  private int _cascadeDelete;

  private AmberCompletion _tableDeleteCompletion;
  private AmberCompletion _tableUpdateCompletion;

  /**
   * Creates the table link.
   */
  public LinkColumns(AmberTable sourceTable, AmberTable targetTable,
                     ArrayList<ForeignColumn> columns)
  {
    _sourceTable = sourceTable;
    _targetTable = targetTable;

    _columns = columns;

    _tableDeleteCompletion = sourceTable.getDeleteCompletion();
    _tableUpdateCompletion = sourceTable.getUpdateCompletion();

    _sourceTable.addOutgoingLink(this);
    _targetTable.addIncomingLink(this);
  }

  /**
   * Sets the cascade-delete of the source when the target is deleted,
   * i.e. a one-to-many cascaded delete like an identifying relation.
   */
  public void setSourceCascadeDelete(boolean isCascadeDelete)
  {
    if (isCascadeDelete) {
      assert(_cascadeDelete != TARGET_CASCADE_DELETE);

      _cascadeDelete = SOURCE_CASCADE_DELETE;
    }
    else if (_cascadeDelete == SOURCE_CASCADE_DELETE)
      _cascadeDelete = NO_CASCADE_DELETE;
  }

  /**
   * Sets the cascade-delete of the target when the source is deleted.
   */
  public void setTargetCascadeDelete(boolean isCascadeDelete)
  {
    if (isCascadeDelete) {
      assert(_cascadeDelete != SOURCE_CASCADE_DELETE);

      _cascadeDelete = TARGET_CASCADE_DELETE;
    }
    else if (_cascadeDelete == TARGET_CASCADE_DELETE)
      _cascadeDelete = NO_CASCADE_DELETE;
  }

  /**
   * Return true if the source is deleted when the target is deleted.
   */
  public boolean isSourceCascadeDelete()
  {
    return _cascadeDelete == SOURCE_CASCADE_DELETE;
  }

  /**
   * Return true if the source is deleted when the target is deleted.
   */
  public boolean isTargetCascadeDelete()
  {
    return _cascadeDelete == TARGET_CASCADE_DELETE;
  }

  /**
   * Returns the source table.
   */
  public AmberTable getSourceTable()
  {
    return _sourceTable;
  }

  /**
   * Returns the target table.
   */
  public AmberTable getTargetTable()
  {
    return _targetTable;
  }

  /**
   * Returns the component list.
   */
  public ArrayList<ForeignColumn> getColumns()
  {
    return _columns;
  }

  /**
   * Returns the sql column for the source corresponding to the target key.
   */

  /**
   * Generates the linking for a link
   */
  public ForeignColumn getSourceColumn(AmberColumn targetKey)
  {
    for (int i = _columns.size() - 1; i >= 0; i--) {
      ForeignColumn column = _columns.get(i);
      
      if (column.getTargetColumn() == targetKey)
        return column;
    }


    return null;
  }

  /**
   * Generates SQL select.
   */
  public String generateSelectSQL(String table)
  {
    CharBuffer cb = new CharBuffer();

    for (int i = 0; i < _columns.size(); i++) {
      if (i != 0)
        cb.append(", ");

      if (table != null) {
        cb.append(table);
        cb.append(".");
      }

      cb.append(_columns.get(i).getName());
    }

    return cb.toString();
  }

  /**
   * Generates SQL insert.
   */
  public void generateInsert(ArrayList<String> columns)
  {
    for (int i = 0; i < _columns.size(); i++)
      columns.add(_columns.get(i).getName());
  }

  /**
   * Generates SQL select.
   */
  public String generateUpdateSQL()
  {
    CharBuffer cb = new CharBuffer();

    for (int i = 0; i < _columns.size(); i++) {
      if (i != 0)
        cb.append(", ");

      cb.append(_columns.get(i).getName() + "=?");
    }

    return cb.toString();
  }

  /**
   * Generates SQL match.
   */
  public String generateMatchArgSQL(String table)
  {
    CharBuffer cb = new CharBuffer();

    for (int i = 0; i < _columns.size(); i++) {
      if (i != 0)
        cb.append(" and ");

      if (table != null) {
        cb.append(table);
        cb.append(".");
      }

      cb.append(_columns.get(i).getName());
      cb.append("=?");
    }

    return cb.toString();
  }

  /**
   * Generates the linking for a join
   *
   * @param sourceTable the SQL table name for the source
   * @param targetTable the SQL table name for the target
   */
  public String generateJoin(String sourceTable,
                             String targetTable)
  {
    return generateJoin(sourceTable, targetTable, false);
  }

  /**
   * Generates the linking for a join
   *
   * @param sourceTable the SQL table name for the source
   * @param targetTable the SQL table name for the target
   * @param isArg true if targetTable is an argument "?"
   */
  public String generateJoin(String sourceTable,
                             String targetTable,
                             boolean isArg)
  {
    CharBuffer cb = new CharBuffer();

    cb.append('(');

    for (int i = 0; i < _columns.size(); i++) {
      ForeignColumn column = _columns.get(i);

      if (i != 0)
        cb.append(" and ");

      cb.append(sourceTable);
      cb.append('.');
      cb.append(column.getName());

      cb.append(" = ");

      cb.append(targetTable);

      if (isArg)
        continue;

      cb.append('.');
      cb.append(column.getTargetColumn().getName());
    }

    cb.append(')');

    return cb.toString();
  }

  /**
   * Generates the many-to-many linking.
   * This join is the one-to-many join and the other
   * join is passed in as an argument used to link
   * the two source tables that are pointing to the
   * same target table.
   *
   * @param join the many-to-one join
   * @param sourceTable1 the SQL table name for the 1st source
   * @param sourceTable2 the SQL table name for the 2nd source
   */
  public String generateJoin(LinkColumns manyToOneJoin,
                             String sourceTable1,
                             String sourceTable2)
  {
    // Implemented for jpa/10cb

    if (manyToOneJoin._columns.size() != _columns.size())
      return "";

    CharBuffer cb = new CharBuffer();

    cb.append('(');

    for (int i = 0; i < _columns.size(); i++) {
      ForeignColumn column = _columns.get(i);
      ForeignColumn otherColumn = manyToOneJoin._columns.get(i);

      if (i != 0)
        cb.append(" and ");

      cb.append(sourceTable1);
      cb.append('.');
      cb.append(column.getName());

      cb.append(" = ");

      cb.append(sourceTable2);

      cb.append('.');
      cb.append(otherColumn.getName());
    }

    cb.append(')');

    return cb.toString();
  }

  /**
   * Generates the linking for a where clause
   *
   * @param sourceTable the SQL table name for the source
   * @param targetTable the SQL table name for the target
   */
  public String generateWhere(String sourceTable,
                              String targetTable)
  {
    CharBuffer cb = new CharBuffer();

    cb.append('(');

    for (int i = 0; i < _columns.size(); i++) {
      ForeignColumn column = _columns.get(i);

      if (i != 0)
        cb.append(" and ");

      if (! column.isNotNull()) {

        if (sourceTable == null)
          cb.append('?');
        else {
          cb.append(sourceTable);
          cb.append('.');
          cb.append(column.getName());
        }

        cb.append(" is not null ");
      }

      cb.append(" and ");

      // jpa/10c9
      if (sourceTable == null) {
        cb.append('?');
      }
      else {
        cb.append(sourceTable);
        cb.append('.');
        cb.append(column.getName());
      }

      cb.append(" = ");

      cb.append(targetTable);
      cb.append('.');
      cb.append(column.getTargetColumn().getName());
    }

    cb.append(')');

    return cb.toString();
  }

  /**
   * Cleans up any fields from a delete.
   */
  public void beforeTargetDelete(AmberConnection aConn, Entity entity)
    throws SQLException
  {
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    String sql = null;

    try {
      // commented out: jpa/0h25
      // aConn.flushNoChecks();

      String sourceTable = _sourceTable.getName();

      ArrayList<LinkColumns> outgoingLinks = _sourceTable.getOutgoingLinks();

      boolean isOwner = false;

      // jpa/0s2d: only deletes a relationship if the owner is deleted.
      if (outgoingLinks != null && outgoingLinks.size() > 0) {
        // XXX: assume link columns are introspected and ordered
        // with owning side first.
        // XXX: also, many-to-many bidirectional either side may be
        // the owning side.
        LinkColumns linkColumns = outgoingLinks.get(0);

        if (linkColumns._targetTable == entity.__caucho_getEntityType().getTable())
          isOwner = true;
      }

      boolean isJPA = aConn.getPersistenceUnit().isJPA();

      // ejb/06c5 vs jpa/0h60
      // jpa/0h60, the application should be responsible for deleting
      // the incoming links even when there are FK constraints.
      if (! (isJPA || isSourceCascadeDelete())) {
        CharBuffer cb = new CharBuffer();

        cb.append("update " + sourceTable + " set ");

        ArrayList<ForeignColumn> columns = getColumns();

        for (int i = 0; i < columns.size(); i++) {
          if (i != 0)
            cb.append (", ");

          cb.append(columns.get(i).getName() + "=null");
        }

        cb.append(" where ");

        for (int i = 0; i < columns.size(); i++) {
          if (i != 0)
            cb.append (" and ");

          cb.append(columns.get(i).getName() + "=?");
        }

        // See catch (Exception) below.
        sql = cb.toString();

        pstmt = aConn.prepareStatement(sql);

        entity.__caucho_setKey(pstmt, 1);

        pstmt.executeUpdate();

        aConn.addCompletion(_sourceTable.getUpdateCompletion());
      }
      else if (_sourceTable.isCascadeDelete()) {
        // if the link cascades deletes to the source and the source
        // table also has cascade deletes, then we need to load the
        // target entities and delete them recursively
        //
        // in theory, this could cause a loop, but we're ignoring that
        // case for now

        EntityType entityType = (EntityType) _sourceTable.getType();

        CharBuffer cb = new CharBuffer();

        cb.append("select ");
        cb.append(entityType.getId().generateSelect("o"));
        cb.append(" from " + sourceTable + " o");
        cb.append(" where ");

        ArrayList<ForeignColumn> columns = getColumns();

        for (int i = 0; i < columns.size(); i++) {
          if (i != 0)
            cb.append (" and ");

          cb.append(columns.get(i).getName() + "=?");
        }

        // See catch (Exception) below.
        sql = cb.toString();

        pstmt = aConn.prepareStatement(sql);

        entity.__caucho_setKey(pstmt, 1);

        ArrayList<Object> proxyList = new ArrayList<Object>();

        rs = pstmt.executeQuery();
        while (rs.next()) {
          proxyList.add(entityType.getHome().loadLazy(aConn, rs, 1));
        }
        rs.close();

        for (Object obj : proxyList) {
          entityType.getHome().getEntityFactory().delete(aConn, obj);
        }
      } // jpa/0i5e vs. jpa/0h25, jpa/0s2d
      else if ((! isJPA) || (isOwner && (_sourceTable.getType() == null))) {
        CharBuffer cb = new CharBuffer();

        cb.append("delete from " + sourceTable +
                  " where ");

        ArrayList<ForeignColumn> columns = getColumns();

        for (int i = 0; i < columns.size(); i++) {
          if (i != 0)
            cb.append (" and ");

          cb.append(columns.get(i).getName() + "=?");
        }

        // See catch (Exception) below.
        sql = cb.toString();

        pstmt = aConn.prepareStatement(sql);

        entity.__caucho_setKey(pstmt, 1);

        pstmt.executeUpdate();

        aConn.addCompletion(_sourceTable.getDeleteCompletion());
      }

      aConn.expire();
    }
    catch (Exception e) {
      // Close statements only on exception.
      // See com.caucho.amber.manager.AmberConnection for statement caching.
      if (pstmt != null)
        aConn.closeStatement(sql);

      if (e instanceof SQLException)
        throw (SQLException) e;

      if (e instanceof RuntimeException)
        throw (RuntimeException) e;

      throw new EJBExceptionWrapper(e);
    } finally {
      if (rs != null)
        rs.close();
    }
  }

  /**
   * Cleans up any fields from a delete.
   */
  public void afterSourceDelete(AmberConnection aConn, Entity entity)
    throws SQLException
  {
    // this should be handled programmatically
  }

  public String toString()
  {
    return "[" + _sourceTable + ", " + _targetTable + ", " + _columns + "]";
  }
}
