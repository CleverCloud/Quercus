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
 * Root for saving copy information for the serialization cache.
 */
public class CopyRoot implements EnvCleanup
{
  private final UnserializeCacheEntry _entry;
  
  private Value _root;
  private boolean _isModified;

  private IdentityHashMap<Value,Value> _copyMap
    = new IdentityHashMap<Value,Value>();
  
  public CopyRoot(UnserializeCacheEntry entry)
  {
    _entry = entry;
  }

  /**
   * Indicate that the contents are modified
   */
  public void setModified()
  {
    _isModified = true;
  }

  /**
   * True if it's modified
   */
  public boolean isModified()
  {
    return _isModified;
  }

  /**
   * Returns the root
   */
  public Value getRoot()
  {
    return _root;
  }

  public void setRoot(Value root)
  {
    _root = root;

    // clear when setting root since the unserialization process itself
    // sets the modify flag
    _isModified = false;
  }
  
  public void putCopy(Value value, Value copy)
  {
    _copyMap.put(value, copy);
  }
  
  public Value getCopy(Value value)
  {
    return _copyMap.get(value);
  }

  public void allocate(Env env)
  {
    env.addCleanup(this);
  }

  public void cleanup()
    throws Exception
  {
    if (! _isModified)
      _entry.free(this);
  }
}
