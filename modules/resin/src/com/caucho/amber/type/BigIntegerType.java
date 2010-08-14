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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * The type of a property.
 */
public class BigIntegerType extends AmberType {
  private static final L10N L = new L10N(BigIntegerType.class);

  private BigIntegerType()
  {
  }

  /**
   * Returns the singleton BigInteger type.
   */
  public static BigIntegerType create()
  {
    return new BigIntegerType();
  }

  /**
   * Returns the type name.
   */
  public String getName()
  {
    return "java.math.BigInteger";
  }

  /**
   * Returns true for a numeric type.
   */
  public boolean isNumeric()
  {
    return true;
  }

  /**
   * Generates a string to load the property.
   */
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
    out.print(rs + ".getBigDecimal(" + indexVar + " + " + index + ")");
    out.print(" == null || " + rs + ".wasNull() ? null : ");
    out.print(rs + ".getBigDecimal(" + indexVar + " + " + index + ").toBigInteger()");

    return index + 1;
  }

  /**
   * Generates a string to load the property.
   */
  @Override
  public int generateLoadNative(JavaWriter out, int index)
    throws IOException
  {
    out.print("com.caucho.amber.type.BigIntegerType.toBigInteger(rs.getBigDecimal(columnNames[" + index + "]), rs.wasNull())");

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
    out.println("  " + pstmt + ".setNull(" + index + "++, java.sql.Types.DECIMAL);");
    out.println("else");
    out.println("  " + pstmt + ".setBigDecimal(" + index + "++, new java.math.BigDecimal(" + value + "));");
  }

  /**
   * Sets the value.
   */
  public void setParameter(PreparedStatement pstmt, int index, Object value)
    throws SQLException
  {
    if (value == null)
      pstmt.setNull(index, java.sql.Types.DECIMAL);
    else
      pstmt.setBigDecimal(index, new BigDecimal((BigInteger) value));
  }

  /**
   * Gets the value.
   */
  public Object getObject(ResultSet rs, int index)
    throws SQLException
  {
    BigDecimal v = rs.getBigDecimal(index);

    if (rs.wasNull())
      return null;

    return v.toBigInteger();
  }

  public String generateCreateColumnSQL(AmberPersistenceUnit manager,
                                        int length,
                                        int precision,
                                        int scale)
  {
    return manager.getCreateColumnSQL(Types.NUMERIC, length, precision, scale);
  }

  /**
   * Converts a value to a int.
   */
  public static BigInteger toBigInteger(BigDecimal value, boolean wasNull)
  {
    if (wasNull || value == null)
      return null;
    else
      return value.toBigInteger();
  }
}
