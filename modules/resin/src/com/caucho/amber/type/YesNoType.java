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
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The type of a property.
 */
public class YesNoType extends AmberType {
  private static final L10N L = new L10N(YesNoType.class);

  private static final YesNoType YES_NO_TYPE = new YesNoType();

  private YesNoType()
  {
  }

  /**
   * Returns the singleton type.
   */
  public static YesNoType create()
  {
    return YES_NO_TYPE;
  }

  /**
   * Returns the type name.
   */
  public String getName()
  {
    return "java.lang.Boolean";
  }

  /**
   * Generates a string to load the property.
   */
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
    out.print("com.caucho.amber.type.YesNoType.toBoolean(" +
              rs + ".getString(" + indexVar + " + " + index + "), " +
              rs + ".wasNull())");

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
    out.println("  " + pstmt + ".setNull(" + index + "++, 0);");
    out.println("else");
    out.println("  " + pstmt + ".setString(" + index + "++, " +
                value + ".booleanValue() ? \"y\" : \"n\");");
  }

  /**
   * Converts a value to a boolean.
   */
  public static Boolean toBoolean(String value, boolean wasNull)
  {
    if (wasNull)
      return null;
    else if ("y".equalsIgnoreCase(value))
      return Boolean.TRUE;
    else if ("n".equalsIgnoreCase(value))
      return Boolean.FALSE;
    else
      return Boolean.FALSE;
  }

  /**
   * Gets the value.
   */
  public Object getObject(ResultSet rs, int index)
    throws SQLException
  {
    String value = rs.getString(index);

    if (rs.wasNull())
      return null;
    else if ("y".equalsIgnoreCase(value))
      return Boolean.TRUE;
    else if ("n".equalsIgnoreCase(value))
      return Boolean.FALSE;
    else
      return Boolean.FALSE;
  }
}
