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

package com.caucho.amber.table;

import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.type.AmberType;
import com.caucho.config.ConfigException;
import com.caucho.config.LineConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * AmberColumn configuration.
 */
public class AmberColumn {
  private static final L10N L = new L10N(AmberColumn.class);

  private AmberTable _table;

  private String _name;

  private String _configLocation;

  private AmberType _type;

  private boolean _isPrimaryKey;

  private String _sqlType;

  private boolean _isNotNull;
  private boolean _isUnique;
  private int _length;
  private int _precision;
  private int _scale;

  private String _generatorType;
  private String _generator;

  // getter/setter stuff
  private String _fieldName;


  AmberColumn(AmberTable table, String name)
  {
    _table = table;
    _name = name;
  }

  /**
   * Creates the column.
   *
   * @param table the owning table
   * @param name the column sql name
   * @param type the column's type
   */
  public AmberColumn(AmberTable table, String name, AmberType type)
  {
    _table = table;
    _name = name;
    _type = type;
  }

  /**
   * Returns the owning table.
   */
  public AmberTable getTable()
  {
    return _table;
  }

  /**
   * Gets the column name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the column name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Sets the config location.
   */
  public void setConfigLocation(String location)
  {
    _configLocation = location;
  }

  /**
   * Returns the type.
   */
  public AmberType getType()
  {
    return _type;
  }

  /**
   * Sets the primary key property.
   */
  public void setPrimaryKey(boolean isPrimaryKey)
  {
    _isPrimaryKey = isPrimaryKey;
  }

  /**
   * Return true for a primary key column.
   */
  public boolean isPrimaryKey()
  {
    return _isPrimaryKey;
  }

  /**
   * Sets the generator type.
   */
  public void setGeneratorType(String type)
  {
    _generatorType = type;
  }

  /**
   * Generates the insert name.
   */
  public String generateInsertName()
  {
    return _name;
  }

  /**
   * Sets the sql type for create table
   */
  public void setSQLType(String sqlType)
  {
    _sqlType = sqlType;
  }

  /**
   * Gets the sql type for the create table
   */
  public String getSQLType()
  {
    return _sqlType;
  }

  /**
   * Sets the length property.
   */
  public void setLength(int length)
  {
    _length = length;
  }

  /**
   * Gets the length property.
   */
  public int getLength()
  {
    return _length;
  }

  /**
   * Sets the not-null property.
   */
  public void setNotNull(boolean isNotNull)
  {
    _isNotNull = isNotNull;
  }

  /**
   * Gets the not-null property.
   */
  public boolean isNotNull()
  {
    return _isNotNull;
  }

  /**
   * Set the precision property.
   */
  public void setPrecision(int precision) {
    _precision = precision;
  }

  /**
   * Gets the precision property.
   */
  public int getPrecision() {
    return _precision;
  }

  /**
   * Set the scale property
   */
  public void setScale(int scale) {
    _scale = scale;
  }

  /**
   * Get the scale property
   */
  public int getScale() {
    return _scale;
  }

  /**
   * Sets the unique property.
   */
  public void setUnique(boolean isUnique)
  {
    _isUnique = isUnique;
  }

  /**
   * Gets the unique property.
   */
  public boolean isUnique()
  {
    return _isUnique;
  }

  /**
   * Generates the clause to create the column.
   */
  String generateCreateTableSQL(AmberPersistenceUnit manager)
  {
    CharBuffer cb = new CharBuffer();
    cb.append(_name + " ");
    String sqlType = _sqlType;

    if (_sqlType != null)
      sqlType = _sqlType;
    else {
      sqlType = _type.generateCreateColumnSQL(manager, _length, _precision, _scale);
    }

    String generatorType = _generatorType;
    
    if ("auto".equals(_generatorType)
        && manager.getMetaData().supportsIdentity())
      generatorType = "identity";

    if ("identity".equals(generatorType))
      cb.append(manager.getMetaData().createIdentitySQL(sqlType));
    else
      cb.append(sqlType);

    if (isPrimaryKey()) {
      cb.append(" primary key");
    } else if (! "identity".equals(generatorType)) {
      if (isNotNull())
        cb.append(" not null");
      if (isUnique())
        cb.append(" unique");
    }

    return cb.toString();
  }

  /**
   * Creates the table if missing.
   */
  void validateDatabase(AmberPersistenceUnit amberPersistenceUnit)
    throws ConfigException
  {
    try {
      DataSource ds = amberPersistenceUnit.getDataSource();
      Connection conn = ds.getConnection();
      try {
        Statement stmt = conn.createStatement();

        String sql = "select " + getName() + " from " + _table.getName() + " where 1=0";

        try {
          // If the table exists, return

          ResultSet rs = stmt.executeQuery(sql);
          rs.close();
          return;
        } catch (SQLException e) {
          throw error(L.l("'{0}' is not a valid database column in table '{1}'.  Either the table needs to be created or the create-database-tables attribute must be set.\n\n  {2}\n\n{3}",
                          getName(),
                          getTable().getName(),
                          sql,
                          e.toString()),
                      e);
        }
      } finally {
        conn.close();
      }
    } catch (ConfigException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Generates the clause to load the column.
   */
  public String generateSelect(String id)
  {
    if (id != null)
      return id + "." + _name;
    else
      return _name;
  }

  /**
   * Generates the where clause.
   */
  public String generateMatchArgWhere(String id)
  {
    if (id != null)
      return id + "." + _name + "=?";
    else
      return _name + "=?";
  }

  /**
   * Generates the update clause.
   */
  public String generateUpdateSet()
  {
    return _name + "=?";
  }

  /**
   * Generates the update clause setting to null.
   */
  public String generateUpdateSetNull()
  {
    return _name + "=null";
  }

  /**
   * Generates the prologue.
   */
  public void generatePrologue(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Returns the field name.
   */
  public String getFieldName()
  {
    return "__amber_" + getName();
  }

  /**
   * Generates a string to load the type as a property.
   */
  public void generateSet(JavaWriter out, String pstmt,
                          String index, String value)
    throws IOException
  {
    if (value != null)
      _type.generateSet(out, pstmt, index, value);
    else
      _type.generateSetNull(out, pstmt, index);
  }

  /**
   * Generates a string to load the type as a property.
   */
  public void generateSetVersion(JavaWriter out,
                                 String pstmt,
                                 String index,
                                 String value)
    throws IOException
  {
    _type.generateSetVersion(out, pstmt, index, value);
  }

  /**
   * Generates a string to load the type as a property.
   */
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
    return _type.generateLoad(out, rs, indexVar, index);
  }

  /**
   * Generates a string to load the type as a property.
   */
  public int generateLoadNative(JavaWriter out, int index)
    throws IOException
  {
    return _type.generateLoadNative(out, index);
  }

  /**
   * Generates a string to load the type as a property.
   */
  public void generateNativeColumnNames(ArrayList<String> names)
  {
    names.add(_name);
  }

  /**
   * Converts to the object key.
   */
  public Object toObjectKey(long value)
  {
    return getType().toObject(value);
  }

  protected ConfigException error(String msg, Throwable e)
  {
    if (_configLocation != null)
      return new LineConfigException(_configLocation + msg, e);
    else if (_table.getLocation() != null)
      return new LineConfigException(_table.getLocation() + msg, e);
    else
      return new ConfigException(msg, e);
  }

  /**
   * Returns the name.
   */
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getName() + "]";
  }
}
