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

package com.caucho.server.cache;

import com.caucho.config.types.Bytes;
import com.caucho.server.webapp.WebApp;
import com.caucho.vfs.Path;

import javax.servlet.FilterChain;

/**
 * Cached response.
 */
public class AbstractCache
{
  /**
   * Sets the path to the cache directory.
   */
  public void setPath(Path path)
  {
  }
  
  /**
   * Returns the path from the cache directory.
   */
  public Path getPath()
  {
    return null;
  }

  /**
   * Sets the disk size of the cache
   */
  public void setDiskSize(Bytes size)
  {
  }

  /**
   * Sets the max entry size of the cache
   */
  public int getMaxEntrySize()
  {
    return 0;
  }

  /**
   * Set true if enabled.
   */
  public void setEnable(boolean isEnabled)
  {
  }

  /**
   * Return true if enabled.
   */
  public boolean isEnable()
  {
    return false;
  }

  /**
   * Sets the max number of entries.
   */
  public void setEntries(int entries)
  {
  }

  /**
   * Sets the path to the cache directory (backwards compatibility).
   */
  public void setDir(Path path)
  {
  }

  /**
   * Sets the size of the the cache (backwards compatibility).
   */
  public void setSize(Bytes size)
  {
  }
  
  /**
   * Creates the filter.
   */
  public FilterChain createFilterChain(FilterChain next,
                                       WebApp app)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Clears the cache.
   */
  public void clear()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the hit count.
   */
  public long getHitCount()
  {
    return 0;
  }

  /**
   * Returns the miss count.
   */
  public long getMissCount()
  {
    return 0;
  }

  /**
   * Returns the memory block hit count.
   */
  public long getMemoryBlockHitCount()
  {
    return 0;
  }

  /**
   * Returns the memory block miss count.
   */
  public long getMemoryBlockMissCount()
  {
    return 0;
  }
}
