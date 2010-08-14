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
 * @author Charles Reich
 */

package com.caucho.quercus.lib;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

import java.util.logging.Logger;

/**
 * memcache object oriented API facade
 */
public class Memcache {
  private static final Logger log = Logger.getLogger(Memcache.class.getName());
  private static final L10N L = new L10N(Memcache.class);

  private Cache _cache;

  /**
   * Adds a server.
   */
  public boolean addServer(Env env,
                           String host,
                           @Optional int port,
                           @Optional boolean persistent,
                           @Optional int weight,
                           @Optional int timeout,
                           @Optional int retryInterval)
  {
    if (_cache == null)
      connect(env, host, port, timeout);

    return true;
  }

  /**
   * Connect to a server.
   */
  public boolean connect(Env env,
                         String host,
                         @Optional int port,
                         @Optional("1") int timeout)
  {
    // Always true since this is a local copy

    String name = "memcache::" + host + ":" + port;

    _cache = (Cache) env.getQuercus().getSpecial(name);

    if (_cache == null) {
      _cache = new Cache();

      env.getQuercus().setSpecial(name, _cache);
    }

    return true;
  }

  /**
   * Returns a value.
   */
  public Value get(Env env, Value keys)
  {
    if (keys.isArray())
      return BooleanValue.FALSE;

    String key = keys.toString();

    Value value = _cache.get(key);

    if (value != null)
      return value.copy(env);
    else
      return BooleanValue.FALSE;
  }
  
  /*
   * Removes a value.
   */
  public boolean delete(Env env,
                        String key,
                        @Optional int timeout)
  {
    _cache.remove(key);
    
    return true;
  }

  /*
   * Clears the cache.
   */
  public boolean flush(Env env)
  {
    _cache.clear();
    
    return true;
  }
  
  /**
   * Returns version information.
   */
  public String getVersion()
  {
    return "1.0";
  }

  /**
   * Connect to a server.
   */
  public boolean pconnect(Env env,
                          String host,
                          @Optional int port,
                          @Optional("1") int timeout)
  {
    return connect(env, host, port, timeout);
  }

  /**
   * Sets a value.
   */
  public boolean set(Env env,
                     String key,
                     Value value,
                     @Optional int flag,
                     @Optional int expire)
  {
    _cache.set(key, value.copy(env));

    return true;
  }

  /**
   * Sets the compression threshold
   */
  public boolean setCompressThreshold(int threshold,
                                      @Optional double minSavings)
  {
    return true;
  }

  /**
   * Closes the connection.
   */
  public boolean close()
  {
    return true;
  }

  public String toString()
  {
    return "Memcache[]";
  }

  static class Cache extends Value {
    private LruCache<String,Value> _map = new LruCache<String,Value>(256);

    public Value get(String key)
    {
      return _map.get(key);
    }

    public void set(String key, Value value)
    {
      _map.put(key, value);
    }
    
    public Value remove(String key)
    {
      return _map.remove(key);
    }
    
    public void clear()
    {
      _map.clear();
    }
  }
}
