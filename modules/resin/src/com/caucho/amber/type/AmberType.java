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

package com.caucho.amber.type;

import com.caucho.amber.entity.EntityItem;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * The type of a property.
 */
abstract public class AmberType {
  private static final L10N L = new L10N(AmberType.class);

  /**
   * Returns the type name.
   */
  abstract public String getName();

  /**
   * Returns true for a boolean type.
   */
  public boolean isBoolean()
  {
    return false;
  }

  /**
   * Returns true for a numeric type.
   */
  public boolean isNumeric()
  {
    return false;
  }

  /**
   * Returns the java type.
   */
  public String getJavaTypeName()
  {
    return getName();
  }

  /**
   * Returns the number of columns the type takes up.
   */
  public int getColumnCount()
  {
    return 1;
  }

  /**
   * Initialize the type.
   */
  public void init()
    throws ConfigException
  {
  }

  /**
   * Returns the type as a foreign key.
   */
  public AmberType getForeignType()
  {
    return this;
  }

  /**
   * Returns the java class of the type as a foreign key.
   */
  public String getForeignTypeName()
  {
    return getForeignType().getJavaTypeName();
  }

  /**
   * Returns true if the value is assignable to the Java type.
   */
  public boolean isAssignableTo(Class javaType)
  {
    return true;
  }

  /**
   * Generates the type for the table.
   */
  public String generateCreateColumnSQL(AmberPersistenceUnit manager, int length, int precision, int scale)
  {
    if (length == 0)
      length = 255;

    return manager.getCreateColumnSQL(Types.VARCHAR, length, precision, scale);
  }

  /**
   * Generates a string to load the type as a property.
   */
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Generates a string to load the type as a property.
   */
  public int generateLoadNative(JavaWriter out, int index)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Generates a string to load the type as a property.
   */
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index,
                          Class targetType)
    throws IOException
  {
    String i = indexVar + " + " + index;

    if ("java.lang.Byte".equals(targetType.getName()))
      out.print("new Byte((byte) " + rs + ".getInt(" + i + "))");
    else if ("java.lang.Short".equals(targetType.getName()))
      out.print("new Short((short) " + rs + ".getInt(" + i + "))");
    else if ("java.lang.Integer".equals(targetType.getName())) {
      // ejb/0629
      out.print("new Integer(" + rs + ".getInt(" + i + "))");
    }
    else if ("java.lang.Long".equals(targetType.getName()))
      out.print("new Long(" + rs + ".getLong(" + i + "))");
    else if ("java.lang.Float".equals(targetType.getName()))
      out.print("new Float((float) " + rs + ".getDouble(" + i + "))");
    else if ("java.lang.Double".equals(targetType.getName()))
      out.print("new Double(" + rs + ".getDouble(" + i + "))");
    else if ("java.lang.String".equals(targetType.getName()))
      out.print(rs + ".getString(" + i + ")");
    else {
      out.print("(" + targetType.getName() + ") ");
      out.print(rs + ".getObject(" + i + ")");
    }

    return index + 1;
  }

  /**
   * Generates a string to load the type as a property.
   */
  public int generateLoadForeign(JavaWriter out, String rs,
                                 String indexVar, int index)
    throws IOException
  {
    return getForeignType().generateLoad(out, rs, indexVar, index);
  }

  /**
   * Generates a string to set the type as a property.
   */
  public void generateSet(JavaWriter out, String pstmt,
                          String index, String value)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Generates a string to set the type as a property.
   */
  public void generateSetVersion(JavaWriter out,
                                 String pstmt,
                                 String index,
                                 String value)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Generates the increment version.
   */
  public String generateIncrementVersion(String value)
    throws IOException
  {
    return value + " + 1";
  }

  /**
   * Generates a string to set the property.
   */
  public void generateSetNull(JavaWriter out, String pstmt, String index)
    throws IOException
  {
    generateSet(out, pstmt, index, null);
  }

  /**
   * Sets the value.
   */
  public void setParameter(PreparedStatement pstmt, int index, Object value)
    throws SQLException
  {
    pstmt.setObject(index, value);
  }

  /**
   * Gets the value.
   */
  public Object getObject(ResultSet rs, int index)
    throws SQLException
  {
    return rs.getObject(index);
  }

  /**
   * Finds the object
   */
  public EntityItem findItem(AmberConnection aConn, ResultSet rs, int index)
    throws SQLException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Gets the value.
   */
  public Object getObject(AmberConnection aConn, ResultSet rs, int index)
    throws SQLException
  {
    return getObject(rs, index);
  }

  /**
   * Converts to an object.
   */
  public String toObject(String value)
  {
    return value;
  }

  /**
   * Converts from an object.
   */
  public String fromObject(String value)
  {
    return generateCastFromObject(value);
  }

  /**
   * Converts to an object.
   */
  public Object toObject(long value)
  {
    return new Long(value);
  }

  /**
   * Converts the value.
   */
  public String generateCastFromObject(String value)
  {
    return "((" + getName() + ") " + value + ")";
  }

  /**
   * Returns a boolean equality.
   */
  public String generateEquals(String a, String b)
  {
    return a + ".equals(" + b + ")";
  }

  /**
   * Returns a test for null.
   */
  public String generateIsNull(String value)
  {
    return "(" + value + " == " + generateNull() + ")";
  }

  /**
   * Returns a test for null.
   */
  public String generateNull()
  {
    return "null";
  }

  /**
   * Returns true for an auto-increment type.
   */
  public boolean isAutoIncrement()
  {
    return false;
  }
}
