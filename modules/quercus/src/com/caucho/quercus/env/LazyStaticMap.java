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

import com.caucho.util.IntMap;

import java.util.*;

/**
 * Represents the Quercus static environment.
 */
public class LazyStaticMap extends AbstractMap<StringValue,Var> {
  private final IntMap _intMap;
  private final Value []_values;
  
  private HashMap<StringValue,Var> _extMap = new HashMap<StringValue,Var>();

  public LazyStaticMap(IntMap intMap, Value []values)
  {
    _intMap = intMap;
    _values = values;
  }

  /**
   * Returns the matching value, or null.
   */
  public Var get(Object key)
  {
    return (Var) get((StringValue) key);
  }

  /**
   * Returns the matching value, or null.
   */
  public Var get(StringValue key)
  {
    Var var = _extMap.get(key);

    if (var == null) {
      int id = _intMap.get(key);

      if (id >= 0 && _values[id] != null) {
        var = new Var();
        // var.setGlobal();

        _extMap.put(key, var);
      
        Env env = Env.getCurrent();

        Value value = _values[id].copy(env);

        var.set(value);
      }
    }
    
    return var;
  }

  /**
   * Returns the matching value, or null.
   */
  @Override
  public Var put(StringValue key, Var newVar)
  {
    return _extMap.put(key, newVar);
  }

  public Set<Map.Entry<StringValue,Var>> entrySet()
  {
    return _extMap.entrySet();
  }
}

