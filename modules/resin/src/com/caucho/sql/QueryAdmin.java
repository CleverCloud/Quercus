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
 * @author Emil Ong
 */


package com.caucho.sql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.ArrayList;

import javax.management.*;

import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.JdbcQueryMXBean;
import com.caucho.management.server.JdbcQueryResult;
import com.caucho.management.server.JdbcTableColumn;

public class QueryAdmin extends AbstractManagedObject
  implements JdbcQueryMXBean
{
  private DBPool _pool;

  public QueryAdmin(DBPool pool)
  {
    _pool = pool;
  }

  public String getUrl()
  {
    return _pool.getURL();
  }

  public JdbcQueryResult query(String sql)
    throws SQLException
  {
    Connection connection = null;
    
    try {
      connection = _pool.getConnection();
      Statement statement = connection.createStatement();

      JdbcQueryResult result = new JdbcQueryResult();

      if (statement.execute(sql)) {
        ArrayList<String[]> rows = new ArrayList<String[]>();

        ResultSet resultSet = statement.getResultSet();
        ResultSetMetaData metadata = resultSet.getMetaData();
        int columnCount = metadata.getColumnCount();

        // add column names
        String []names = new String[columnCount];

        for (int i = 0; i < columnCount; i++)
          names[i] = metadata.getColumnName(i + 1);

        result.setRowNames(names);

        // add results
        while (resultSet.next()) {
          String []row = new String[columnCount];

          for (int i = 0; i < columnCount; i++) {
            try {
              row[i] = resultSet.getString(i + 1);
            }
            catch (SQLException e) {
              // MySQL zero date conversion issue
              if (e.getMessage().startsWith("Cannot convert value '0000-00-00"))
                row[i] = "0000-00-00 00:00:00";
              else
                throw e;
            }
          }

          rows.add(row);
        }

        String [][]rowsArray = new String[rows.size()][];
        rows.toArray(rowsArray);

        result.setResultData(rowsArray);

        return result;
      }

      return null;
    }
    finally {
      if (connection != null)
        connection.close();
    }
  }

  public String []listTables()
    throws SQLException
  {
    Connection connection = null;
    
    try {
      connection = _pool.getConnection();

      String catalog = connection.getCatalog();
      DatabaseMetaData metadata = connection.getMetaData();
      ResultSet results = metadata.getTables(catalog, null, null, null);
      ArrayList<String> tables = new ArrayList<String>(); 

      while (results.next()) {
        String table = results.getString("TABLE_NAME");
        tables.add(table);
      }

      String []tablesArray = new String[tables.size()];
      tables.toArray(tablesArray);

      return tablesArray;
    }
    finally {
      if (connection != null)
        connection.close();
    }
  }

  public JdbcTableColumn []listColumns(String table)
    throws SQLException
  {
    Connection connection = null;
    
    try {
      connection = _pool.getConnection();

      DatabaseMetaData metadata = connection.getMetaData();
      ResultSet results = metadata.getColumns(null, null, table, null);
      ArrayList<JdbcTableColumn> columns = new ArrayList<JdbcTableColumn>(); 

      while (results.next()) {
        String name = results.getString("COLUMN_NAME");
        String type = results.getString("TYPE_NAME");
        columns.add(new JdbcTableColumn(name, type));
      }

      JdbcTableColumn []columnsArray = new JdbcTableColumn[columns.size()];
      columns.toArray(columnsArray);

      return columnsArray;
    }
    finally {
      if (connection != null)
        connection.close();
    }
  }
  
  @Override
  public String getName()
  {
    return _pool.getName();
  }

  void register()
  {
    registerSelf();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getObjectName() + "]";
  }
}
