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

/**
 * Represents a PHP number value (double or long).
 */
@SuppressWarnings("serial")
public abstract class NumberValue extends Value {
  /**
   * Returns true for equality
   */
  @Override
  public int cmp(Value rValue)
  {
    if (rValue.isBoolean() || rValue.isNull()) {
      boolean lBool = toBoolean();
      boolean rBool = rValue.toBoolean();
      
      if (! lBool && rBool)
        return -1;
      if (lBool && ! rBool)
        return 1;
      
      return 0;
    }
    
    double l = toDouble();
    double r = rValue.toDouble();

    if (l == r)
      return 0;
    else if (l < r)
      return -1;
    else
      return 1;
  }

  /**
   *  Compare two numbers.
   */
  public static int compareNum(Value lValue, Value rValue)
  {
    Value lVal = lValue.toValue();
    Value rVal = rValue.toValue();

    if (lVal instanceof DoubleValue || rVal instanceof DoubleValue) {
      double lDouble = lVal.toDouble();
      double rDouble = rVal.toDouble();
      if (lDouble < rDouble) return -1;
      if (lDouble > rDouble) return 1;
      return 0;
    }
    long lLong = lVal.toLong();
    long rLong = rVal.toLong();
    if (lLong < rLong) return -1;
    if (lLong > rLong) return 1;
    return 0;
  }
  
  /**
   * Encodes the value in JSON.
   */
  @Override
  public void jsonEncode(Env env, StringValue sb)
  {
    sb.append(toStringValue());
  }
}
