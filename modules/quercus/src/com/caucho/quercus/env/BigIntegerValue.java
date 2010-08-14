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

import com.caucho.quercus.program.JavaClassDef;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.logging.Logger;

/**
 * Represents a Quercus java BigInteger value.
 */
public class BigIntegerValue extends JavaValue {
  private static final Logger log
    = Logger.getLogger(JavaURLValue.class.getName());

  private final BigInteger _val;

  public BigIntegerValue(Env env, BigInteger val, JavaClassDef def)
  {
    super(env, val, def);
    _val = val;
  }
  
  /**
   * Converts to a long.
   */
  @Override
  public long toLong()
  {
    return _val.longValue();
  }
  
  /**
   * Converts to a double.
   */
  @Override
  public double toDouble()
  {
    return _val.doubleValue();
  }
  
  /**
   * Converts to a Java BigDecimal.
   */
  @Override
  public BigDecimal toBigDecimal()
  {
    return new BigDecimal(toString());
  }
  
  /**
   * Converts to a Java BigDecimal.
   */
  @Override
  public BigInteger toBigInteger()
  {
    return _val;
  }
  
  /**
   * Returns true for a double-value.
   */
  @Override
  public boolean isDoubleConvertible()
  {
    return true;
  }

  /**
   * Returns true for a long-value.
   */
  @Override
  public boolean isLongConvertible()
  {
    return true;
  }
  
  public String toString()
  {
    return "BigInteger[" + _val.toString() + "]";
  }
}

