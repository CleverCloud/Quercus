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

import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The type of a property.
 */
public class ByteArrayType extends ArrayType {
  private static final L10N L = new L10N(ByteArrayType.class);

  private ByteArrayType()
  {
  }

  /**
   * Returns the singleton ByteArray type.
   */
  public static ByteArrayType create()
  {
    return new ByteArrayType();
  }

  /**
   * Returns the type name.
   */
  public String getName()
  {
    return "java.lang.Byte[]";
  }

  /**
   * Returns the java type.
   */
  public String getJavaTypeName()
  {
    return "java.lang.Byte[]";
  }

  /**
   * Returns the java type for a single entry.
   */
  public String getJavaObjectTypeName()
  {
    return "java.lang.Byte";
  }

  /**
   * Returns the corresponding primitive array
   * type name.
   */
  public String getPrimitiveArrayTypeName()
  {
    return "byte[]";
  }

  /**
   * Generates a string to load the property.
   */
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
    out.print(rs + ".getBytes(" + indexVar + " + " + index + ")");

    return index + 1;
  }

  /**
   * Generates a string to load the property.
   */
  public int generateLoadNative(JavaWriter out, int index)
    throws IOException
  {
    out.print("rs.getBytes(columnNames[" + index + "])");

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
    out.println("  " + pstmt + ".setNull(" + index + "++, java.sql.Types.LONGVARBINARY);");
    out.println("else {");
    out.println("  java.lang.Byte[] super_temp_" + index + " = " + value + ";");
    out.println("  byte[] temp_" + index + " = new byte[super_temp_" + index + ".length];");
    out.println("  for (int i=0; i < temp_" + index + ".length; i++)");
    out.println("    temp_" + index + "[i] = super_temp_" + index + "[i].byteValue();");
    out.println("  " + pstmt + ".setBytes(" + index + "++, temp_" + index + ");");
    out.println("}");
  }

  /**
   * Sets the value.
   */
  public void setParameter(PreparedStatement pstmt, int index, Object value)
    throws SQLException
  {
    Byte[] wrapperByte = (Byte []) value;

    byte[] primitiveByte = new byte[wrapperByte.length];
    for (int i=0; i<wrapperByte.length; i++)
      primitiveByte[i] = wrapperByte[i].byteValue();

    pstmt.setBytes(index, primitiveByte);
  }

  /**
   * Gets the value.
   */
  public Object getObject(ResultSet rs, int index)
    throws SQLException
  {
    byte[] primitiveByte = rs.getBytes(index);

    if (rs.wasNull())
      return null;

    Byte[] wrapperByte = new Byte[primitiveByte.length];
    for (int i=0; i < primitiveByte.length; i++)
      wrapperByte[i] = new Byte(primitiveByte[i]);

    return wrapperByte;
  }
}
