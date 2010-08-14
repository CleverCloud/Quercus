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

package com.caucho.quercus.env;

import com.caucho.vfs.WriteStream;
import com.caucho.quercus.marshal.Marshal;

import java.util.IdentityHashMap;
import java.io.IOException;

/**
 * Represents a 16-bit unicode string value.
 */
abstract public class UnicodeValue extends StringValue {
  protected UnicodeValue()
  {
  }
  
  @Override
  public String toDebugString()
  {
    StringBuilder sb = new StringBuilder();

    int length = length();

    sb.append("unicode(");
    sb.append(length);
    sb.append(") \"");

    int appendLength = length > 256 ? 256 : length;

    for (int i = 0; i < appendLength; i++)
      sb.append(charAt(i));

    if (length > 256)
      sb.append(" ...");

    sb.append('"');

    return sb.toString();
  }

  @Override
  public void varDumpImpl(Env env,
                          WriteStream out,
                          int depth,
                          IdentityHashMap<Value, String> valueSet)
    throws IOException
  {
    int length = length();

    if (length < 0)
        length = 0;
    
    out.print("unicode(");
    out.print(length);
    out.print(") \"");

    for (int i = 0; i < length; i++)
      out.print(charAt(i));

    out.print("\"");
  }

  /**
   * Convert to a unicode value.
   */
  @Override
  public StringValue toUnicodeValue()
  {
    return this;
  }

  /**
   * Convert to a unicode value.
   */
  @Override
  public StringValue toUnicodeValue(Env env)
  {
    return this;
  }

  /**
   * Decodes from charset and returns UnicodeValue.
   *
   * @param env
   * @param charset
   */
  @Override
  public StringValue toUnicodeValue(Env env, String charset)
  {
    return this;
  }

  /**
   * Converts to a string builder
   */
  @Override
  public StringValue toStringBuilder()
  {
    UnicodeBuilderValue sb = new UnicodeBuilderValue();

    sb.append(this);

    return sb;
  }

  /**
   * Returns true for UnicodeValue
   */
  @Override
  public boolean isUnicode()
  {
    return true;
  }

  /**
   * Cost to convert to a UnicodeValue
   */
  public int toUnicodeValueMarshalCost()
  {
    return Marshal.COST_IDENTICAL;
  }

  /**
   * Returns true for equality
   */
  /*
  @Override
  public boolean eq(Value rValue)
  {
    rValue = rValue.toValue();
    
    if (rValue.isBoolean()) {
      return toBoolean() == rValue.toBoolean();
    }

    int type = getNumericType();
    
    if (type == IS_STRING) {
      if (rValue.isUnicode())
        return equals(rValue);
      else if (rValue.isBinary())
        return equals(rValue.toUnicodeValue(Env.getInstance()));
      else if (rValue.isLongConvertible())
        return toLong() ==  rValue.toLong();
      else
        return equals(rValue.toStringValue());
    }
    else if (rValue.isString() && rValue.length() == 0)
      return length() == 0;
    else if (rValue.isNumberConvertible())
      return toDouble() == rValue.toDouble();
    else
      return equals(rValue.toStringValue());
  }
  */
}

