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
 * @author Sam
 */

package com.caucho.quercus.program;

import java.util.Map;
import java.util.Iterator;

import com.caucho.quercus.env.*;

/**
 * A delegate that performs Array operations for Quercus objects.
 */
public class FunctionArrayDelegate implements ArrayDelegate {
  private JavaInvoker _arrayGet;
  private JavaInvoker _arrayPut;

  public FunctionArrayDelegate()
  {
  }

  /**
   * Sets the custom function for the array get.
   */
  public void setArrayGet(JavaInvoker arrayGet)
  {
    _arrayGet = arrayGet;
  }

  /**
   * Sets the custom function for the array set.
   */
  public void setArrayPut(JavaInvoker arrayPut)
  {
    _arrayPut = arrayPut;
  }
  
  /**
   * Returns the value for the specified key.
   */
  public Value get(ObjectValue qThis, Value key)
  {
    if (_arrayGet != null) {
      return _arrayGet.callMethod(Env.getInstance(),
                                  _arrayGet.getQuercusClass(),
                                  qThis, 
                                  new Value[] { key });
    }
    else
      return UnsetValue.UNSET;
  }

  /**
   * Sets the value for the spoecified key.
   */
  public Value put(ObjectValue qThis, Value key, Value value)
  {
    if (_arrayPut != null)
      return _arrayPut.callMethod(Env.getInstance(),
                                  _arrayPut.getQuercusClass(),
                                  qThis, key, value);
    else
      return UnsetValue.UNSET;
  }

  /**
   * Appends a value.
   */
  public Value put(ObjectValue qThis, Value value)
  {
    if (_arrayPut != null)
      return _arrayPut.callMethod(Env.getInstance(), 
                                  _arrayPut.getQuercusClass(),
                                  qThis, value);
    else
      return UnsetValue.UNSET;
  }

  /**
   * Returns true if the value is set
   */
  public boolean isset(ObjectValue qThis, Value key)
  {
    return get(qThis, key).isset();
  }

  /**
   * Removes the value at the speified key.
   */
  public Value unset(ObjectValue qThis, Value key)
  {
    return UnsetValue.UNSET;
  }
}
