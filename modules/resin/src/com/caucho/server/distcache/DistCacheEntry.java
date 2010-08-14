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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.caucho.cloud.topology.TriadOwner;
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.util.HashKey;
import com.caucho.util.Hex;

/**
 * An entry in the cache map
 */
abstract public class DistCacheEntry implements ExtCacheEntry {
  private final HashKey _keyHash;

  private final TriadOwner _owner;

  private Object _key;

  private final AtomicBoolean _isReadUpdate = new AtomicBoolean();

  private final AtomicReference<MnodeValue> _mnodeValue
    = new AtomicReference<MnodeValue>();

  public DistCacheEntry(Object key,
                        HashKey keyHash,
                        TriadOwner owner)
  {
    _key = key;
    _keyHash = keyHash;
    _owner = owner;
  }

  public DistCacheEntry(Object key,
                        HashKey keyHash,
                        TriadOwner owner,
                        CacheConfig config)
  {
    _key = key;
    _keyHash = keyHash;
    _owner = owner;
  }

  /**
   * Returns the key for this entry in the Cache.
   */
  public final Object getKey()
  {
    return _key;
  }

  /**
   * Returns the keyHash
   */
  public final HashKey getKeyHash()
  {
    return _keyHash;
  }

  /**
   * Returns the value of the cache entry.
   */
  public Object getValue()
  {
    return getMnodeValue().getValue();
  }

  /**
   * Returns true if the value is null.
   */
  public boolean isValueNull()
  {
    return getMnodeValue().isValueNull();
  }

  /**
   * Returns the cacheHash
   */
  public final HashKey getCacheHash()
  {
    MnodeValue value = getMnodeValue();

    if (value != null)
      return value.getCacheHashKey();
    else
      return null;
  }

  /**
   * Returns the owner
   */
  public final TriadOwner getOwner()
  {
    return _owner;
  }

  /**
   * Returns the value section of the entry.
   */
  public final MnodeValue getMnodeValue()
  {
    return _mnodeValue.get();
  }

  /**
   * Peeks the current value without checking the backing store.
   */
  public Object peek()
  {
    return getMnodeValue().getValue();
  }

  /**
   * Returns the object, checking the backing store if necessary.
   */
  abstract public Object get(CacheConfig config);

  /**
   * Returns the object, updating the backing store if necessary.
   */
  public Object getLazy(CacheConfig config)
  {
    return get(config);
  }

  /**
   * Fills the value with a stream
   */
  abstract public boolean getStream(OutputStream os, CacheConfig config)
    throws IOException;


  /**
   * Returns the current value.
   */
  public MnodeValue getMnodeValue(CacheConfig config)
  {
    return getMnodeValue();
  }

  /**
   * Sets the value by an input stream
   */
  abstract public Object put(Object value, CacheConfig config);

  /**
   * Sets the value by an input stream
   */
  abstract public ExtCacheEntry put(InputStream is,
                                    CacheConfig config,
                                    long idleTimeout)
    throws IOException;

  /**
   * Remove the value
   */
  abstract public boolean remove(CacheConfig config);

  /**
   * Conditionally starts an update of a cache item, allowing only a
   * single thread to update the data.
   *
   * @return true if the thread is allowed to update
   */
  public final boolean startReadUpdate()
  {
    return _isReadUpdate.compareAndSet(false, true);
  }

  /**
   * Completes an update of a cache item.
   */
  public final void finishReadUpdate()
  {
    _isReadUpdate.set(false);
  }

  /**
   * Sets the current value.
   */
  public final boolean compareAndSet(MnodeValue oldMnodeValue,
                                     MnodeValue mnodeValue)
  {
    return _mnodeValue.compareAndSet(oldMnodeValue, mnodeValue);
  }

  public HashKey getValueHashKey()
  {
    return getMnodeValue().getValueHashKey();
  }

  public byte []getValueHashArray()
  {
    return getMnodeValue().getValueHash();
  }

  public long getIdleTimeout()
  {
    return getMnodeValue().getIdleTimeout();
  }

  public long getLeaseTimeout()
  {
    return getMnodeValue().getLeaseTimeout();
  }

  public int getLeaseOwner()
  {
    return getMnodeValue().getLeaseOwner();
  }

  public void clearLease()
  {
    MnodeValue mnodeValue = getMnodeValue();

    if (mnodeValue != null)
      mnodeValue.clearLease();
  }

  public long getCost()
  {
    return 0;
  }

  public long getCreationTime()
  {
    return getMnodeValue().getCreationTime();
  }

  public long getExpirationTime()
  {
    return getMnodeValue().getExpirationTime();
  }

  public int getHits()
  {
    return getMnodeValue().getHits();
  }

  public long getLastAccessTime()
  {
    return getMnodeValue().getLastAccessTime();
  }

  public long getLastUpdateTime()
  {
    return getMnodeValue().getLastUpdateTime();
  }

  public long getVersion()
  {
    return getMnodeValue().getVersion();
  }

  public boolean isValid()
  {
    return getMnodeValue().isValid();
  }


  public Object setValue(Object value)
  {
    return getMnodeValue().setValue(value);
  }

  public String toString()
  {
    return (getClass().getSimpleName()
            + "[key=" + _key
            + ",keyHash=" + Hex.toHex(_keyHash.getHash(), 0, 4)
            + ",owner=" + _owner
            + "]");
  }
}
