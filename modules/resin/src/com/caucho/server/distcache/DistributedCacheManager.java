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
import java.security.MessageDigest;

import com.caucho.cloud.topology.CloudCluster;
import com.caucho.cloud.topology.CloudPod;
import com.caucho.distcache.CacheSerializer;
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.server.cluster.Server;
import com.caucho.util.HashKey;

/**
 * Manages the distributed cache
 */
abstract public class DistributedCacheManager
{
  private final Server _server;

  protected DistributedCacheManager(Server server)
  {
    _server = server;
  }

  /**
   * Returns the owning server
   */
  protected Server getServer()
  {
    return _server;
  }

  /**
   * Returns the owning cluster.
   */
  protected CloudCluster getCluster()
  {
    return _server.getCluster();
  }

  /**
   * Returns the owning pod.
   */
  protected CloudPod getPod()
  {
    return _server.getPod();
  }

  /**
   * Starts the service
   */
  public void start()
  {
  }

  /**
   * Gets a cache key entry
   */
  abstract public DistCacheEntry getCacheEntry(Object key, CacheConfig config);

  /**
   * Sets a cache entry
   */
  public void put(HashKey hashKey,
                  Object value,
                  CacheConfig config)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets a cache entry
   */
  public ExtCacheEntry put(HashKey hashKey,
                           InputStream is,
                           CacheConfig config,
                           long idleTimeout)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Removes a cache entry
   */
  abstract public boolean remove(HashKey hashKey);

  /**
   * Closes the manager
   */
  public void close()
  {
  }

  /**
   * Returns the key hash
   */
  protected HashKey createHashKey(Object key, CacheConfig config)
  {
    try {
      MessageDigest digest
        = MessageDigest.getInstance(HashManager.HASH_ALGORITHM);

      NullDigestOutputStream dOut = new NullDigestOutputStream(digest);

      Object []fullKey = new Object[] { config.getGuid(), key };
      
      config.getKeySerializer().serialize(fullKey, dOut);

      HashKey hashKey = new HashKey(dOut.digest());

      return hashKey;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the key hash
   */
  public HashKey createSelfHashKey(Object key, CacheSerializer keySerializer)
  {
    try {
      MessageDigest digest
        = MessageDigest.getInstance(HashManager.HASH_ALGORITHM);

      NullDigestOutputStream dOut = new NullDigestOutputStream(digest);

      keySerializer.serialize(key, dOut);

      HashKey hashKey = new HashKey(dOut.digest());

      return hashKey;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _server.getServerId() + "]";
  }

  static class NullDigestOutputStream extends OutputStream {
    private MessageDigest _digest;

    NullDigestOutputStream(MessageDigest digest)
    {
      _digest = digest;
    }

    public void write(int value)
    {
      _digest.update((byte) value);
    }

    public void write(byte []buffer, int offset, int length)
    {
      _digest.update(buffer, offset, length);
    }

    public byte []digest()
    {
      return _digest.digest();
    }

    public void flush()
    {
    }

    public void close()
    {
    }
  }
}
