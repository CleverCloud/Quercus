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

package com.caucho.amber.type;

import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import javax.persistence.TemporalType;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * The type of a property.
 */
public class UtilDateType extends AmberType {
  private static final L10N L = new L10N(UtilDateType.class);

  public static final UtilDateType
    TEMPORAL_DATE_TYPE = new UtilDateType(TemporalType.DATE);
  public static final UtilDateType
    TEMPORAL_TIME_TYPE = new UtilDateType(TemporalType.TIME);
  public static final UtilDateType
    TEMPORAL_TIMESTAMP_TYPE = new UtilDateType(TemporalType.TIMESTAMP);

  private TemporalType _temporalType;

  private UtilDateType(TemporalType temporalType)
  {
    _temporalType = temporalType;
  }

  /**
   * Returns the singleton UtilDate type.
   */
  public static UtilDateType create()
  {
    return TEMPORAL_TIMESTAMP_TYPE;
  }

  /**
   * Returns the type name.
   */
  public String getName()
  {
    return "java.util.Date";
  }

  /**
   * Returns true if the value is assignable to the Java type.
   */
  @Override
  public boolean isAssignableTo(Class javaType)
  {
    return javaType.isAssignableFrom(java.util.Date.class);
  }

  /**
   * Generates the type for the table.
   */
  @Override
  public String generateCreateColumnSQL(AmberPersistenceUnit manager,
                                        int length,
                                        int precision,
                                        int scale)
  {
    return manager.getCreateColumnSQL(Types.TIMESTAMP, length, precision, scale);
  }

  /**
   * Generates a string to load the property.
   */
  @Override
  public int generateLoad(JavaWriter out,
                          String rs,
                          String indexVar,
                          int index)
    throws IOException
  {
    out.print("com.caucho.amber.type.UtilDateType.toDate(" + rs + ".getTimestamp(" + indexVar + " + " + index + "))");

    return index + 1;
  }

  /**
   * Generates a string to load the property.
   */
  @Override
  public int generateLoadNative(JavaWriter out, int index)
    throws IOException
  {
    out.print("com.caucho.amber.type.UtilDateType.toDate(rs.getTimestamp(columnNames[" + index + "]))");

    return index + 1;
  }

  /**
   * Generates a string to set the property.
   */
  @Override
  public void generateSet(JavaWriter out,
                          String pstmt,
                          String index,
                          String value)
    throws IOException
  {
    out.println("if (" + value + " == null)");
    out.println("  " + pstmt + ".setNull(" + index + "++, java.sql.Types.TIMESTAMP);");
    out.println("else");
    out.println("  " + pstmt + ".setTimestamp(" + index + "++, new java.sql.Timestamp(" + value + ".getTime()));");
  }

  /**
   * Gets the value.
   */
  public static java.util.Date toDate(java.sql.Timestamp time)
    throws SQLException
  {
    return time;
  }

  /**
   * Gets the value.
   */
  @Override
  public Object getObject(ResultSet rs, int index)
    throws SQLException
  {
    java.sql.Timestamp date = rs.getTimestamp(index);

    if (date == null)
      return null;
    else
      return new java.util.Date(date.getTime());
  }

  /**
   * Sets the value.
   */
  @Override
  public void setParameter(PreparedStatement pstmt,
                           int index,
                           Object value)
    throws SQLException
  {
    switch (_temporalType) {
    case DATE:
      pstmt.setObject(index, value, Types.DATE);
      break;

    case TIME:
      pstmt.setObject(index, value, Types.TIME);
      break;

    default:
      pstmt.setObject(index, value, Types.TIMESTAMP);
      break;
    }
  }
}
