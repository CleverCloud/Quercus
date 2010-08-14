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
 * @author Nam Nguyen
 */

package com.caucho.quercus.env;

/**
 * Represents a StringValue that is never modified.
 * For compiled code.
 */
public final class CompiledConstStringValue
  extends ConstStringValue
{
  private final int _compiledHashCode;

  public CompiledConstStringValue(StringValue s)
  {
    super(s);

    _longValue = s.toLongValue();
    _doubleValue = s.toDoubleValue();
    _string = s.toString();

    _valueType = s.getValueType();
    _compiledHashCode = s.hashCode();
    _key = s.toKey();
  }

  public CompiledConstStringValue(String s)
  {
    super(s);

    _longValue = super.toLongValue();
    _doubleValue = super.toDoubleValue();
    _string = s;
    _valueType = super.getValueType();
    _compiledHashCode = super.hashCode();
    _key = super.toKey();
  }

  public CompiledConstStringValue(char ch,
                                  LongValue longValue,
                                  DoubleValue doubleValue,
                                  ValueType valueType,
                                  Value key,
                                  int hashCode)
  {
    super(ch);

    _string = String.valueOf(ch);
    _longValue = longValue;
    _doubleValue = doubleValue;

    _valueType = valueType;
    _key = key;
    _compiledHashCode = hashCode;
  }

  public CompiledConstStringValue(char ch,
                                  LongValue longValue,
                                  DoubleValue doubleValue,
                                  ValueType valueType,
                                  int hashCode)
  {
    super(ch);

    _string = String.valueOf(ch);
    _longValue = longValue;
    _doubleValue = doubleValue;

    _valueType = valueType;
    _key = super.toKey();
    _compiledHashCode = hashCode;
  }

  public CompiledConstStringValue(String s,
                                  LongValue longValue,
                                  DoubleValue doubleValue,
                                  ValueType valueType,
                                  Value key,
                                  int hashCode)
  {
    super(s);

    _string = s;
    _longValue = longValue;
    _doubleValue = doubleValue;
    _valueType = valueType;

    _key = key;
    _compiledHashCode = hashCode;
  }

  public CompiledConstStringValue(String s,
                                  LongValue longValue,
                                  DoubleValue doubleValue,
                                  ValueType valueType,
                                  int hashCode)
  {
    super(s);

    _string = s;
    _longValue = longValue;
    _doubleValue = doubleValue;
    _valueType = valueType;

    _key = super.toKey();
    _compiledHashCode = hashCode;
  }

  public boolean isStatic()
  {
    return true;
  }

  /**
   * Converts to a long vaule
   */
  @Override
  public LongValue toLongValue()
  {
    return _longValue;
  }

  /**
   * Converts to a double vaule
   */
  @Override
  public DoubleValue toDoubleValue()
  {
    return _doubleValue;
  }

  /**
   * Converts to a long.
   */
  @Override
  public long toLong()
  {
    return toLongValue().toLong();
  }

  /**
   * Converts to a double.
   */
  @Override
  public double toDouble()
  {
    return toDoubleValue().toDouble();
  }

  /**
   * Returns the ValueType.
   */
  @Override
  public ValueType getValueType()
  {
    return _valueType;
  }

  /**
   * Converts to a key.
   */
  @Override
  public final Value toKey()
  {
    return _key;
  }

  @Override
  public final int hashCode()
  {
    return _compiledHashCode;
  }

  public final String toString()
  {
    return _string;
  }
}
