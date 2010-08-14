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

import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The type of a property.
 */
public class StringType extends AmberType {
  private static final L10N L = new L10N(StringType.class);

  private static final StringType STRING_TYPE = new StringType();

  private StringType()
  {
  }

  /**
   * Returns the string type.
   */
  public static StringType create()
  {
    return STRING_TYPE;
  }

  /**
   * Returns the type name.
   */
  public String getName()
  {
    return "java.lang.String";
  }

  /**
   * Generates the type for the table.
   */
  public String generateCreateColumnSQL(AmberPersistenceUnit manager, int length, int precision, int scale)
  {
    if (length == 0)
      length = 255;

    return "varchar(" + length + ")";
    // return manager.getCreateColumnSQL(Types.VARCHAR, length);
  }

  /**
   * Generates a string to load the property.
   */
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
    out.print(rs + ".getString(" + indexVar + " + " + index + ")");

    return index + 1;
  }

  /**
   * Generates a string to load the property.
   */
  public int generateLoadNative(JavaWriter out, int index)
    throws IOException
  {
    out.print("rs.getString(columnNames[" + index + "])");

    return index + 1;
  }

  /**
   * Generates a string to set the property.
   */
  public void generateSet(JavaWriter out, String pstmt,
                          String index, String value)
    throws IOException
  {
    if (pstmt.startsWith("query"))
      out.println(pstmt + ".setString(" + index + "++, " + value + ");");
    else
      out.println("StringType.setString(" + pstmt + ", " + index + "++, " + value + ");");
  }

  /**
   * Sets the value.
   */
  public void setParameter(PreparedStatement pstmt, int index, Object value)
    throws SQLException
  {
    if (value == null) {
      // XXX: issue with derby.
      // pstmt.setNull(index, java.sql.Types.OTHER);
      pstmt.setString(index, null);
    }
    else if (value instanceof String)
      pstmt.setString(index, (String) value);
    else // ejb/0623
      pstmt.setObject(index, value);
  }

  /**
   * Sets the value.
   */
  public void setParameter(PreparedStatement pstmt, int index, String value)
    throws SQLException
  {
    if (value == null)
      pstmt.setNull(index, java.sql.Types.VARCHAR);
    else
      pstmt.setString(index, value);
  }

  /**
   * Gets the value.
   */
  public Object getObject(ResultSet rs, int index)
    throws SQLException
  {
    return rs.getString(index);
  }

  /**
   * Sets the value.
   */
  public static void setString(PreparedStatement pstmt, int index, Object value)
    throws SQLException
  {
    if (value == null) {
      // XXX: issue with derby.
      // pstmt.setNull(index, java.sql.Types.OTHER);
      pstmt.setString(index, null);
    }
    else if (value instanceof String)
      pstmt.setString(index, (String) value);
    else // ejb/0623
      pstmt.setObject(index, value);
  }

  /**
   * Sets the value.
   */
  public static void setString(PreparedStatement pstmt, int index, String value)
    throws SQLException
  {
    if (value == null)
      pstmt.setNull(index, java.sql.Types.VARCHAR);
    else
      pstmt.setString(index, value);
  }
}
