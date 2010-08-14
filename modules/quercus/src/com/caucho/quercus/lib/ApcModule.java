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

package com.caucho.quercus.lib;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.Reference;
import com.caucho.quercus.env.*;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.IniDefinitions;
import com.caucho.quercus.module.IniDefinition;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.Path;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * APC object oriented API facade
 */
public class ApcModule extends AbstractQuercusModule
{
  private static final Logger log
    = Logger.getLogger(ApcModule.class.getName());
  private static final L10N L = new L10N(ApcModule.class);

  private static final IniDefinitions _iniDefinitions = new IniDefinitions();

  private static final int _defaultSize = 4096;
  
  private LruCache<String,Entry> _cache;

  private HashMap<String,Value> _constMap = new HashMap<String,Value>();

  /**
   * Returns true for the mysql extension.
   */
  public String []getLoadedExtensions()
  {
    return new String[] { "apc" };
  }

  /**
   * Returns the default php.ini values.
   */
  public IniDefinitions getIniDefinitions()
  {
    return _iniDefinitions;
  }

  /**
   * Returns cache information.
   */
  public Value apc_cache_info(Env env,
                              @Optional String type,
                              @Optional boolean limited)
  {
    ArrayValue value = new ArrayValueImpl();

    if (_cache != null) {
      value.put("num_slots", _cache.getCapacity());
      value.put("ttl", 0);
      value.put("num_hits", _cache.getHitCount());
      value.put("num_misses", _cache.getMissCount());
      value.put("start_time", 0);
    }
    else {
      value.put("num_slots", 0);
      value.put("ttl", 0);
      value.put("num_hits", 0);
      value.put("num_misses", 0);
      value.put("start_time", 0);
    }

    ArrayValueImpl cacheList = new ArrayValueImpl();
    value.put(env.createString("cache_list"), cacheList);

    if ("user".equals(type) && _cache != null && ! limited) {
      ArrayList<String> keys = new ArrayList<String>();
      ArrayList<Entry> values = new ArrayList<Entry>();

      synchronized (_cache) {
        Iterator<LruCache.Entry<String,Entry>> iter = _cache.iterator();

        while (iter.hasNext()) {
          LruCache.Entry<String,Entry> lruEntry = iter.next();
          
          keys.add(lruEntry.getKey());
          values.add(lruEntry.getValue());
        }
      }

      for (int i = 0; i < keys.size(); i++) {
        String key = keys.get(i);
        Entry entryValue = values.get(i);

        if (entryValue.isValid(env)) {
          ArrayValueImpl array = new ArrayValueImpl();
          cacheList.put(array);

          array.put(env.createString("info"), env.createString(key));
          array.put(env.createString("ttl"),
                    LongValue.create(entryValue.getTTL(env)));
          array.put(env.createString("type"), env.createString("user"));
          array.put(env.createString("num_hits"),
                    LongValue.create(entryValue.getHitCount()));
        }
      }
    }

    return value;
  }

  /**
   * Clears the cache
   */
  public boolean apc_clear_cache(Env env, @Optional String type)
  {
    if (_cache != null)
      _cache.clear();

    return true;
  }
  
  /**
   * Preloads the specified file.
   */
  public boolean apc_compile_file(Env env, StringValue name)
  {
    try {
      Path path = env.lookup(name);
      
      if (path != null && path.canRead()) {
        env.getQuercus().parse(path);
      
        return true;
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.getMessage(), e);
    }
    
    return false;
  }

  /**
   * Defines constants
   */
  public boolean apc_define_constants(Env env,
                                      String key,
                                      ArrayValue values,
                                      @Optional("true") boolean caseSensitive)
  {
    _constMap.put(key, values.copy(env));

    return true;
  }

  /**
   * Deletes a value.
   */
  public boolean apc_delete(Env env, String key)
  {
    if (_cache == null)
      return false;
    
    return _cache.remove(key) != null;
  }

  /**
   * Returns a value.
   */
  public Value apc_fetch(Env env, 
                         String key,
                         @Optional @Reference Value isSuccessful)
  {
    isSuccessful.set(BooleanValue.FALSE);
    
    if (_cache == null)
      return BooleanValue.FALSE;
    
    Entry entry = _cache.get(key);

    if (entry == null)
      return BooleanValue.FALSE;

    Value value = entry.getValue(env);
    
    if (value != null)
      initObject(env, new IdentityHashMap<Value,Value>(), value);

    if (value != null) {
      isSuccessful.set(BooleanValue.TRUE);
      return value;
    }
    else
      return BooleanValue.FALSE;
  }
  
  /**
   * Updates the value's class with a currently available one.
   */
  private static void initObject(Env env,
                                 IdentityHashMap<Value,Value> valueMap,
                                 Value value)
  {
    if (value.isObject()) {
      if (valueMap.containsKey(value))
        return;
      
      valueMap.put(value, value);
      
      ObjectValue obj = (ObjectValue) value.toValue().toObject(env);

      String className;
      
      if (obj.isIncompleteObject())
        className = obj.getIncompleteObjectName();
      else
        className = obj.getName();
      
      QuercusClass cls = env.findClass(className);
        
      if (cls != null) {
        obj.initObject(env, cls);
      }
    }
    else if (value.isArray()) {
      if (valueMap.containsKey(value))
        return;
      
      valueMap.put(value, value);
      
      Iterator<Value> iter = value.getValueIterator(env);
      
      while (iter.hasNext()) {
        initObject(env, valueMap, iter.next());
      }
    }
  }

  /**
   * Defines constants
   */
  public boolean apc_load_constants(Env env,
                                    String key,
                                    @Optional("true") boolean caseSensitive)
  {
    ArrayValue array = (ArrayValue) _constMap.get(key);

    if (array == null)
      return false;

    for (Map.Entry<Value,Value> entry : array.entrySet()) {
      env.addConstant(entry.getKey().toString(),
                      entry.getValue().copy(env),
                      ! caseSensitive);
    }

    return true;
  }

  /**
   * Returns cache information.
   */
  public Value apc_sma_info(Env env, @Optional String type)
  {
    ArrayValue value = new ArrayValueImpl();

    value.put("num_seg", 1);
    value.put("seg_size", 1024 * 1024);
    value.put("avail_mem", 1024 * 1024);
    value.put(env.createString("block_lists"), new ArrayValueImpl());

    return value;
  }

  /**
   * Returns a value.
   */
  public Value apc_store(Env env, String key, Value value,
                         @Optional("0") int ttl)
  {
    if (_cache == null) {
      long size = env.getIniLong("apc.user_entries_hint");

      if (size <= 0)
        size = _defaultSize;

      _cache = new LruCache<String,Entry>((int) size);
    }
    
    _cache.put(key, new Entry(env, value, ttl));

    return BooleanValue.TRUE;
  }

  static class Entry extends UnserializeCacheEntry {
    private long _createTime;
    private long _accessTime;
    
    private long _expire;
    private int _hitCount;

    Entry(Env env, Value value, int ttl)
    {
      super(env, value);

      if (ttl <= 0)
        _expire = Long.MAX_VALUE / 2;
      else
        _expire = env.getCurrentTime() + ttl * 1000L;

      _createTime = env.getCurrentTime();
    }

    public long getTTL(Env env)
    {
      if (_expire >= Long.MAX_VALUE / 2)
        return 0;
      else
        return (_expire - env.getCurrentTime()) / 1000L;
    }

    public long getHitCount()
    {
      return _hitCount;
    }

    public boolean isValid(Env env)
    {
      if (env.getCurrentTime() <= _expire)
        return true;
      else {
        clear();

        return false;
      }
    }

    public Value getValue(Env env)
    {
      if (env.getCurrentTime() <= _expire) {
        _accessTime = env.getCurrentTime();
        _hitCount++;
        
        return super.getValue(env);
      }
      else {
        return null;
      }
    }
  }

  static final IniDefinition INI_APC_ENABLED
    = _iniDefinitions.add("apc.enabled", true, PHP_INI_ALL);
  static final IniDefinition INI_APC_SHM_SEGMENTS
    = _iniDefinitions.add("apc.shm_segments", 1, PHP_INI_SYSTEM);
  static final IniDefinition INI_APC_SHM_SIZE
    = _iniDefinitions.add("apc.shm_size", 30, PHP_INI_SYSTEM);
  static final IniDefinition INI_APC_OPTIMIZATION
    = _iniDefinitions.add("apc.optimization", false, PHP_INI_ALL);
  static final IniDefinition INI_APC_NUM_FILES_HINT
    = _iniDefinitions.add("apc.num_files_hint", 1000, PHP_INI_SYSTEM);
  static final IniDefinition INI_APC_USER_ENTRIES_HINT
    = _iniDefinitions.add(
      "apc.user_entries_hint", _defaultSize, PHP_INI_SYSTEM);
  static final IniDefinition INI_APC_TTL
    = _iniDefinitions.add("apc.ttl", 0, PHP_INI_SYSTEM);
  static final IniDefinition INI_APC_USER_TTL
    = _iniDefinitions.add("apc.user_ttl", 0, PHP_INI_SYSTEM);
  static final IniDefinition INI_APC_GC_TTL
    = _iniDefinitions.add("apc.gc_ttl", "3600", PHP_INI_SYSTEM);
  static final IniDefinition INI_APC_CACHE_BY_DEFAULT
    = _iniDefinitions.add("apc.cache_by_default", true, PHP_INI_SYSTEM);
  static final IniDefinition INI_APC_FILTERS
    = _iniDefinitions.add("apc.filters", "", PHP_INI_SYSTEM);
  static final IniDefinition INI_APC_MMAP_FILE_MASK
    = _iniDefinitions.add("apc.mmap_file_mask", "", PHP_INI_SYSTEM);
  static final IniDefinition INI_APC_SLAM_DEFENSE
    = _iniDefinitions.add("apc.slam_defense", false, PHP_INI_SYSTEM);
  static final IniDefinition INI_APC_FILE_UPDATE_PROTECTION
    = _iniDefinitions.add("apc.file_update_protection", "2", PHP_INI_SYSTEM);
  static final IniDefinition INI_APC_ENABLE_CLI
    = _iniDefinitions.add("apc.enable_cli", false, PHP_INI_SYSTEM);
  static final IniDefinition INI_APC_MAX_FILE_SIZE
    = _iniDefinitions.add("apc.max_file_size", "1M", PHP_INI_SYSTEM);
  static final IniDefinition INI_APC_STAT
    = _iniDefinitions.add("apc.stat", "1", PHP_INI_SYSTEM);
  static final IniDefinition INI_APC_WRITE_LOCK
    = _iniDefinitions.add("apc.write_lock", "1", PHP_INI_SYSTEM);
  static final IniDefinition INI_APC_LOCALCACHE
    = _iniDefinitions.add("apc.localcache", "0", PHP_INI_SYSTEM);
  static final IniDefinition INI_APC_LOCALCACHE_SIZE
    = _iniDefinitions.add(
      "apc.localcache.size", "" + _defaultSize, PHP_INI_SYSTEM);
}
