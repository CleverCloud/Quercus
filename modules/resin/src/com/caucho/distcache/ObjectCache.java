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

import javax.cache.Cache;

/**
 * Interface for a distributed cache.
 */
public interface ObjectCache extends Cache
{
  /**
   * Returns the extended entry
   */
  public ExtCacheEntry getExtCacheEntry(Object entry);

  /**
   * Gets the value, with a lazy update
   */
  public Object getLazy(Object key);

  /**
   * Updates the cache if the old value hash matches the current value.
   * A null value for the old value hash only adds the entry if it's new
   *
   * @param key the key to compare
   * @param version the version of the old value returned by getEntry
   * @param value the new value
   *
   * @return true if the update succeeds, false if it fails
   */
  public boolean compareAndPut(Object key, long version, Object value);

  /**
   * Removes the entry from the cache if the current entry's version
   * matches.
   */
  public boolean compareAndRemove(Object key, long version);

  /**
   * Returns true if the cache is closed
   */
  public boolean isClosed();
}
