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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.type;

import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The enum type.
 */
public class EnumType extends AmberType {
  private static final Logger log = Log.open(EnumType.class);
  private static final L10N L = new L10N(EnumType.class);

  private Class _beanClass;

  private String _name;

  private boolean _isOrdinal = true;


  public EnumType()
  {
  }

  /**
   * Gets the bean class.
   */
  public Class getBeanClass()
  {
    return _beanClass;
  }

  /**
   * Sets the bean class.
   */
  public void setBeanClass(Class beanClass)
  {
    _beanClass = beanClass;
  }

  /**
   * Returns the type name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Returns true for a numeric type.
   */
  @Override
  public boolean isNumeric()
  {
    return isOrdinal();
  }

  /**
   * Returns true for ordinal
   */
  public boolean isOrdinal()
  {
    return _isOrdinal;
  }

  /**
   * Sets true for ordinal
   */
  public void setOrdinal(boolean isOrdinal)
  {
    _isOrdinal = isOrdinal;
  }

  /**
   * Returns the type as a foreign key.
   */
  @Override
  public AmberType getForeignType()
  {
    return IntegerType.create();
  }

  /**
   * Generates the type for the table.
   */
  @Override
  public String generateCreateColumnSQL(AmberPersistenceUnit manager, int length, int precision, int scale)
  {
    if (_isOrdinal)
      return manager.getCreateColumnSQL(Types.INTEGER, length, precision, scale);
    else {
      if (length == 0)
        length = 255;

      return "varchar(" + length + ")";
    }
  }

  /**
   * Generates a string to load the property.
   */
  @Override
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
    if (_isOrdinal) {
      out.print("(" + getName() + ") com.caucho.amber.type.EnumType.toEnum(" +
                rs + ".getInt(" + indexVar + " + " + index + "), " +
                rs + ".wasNull(), "+
                getName() + ".values())");
    }
    else {
      out.print("(" + getName() + ") com.caucho.amber.type.EnumType.toEnum(" +
                rs + ".getString(" + indexVar + " + " + index + "), " +
                rs + ".wasNull(), "+
                getName() + ".class)");
    }

    return index + 1;
  }

  /**
   * Generates a string to load the property.
   */
  @Override
  public int generateLoadNative(JavaWriter out, int index)
    throws IOException
  {
    if (_isOrdinal) {
      out.print("(" + getName() + ") com.caucho.amber.type.EnumType.toEnum("
                + "rs.getInt(columnNames[" + index + "]), "
                + "rs.wasNull(), "
                + getName() + ".values())");
    }
    else {
      out.print("(" + getName() + ") com.caucho.amber.type.EnumType.toEnum("
                + "rs.getString(columnNames[" + index + "]), "
                + "rs.wasNull(), "
                + getName() + ".class)");
    }

    return index + 1;
  }

  /**
   * Generates a string to load the property.
   */
  @Override
  public int generateLoadForeign(JavaWriter out, String rs,
                                 String indexVar, int index)
    throws IOException
  {
    if (_isOrdinal) {
      out.print("(" + getName() + ") com.caucho.amber.type.EnumType.toEnum(" +
                rs + ".getInt(" + indexVar + " + " + index + "), " +
                rs + ".wasNull(), "+
                getName() + ".values())");
    }
    else {
      out.print("(" + getName() + ") com.caucho.amber.type.EnumType.toEnum(" +
                rs + ".getString(" + indexVar + " + " + index + "), " +
                rs + ".wasNull(), "+
                getName() + ".class)");
    }

    return index + 1;
  }

  /**
   * Generates a string to set the property.
   */
  @Override
  public void generateSet(JavaWriter out, String pstmt,
                          String index, String value)
    throws IOException
  {
    if (_isOrdinal) {
      out.println("if (" + value + " == null)");
      out.println("  " + pstmt + ".setNull(" + index + "++, java.sql.Types.INTEGER);");
      out.println("else");
      out.println("  " + pstmt + ".setInt(" + index + "++, " + value + ".ordinal());");
    }
    else {
      if (pstmt.startsWith("query"))
        out.println(pstmt + ".setString(" + index + "++, " + value + ");");
      else
        out.println("StringType.setString(" + pstmt + ", " + index + "++, " + value + ");");
    }
  }

  /**
   * Sets the value.
   */
  @Override
  public void setParameter(PreparedStatement pstmt, int index, Object value)
    throws SQLException
  {
    if (_isOrdinal) {
      if (value == null)
        pstmt.setNull(index, Types.INTEGER);
      else
        pstmt.setInt(index, ((Enum) value).ordinal());
    }
    else {
      if (value == null)
        pstmt.setNull(index, java.sql.Types.OTHER);
      else
        pstmt.setString(index, value.toString());
    }
  }

  /**
   * Converts to an object.
   */
  @Override
  public String toObject(String value)
  {
    return value;
  }

  /**
   * Converts the value.
   */
  @Override
  public String generateCastFromObject(String value)
  {
    return value + ".ordinal()";
  }

  /**
   * Converts a value to a enum.
   */
  public static Object toEnum(int ordinal,
                              boolean wasNull,
                              Object values[])
  {
    if (wasNull)
      return null;
    else
      return values[ordinal];
  }

  /**
   * Converts a value to a enum.
   */
  public static Object toEnum(String name,
                              boolean wasNull,
                              Class cl)
  {
    if (wasNull)
      return null;
    else
      return Enum.valueOf(cl, name);
  }

  /**
   * Gets the value.
   */
  @Override
  public Object getObject(ResultSet rs, int index)
    throws SQLException
  {
    if (_isOrdinal) {
      Object[] values = getValues();

      if (values == null)
        return null;

      int v = rs.getInt(index);

      return rs.wasNull() ? null : values[v];
    }
    else {
      Class cl = getBeanClass();

      String name = rs.getString(index);

      return rs.wasNull() ? null : Enum.valueOf(cl, name);
    }
  }

  /**
   * Converts to an object.
   */
  @Override
  public Object toObject(long value)
  {
    if (_isOrdinal) {
      Object[] values = getValues();

      if (values == null)
        return null;

      return values[(int) value];
    }

    return null;
  }

  private Object[] getValues()
  {
    try {
      Class cl = getBeanClass();

      Method method = cl.getDeclaredMethod("values");

      Object object = method.invoke(cl);

      return (Object []) object;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      return null;
    }
  }
}
