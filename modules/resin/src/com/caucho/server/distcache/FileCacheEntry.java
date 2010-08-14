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

package com.caucho.server.distcache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.caucho.cloud.topology.TriadOwner;
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.util.Alarm;
import com.caucho.util.HashKey;

/**
 * An entry in the cache map
 */
public class FileCacheEntry extends DistCacheEntry {
  private final FileCacheManager _manager;

  public FileCacheEntry(Object key,
                        HashKey keyHash,
                        TriadOwner owner,
                        FileCacheManager manager)
  {
    super(key, keyHash, owner);

    _manager = manager;
  }

  public FileCacheEntry(Object key,
                        HashKey keyHash,
                        TriadOwner owner,
                        FileCacheManager manager,
                        CacheConfig config)
  {
    super(key, keyHash, owner, config);

    _manager = manager;
  }

  /**
   * Peeks the current value
   */
  @Override
  public Object peek()
  {
    return null;
  }

  /**
   * Fills the value with a stream
   */
  //@Override
  public Object get(CacheConfig config)
  {
    long now = Alarm.getCurrentTime();
  
  return _manager.get(this, config, now);
  }

  /**
   * Fills the value with a stream
   */
  @Override
  public boolean getStream(OutputStream os, CacheConfig config)
    throws IOException
  {
    return _manager.getStream(this, os, config);
  }

  /**
   * Sets the current value
   */
  @Override
  public Object put(Object value, CacheConfig config)
  {
    return _manager.put(this, value, config);
  }

  /**
   * Sets the value by an input stream
   */
  @Override
  public ExtCacheEntry put(InputStream is,
                           CacheConfig config,
                           long idleTimeout)
    throws IOException
  {
    return _manager.putStream(this, is, config, idleTimeout);
  }

  /**
   * Remove the value
   */
  @Override
  public boolean remove(CacheConfig config)
  {
    return _manager.remove(this, config);
  }
}
