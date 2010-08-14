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

package com.caucho.security;

import java.security.Principal;

import javax.annotation.PostConstruct;

import com.caucho.util.LruCache;

/**
 * Memory-based cache for single-signon.
 *
 * @since Resin 4.0.0
 */
@com.caucho.config.Service
public class MemorySingleSignon implements SingleSignon {
  private int _cacheSize = 4096;
  private LruCache<String,Principal> _cache;

  /**
   * Returns the size of the principal cache.
   */
  public int getCacheSize()
  {
    return _cacheSize;
  }

  /**
   * Sets the size of the principal cache.
   */
  public void setCacheSize(int size)
  {
    _cacheSize = size;
  }
  
  /**
   * Initialize the single signon.
   */
  @PostConstruct
  public void init()
  {
    if (_cacheSize > 0)
      _cache = new LruCache<String,Principal>(_cacheSize);
  }
  
  /**
   * Returns any saved single signon entry.
   */
  public Principal get(String id)
  {
    if (_cache != null) {
      return _cache.get(id);
    }

    return null;
  }
  
  /**
   * Adds a principal to the cache
   *
   * @return the logged in principal on success, null on failure.
   */
  public void put(String id, Principal user)
  {
    synchronized (_cache) {
      _cache.put(id, user);
    }
  }
  
  /**
   * Adds a principal to the cache
   *
   * @return the logged in principal on success, null on failure.
   */
  public boolean remove(String id)
  {
    _cache.remove(id);

    return false;
  }
}
