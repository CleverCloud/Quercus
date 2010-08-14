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

import java.util.IdentityHashMap;

/**
 * Represents a PHP array value copied as part of deserialization or APC.
 *
 * Any modification to the array will set the CopyRoot modified 
 */
public class ArrayCopyValueImpl extends ArrayValueImpl
{
  private final CopyRoot _root;

  /**
   * Copy for unserialization.
   *
   * XXX: need to update for references
   */
  protected ArrayCopyValueImpl(Env env, ArrayValue copy, CopyRoot root)
  {
    super(env, copy, root);

    _root = root;
  }

  /**
   * Clears the array
   */
  public void clear()
  {
    _root.setModified();

    super.clear();
  }
  
  /**
   * Adds a new value.
   */
  public Value put(Value key, Value value)
  {
    if (_root != null)
      _root.setModified();

    return super.put(key, value);
  }
  
  /**
   * Adds a new value.
   */
  public ArrayValue append(Value key, Value value)
  {
    if (_root != null)
      _root.setModified();

    return super.append(key, value);
  }

  /**
   * Add to the beginning
   */
  public ArrayValue unshift(Value value)
  {
    _root.setModified();

    return super.unshift(value);
  }

  /**
   * Replace a section of the array.
   */
  public ArrayValue splice(int start, int end, ArrayValue replace)
  {
    _root.setModified();

    return super.splice(start, end, replace);
  }

  /**
   * Returns the value as an argument which may be a reference.
   */
  @Override
  public Value getArg(Value index, boolean isTop)
  {
    // XXX:
    return super.getArg(index, isTop);
  }

  /**
   * Returns the value as an array, using copy on write if necessary.
   */
  public Value getDirty(Value index)
  {
    _root.setModified();

    return super.getDirty(index);
  }
  
  /**
   * Add
   */
  public Value put(Value value)
  {
    _root.setModified();

    return super.put(value);
  }

  /**
   * Sets the array ref.
   */
  public Var putVar()
  {
    _root.setModified();

    return super.putVar();
  }

  /**
   * Removes a value.
   */
  @Override
  public Value remove(Value key)
  {
    _root.setModified();

    return super.remove(key);
  }

  /**
   * Returns the array ref.
   */
  public Var getVar(Value index)
  {
    _root.setModified();

    return super.getVar(index);
  }

  /**
   * Shuffles the array
   */
  @Override
  public Value shuffle()
  {
    _root.setModified();

    return super.shuffle();
  }
  
  /**
   * Copy the value.
   */
  public Value copy()
  {
    return copy(Env.getInstance());
  }
  
  /**
   * Convert to an argument value.
   */
  @Override
  public Value toLocalRef()
  {
    return copy();
  }
  
  /**
   * Copy for return.
   */
  @Override
  public Value copyReturn()
  {
    return copy();
  }
  
  /**
   * Copy for saving a method's arguments.
   */
  public Value copySaveFunArg()
  {
    return copy();
  }
}
