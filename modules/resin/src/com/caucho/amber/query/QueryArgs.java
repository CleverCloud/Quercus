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

package com.caucho.amber.query;

import com.caucho.amber.type.*;

/**
 * Represents the arguments to a query.
 */
public class QueryArgs {
  private AmberType []_argTypes;
  private Object []_argValues;

  public QueryArgs(int length)
  {
    _argTypes = new AmberType[length];
    _argValues = new Object[length];
  }

  /**
   * Returns the arg type array.
   */
  AmberType []getArgTypes()
  {
    return _argTypes;
  }

  /**
   * Returns the arg values
   */
  Object []getArgValues()
  {
    return _argValues;
  }

  /**
   * Sets the argument with a string
   */
  public void setString(int index, String v)
  {
    _argTypes[index - 1] = StringType.create();
    _argValues[index - 1] = v;
  }

  /**
   * Sets the argument with a byte
   */
  public void setByte(int index, byte v)
  {
    _argTypes[index - 1] = ByteType.create();
    _argValues[index - 1] = new Integer(v);
  }

  /**
   * Sets the argument with a short
   */
  public void setShort(int index, short v)
  {
    _argTypes[index - 1] = ShortType.create();
    _argValues[index - 1] = new Integer(v);
  }

  /**
   * Sets the argument with an int
   */
  public void setInt(int index, int v)
  {
    _argTypes[index - 1] = IntegerType.create();
    _argValues[index - 1] = new Integer(v);
  }

  /**
   * Sets the argument with a string
   */
  public void setLong(int index, long v)
  {
    _argTypes[index - 1] = LongType.create();
    _argValues[index - 1] = new Long(v);
  }

  /**
   * Sets the argument with a double
   */
  public void setDouble(int index, double v)
  {
    _argTypes[index - 1] = DoubleType.create();
    _argValues[index - 1] = new Double(v);
  }

  /**
   * Sets the argument with a timestamp
   */
  public void setTimestamp(int index, java.sql.Timestamp v)
  {
    _argTypes[index - 1] = SqlTimestampType.create();
    _argValues[index - 1] = v;
  }

  /**
   * Sets the argument with a date
   */
  public void setDate(int index, java.sql.Date v)
  {
    _argTypes[index - 1] = SqlDateType.create();
    _argValues[index - 1] = v;
  }

  /**
   * Sets the argument with a null
   */
  public void setNull(int index, int v)
  {
    _argTypes[index - 1] = StringType.create();
    _argValues[index - 1] = null;
  }

  /**
   * Returns a hash code.
   */
  public int hashCode()
  {
    int hash = 37;

    for (int i = _argValues.length - 1; i >= 0; i--) {
      Object v = _argValues[i];

      if (v != null)
        hash = 31 * hash + v.hashCode();
      else
        hash = 31 * hash + 17;
    }

    return hash;
  }

  /**
   * Returns true if equals.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof QueryArgs))
      return false;

    QueryArgs args = (QueryArgs) o;

    if (_argValues.length != args._argValues.length)
      return false;

    for (int i = _argValues.length - 1; i >= 0; i--) {
      Object a = args._argValues[i];
      Object b = _argValues[i];
      
      if (a != b && (a == null || ! a.equals(b)))
        return false;
    }

    return true;
  }

  public String toString()
  {
    return "QueryArgs[]";
  }
}
