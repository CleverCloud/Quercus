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

import com.caucho.distcache.CacheSerializer;
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.env.service.RootDirectoryService;
import com.caucho.config.ConfigException;
import com.caucho.server.cluster.ClusterPod;
import com.caucho.server.cluster.Server;
import com.caucho.util.Alarm;
import com.caucho.util.HashKey;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.util.Sha256OutputStream;
import com.caucho.vfs.Path;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.TempOutputStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.cache.CacheLoader;

/**
 * Manages the distributed cache
 */
abstract public class AbstractDataCacheManager<E extends DistCacheEntry>
  extends DistributedCacheManager
{
  private static final Logger log
    = Logger.getLogger(AbstractDataCacheManager.class.getName());

  private static final L10N L = new L10N(AbstractDataCacheManager.class);

  private final MnodeStore _mnodeStore;
  private final DataStore _dataStore;
  
  private final LruCache<HashKey, E> _entryCache
    = new LruCache<HashKey, E>(64 * 1024);
  
  private final AdminPersistentStore _admin;

  public AbstractDataCacheManager(Server server)
  {
    super(server);

    try {
      Path dataDirectory = RootDirectoryService.getCurrentDataDirectory();
      
      String serverId = server.getUniqueServerName();

      _mnodeStore = new MnodeStore(dataDirectory, serverId);
      _dataStore = new DataStore(serverId, _mnodeStore);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
    
    _admin = new AdminPersistentStore(this);
  }

  protected DataStore getDataStore()
  {
    return _dataStore;
  }

  protected MnodeStore getMnodeStore()
  {
    return _mnodeStore;
  }

  /**
   * Returns the key entry.
   */
  @Override
  public E getCacheEntry(Object key, CacheConfig config)
  {
    HashKey hashKey = createHashKey(key, config);

    E cacheEntry = _entryCache.get(hashKey);

    while (cacheEntry == null) {
      cacheEntry = createCacheEntry(key, hashKey);

      cacheEntry = _entryCache.putIfNew(cacheEntry.getKeyHash(), cacheEntry);
    }

    return cacheEntry;
  }

  abstract protected E createCacheEntry(Object key, HashKey hashKey);

  /**
   * Returns the key entry.
   */
  public E getCacheEntry(HashKey hashKey)
  {
    E cacheEntry = _entryCache.get(hashKey);

    while (cacheEntry == null) {
      cacheEntry = createCacheEntry(null, hashKey);

      if (! _entryCache.compareAndPut(null,
                                      cacheEntry.getKeyHash(),
                                      cacheEntry)) {
        cacheEntry = _entryCache.get(hashKey);
      }
    }

    return cacheEntry;
  }

  /**
   * Gets a cache entry
   */
  public Object get(E entry,
                    CacheConfig config,
                    long now)
  {
    return get(entry, config, now, false);
  }

  /**
   * Gets a cache entry
   */
  public Object getLazy(E entry,
                        CacheConfig config,
                        long now)
  {
    return get(entry, config, now, true);
  }

  private Object get(E entry,
                     CacheConfig config,
                     long now,
                     boolean isLazy)
  {
    MnodeValue mnodeValue = getMnodeValue(entry, config, now, isLazy);

    if (mnodeValue == null)
      return null;

    Object value = mnodeValue.getValue();

    if (value != null)
      return value;

    HashKey valueHash = mnodeValue.getValueHashKey();

    if (valueHash == null || valueHash == HashManager.NULL)
      return null;

    updateAccessTime(entry, mnodeValue, now);

    value = readData(valueHash,
                     config.getFlags(),
                     config.getValueSerializer());

    if (value == null) {
      // Recovery from dropped or corrupted data
      log.warning("Missing or corrupted data for " + mnodeValue);
      remove(entry, config);
    }

    mnodeValue.setObjectValue(value);

    return value;
  }

  /**
   * Gets a cache entry as a stream
   */
  public boolean getStream(E entry,
                           OutputStream os,
                           CacheConfig config)
    throws IOException
  {
    long now = Alarm.getCurrentTime();

    MnodeValue mnodeValue = getMnodeValue(entry, config, now, false);

    if (mnodeValue == null)
      return false;

    updateAccessTime(entry, mnodeValue, now);

    HashKey valueHash = mnodeValue.getValueHashKey();

    if (valueHash == null || valueHash == HashManager.NULL)
      return false;

    boolean isData = readData(valueHash, config.getFlags(), os);
    
    if (! isData) {
      log.warning("Missing or corrupted data for " + mnodeValue);
      // Recovery from dropped or corrupted data
      remove(entry, config);
    }

    return isData;
  }

  public MnodeValue getMnodeValue(E entry,
                                  CacheConfig config,
                                  long now,
                                  boolean isLazy)
  {
    MnodeValue mnodeValue = loadMnodeValue(entry);

    if (mnodeValue == null)
      reloadValue(entry, config, now);
    else if (isLocalReadValid(mnodeValue, now)) {
    }
    else if (! isLazy) {
      reloadValue(entry, config, now);
    }
    else {
      lazyValueUpdate(entry, config);
    }

    mnodeValue = entry.getMnodeValue();

    // server/016q
    if (mnodeValue != null) {
      updateIdleTime(entry, mnodeValue);
    }

    return entry.getMnodeValue();
  }

  private void reloadValue(E entry,
                           CacheConfig config,
                           long now)
  {
    // only one thread may update the expired data
    if (entry.startReadUpdate()) {
      try {
        loadExpiredValue(entry, config, now);
      } finally {
        entry.finishReadUpdate();
      }
    }
  }
  
  protected void lazyValueUpdate(E entry, CacheConfig config)
  {
    reloadValue(entry, config, Alarm.getCurrentTime());
  }

  protected boolean isLocalReadValid(MnodeValue mnodeValue, long now)
  {
    return ! mnodeValue.isEntryExpired(now);
  }

  private void updateAccessTime(E entry,
                                MnodeValue mnodeValue,
                                long now)
  {
    if (mnodeValue != null) {
      long idleTimeout = mnodeValue.getIdleTimeout();
      long updateTime = mnodeValue.getLastUpdateTime();

      if (idleTimeout < CacheConfig.TIME_INFINITY
          && updateTime + mnodeValue.getIdleWindow() < now) {
        MnodeValue newMnodeValue
          = new MnodeValue(mnodeValue, idleTimeout, now);

        saveUpdateTime(entry, newMnodeValue);
      }
    }
  }

  private void loadExpiredValue(E entry,
                                CacheConfig config,
                                long now)
  {
    MnodeValue mnodeValue = loadClusterValue(entry, config);

    if (mnodeValue == null || mnodeValue.isEntryExpired(now)) {
      CacheLoader loader = config.getCacheLoader();

      if (loader != null && entry.getKey() != null) {
        Object value = loader.load(entry.getKey());

        if (value != null) {
          put(entry, value, config, now, mnodeValue);

          return;
        }
      }

      MnodeValue nullMnodeValue = new MnodeValue(null, null, null,
                                                 0, 0,
                                                 config.getExpireTimeout(),
                                                 config.getIdleTimeout(),
                                                 config.getLeaseTimeout(),
                                                 config.getLocalReadTimeout(),
                                                 now, now,
                                                 true, true);

      entry.compareAndSet(mnodeValue, nullMnodeValue);
    }
    else
      mnodeValue.setLastAccessTime(now);
  }

  protected MnodeValue loadClusterValue(E entry, CacheConfig config)
  {
    return null;
    // return _cacheService.get(entry, config);
  }
  
  /**
   * Sets a cache entry
   */
  public Object put(E entry,
                    Object value,
                    CacheConfig config)
  {
    long now = Alarm.getCurrentTime();

    // server/60a0 - on server '4', need to read update from triad
    MnodeValue mnodeValue = getMnodeValue(entry, config, now, false);

    return put(entry, value, config, now, mnodeValue);
  }

  /**
   * Sets a cache entry
   */
  protected Object put(E entry,
                       Object value,
                       CacheConfig config,
                       long now,
                       MnodeValue mnodeValue)
  {
    long idleTimeout = config.getIdleTimeout() * 5L / 4;
    HashKey key = entry.getKeyHash();

    HashKey oldValueHash = (mnodeValue != null
                            ? mnodeValue.getValueHashKey()
                            : null);
    Object oldValue = mnodeValue != null ? mnodeValue.getValue() : null;
    long version = mnodeValue != null ? mnodeValue.getVersion() : 0;

    HashKey valueHash = writeData(oldValueHash, value,
                                  config.getValueSerializer());

    HashKey cacheKey = config.getCacheKey();

    int leaseOwner = mnodeValue != null ? mnodeValue.getLeaseOwner() : -1;

    mnodeValue = putLocalValue(entry, version + 1,
                               valueHash, value, cacheKey,
                               config.getFlags(),
                               config.getExpireTimeout(),
                               idleTimeout,
                               config.getLeaseTimeout(),
                               config.getLocalReadTimeout(),
                               leaseOwner);

    if (mnodeValue == null)
      return oldValue;

    putCluster(key, valueHash, cacheKey, mnodeValue);

    return oldValue;
  }

  public ExtCacheEntry putStream(E entry,
                                 InputStream is,
                                 CacheConfig config,
                                 long idleTimeout)
    throws IOException
  {
    HashKey key = entry.getKeyHash();
    MnodeValue mnodeValue = loadMnodeValue(entry);

    HashKey oldValueHash = (mnodeValue != null
                            ? mnodeValue.getValueHashKey()
                            : null);
    long version = mnodeValue != null ? mnodeValue.getVersion() : 0;

    HashKey valueHash = writeData(oldValueHash, is);

    if (valueHash != null && valueHash.equals(oldValueHash)) {
      return mnodeValue;
    }

    HashKey cacheHash = config.getCacheKey();

    // add 25% window for update efficiency
    idleTimeout = idleTimeout * 5L / 4;

    int leaseOwner = (mnodeValue != null) ? mnodeValue.getLeaseOwner() : -1;

    mnodeValue = putLocalValue(entry, version + 1,
                               valueHash, null, cacheHash,
                               config.getFlags(),
                               config.getExpireTimeout(),
                               idleTimeout,
                               config.getLeaseTimeout(),
                               config.getLocalReadTimeout(),
                               leaseOwner);

    if (mnodeValue == null)
      return null;

    putCluster(key, valueHash, cacheHash, mnodeValue);

    return mnodeValue;
  }

  protected void putCluster(HashKey key, HashKey value, HashKey cacheKey,
                            MnodeValue mnodeValue)
  {
  }

  /*
  DataStreamSource createDataSource(HashKey valueHash)
  {
    return new DataStreamSource(valueHash, _dataStore);
  }
  */

  E getLocalEntry(HashKey key)
  {
    if (key == null)
      throw new NullPointerException();

    E entry = getCacheEntry(key);

    return entry;
  }

  E loadLocalEntry(HashKey key)
  {
    if (key == null)
      throw new NullPointerException();

    E entry = getCacheEntry(key);

    long now = Alarm.getCurrentTime();

    if (entry.getMnodeValue() == null
        || entry.getMnodeValue().isEntryExpired(now)) {
      forceLoadMnodeValue(entry);
    }

    return entry;
  }

  E getLocalEntryAndUpdateIdle(HashKey key)
  {
    E entry = getLocalEntry(key);

    MnodeValue mnodeValue = entry.getMnodeValue();

    if (mnodeValue != null) {
      updateIdleTime(entry, mnodeValue);
    }

    return entry;
  }

  protected void updateIdleTime(E entry, MnodeValue mnodeValue)
  {
    long idleTimeout = mnodeValue.getIdleTimeout();
    long updateTime = mnodeValue.getLastUpdateTime();

    long now = Alarm.getCurrentTime();

    if (idleTimeout < CacheConfig.TIME_INFINITY
        && updateTime + mnodeValue.getIdleWindow() < now) {
      MnodeValue newMnodeValue
        = new MnodeValue(mnodeValue, idleTimeout, now);

      saveUpdateTime(entry, newMnodeValue);
    }
  }

  /**
   * Returns the local value from the database
   */
  public MnodeValue loadLocalEntryValue(HashKey key)
  {
    return _mnodeStore.load(key);
  }

  /**
   * Gets a cache entry
   */
  public MnodeValue loadMnodeValue(DistCacheEntry cacheEntry)
  {
    HashKey key = cacheEntry.getKeyHash();
    MnodeValue mnodeValue = cacheEntry.getMnodeValue();

    long now = Alarm.getCurrentTime();

    if (mnodeValue == null) {
      MnodeValue newMnodeValue = _mnodeStore.load(key);

      cacheEntry.compareAndSet(null, newMnodeValue);

      mnodeValue = cacheEntry.getMnodeValue();
    }

    return mnodeValue;
  }

  /**
   * Gets a cache entry
   */
  private MnodeValue forceLoadMnodeValue(DistCacheEntry cacheEntry)
  {
    HashKey key = cacheEntry.getKeyHash();
    MnodeValue mnodeValue = cacheEntry.getMnodeValue();

    long now = Alarm.getCurrentTime();

    MnodeValue newMnodeValue = _mnodeStore.load(key);

    cacheEntry.compareAndSet(mnodeValue, newMnodeValue);

    mnodeValue = cacheEntry.getMnodeValue();

    return mnodeValue;
  }

  /**
   * Sets a cache entry
   */
  MnodeValue putLocalValue(HashKey key, MnodeValue mnodeValue)
  {
    E entry = getCacheEntry(key);

    long timeout = 60000L;

    MnodeValue oldEntryValue = entry.getMnodeValue();

    if (oldEntryValue != null && mnodeValue.compareTo(oldEntryValue) <= 0) {
      return oldEntryValue;
    }

    // the failure cases are not errors because this put() could
    // be immediately followed by an overwriting put()

    if (! entry.compareAndSet(oldEntryValue, mnodeValue)) {
      log.fine(this + " mnodeValue update failed due to timing conflict"
        + " (key=" + key + ")");

      return entry.getMnodeValue();
    }

    if (oldEntryValue == null
        || oldEntryValue.isImplicitNull()
        || oldEntryValue == MnodeValue.NULL) {
      if (_mnodeStore.insert(key, mnodeValue.getValueHashKey(),
                             mnodeValue.getCacheHashKey(),
                             mnodeValue.getFlags(),
                             mnodeValue.getVersion(),
                             mnodeValue.getExpireTimeout(),
                             mnodeValue.getIdleTimeout(),
                             mnodeValue.getLeaseTimeout(),
                             mnodeValue.getLocalReadTimeout())) {
        return mnodeValue;
      } else {
        log.fine(this + " db insert failed due to timing conflict"
                 + "(key=" + key + ")");

        return entry.getMnodeValue();
      }
    } else {
      if (_mnodeStore.updateSave(key, mnodeValue.getValueHashKey(),
                                      mnodeValue.getVersion(), timeout)) {
        return mnodeValue;
      }
      else if (_mnodeStore.insert(key, mnodeValue.getValueHashKey(),
                                  mnodeValue.getCacheHashKey(),
                                  mnodeValue.getFlags(),
                                  mnodeValue.getVersion(),
                                  mnodeValue.getExpireTimeout(),
                                  mnodeValue.getIdleTimeout(),
                                  mnodeValue.getLeaseTimeout(),
                                  mnodeValue.getLocalReadTimeout())) {
        return mnodeValue;
      }
      else {
        log.fine(this + " db update failed due to timing conflict"
                 + "(key=" + key + ")");

        return entry.getMnodeValue();
      }
    }
  }

  /**
   * Sets a cache entry
   */
  MnodeValue saveUpdateTime(E entryKey,
                            MnodeValue mnodeValue)
  {
    MnodeValue newEntryValue = saveLocalUpdateTime(entryKey, mnodeValue);

    if (newEntryValue.getVersion() != mnodeValue.getVersion())
      return newEntryValue;

    updateCacheTime(entryKey.getKeyHash(), mnodeValue);

    return mnodeValue;
  }

  protected void updateCacheTime(HashKey key, MnodeValue mnodeValue)
  {
    // _cacheService.updateTime(entryKey.getKeyHash(), mnodeValue);
  }

  /**
   * Sets a cache entry
   */
  void saveLocalUpdateTime(HashKey key,
                           long version,
                           long idleTimeout,
                           long updateTime)
  {
    DistCacheEntry entry = _entryCache.get(key);

    if (entry == null)
      return;

    MnodeValue oldEntryValue = entry.getMnodeValue();

    if (oldEntryValue == null || version != oldEntryValue.getVersion())
      return;

    MnodeValue mnodeValue
      = new MnodeValue(oldEntryValue, idleTimeout, updateTime);

    saveLocalUpdateTime(entry, mnodeValue);
  }

  /**
   * Sets a cache entry
   */
  MnodeValue saveLocalUpdateTime(DistCacheEntry entry,
                                 MnodeValue mnodeValue)
  {
    MnodeValue oldEntryValue = entry.getMnodeValue();

    if (oldEntryValue != null
        && mnodeValue.getVersion() < oldEntryValue.getVersion()) {
      return oldEntryValue;
    }

    // the failure cases are not errors because this put() could
    // be immediately followed by an overwriting put()

    if (! entry.compareAndSet(oldEntryValue, mnodeValue)) {
      log.fine(this + " mnodeValue updateTime failed due to timing conflict"
               + " (key=" + entry.getKeyHash() + ")");

      return entry.getMnodeValue();
    }

    if (_mnodeStore.updateUpdateTime(entry.getKeyHash(),
                                     mnodeValue.getVersion(),
                                     mnodeValue.getIdleTimeout(),
                                     mnodeValue.getLastUpdateTime())) {
      return mnodeValue;
    } else {
      log.fine(this + " db updateTime failed due to timing conflict"
               + "(key=" + entry.getKeyHash() + ", version=" + mnodeValue.getVersion() + ")");

      return entry.getMnodeValue();
    }
  }

  /**
   * Sets a cache entry
   */
  public boolean remove(E entry, CacheConfig config)
  {
    HashKey key = entry.getKeyHash();

    MnodeValue mnodeValue = loadMnodeValue(entry);
    HashKey oldValueHash = mnodeValue != null ? mnodeValue.getValueHashKey() : null;

    HashKey cacheKey = entry.getCacheHash();

    int flags = mnodeValue != null ? mnodeValue.getFlags() : 0;
    long version = mnodeValue != null ? mnodeValue.getVersion() : 0;

    long expireTimeout = config.getExpireTimeout();
    long idleTimeout = (mnodeValue != null
                        ? mnodeValue.getIdleTimeout()
                        : config.getIdleTimeout());
    long leaseTimeout = (mnodeValue != null
                         ? mnodeValue.getLeaseTimeout()
                         : config.getLeaseTimeout());
    long localReadTimeout = (mnodeValue != null
                             ? mnodeValue.getLocalReadTimeout()
                             : config.getLocalReadTimeout());
    int leaseOwner = (mnodeValue != null ? mnodeValue.getLeaseOwner() : -1);

    mnodeValue = putLocalValue(entry, version + 1, null, null, cacheKey,
                               flags,
                               expireTimeout, idleTimeout,
                               leaseTimeout, localReadTimeout,
                               leaseOwner);

    if (mnodeValue == null)
      return oldValueHash != null;

    removeCluster(key, cacheKey, mnodeValue);

    return oldValueHash != null;
  }

  protected void removeCluster(HashKey key,
                               HashKey cacheKey,
                               MnodeValue mnodeValue)
  {
  }

  /**
   * Sets a cache entry
   */
  @Override
  public boolean remove(HashKey key)
  {
    E entry = getCacheEntry(key);
    MnodeValue mnodeValue = entry.getMnodeValue();

    HashKey oldValueHash = mnodeValue != null ? mnodeValue.getValueHashKey() : null;
    HashKey cacheKey = mnodeValue != null ? mnodeValue.getCacheHashKey() : null;

    int flags = mnodeValue != null ? mnodeValue.getFlags() : 0;
    long version = mnodeValue != null ? mnodeValue.getVersion() : 0;

    long expireTimeout = mnodeValue != null ? mnodeValue.getExpireTimeout() : -1;
    long idleTimeout = mnodeValue != null ? mnodeValue.getIdleTimeout() : -1;
    long leaseTimeout = mnodeValue != null ? mnodeValue.getLeaseTimeout() : -1;
    long localReadTimeout = mnodeValue != null ? mnodeValue.getLocalReadTimeout() : -1;
    int leaseOwner = mnodeValue != null ? mnodeValue.getLeaseOwner() : -1;

    mnodeValue = putLocalValue(entry, version + 1, null, null, cacheKey,
                               flags,
                               expireTimeout, idleTimeout,
                               leaseTimeout, localReadTimeout, leaseOwner);

    if (mnodeValue == null)
      return oldValueHash != null;

    putCluster(key, null, cacheKey, mnodeValue);
    /*
    _cacheService.put(key, null, cacheKey,
                      mnodeValue.getFlags(), mnodeValue.getVersion(),
                      mnodeValue.getExpireTimeout(),
                      mnodeValue.getIdleTimeout(),
                      mnodeValue.getLeaseTimeout(),
                      mnodeValue.getLocalReadTimeout(),
                      getSelfIndex(),
                      null);
    */

    return oldValueHash != null;
  }

  /**
   * Sets a cache entry
   */
  MnodeValue putLocalValue(DistCacheEntry entry,
                           long version,
                           HashKey valueHash,
                           Object value,
                           HashKey cacheHash,
                           int flags,
                           long expireTimeout,
                           long idleTimeout,
                           long leaseTimeout,
                           long localReadTimeout,
                           int leaseOwner)
  {
    HashKey key = entry.getKeyHash();

    MnodeValue oldEntryValue = loadMnodeValue(entry);

    HashKey oldValueHash
      = oldEntryValue != null ? oldEntryValue.getValueHashKey() : null;

    long oldVersion = oldEntryValue != null ? oldEntryValue.getVersion() : 0;
    long now = Alarm.getCurrentTime();

    if (version < oldVersion
        || (version == oldVersion
            && valueHash != null
            && valueHash.compareTo(oldValueHash) <= 0)) {
      // lease ownership updates even if value doesn't
      if (oldEntryValue != null) {
        oldEntryValue.setLeaseOwner(leaseOwner, now);

        // XXX: access time?
        oldEntryValue.setLastAccessTime(now);
      }

      return oldEntryValue;
    }

    long accessTime = now;
    long updateTime = accessTime;

    MnodeValue mnodeValue = new MnodeValue(valueHash, value,
                                           cacheHash,
                                           flags, version,
                                           expireTimeout,
                                           idleTimeout,
                                           leaseTimeout,
                                           localReadTimeout,
                                           accessTime,
                                           updateTime,
                                           true,
                                           false);
    mnodeValue.setLeaseOwner(leaseOwner, now);

    // the failure cases are not errors because this put() could
    // be immediately followed by an overwriting put()

    if (! entry.compareAndSet(oldEntryValue, mnodeValue)) {
      log.fine(this + " mnodeValue update failed due to timing conflict"
               + " (key=" + key + ")");

      return null;
    }

    if (oldEntryValue == null
        || oldEntryValue.isImplicitNull()
        || oldEntryValue == MnodeValue.NULL) {
      if (_mnodeStore.insert(key, valueHash, cacheHash,
                                  flags, version,
                                  expireTimeout,
                                  idleTimeout,
                                  leaseTimeout,
                                  localReadTimeout)) {
        return mnodeValue;
      } else {
        log.fine(this + " db insert failed due to timing conflict"
          + "(key=" + key + ", version=" + version + ")");
      }
    } else {
      if (_mnodeStore.updateSave(key, valueHash, version, idleTimeout)) {
        return mnodeValue;
      }
      else if (_mnodeStore.insert(key, valueHash, cacheHash,
                                  flags, version,
                                  expireTimeout,
                                  idleTimeout,
                                  leaseTimeout,
                                  localReadTimeout)) {
        return mnodeValue;
      }
      else {
        log.fine(this + " db update failed due to timing conflict"
          + "(key=" + key + ", version=" + version + ")");
      }
    }

    return null;
  }

  protected HashKey writeData(HashKey oldValueHash,
                              Object value,
                              CacheSerializer serializer)
  {
    TempOutputStream os = null;

    try {
      os = new TempOutputStream();

      Sha256OutputStream mOut = new Sha256OutputStream(os);
      DeflaterOutputStream gzOut = new DeflaterOutputStream(mOut);

      serializer.serialize(value, gzOut);

      gzOut.finish();
      mOut.close();

      byte[] hash = mOut.getDigest();

      HashKey valueHash = new HashKey(hash);

      if (valueHash.equals(oldValueHash))
        return valueHash;

      int length = os.getLength();

      StreamSource source = new StreamSource(os);
      if (! _dataStore.save(valueHash, source, length)) {
        throw new IllegalStateException(L.l("Can't save the data '{0}'",
                                       valueHash));
      }

      return valueHash;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (os != null)
        os.close();
    }
  }

  protected HashKey writeData(HashKey oldValueHash,
                              InputStream is)
    throws IOException
  {
    TempOutputStream os = null;

    try {
      os = new TempOutputStream();

      Sha256OutputStream mOut = new Sha256OutputStream(os);

      WriteStream out = Vfs.openWrite(mOut);

      out.writeStream(is);

      out.close();

      mOut.close();

      byte[] hash = mOut.getDigest();

      HashKey valueHash = new HashKey(hash);

      if (valueHash.equals(oldValueHash)) {
        os.destroy();
        return valueHash;
      }

      int length = os.getLength();
      StreamSource source = new StreamSource(os);

      if (! _dataStore.save(valueHash, source, length))
        throw new RuntimeException(L.l("Can't save the data '{0}'",
                                       valueHash));

      return valueHash;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (os != null)
        os.close();
    }
  }

  protected void writeData(HashKey valueHash, StreamSource source, int length)
    throws IOException
  {
    _dataStore.save(valueHash, source, length);
  }

  protected Object readData(HashKey valueKey,
                            int flags,
                            CacheSerializer serializer)
  {
    if (valueKey == null || valueKey == HashManager.NULL)
      return null;

    TempOutputStream os = null;

    try {
      os = new TempOutputStream();

      WriteStream out = Vfs.openWrite(os);

      if (! _dataStore.load(valueKey, out)) {
        requestClusterData(valueKey, flags);

        if (! _dataStore.load(valueKey, out)) {
          out.close();
          System.out.println("MISSING_DATA: " + valueKey);
        
          return null;
        }
      }

      out.close();

      InputStream is = os.openInputStream();

      try {
        InflaterInputStream gzIn = new InflaterInputStream(is);

        Object value = serializer.deserialize(gzIn);

        gzIn.close();

        return value;
      } finally {
        is.close();
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      
      return null;
    } finally {
      if (os != null)
        os.close();
    }
  }

  protected boolean readData(HashKey valueKey,
                             int flags,
                             OutputStream os)
    throws IOException
  {
    if (valueKey == null || valueKey == HashManager.NULL)
      throw new IllegalStateException(L.l("readData may not be called with a null value"));

    WriteStream out = Vfs.openWrite(os);

    try {
      if (_dataStore.load(valueKey, out))
        return true;

      requestClusterData(valueKey, flags);

      if (_dataStore.load(valueKey, out))
        return true;

      log.warning(this + " unexpected load failure");

      // XXX: error?  since we have the value key, it should exist

      return false;
    } finally {
      out.close();
    }
  }

  protected void requestClusterData(HashKey valueKey, int flags)
  {
    // _cacheService.requestData(valueKey, flags);
  }

  protected boolean isDataAvailable(HashKey valueKey)
  {
    if (valueKey == null || valueKey == HashManager.NULL)
      return false;

    return _dataStore.isDataAvailable(valueKey);
  }

  /**
   * Returns the last update time on server startup.
   */
  public long getStartupLastUpdateTime()
  {
    return _mnodeStore.getStartupLastUpdateTime();
  }

  /**
   * Returns a set of entries since an access time.
   */
  public ArrayList<CacheData> getUpdates(long accessTime, int offset)
  {
    return _mnodeStore.getUpdates(accessTime, offset);
  }

  /**
   * Returns a set of entries since an access time.
   */
  public ArrayList<CacheData> getGlobalUpdates(long accessTime, int offset)
  {
    return _mnodeStore.getGlobalUpdates(accessTime, offset);
  }

  /**
   * Clears leases on server start/stop
   */
  public void clearLeases()
  {
    Iterator<E> iter = _entryCache.values();

    while (iter.hasNext()) {
      E entry = iter.next();

      entry.clearLease();
    }
  }

  /**
   * Clears ephemeral data on startup.
   */
  public void clearEphemeralEntries()
  {
  }

  /**
   * Closes the manager.
   */
  @Override
  public void close()
  {
    _mnodeStore.close();
  }
}
