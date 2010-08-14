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

import java.lang.ref.*;
import com.caucho.util.*;

/**
 * Cache entry root
 */
public class UnserializeCacheEntry
{
  private FreeList<SoftReference<CopyRoot>> _freeList;
  private SoftReference<Value> _valueRef;

  public UnserializeCacheEntry(Value value)
  {
    _valueRef = new SoftReference<Value>(value);
  }

  public UnserializeCacheEntry(Env env, Value value)
  {
    CopyRoot root = new CopyRoot(this);

    value = value.copyTree(env, root);
    
    _valueRef = new SoftReference<Value>(value);
  }

  public Value getValue(Env env)
  {
    SoftReference<CopyRoot> copyRef = null;

    if (_freeList != null)
      copyRef = _freeList.allocate();

    if (copyRef != null) {
      CopyRoot copy = copyRef.get();

      if (copy != null) {
        copy.allocate(env);

        return copy.getRoot();
      }
    }

    Value value = null;

    if (_valueRef != null)
      value = _valueRef.get();

    if (value != null) {
      CopyRoot root = new CopyRoot(this);

      root.allocate(env);
      
      Value copy = value.copyTree(env, root);

      root.setRoot(copy);

      return copy;
    }
    else
      return null;
  }

  public void clear()
  {
    _valueRef = null;
    _freeList = null;
  }

  void free(CopyRoot root)
  {
    if (_freeList == null)
      _freeList = new FreeList<SoftReference<CopyRoot>>(2);
    
    _freeList.free(new SoftReference<CopyRoot>(root));
  }
}
