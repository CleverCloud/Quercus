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

package com.caucho.jdbc;

import com.caucho.util.L10N;
import com.caucho.util.Log;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract way of grabbing data from the JDBC connection.
 */
abstract public class JdbcMetaData {
  private static final L10N L = new L10N(JdbcMetaData.class);
  private static final Logger log = Log.open(JdbcMetaData.class);

  private DataSource _ds;

  /**
   * Create a new JDBC backing store.
   */
  protected JdbcMetaData(DataSource ds)
  {
    _ds = ds;
  }

  /**
   * Create based on the connection.
   */
  public static JdbcMetaData create(DataSource ds)
  {
    Connection conn = null;

    try {
      conn = ds.getConnection();

      DatabaseMetaData md = conn.getMetaData();

      String name = md.getDatabaseProductName();

      log.fine(L.l("Database '{0}' metadata.", name));

      if ("oracle".equalsIgnoreCase(name))
        return new OracleMetaData(ds);
      else if ("resin".equalsIgnoreCase(name))
        return new ResinMetaData(ds);
      else if ("postgres".equalsIgnoreCase(name) ||
               "PostgreSQL".equalsIgnoreCase(name))
        return new PostgresMetaData(ds);
      else if ("mysql".equalsIgnoreCase(name))
        return new MysqlMetaData(ds);
      else if ("Microsoft SQL Server".equalsIgnoreCase(name))
        return new SqlServerMetaData(ds);
      else if ("Apache Derby".equalsIgnoreCase(name))
        return new DerbyMetaData(ds);
      else {
        log.fine(name + " is an unknown database type, using generic sql");
        return new GenericMetaData(ds);
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return new GenericMetaData(ds);
    } finally {
      try {
        if (conn != null) conn.close();
      } catch (SQLException e) {
      }
    }
  }

  /**
   * Returns the database name.
   */
  public String getDatabaseName()
  {
    Connection conn = null;

    try {
      conn = getConnection();

      DatabaseMetaData md = conn.getMetaData();

      return md.getDatabaseProductName();
    } catch (SQLException e) {
      log.log(Level.WARNING, e.toString(), e);
      return "unknown";
    } finally {
      try {
        if (conn != null) conn.close();
      } catch (SQLException e) {
      }
    }
  }

  /**
   * Returns the blob type.
   */
  abstract public String getBlobType();

  /**
   * True if blobs must be truncated on delete.
   */
  public boolean isTruncateBlobBeforeDelete()
  {
    return false;
  }

  /**
   * True if the generated keys is supported
   */
  abstract public boolean supportsGetGeneratedKeys();

  /**
   * Returns the literal for FALSE.
   */
  abstract public String getFalseLiteral();

  /**
   * Returns true if the POSITION function is supported.
   */
  abstract public boolean supportsPositionFunction();

  /**
   * Returns true if table alias name with UPDATE is supported.
   */
  abstract public boolean supportsUpdateTableAlias();

  /**
   * Returns true if table list with UPDATE is supported:
   * UPDATE table1 a, table2 b SET ...
   */
  abstract public boolean supportsUpdateTableList();

  /**
   * Returns true if the sql state is a "foreign key violation" error.
   */
  public boolean isForeignKeyViolationSQLState(String sqlState)
  {
    if (sqlState == null)
      return false;

    return sqlState.equals("23503");
  }

  /**
   * Returns true if the sql state is a "duplicate primary key" error.
   */
  public boolean isUniqueConstraintSQLState(String sqlState)
  {
    if (sqlState == null)
      return false;

    return sqlState.equals("23000") || sqlState.equals("23505");
  }

  /**
   * Returns the long type.
   */
  abstract public String getLongType();

  /**
   * Returns true if identity is supported.
   */
  abstract public boolean supportsIdentity();

  /**
   * Returns the identity property
   */
  abstract public String createIdentitySQL(String sqlType);

  /**
   * Returns true if sequences are supported.
   */
  abstract public boolean supportsSequences();

  /**
   * Returns a sequence select expression.
   */
  abstract public String createSequenceSQL(String name, int size);

  /**
   * Returns a sequence select expression.
   */
  abstract public String selectSequenceSQL(String name);

  /**
   * Returns a sequence select expression.
   */
  public String testSequenceSQL(String name)
  {
    return selectSequenceSQL(name) + " WHERE 1=0";
  }

  /**
   * Returns the code to test for a boolean value for a term.
   */
  public String generateBoolean(String term)
  {
    return term;
  }

  /**
   * Returns true if the metadata can handle limit
   */
  public boolean isLimit()
  {
    return false;
  }

  /**
   * Returns true if the metadata can handle limit and offset
   */
  public boolean isLimitOffset()
  {
    return false;
  }
    
  /**
   * Returns a limit.
   */
  public String limit(String sql, int firstResults, int maxResults)
  {
    return sql;
  }

  /**
   * New version to Return SQL for the table with the given
   * SQL type.  Takes, length, precision and scale.
   */
  abstract public String getCreateColumnSQL(int sqlType,
                                            int length,
                                            int precision,
                                            int scale);


  /**
   * Returns a connection, which must then be closed.
   */
  protected Connection getConnection()
    throws SQLException
  {
    return _ds.getConnection();
  }
}
