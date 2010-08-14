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

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * The primitive short type.
 */
public class PrimitiveShortType extends PrimitiveType {
  private static final L10N L = new L10N(PrimitiveShortType.class);

  private static final PrimitiveShortType SHORT_TYPE = new PrimitiveShortType();

  private PrimitiveShortType()
  {
  }

  /**
   * Returns the boolean type.
   */
  public static PrimitiveShortType create()
  {
    return SHORT_TYPE;
  }

  /**
   * Returns the type name.
   */
  public String getName()
  {
    return "short";
  }

  /**
   * Returns true for a numeric type.
   */
  public boolean isNumeric()
  {
    return true;
  }

  /**
   * Returns the type as a foreign key.
   */
  public AmberType getForeignType()
  {
    return ShortType.create();
  }

  /**
   * Generates the type for the table.
   */
  public String generateCreateColumnSQL(AmberPersistenceUnit manager, int length, int precision, int scale)
  {
    return manager.getCreateColumnSQL(Types.SMALLINT, length, precision, scale);
  }

  /**
   * Generates a string to load the property.
   */
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
    out.print(rs + ".getShort(" + indexVar + " + " + index + ")");

    return index + 1;
  }

  /**
   * Generates a string to load the property.
   */
  public int generateLoadNative(JavaWriter out, int index)
    throws IOException
  {
    out.print("rs.getShort(columnNames[" + index + "])");

    return index + 1;
  }

  /**
   * Generates a string to set the property.
   */
  public void generateSet(JavaWriter out, String pstmt,
                          String index, String value)
    throws IOException
  {
    out.println(pstmt + ".setShort(" + index + "++, " + value + ");");
  }

  /**
   * Generates a string to set the property.
   */
  public void generateSetNull(JavaWriter out, String pstmt, String index)
    throws IOException
  {
    out.println(pstmt + ".setNull(" + index + "++, java.sql.Types.SMALLINT);");
  }

  /**
   * Generates a string to set the property.
   */
  public void generateSetVersion(JavaWriter out,
                                 String pstmt,
                                 String index,
                                 String value)
    throws IOException
  {
    out.println(pstmt + ".setShort(" + index + "++, " + value + " + 1);");
  }

  /**
   * Converts to an object.
   */
  public String toObject(String value)
  {
    return "new Short(" + value + ")";
  }

  /**
   * Converts the value.
   */
  public String generateCastFromObject(String value)
  {
    return "((Number) " + value + ").shortValue()";
  }

  /**
   * Gets the value.
   */
  public Object getObject(ResultSet rs, int index)
    throws SQLException
  {
    short v = rs.getShort(index);

    return rs.wasNull() ? null : new Short(v);
  }

  /**
   * Converts to an object.
   */
  public Object toObject(long value)
  {
    return new Short((short) value);
  }

  /**
   * Sets the value.
   */
  public void setParameter(PreparedStatement pstmt, int index, Object value)
    throws SQLException
  {
    if (value instanceof Number)
      pstmt.setString(index, value.toString());
    else
      throw new IllegalArgumentException("Invalid argument for setParameter.");
  }
}
