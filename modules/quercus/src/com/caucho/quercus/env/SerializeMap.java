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

import java.util.IdentityHashMap;

/*
 * Holds reference indexes for serialization.
 */
public final class SerializeMap {
  private IdentityHashMap<Value, Integer> _varMap
    = new IdentityHashMap<Value, Integer>();
  
  private int _index = 1;
  
  public SerializeMap()
  {
  }
  
  /*
   * Increments the index of values.
   */
  public void incrementIndex()
  {
    _index++;
  }
  
  /*
   * Stores the position of this value in the serialization process.
   */
  public void put(Value value)
  {
    _varMap.put(value, Integer.valueOf(_index));
  }
  
  /*
   * Retrieves the position of this value in the serialization.
   */
  public Integer get(Value value)
  {
    Integer index = _varMap.get(value);
    
    if (index == null && value instanceof Var)
      return _varMap.get(value.toValue());
    
    return index;
  }
  
}

