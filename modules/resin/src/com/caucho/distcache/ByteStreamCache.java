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

package com.caucho.distcache;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * Interface for a distributed cache.
 */
public interface ByteStreamCache
{
  /**
   * Fills a stream for the content with the given key.
   */
  public boolean get(Object key, OutputStream os)
    throws IOException;
  
  /**
   * Returns the cache entry for the object with the given key.
   */
  public ExtCacheEntry getExtCacheEntry(Object key);
  
  /**
   * Returns the cache entry for the object with the given key, without
   * triggering a load.
   */
  public ExtCacheEntry peekExtCacheEntry(Object key);
  
  /**
   * Puts a new item in the cache.
   *
   * @param key the key of the item to put
   * @param is stream to contain the value
   */
  public ExtCacheEntry put(Object key, InputStream is,
                           long idleTimeout)
    throws IOException;
  
  /**
   * Updates the cache if the old value hash matches the current value.
   * A null value for the old value hash only adds the entry if it's new
   *
   * @param key the key to compare
   * @param oldVersion the version of the old value, returned by getEntry
   * 
   *
   * @return true if the update succeeds, false if it fails
   */
  public boolean compareAndPut(Object key,
                               long oldVersion,
                               InputStream is)
    throws IOException;

  /**
   * Removes the entry from the cache
   */
  public Object remove(Object key);

  /**
   * Removes the entry from the cache if the current entry matches the hash
   */
  public boolean compareAndRemove(Object key, long oldVersion);
}
