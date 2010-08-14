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

package com.caucho.server.dispatch;

import com.caucho.config.ConfigException;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.util.LruCache;
import com.caucho.vfs.Dependency;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * The dispatch server is responsible for building Invocations,
 * specifically for creating the FilterChain for the invocation.
 */
public class DispatchServer implements Dependency {
  private String _serverId = "";

  private DispatchBuilder _dispatchBuilder;

  // Cache of uri -> invocation maps
  private LruCache<Object,Invocation> _invocationCache;

  private InvocationDecoder _invocationDecoder;

  private HashMap<String,Object> _attributeMap
    = new HashMap<String,Object>();

  private ArrayList<ServerListener> _listeners
    = new ArrayList<ServerListener>();

  private int _invocationCacheSize = 64 * 1024;
  private int _maxURLLength = 256;
  //sets a limit on URIs Resin serves
  private int _maxURILength = 1024;

  private final Lifecycle _lifecycle = new Lifecycle();

  /**
   * Gets the server's id.
   */
  public String getServerId()
  {
    return _serverId;
  }

  /**
   * Gets the server's id.
   */
  public void setServerId(String serverId)
  {
    _serverId = serverId;
  }

  /**
   * Gets the class loader.
   */
  public ClassLoader getClassLoader()
  {
    return null;
  }

  /**
   * Sets the dispatch builder.
   */
  public void setDispatchBuilder(DispatchBuilder builder)
  {
    _dispatchBuilder = builder;
  }

  /**
   * Gets the dispatch builder.
   */
  public DispatchBuilder getDispatchBuilder()
  {
    return _dispatchBuilder;
  }

  /**
   * Return true for ignoring client disconnect.
   */
  public boolean isIgnoreClientDisconnect()
  {
    return true;
  }

  /**
   * Sets the invocation cache size.
   */
  public void setInvocationCacheSize(int size)
  {
    _invocationCacheSize = size;

    if (_invocationCacheSize <= 16)
      _invocationCacheSize = 16;
  }

  /**
   * Sets the max url length.
   */
  public void setInvocationCacheMaxURLLength(int length)
  {
    _maxURLLength = length;
  }

  public int getMaxURILength()
  {
    return _maxURILength;
  }

  /**
   * Sets max uri length
   */
  public void setMaxURILength(int maxURILength)
  {
    _maxURILength = maxURILength;
  }

  /**
   * Initializes the server.
   */
  @PostConstruct
  public void init()
  {
    _invocationCache = new LruCache<Object,Invocation>(_invocationCacheSize);
  }

  /**
   * Returns the InvocationDecoder.
   */
  public InvocationDecoder getInvocationDecoder()
  {
    if (_invocationDecoder == null) {
      _invocationDecoder = new InvocationDecoder();
      _invocationDecoder.setMaxURILength(_maxURILength);
    }

    return _invocationDecoder;
  }

  /**
   * Sets URL encoding.
   */
  public String getURLCharacterEncoding()
  {
    return getInvocationDecoder().getEncoding();
  }

  /**
   * Returns the invocation decoder for configuration.
   */
  public InvocationDecoder createInvocationDecoder()
  {
    return getInvocationDecoder();
  }

  /**
   * Returns the cached invocation.
   */
  public final Invocation getInvocation(Object protocolKey)
  {
    Invocation invocation = null;

    // XXX: see if can remove this
    LruCache<Object,Invocation> invocationCache = _invocationCache;

    if (invocationCache != null)
      invocation = invocationCache.get(protocolKey);

    if (invocation == null)
      return null;
    else if (invocation.isModified()) {
      return null;
    }
    else {
      return invocation;
    }
  }

  /**
   * Creates an invocation.
   */
  public Invocation createInvocation()
  {
    return new Invocation();
  }

  /**
   * Builds the invocation, saving its value keyed by the protocol key.
   *
   * @param protocolKey protocol-specific key to save the invocation in
   * @param invocation the invocation to build.
   */
  public Invocation buildInvocation(Object protocolKey, Invocation invocation)
    throws ConfigException
  {
    invocation = buildInvocation(invocation);

    // XXX: see if can remove this, and rely on the invocation cache existing
    LruCache<Object,Invocation> invocationCache = _invocationCache;

    if (invocationCache != null) {
      Invocation oldInvocation;
      oldInvocation = invocationCache.get(protocolKey);

      // server/10r2
      if (oldInvocation != null && ! oldInvocation.isModified())
        return oldInvocation;

      if (invocation.getURLLength() < _maxURLLength) {
        invocationCache.put(protocolKey, invocation);
      }
    }

    return invocation;
  }

  /**
   * Builds the invocation.
   */
  public Invocation buildInvocation(Invocation invocation)
    throws ConfigException
  {
    return getDispatchBuilder().buildInvocation(invocation);
  }

  /**
   * Clears the proxy cache.
   */
  public void clearCache()
  {
    // XXX: see if can remove this, and rely on the invocation cache existing
    LruCache<Object,Invocation> invocationCache = _invocationCache;

    if (invocationCache != null) {
      invocationCache.clear();
    }
  }

  /**
   * Clears matching entries.
   */
  protected void invalidateMatchingInvocations(InvocationMatcher matcher)
  {
    // XXX: see if can remove this, and rely on the invocation cache existing
    LruCache<Object,Invocation> invocationCache = _invocationCache;

    if (invocationCache != null) {
      synchronized (invocationCache) {
        Iterator<LruCache.Entry<Object,Invocation>> iter;
        iter = invocationCache.iterator();

        while (iter.hasNext()) {
          LruCache.Entry<Object,Invocation> entry = iter.next();
          Invocation value = entry.getValue();

          if (value != null && matcher.isMatch(value)) {
            iter.remove();
          }
        }
      }
    }
  }

  /**
   * Returns the invocations.
   */
  public ArrayList<Invocation> getInvocations()
  {
    // XXX: see if can remove this, and rely on the invocation cache existing
    LruCache<Object,Invocation> invocationCache = _invocationCache;

    if (invocationCache != null) {
      ArrayList<Invocation> invocationList = new ArrayList<Invocation>();
      
      synchronized (invocationCache) {
        Iterator<Invocation> iter;
        iter = invocationCache.values();

        while (iter.hasNext()) {
          invocationList.add(iter.next());
        }
      }

      return invocationList;
    }

    return null;
  }

  /**
   * Returns the invocation cache hit count.
   */
  public long getInvocationCacheHitCount()
  {
    LruCache<Object,Invocation> invocationCache = _invocationCache;

    if (invocationCache != null)
      return invocationCache.getHitCount();
    else
      return 0;
  }

  /**
   * Returns the invocation cache hit count.
   */
  public long getInvocationCacheMissCount()
  {
    LruCache<Object,Invocation> invocationCache = _invocationCache;

    if (invocationCache != null)
      return invocationCache.getMissCount();
    else
      return 0;
  }

  /**
   * Returns the named attribute.
   *
   * @param key the attribute key
   *
   * @return the attribute value
   */
  public synchronized Object getAttribute(String key)
  {
    return _attributeMap.get(key);
  }

  /**
   * Returns an iteration of attribute names.
   *
   * @return the iteration of names
   */
  public synchronized Iterator<String> getAttributeNames()
  {
    return _attributeMap.keySet().iterator();
  }

  /**
   * Sets the named attribute.
   *
   * @param key the attribute key
   * @param value the attribute value
   *
   * @return the old attribute value
   */
  public synchronized Object setAttribute(String key, Object value)
  {
    return _attributeMap.put(key, value);
  }

  /**
   * Removes the named attribute.
   *
   * @param key the attribute key
   *
   * @return the old attribute value
   */
  public synchronized Object removeAttribute(String key)
  {
    return _attributeMap.remove(key);
  }

  /**
   * Returns true if the server has been modified and needs restarting.
   */
  public boolean isModified()
  {
    return false;
  }

  /**
   * Log the reason for modification.
   */
  public boolean logModified(Logger log)
  {
    return false;
  }

  /**
   * Adds a listener.
   */
  public void addServerListener(ServerListener listener)
  {
    _listeners.add(listener);
  }

  /**
   * Returns true if the server is destroyed.
   */
  public boolean isDestroyed()
  {
    return _lifecycle.isDestroyed();
  }

  /**
   * Closes the server.
   */
  public void update()
  {
    destroy();
  }

  /**
   * Closes the server.
   */
  public void restart()
  {
    destroy();
  }

  /**
   * Closes the server.
   */
  public void destroy()
  {
    ArrayList<ServerListener> listeners;
    listeners = new ArrayList<ServerListener>(_listeners);
    _listeners.clear();

    for (int i = 0; i < listeners.size(); i++) {
      ServerListener listener = listeners.get(i);

      listener.closeEvent(this);
    }

    _invocationCache = null;
  }
}
