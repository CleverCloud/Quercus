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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.entity;

import com.caucho.amber.type.EntityType;

/**
 * Key to handle the merged identity hash code.
 */
public class EntityKey {
  private Class _type;
  private Object _key;

  public EntityKey()
  {
  }

  public EntityKey(Class type, Object key)
  {
    _type = type;
    _key = key;
  }

  public void init(Class type, Object key)
  {
    _type = type;
    _key = key;
  }

  /**
   * Returns the home value.
   */
  public Class getType()
  {
    return _type;
  }

  /**
   * Returns the key
   */
  public Object getKey()
  {
    return _key;
  }

  /**
   * Returns the hash.
   */
  public int hashCode()
  {
    return 65521 * _type.hashCode() + _key.hashCode();
  }

  /**
   * Returns equality.
   */
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof EntityKey))
      return false;

    EntityKey key = (EntityKey) o;

    return _type == key._type && _key.equals(key._key);
  }
    
  public String toString()
  {
    return "EntityKey[" + _type.getName() + ", " + _key + "]";
  }
}
