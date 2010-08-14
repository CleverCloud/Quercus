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

package com.caucho.amber.idgen;

import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.type.GeneratorTableType;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Generator table.
 */
public class AmberTableGenerator extends IdGenerator {
  private static final L10N L = new L10N(AmberTableGenerator.class);
  private static final Logger log = Log.open(AmberTableGenerator.class);
  
  private AmberPersistenceUnit _manager;
  private GeneratorTableType _table;
  private String _name;

  private String _selectSQL;
  private String _updateSQL;

  private boolean _isInit;
  
  /**
   * Creates the table generator.
   */
  public AmberTableGenerator(AmberPersistenceUnit manager,
                             GeneratorTableType table,
                             String name)
  {
    _manager = manager;
    _table = table;
    _name = name;
  }

  /**
   * Allocates the next group of ids.
   */
  public long allocateGroup(AmberConnection aConn)
    throws SQLException
  {
    int groupSize = getGroupSize();
    
    int retry = 5;

    // XXX: should use non-XA
    Connection conn = aConn.getConnection();
    PreparedStatement selectStmt = conn.prepareStatement(_selectSQL);
    PreparedStatement updateStmt = conn.prepareStatement(_updateSQL);

    selectStmt.setString(1, _name);
    updateStmt.setString(2, _name);

    while (retry-- > 0) {
      ResultSet rs = selectStmt.executeQuery();
      if (rs.next()) {
        long value = rs.getLong(1);
        rs.close();

        updateStmt.setLong(1, value + groupSize);
        updateStmt.setLong(3, value);

        if (updateStmt.executeUpdate() == 1)
          return value;
      }
      rs.close();
    }

    throw new SQLException(L.l("Can't allocate id from table '{0}'",
                               _table.getTable().getName()));
  }

  /**
   * Initialize the table.
   */
  public void init(AmberPersistenceUnit amberPersistenceUnit)
    throws SQLException
  {
    if (_isInit)
      return;
    _isInit = true;

    _selectSQL = ("select " + _table.getValueColumn() +
                  " from " + _table.getTable().getName() +
                  " where " + _table.getKeyColumn() + "=?");

    _updateSQL = ("update " + _table.getTable().getName() +
                  " set " + _table.getValueColumn() + "=?" +
                  " where " + _table.getKeyColumn() + "=? " +
                  "  and " + _table.getValueColumn() + "=?");
    
    DataSource ds = amberPersistenceUnit.getDataSource();
    Connection conn = ds.getConnection();
    try {
      try {
        PreparedStatement pstmt = conn.prepareStatement(_selectSQL);

        pstmt.setString(1, _name);

        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
          rs.close();
          return;
        }
      } catch (SQLException e) {
      }

      String sql = ("INSERT INTO " + _table.getTable().getName() + " (" +
                    _table.getKeyColumn() + "," +
                    _table.getValueColumn() + ") VALUES " +
                    "('" + _name + "', 1)");

      Statement stmt = conn.createStatement();
      stmt.executeUpdate(sql);
      stmt.close();
    } finally {
      conn.close();
    }
  }
}
