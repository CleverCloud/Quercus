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

import com.caucho.distcache.ExtCacheEntry;
import com.caucho.util.Alarm;
import com.caucho.util.HashKey;

import java.lang.ref.SoftReference;

/**
 * An entry in the cache map
 */
public final class MnodeValue implements ExtCacheEntry {
  public static final MnodeValue NULL
    = new MnodeValue(null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, false, true);
  
  private final HashKey _valueHash;
  private final HashKey _cacheHash;
  private final int _flags;
  private final long _version;
  
  private final long _expireTimeout;
  private final long _idleTimeout;
  private final long _leaseTimeout;
  private final long _localReadTimeout;

  private final long _lastUpdateTime;

  private final boolean _isServerVersionValid;

  private final boolean _isImplicitNull;
  
  private volatile long _lastAccessTime;
  
  private int _leaseOwner = -1;
  private long _leaseExpireTime;
  
  private long _lastRemoteAccessTime;

  private int _hits = 0;

  private SoftReference _valueRef;

  public MnodeValue(HashKey valueHash,
                    Object value,
                    HashKey cacheHash,
                    int flags,
                    long version,
                    long expireTimeout,
                    long idleTimeout,
                    long leaseTimeout,
                    long localReadTimeout,
                    long lastAccessTime,
                    long lastUpdateTime,
                    boolean isServerVersionValid,
                    boolean isImplicitNull)
  {
    _valueHash = valueHash;
    _cacheHash = cacheHash;
    _flags = flags;
    _version = version;
    
    _expireTimeout = expireTimeout;
    _idleTimeout = idleTimeout;
    _leaseTimeout = leaseTimeout;
    _localReadTimeout = localReadTimeout;
    
    _lastRemoteAccessTime = lastAccessTime;
    _lastUpdateTime = lastUpdateTime;
    
    _lastAccessTime = Alarm.getExactTime();

    _isImplicitNull = isImplicitNull;
    _isServerVersionValid = isServerVersionValid;

    if (value != null)
      _valueRef = new SoftReference(value);
  }

  public MnodeValue(MnodeValue oldMnodeValue,
                    long idleTimeout,
                    long lastUpdateTime)
  {
    _valueHash = oldMnodeValue.getValueHashKey();
    _cacheHash = oldMnodeValue.getCacheHashKey();
    _flags = oldMnodeValue.getFlags();
    _version = oldMnodeValue.getVersion();
    
    _expireTimeout = oldMnodeValue.getExpireTimeout();
    _idleTimeout = idleTimeout;
    _leaseTimeout = oldMnodeValue.getLeaseTimeout();
    _localReadTimeout = oldMnodeValue.getLocalReadTimeout();
    
    _lastRemoteAccessTime = lastUpdateTime;
    _lastUpdateTime = lastUpdateTime;
    
    _lastAccessTime = Alarm.getExactTime();

    _leaseExpireTime = oldMnodeValue._leaseExpireTime;
    _leaseOwner = oldMnodeValue._leaseOwner;

    _isImplicitNull = oldMnodeValue.isImplicitNull();
    _isServerVersionValid = oldMnodeValue.isServerVersionValid();

    Object value = oldMnodeValue.getValue();
    
    if (value != null)
      _valueRef = new SoftReference(value);
  }

  /**
   * Returns the last access time.
   */
  public long getLastAccessTime()
  {
    return _lastAccessTime;
  }

  /**
   * Sets the last access time.
   */
  public void setLastAccessTime(long accessTime)
  {
    _lastAccessTime = accessTime;
  }

  /**
   * Returns the last remote access time.
   */
  public long getLastRemoteAccessTime()
  {
    return _lastRemoteAccessTime;
  }

  /**
   * Sets the last remote access time.
   */
  public void setLastRemoteAccessTime(long accessTime)
  {
    _lastRemoteAccessTime = accessTime;
  }

  /**
   * Returns the last update time.
   */
  public long getLastUpdateTime()
  {
    return _lastUpdateTime;
  }

  /**
   * Returns the expiration time
   */
  public final long getExpirationTime()
  {
    return _lastUpdateTime + _expireTimeout;
  }

  public final boolean isLocalReadValid(int serverIndex, long now)
  {
    if (! _isServerVersionValid)
      return false;
    else if (now <= _lastAccessTime + _localReadTimeout)
      return true;
    else if (_leaseOwner == serverIndex && now <= _leaseExpireTime)
      return true;
    else
      return false;
  }

  public final boolean isLeaseExpired(long now)
  {
    return _leaseExpireTime <= now;
  }

  /**
   * Returns true is the entry has expired for being idle or having
   * expired.
   */
  public final boolean isEntryExpired(long now)
  {
    return isIdleExpired(now) || isValueExpired(now);
  }

   /**
   * Returns true if the value of the entry has expired.
   */
  public final boolean isValueExpired(long now)
  {
    return _lastUpdateTime + _expireTimeout < now;
  }

  /**
   * Returns true is the entry has remained idle  too long.
   */
  public final boolean isIdleExpired(long now)
  {
    return _lastAccessTime + _idleTimeout < now;
  }

  /**
   * Returns the lease owner
   */
  public final int getLeaseOwner()
  {
    return _leaseOwner;
  }

  /**
   * Sets the owner
   */
  public final void setLeaseOwner(int leaseOwner, long now)
  {
    if (leaseOwner > 2) {
      _leaseOwner = leaseOwner;

      _leaseExpireTime = now + _leaseTimeout;
    }
    else {
      _leaseOwner = -1;

      _leaseExpireTime = 0;
    }
  }

  /**
   * Sets the owner
   */
  public final void clearLease()
  {
    _leaseOwner = -1;

    _leaseExpireTime = 0;
  }

  public int getFlags()
  {
    return _flags;
  }

  /**
   * Returns the expire timeout for this entry.
   */
  public long getExpireTimeout()
  {
    return _expireTimeout;
  }

  /**
   * Returns the idle timeout for this entry.
   */
  public long getIdleTimeout()
  {
    return _idleTimeout;
  }

  /**
   * Returns the idle window to avoid too many updates
   */
  public long getIdleWindow()
  {
    long window = _idleTimeout / 4;
    long windowMax = 15 * 60 * 1000L;

    if (window < windowMax)
      return window;
    else
      return windowMax;
  }

  /**
   * Returns the read timeout for a local cached entry
   */
  public long getLocalReadTimeout()
  {
    return _localReadTimeout;
  }

  /**
   * Returns the timeout for a lease of the cache entry
   */
  public long getLeaseTimeout()
  {
    return _leaseTimeout;
  }

  public long getVersion()
  {
    return _version;
  }

  /**
   * Sets the deserialized value for the entry.
   */
  public final void setObjectValue(Object value)
  {
    if (value != null && (_valueRef == null || _valueRef.get() == null))
      _valueRef = new SoftReference<Object>(value);
  }

  /**
   * Returns true if the value is null
   */
  public boolean isValueNull()
  {
    return _valueHash == null;
  }

  /**
   * Returns the deserialized value for the entry.
   */
  public final Object getValue()
  {
    SoftReference valueRef = _valueRef;

    if (valueRef != null)
    {
      _hits++;
      return valueRef.get();
    }
    else
      return null;
  }

  public byte []getValueHash()
  {
    if (_valueHash != null)
      return _valueHash.getHash();
    else
      return null;
  }

  public HashKey getValueHashKey()
  {
    return _valueHash;
  }

  public byte []getCacheHash()
  {
    if (_cacheHash != null)
      return _cacheHash.getHash();
    else
      return null;
  }

  public HashKey getCacheHashKey()
  {
    return _cacheHash;
  }

  /**
   * Returns true if the server version (startup count) matches
   * the database.
   */
  public boolean isServerVersionValid()
  {
    return _isServerVersionValid;
  }

  /**
   * If the null value is due to a missing item in the database.
   */
  public boolean isImplicitNull()
  {
    return _isImplicitNull;
  }

  /**
   * Compares values
   */
  public int compareTo(MnodeValue mnode)
  {
    if (getVersion() < mnode.getVersion())
      return -1;
    else if (mnode.getVersion() < getVersion())
      return 1;
    else if (getValueHashKey() == null)
      return -1;
    else
      return getValueHashKey().compareTo(mnode.getValueHashKey());
  }

  //
  // jcache stubs
  //

  /**
   * Implements a method required by the interface that should never be
   * called>
   */
  public Object getKey()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
   /**
   * Implements a method required by the interface that should never be
   * called>
   */
  public Object setValue(Object value)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public long getCreationTime()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public boolean isValid()
  {
    return (! isEntryExpired(Alarm.getCurrentTime()));
  }

  public long getCost()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  public int getHits()
  {
    return _hits;
  }

  public String toString()
  {
    return (getClass().getSimpleName()
            + "[value=" + _valueHash
            + ",flags=0x" + Integer.toHexString(_flags)
            + ",version=" + _version
            + ",lease=" + _leaseOwner
            + "]");
  }
}
