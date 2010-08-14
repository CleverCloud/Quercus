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

package com.caucho.db.sql;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;

class ParamExpr extends Expr {
  private static final int NULL = 0;
  private static final int BOOLEAN = NULL + 1;
  private static final int STRING = BOOLEAN + 1;
  private static final int LONG = STRING + 1;
  private static final int DOUBLE = LONG + 1;
  private static final int DATE = DOUBLE + 1;
  private static final int BINARY = DATE + 1;
  private static final int BYTES = BINARY + 1;

  private final int _index;

  /*
  private int _type = NULL;

  private String _stringValue;
  private long _longValue;
  private double _doubleValue;

  private InputStream _binaryStream;
  private int _streamLength;
  private byte []_bytes;
  */

  ParamExpr(int index)
  {
    _index = index + 1;
  }

  /**
   * Returns the type of the expression.
   */
  public Class<?> getType()
  {
    return Object.class;
    
    /*
    switch (_type) {
    case NULL:
      return Object.class;

    case BOOLEAN:
      return boolean.class;

    case STRING:
      return String.class;

    case LONG:
      return long.class;

    case DOUBLE:
      return double.class;

    case DATE:
      return java.util.Date.class;

    case BINARY:
      return java.io.InputStream.class;

    case BYTES:
      return byte[].class;

    default:
      return Object.class;
    }
    */
  }

  /**
   * Returns true for a parameter expression.
   */
  @Override
  public boolean isParam()
  {
    return true;
  }

  /**
   * XXX: Since we're only using this database internally, we can guarantee
   * non-null for parameters.
   */
  @Override
  public boolean isNullable()
  {
    return false;
  }

  /**
   * Returns the subcost based on the given FromList.
   */
  public long subCost(ArrayList<FromItem> fromList)
  {
    return 0;
  }

  /**
   * Clears the value.
   */
  public void clear()
  {
    // _type = NULL;
  }

  /**
   * Sets the value as a string.
   */
  /*
  public void setString(String value)
  {
    if (value == null)
      _type = NULL;
    else {
      _type = STRING;
      _stringValue = value;
    }
  }
  */

  /**
   * Sets the value as a boolean.
   */
  /*
  public void setBoolean(boolean value)
  {
    _type = BOOLEAN;
    _longValue = value ? 1 : 0;
  }
  */

  /**
   * Sets the value as a long.
   */
  /*
  public void setLong(long value)
  {
    _type = LONG;
    _longValue = value;
  }
  */

  /**
   * Sets the value as a double.
   */
  /*
  public void setDouble(double value)
  {
    _type = DOUBLE;
    _doubleValue = value;
  }
  */

  /**
   * Sets the value as a date.
   */
  /*
  public void setDate(long value)
  {
    _type = DATE;
    _longValue = value;
  }
  */

  /**
   * Sets the value as a stream.
   */
  /*
  public void setBinaryStream(InputStream is, int length)
  {
    _type = BINARY;
    _binaryStream = is;
    _streamLength = length;
  }
  */

  /**
   * Sets the value as a stream.
   */
  /*
  public void setBytes(byte []bytes)
  {
    _type = BYTES;
    _bytes = bytes;
  }
  */

  /**
   * Checks if the value is null
   *
   * @param rows the current database tuple
   *
   * @return the string value
   */
  @Override
  public boolean isBinaryStream(QueryContext context)
  {
    return context.isBinaryStream(_index);
  }

  /**
   * Checks if the value is null
   *
   * @param rows the current database tuple
   *
   * @return the string value
   */
  @Override
  public boolean isNull(QueryContext context)
    throws SQLException
  {
    return context.isNull(_index);
  }

  /**
   * Evaluates the expression as a string.
   *
   * @param rows the current database tuple
   *
   * @return the string value
   */
  @Override
  public String evalString(QueryContext context)
    throws SQLException
  {
    return context.getString(_index);
    /*
    switch (_type) {
    case NULL:
      return null;

    case BOOLEAN:
      return _longValue != 0 ? "1" : "0";

    case STRING:
      return _stringValue;

    case LONG:
      return String.valueOf(_longValue);

    case DATE:
      return QDate.formatISO8601(_longValue);

    case DOUBLE:
      return String.valueOf(_doubleValue);

    case BYTES:
    {
      StringBuilder sb = new StringBuilder();
      int len = _bytes.length;
      for (int i = 0; i < len; i++) {
        sb.append((char) (_bytes[i] & 0xff));
      }

      return sb.toString();
    }

    default:
      throw new UnsupportedOperationException(String.valueOf(_type));
    }
    */
  }

  /**
   * Evaluates the expression as a boolean.
   *
   * @param rows the current database tuple
   *
   * @return the boolean value
   */
  public int evalBoolean(QueryContext context)
    throws SQLException
  {
    return context.getBoolean(_index);
    /*
    switch (_type) {
    case NULL:
      return UNKNOWN;

    case BOOLEAN:
    case LONG:
      return _longValue != 0 ? TRUE : FALSE;

    case DOUBLE:
      return _doubleValue != 0 ? TRUE : FALSE;

    default:
      throw new UnsupportedOperationException();
    }
    */
  }

  /**
   * Evaluates the expression as a long.
   *
   * @param rows the current database tuple
   *
   * @return the long value
   */
  @Override
  public long evalLong(QueryContext context)
    throws SQLException
  {
    return context.getLong(_index);
    
    /*
    switch (_type) {
    case NULL:
      return 0;

    case BOOLEAN:
    case LONG:
    case DATE:
      return _longValue;

    case DOUBLE:
      return (long) _doubleValue;

    case STRING:
      return Long.parseLong(_stringValue);

    default:
      throw new UnsupportedOperationException("" + _type);
    }
    */
  }

  /**
   * Evaluates the expression as a double.
   *
   * @param rows the current database tuple
   *
   * @return the double value
   */
  @Override
  public double evalDouble(QueryContext context)
    throws SQLException
  {
    return context.getDouble(_index);
    
    /*
    switch (_type) {
    case NULL:
      return 0;

    case LONG:
    case DATE:
      return _longValue;

    case DOUBLE:
      return _doubleValue;

    case STRING:
      return Double.parseDouble(_stringValue);

    default:
      throw new UnsupportedOperationException(_index + ":" + _type + " " + toString());
    }
    */
  }

  /**
   * Evaluates the expression as a date
   *
   * @param rows the current database tuple
   *
   * @return the date value
   */
  @Override
  public long evalDate(QueryContext context)
    throws SQLException
  {
    return context.getDate(_index);
    
    /*
    switch (_type) {
    case NULL:
      return 0;

    case LONG:
    case DATE:
      return _longValue;

    case DOUBLE:
      return (long) _doubleValue;

    default:
      throw new UnsupportedOperationException();
    }
    */
  }

  /**
   * Evaluates the expression as a stream.
   *
   * @param rows the current database tuple
   *
   * @return the string value
   */
  @Override
  public byte []evalBytes(QueryContext context)
    throws SQLException
  {
    return context.getBytes(_index);
    
    /*
    switch (_type) {
    case NULL:
      return null;

    case BINARY:
      return _binaryStream;

    default:
      throw new UnsupportedOperationException();
    }
    */
  }
  
  @Override
  public InputStream evalStream(QueryContext context)
  {
    return context.getBinaryStream(_index);
  }

  /**
   * Evaluates the expression to a buffer
   *
   * @param result the result buffer
   *
   * @return the length of the result
   */
  public int evalToBuffer(QueryContext context,
                          byte []buffer,
                          int offset)
    throws SQLException
  {
    // context.evalToBuffer(_index, buffer, offset);
    throw new UnsupportedOperationException(getClass().getName());
  }

  public String toString()
  {
    return "?" + _index;
  }
}
