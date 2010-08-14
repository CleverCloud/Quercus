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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.eswrap.java.sql;

import com.caucho.es.Call;
import com.caucho.es.ESBase;
import com.caucho.es.Global;
import com.caucho.util.NullIterator;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.logging.Logger;

public class ResultSetEcmaWrap {
  private static final Logger log = Logger.getLogger(ResultSet.class.getName());

  public static String getString(ResultSet rs, ESBase col)
    throws Throwable
  {
    ESBase key = col.toPrimitive();

    if (key.isString())
      return rs.getString(key.toString());
    else
      return rs.getString((int) key.toNum());
  }

  public static boolean getBoolean(ResultSet rs, ESBase col)
    throws Throwable
  {
    ESBase key = col.toPrimitive();

    if (key.isString())
      return rs.getBoolean(key.toString());
    else
      return rs.getBoolean((int) key.toNum());
  }

  public static byte getByte(ResultSet rs, ESBase col)
    throws Throwable
  {
    ESBase key = col.toPrimitive();

    if (key.isString())
      return rs.getByte(key.toString());
    else
      return rs.getByte((int) key.toNum());
  }

  public static short getShort(ResultSet rs, ESBase col)
    throws Throwable
  {
    ESBase key = col.toPrimitive();

    if (key.isString())
      return rs.getShort(key.toString());
    else
      return rs.getShort((int) key.toNum());
  }

  public static int getInt(ResultSet rs, ESBase col)
    throws Throwable
  {
    ESBase key = col.toPrimitive();

    if (key.isString())
      return rs.getInt(key.toString());
    else
      return rs.getInt((int) key.toNum());
  }

  public static long getLong(ResultSet rs, ESBase col)
    throws Throwable
  {
    ESBase key = col.toPrimitive();

    if (key.isString())
      return rs.getLong(key.toString());
    else
      return rs.getLong((int) key.toNum());
  }

  public static float getFloat(ResultSet rs, ESBase col)
    throws Throwable
  {
    ESBase key = col.toPrimitive();

    if (key.isString())
      return rs.getFloat(key.toString());
    else
      return rs.getFloat((int) key.toNum());
  }

  public static double getDouble(ResultSet rs, ESBase col)
    throws Throwable
  {
    ESBase key = col.toPrimitive();

    if (key.isString())
      return rs.getDouble(key.toString());
    else
      return rs.getDouble((int) key.toNum());
  }

  public static BigDecimal getBigDecimal(ResultSet rs, ESBase col, int i)
    throws Throwable
  {
    ESBase key = col.toPrimitive();

    if (key.isString())
      return rs.getBigDecimal(key.toString(), i);
    else
      return rs.getBigDecimal((int) key.toNum(), i);
  }

  public static byte[] getBytes(ResultSet rs, ESBase col)
    throws Throwable
  {
    ESBase key = col.toPrimitive();

    if (key.isString())
      return rs.getBytes(key.toString());
    else
      return rs.getBytes((int) key.toNum());
  }

  public static ESBase getDate(ResultSet rs, Call call, int len)
    throws Throwable
  {
    ESBase col = call.getArg(0, len);
    ESBase key = col.toPrimitive();
    java.util.Date date;

    if (key.isString())
      date = rs.getDate(key.toString());
    else
      date = rs.getDate((int) key.toNum());

    return call.createDate(date.getTime());
  }

  public static ESBase getTime(ResultSet rs, Call call, int len)
    throws Throwable
  {
    ESBase col = call.getArg(0, len);
    ESBase key = col.toPrimitive();
    java.util.Date date;

    if (key.isString())
      date = rs.getTime(key.toString());
    else
      date = rs.getTime((int) key.toNum());

    return call.createDate(date.getTime());
  }

  public static ESBase getTimestamp(ResultSet rs, Call call, int len)
    throws Throwable
  {
    ESBase col = call.getArg(0, len);
    ESBase key = col.toPrimitive();
    java.util.Date date;

    if (key.isString())
      date = rs.getTimestamp(key.toString());
    else
      date = rs.getTimestamp((int) key.toNum());

    return call.createDate(date.getTime());
  }

  public static InputStream getAsciiStream(ResultSet rs, ESBase col)
    throws Throwable
  {
    ESBase key = col.toPrimitive();

    if (key.isString())
      return rs.getAsciiStream(key.toString());
    else
      return rs.getAsciiStream((int) key.toNum());
  }

  public static InputStream getUnicodeStream(ResultSet rs, ESBase col)
    throws Throwable
  {
    ESBase key = col.toPrimitive();

    if (key.isString())
      return rs.getUnicodeStream(key.toString());
    else
      return rs.getUnicodeStream((int) key.toNum());
  }

  public static InputStream getBinaryStream(ResultSet rs, ESBase col)
    throws Throwable
  {
    ESBase key = col.toPrimitive();

    if (key.isString())
      return rs.getBinaryStream(key.toString());
    else
      return rs.getBinaryStream((int) key.toNum());
  }

  public static Object getObject(ResultSet rs, ESBase col)
    throws Throwable
  {
    ESBase key = col.toPrimitive();

    if (key.isString())
      return rs.getObject(key.toString());
    else
      return rs.getObject((int) key.toNum());
  }

  public static String getByname(ResultSet rs, String string)
    throws SQLException
  {
    return rs.getString(string);
  }

  public static Object get(ResultSet rs, String key)
    throws Throwable
  {
    return get(rs, rs.findColumn(key));
  }

  public static Object get(ResultSet rs, int index)
    throws Throwable
  {
    ResultSetMetaData md = rs.getMetaData();
    
    switch (md.getColumnType(index)) {
    case Types.BIT:
      return new Boolean(rs.getInt(index) == 1);

    case Types.TINYINT:
    case Types.SMALLINT:
    case Types.INTEGER:
    case Types.FLOAT:
    case Types.REAL:
    case Types.DOUBLE:
      return new Double(rs.getDouble(index));

    case Types.CHAR:
    case Types.VARCHAR:
      return rs.getString(index);

    case Types.NULL:
      return null;

      // XXX: the following are bogus
    case Types.BIGINT:
    case Types.NUMERIC:
    case Types.DECIMAL:
      return rs.getString(index);

    case Types.LONGVARCHAR:
      return rs.getAsciiStream(index);

    case Types.DATE:
      return rs.getDate(index);

    case Types.TIME:
      return rs.getTime(index);

    case Types.TIMESTAMP:
      return rs.getTimestamp(index);

    case Types.BINARY:
    case Types.VARBINARY:
      return rs.getBytes(index);

    case Types.LONGVARBINARY:
      return rs.getBinaryStream(index);

    default:
      return rs.getString(index);
    }
  }

  public static Object toObject(ResultSet rs, Call call, int length)
    throws Throwable
  {
    ResultSetMetaData md;

    md = rs.getMetaData();

    Global global = Global.getGlobalProto();
    ESBase obj;
    if (length > 0)
      obj = call.getArg(0, length);
    else
      obj = global.createObject();

    int nColumns = md.getColumnCount();
    for (int i = 0; i < nColumns; i++) {
      String name = md.getColumnName(i + 1);
      Object value = get(rs, i + 1);

      obj.setProperty(name, global.wrap(value));
    }

    return obj;
  }

  public static Iterator keys(ResultSet rs)
  {
    try {
      return new ResultSetIterator(rs);
    } catch (Exception e) {
      return NullIterator.create();
    }
  }

  static class ResultSetIterator implements Iterator {
    ResultSet rs;
    ResultSetMetaData md;
    int nColumns;
    int i;

    public boolean hasNext()
    {
      return i < nColumns;
    }

    public Object next()
    {
      try {
        return md.getColumnName(++i);
      } catch (SQLException e) {
        return null;
      }
    }

    public void remove() { throw new UnsupportedOperationException(); }

    ResultSetIterator(ResultSet rs)
      throws SQLException
    {
      this.rs = rs;

      this.md = rs.getMetaData();
      nColumns = md.getColumnCount();
    }
  }

  private ResultSetEcmaWrap()
  {
  }
}
