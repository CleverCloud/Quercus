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
 * @author Sam
 */

package com.caucho.jdbc;

import com.caucho.util.L10N;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.Connection;
import java.sql.Types;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GenericMetaData
  extends JdbcMetaData
{
  private static final Logger log = Logger.getLogger(GenericMetaData.class.getName());
  private static final L10N L = new L10N(GenericMetaData.class);

  private String _longType;
  private String _blobType;
  private Boolean _supportsPositionFunction;
  private Boolean _supportsGetGeneratedKeys;
  private String _falseLiteral;

  public GenericMetaData(DataSource ds)
  {
    super(ds);
  }

  /**
   * Returns the long type.
   */
  public String getLongType()
  {
    if (_longType != null)
      return _longType;

    Connection conn = null;

    try {
      conn = getConnection();

      DatabaseMetaData md = conn.getMetaData();
      ResultSet rs;

      rs = md.getTypeInfo();
      try {
        while (rs.next()) {
          if (rs.getShort("DATA_TYPE") == Types.BIGINT) {
            _longType = rs.getString("TYPE_NAME");

            return _longType;
          }
        }
      } finally {
        rs.close();
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      try {
        if (conn != null) conn.close();
      } catch (SQLException e) {
      }
    }

    return null;
  }

  /**
   * Returns the blob type.
   */
  public String getBlobType()
  {
    if (_blobType != null)
      return _blobType;

    Connection conn = null;

    try {
      conn = getConnection();

      DatabaseMetaData md = conn.getMetaData();
      ResultSet rs;

      rs = md.getTypeInfo();
      try {
        while (rs.next()) {
          if (rs.getShort("DATA_TYPE") == Types.BLOB) {
            _blobType = rs.getString("TYPE_NAME");

            return _blobType;
          }
        }
      } finally {
        rs.close();
      }

      rs = md.getTypeInfo();
      try {
        while (rs.next()) {
          int dataType = rs.getShort("DATA_TYPE");

          if (rs.getShort("DATA_TYPE") == Types.LONGVARBINARY) {
            _blobType = rs.getString("TYPE_NAME");
            return _blobType;
          }
        }
      } finally {
        rs.close();
      }

      rs = md.getTypeInfo();
      try {
        while (rs.next()) {
          if (rs.getShort("DATA_TYPE") == Types.BINARY) {
            _blobType = rs.getString("TYPE_NAME");
            return _blobType;
          }
        }
      } finally {
        rs.close();
      }

      rs = md.getTypeInfo();
      try {
        while (rs.next()) {
          if (rs.getShort("DATA_TYPE") == Types.VARBINARY) {
            _blobType = rs.getString("TYPE_NAME");
            return _blobType;
          }
        }
      } finally {
        rs.close();
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      try {
        if (conn != null)
          conn.close();
      } catch (Exception e) {
      }
    }

    return null;
  }

  /**
   * Returns the literal for FALSE.
   */
  public String getFalseLiteral()
  {
    if (_falseLiteral != null)
      return _falseLiteral;

    Connection conn = null;

    _falseLiteral = "0";

    try {
      conn = getConnection();

      Statement stmt = null;

      try {
        stmt = conn.createStatement();

        ResultSet rs = null;

        try {
          rs = stmt.executeQuery("select false");

          _falseLiteral = "false";

        } catch (SQLException e) {
          log.log(Level.FINER, e.toString(), e);
        } finally {
          if (rs != null)
            rs.close();
        }
      } finally {
        if (stmt != null)
          stmt.close();
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        if (conn != null)
          conn.close();
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    return _falseLiteral;
  }

  /**
   * True if the generated keys is supported
   */
  public boolean supportsGetGeneratedKeys()
  {
    if (_supportsGetGeneratedKeys != null)
      return _supportsGetGeneratedKeys;

    try {
      Connection conn = getConnection();

      try {
        DatabaseMetaData metaData = conn.getMetaData();

        _supportsGetGeneratedKeys = metaData.supportsGetGeneratedKeys();

        return _supportsGetGeneratedKeys;
      } finally {
        conn.close();
      }
    } catch (Exception e) {
      // Possibly older drivers: UnsupportedOperationException.
      log.log(Level.FINE, e.toString(), e);
      return false;
    } catch (AbstractMethodError e) {
      // Older drivers e.g. SQLServer.
      log.log(Level.FINE, e.toString(), e);
      return false;
    }
  }
  /**
   * Returns true if the POSITION function is supported.
   */
  public boolean supportsPositionFunction()
  {
    if (_supportsPositionFunction != null)
      return _supportsPositionFunction;

    Connection conn = null;

    _supportsPositionFunction = Boolean.FALSE;

    try {
      conn = getConnection();

      Statement stmt = null;

      try {
        stmt = conn.createStatement();

        ResultSet rs = null;

        try {
          rs = stmt.executeQuery("select position('a' in 'abc')");

          _supportsPositionFunction = Boolean.TRUE;

        } catch (SQLException e) {
          log.log(Level.FINER, e.toString(), e);
        } finally {
          if (rs != null)
            rs.close();
        }
      } finally {
        if (stmt != null)
          stmt.close();
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        if (conn != null)
          conn.close();
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    return _supportsPositionFunction;
  }

  /**
   * Returns true if table alias name with UPDATE is supported.
   */
  public boolean supportsUpdateTableAlias()
  {
    return true;
  }

  /**
   * Returns true if table list with UPDATE is supported:
   * UPDATE table1 a, table2 b SET ...
   */
  public boolean supportsUpdateTableList()
  {
    // Normally, MySql is the only one which supports it.
    return false;
  }

  /**
   * Returns true if identity is supported.
   */
  public boolean supportsIdentity()
  {
    return false;
  }

  /**
   * Returns the identity property
   */
  public String createIdentitySQL(String sqlType)
  {
    throw new UnsupportedOperationException("createIdentitySQL");
  }

  /**
   * Returns true if sequences are supported.
   */
  public boolean supportsSequences()
  {
    return false;
  }

  /**
   * Returns a sequence select expression.
   */
  public String createSequenceSQL(String name, int size)
  {
    throw new UnsupportedOperationException( "createSequenceSQL");
  }

  public String selectSequenceSQL(String name)
  {
    throw new UnsupportedOperationException("selectSequenceSQL");
  }

  /**
   * to Return SQL for the table with the given
   * SQL type.  Takes, length, precision and scale.
   */
  public String getCreateColumnSQL(int sqlType,
                                   int length,
                                   int precision,
                                   int scale)
  {
    String type = null;

    switch (sqlType) {
      case Types.BOOLEAN:
        type = getCreateColumnSQLImpl(sqlType, length, precision, scale);
        if (type == null)
          type = getCreateColumnSQLImpl(Types.BIT, length, precision, scale);
        break;

      case Types.DATE:
        type = getCreateColumnSQLImpl(sqlType, length, precision, scale);
        if (type == null)
          type = getCreateColumnSQLImpl(Types.TIMESTAMP, length, precision, scale);
        break;

      case Types.TIME:
        type = getCreateColumnSQLImpl(sqlType, length, precision, scale);
        if (type == null)
          type = getCreateColumnSQLImpl(Types.TIMESTAMP, length, precision, scale);
        break;

      case Types.DOUBLE:
        type = getCreateColumnSQLImpl(Types.DOUBLE, length, precision, scale);
        break;

      case Types.NUMERIC:
        type = getCreateColumnSQLImpl(Types.NUMERIC, length, precision, scale);
        break;

      default:
        type = getCreateColumnSQLImpl(sqlType, length, precision, scale);
        break;
    }

    if (type == null)
      type = getDefaultCreateTableSQL(sqlType, length, precision, scale);

    return type;
  }

  /**
   * Returns the SQL for the table with the given SQL type.
   */
  protected String getCreateColumnSQLImpl(int sqlType,
                                          int length,
                                          int precision,
                                          int scale)
  {
    Connection conn = null;

    try {
      conn = getConnection();

      DatabaseMetaData md = conn.getMetaData();
      ResultSet rs;

      rs = md.getTypeInfo();

      try {
        while (rs.next()) {
          if (rs.getShort("DATA_TYPE") == sqlType) {
            String typeName = rs.getString("TYPE_NAME");
            String params = rs.getString("CREATE_PARAMS");

            if (params == null || params.equals(""))
              return typeName;
            else if (params.startsWith("(M)")) {
              if (length > 0)
                return typeName + "(" + length + ")";
              else
                return typeName;
            }
            else if (params.startsWith("(M,D)") || params.equals("precision,scale")) {
              if (precision > 0) {
                typeName += "(" + precision;
                if (scale > 0) {
                  typeName += "," + scale;
                }
                typeName += ")";
              }
              return typeName;
            }
            else if (params.startsWith("(")) {
              int tail = params.indexOf(')');

              if (tail > 0) {
                String value = params.substring(1, tail);
                boolean isConstant = true;

                for (int i = 0; i < value.length(); i++) {
                  if (value.charAt(i) >= 'a' && value.charAt(i) <= 'z')
                    isConstant = false;
                  else if (value.charAt(i) >= 'A' && value.charAt(i) <= 'Z')
                    isConstant = false;
                }

                if (isConstant)
                  return typeName + "(" + value + ")";
              }

              return typeName;
            }
            else {
              return typeName;
            }
          }
        }
      } finally {
        rs.close();
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      try {
        if (conn != null)
          conn.close();
      } catch (Exception e) {
      }
    }

    return null;
  }

  protected String getDefaultCreateTableSQL(int sqlType, int length, int precision, int scale)
  {
    switch (sqlType) {
      case java.sql.Types.BOOLEAN:
        return "CHAR";

      case java.sql.Types.BIT:
      case java.sql.Types.TINYINT:
      case java.sql.Types.SMALLINT:
      case java.sql.Types.INTEGER:
      case java.sql.Types.BIGINT:
        return "INTEGER";

      case java.sql.Types.NUMERIC:
      case java.sql.Types.DECIMAL:
        String typeString = "NUMERIC";
        if (precision > 0) {
          typeString += "(" + precision;
          if (scale > 0) {
            typeString += "," + scale;
          }
          typeString += ")";
        }
        return typeString;

      case java.sql.Types.DOUBLE:
      case java.sql.Types.FLOAT:
        return "DOUBLE";

      case java.sql.Types.CHAR:
        return "CHAR";

      case java.sql.Types.DATE:
      case java.sql.Types.TIME:
      case java.sql.Types.TIMESTAMP:
        return "TIMESTAMP";

      default:
        return "VARCHAR(" + length + ")";
    }
  }
}
