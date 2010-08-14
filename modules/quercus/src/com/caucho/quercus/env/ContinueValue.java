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
 * Represents a PHP continue value.
 */
public class ContinueValue extends Value {
  public static final ContinueValue CONTINUE = new ContinueValue();

  private int _target;
  
  private ContinueValue()
  {
  }
  
  public ContinueValue(Value target)
  {
    _target = target.toInt();
  }
  
  public ContinueValue(int target)
  {
    _target = target;
  }
  
  public int getTarget()
  {
    return _target;
  }

  /**
   * Converts to a boolean.
   */
  public boolean toBoolean()
  {
    return false;
  }

  /**
   * Converts to a long.
   */
  public long toLong()
  {
    return 0;
  }

  /**
   * Converts to a double.
   */
  public double toDouble()
  {
    return 0;
  }

  /**
   * Converts to an object.
   */
  public Object toObject()
  {
    return "";
  }

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
  {
  }
}

