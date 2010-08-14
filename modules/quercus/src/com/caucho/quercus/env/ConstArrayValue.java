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
 * Represents a PHP array value.
 */
public class ConstArrayValue
  extends ArrayValueImpl
{
  private ConstArrayValue _keys;
  private ConstArrayValue _values;
  
  private Value []_keysArray;
  private Value []_valuesArray;
  
  public ConstArrayValue(ArrayValueImpl source)
  {
    super.copyFrom(source);
  }
  
  public ConstArrayValue(ArrayValueComponent[] components)
  {
    super(components);
  }

  public ConstArrayValue(Value []keys, Value []values)
  {
    super(keys, values);
  }
  
  /**
   * Copy for assignment.
   */
  @Override
  public Value copy()
  {
    //return new CopyArrayValue(this);
    
    return new ArrayValueImpl(this);
  }
  
  /**
   * Shuffles the array
   */
  @Override
  public Value shuffle()
  {
    throw new IllegalStateException();
  }
  
  /**
   * Takes the values of this array and puts them in a java array
   */
  public Value[] keysToArray()
  {
    if (_keysArray == null)
      _keysArray = super.keysToArray();
    
    // XXX: copy?
    return _keysArray;
  }
  
  /**
   * Takes the values of this array and puts them in a java array
   */
  public Value[] valuesToArray()
  {
    if (_valuesArray == null)
      _valuesArray = super.valuesToArray();
    
    // XXX: copy?
    return _valuesArray;
  }
  
  /**
   * Returns the array keys.
   */
  @Override
  public Value getKeys()
  {
    if (_keys == null)
      _keys = new ConstArrayValue((ArrayValueImpl) super.getKeys());
    
    return _keys.copy();
  }
  
  /**
   * Returns the array keys.
   */
  @Override
  public Value getValues()
  {
    if (_values == null)
      _values = new ConstArrayValue((ArrayValueImpl) super.getValues());
   
    return _values.copy();
  }
}

