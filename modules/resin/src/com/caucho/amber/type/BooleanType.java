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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * The type of a property.
 */
public class BooleanType extends AmberType {
  private static final L10N L = new L10N(BooleanType.class);

  private static final BooleanType BOOLEAN_TYPE = new BooleanType();

  private BooleanType()
  {
  }

  /**
   * Returns the string type.
   */
  public static BooleanType create()
  {
    return BOOLEAN_TYPE;
  }

  /**
   * Returns the type name.
   */
  public String getName()
  {
    return "java.lang.Boolean";
  }

  /**
   * Returns true for a boolean type.
   */
  public boolean isBoolean()
  {
    return true;
  }

  /**
   * Generates the type for the table.
   */
  public String generateCreateColumnSQL(AmberPersistenceUnit manager, int length, int precision, int scale)
  {
    return manager.getCreateColumnSQL(Types.BOOLEAN, length, precision, scale);
  }

  /**
   * Generates a string to load the property.
   */
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
    out.print("com.caucho.amber.type.BooleanType.toBoolean(" +
              rs + ".getBoolean(" + indexVar + " + " + index + "), " +
              rs + ".wasNull())");

    return index + 1;
  }

  /**
   * Generates a string to load the property.
   */
  public int generateLoadNative(JavaWriter out, int index)
    throws IOException
  {
    out.print("com.caucho.amber.type.BooleanType.toBoolean("
              + "rs.getBoolean(columnNames[" + index + "]),"
              + "rs.wasNull())");

    return index + 1;
  }

  /**
   * Generates a string to set the property.
   */
  public void generateSet(JavaWriter out, String pstmt,
                          String index, String value)
    throws IOException
  {
    out.println("if (" + value + " == null)");
    out.println("  " + pstmt + ".setNull(" + index + "++, java.sql.Types.BIT);");
    out.println("else");
    out.println("  " + pstmt + ".setBoolean(" + index + "++, " +
                value + ".booleanValue());");
  }

  /**
   * Generates a string to set the property.
   */
  public void generateSetNull(JavaWriter out, String pstmt,
                              String index)
    throws IOException
  {
    out.println(pstmt + ".setNull(" + index + "++, java.sql.Types.BIT);");
  }

  /**
   * Converts a value to a boolean.
   */
  public static Boolean toBoolean(boolean value, boolean wasNull)
  {
    if (wasNull)
      return null;
    else
      return new Boolean(value);
  }

  /**
   * Gets the value.
   */
  public Object getObject(ResultSet rs, int index)
    throws SQLException
  {
    boolean value = rs.getBoolean(index);

    return rs.wasNull() ? null : (value ? Boolean.TRUE : Boolean.FALSE);
  }
}
