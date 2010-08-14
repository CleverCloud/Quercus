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

package com.caucho.rewrite;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.util.InetNetwork;
import com.caucho.util.LruCache;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Match if the remote IP address matches one of the pattern networks.
 * Standard IP network syntax is allowed, so 192.168/16 matches the entire
 * subnetwork.
 *
 * <pre>
 * &lt;resin:Allow url-pattern="/admin/*"
 *                xmlns:resin="urn:java:com.caucho.resin">
 *   &lt;resin:IfNetwork value="192.168.17.0/24"/&gt;
 * &lt;/resin:Allow>
 * </pre>
 * 
 * <pre> 
 * &lt;resin:Forbidden
 *         xmlns:resin="urn:java:com.caucho.resin">
 *   &lt;resin:IfNetwork>
 *     &lt;value>205.11.12.3&lt;/value>
 *     &lt;value>123.4.45.6&lt;/value>
 *     &lt;value>233.15.25.35&lt;/value>
 *     &lt;value>233.14.87.12&lt;/value>
 *   &lt;/resin:IfNetwork&gt;
 * &lt;/resin:Forbidden>
 * </pre>
 *
 * <p>RequestPredicates may be used for both security and rewrite conditions.
 */
@Configurable
public class IfNetwork implements RequestPredicate {
  private static final Logger log
    = Logger.getLogger(IfNetwork.class.getName());
  static L10N L = new L10N(IfNetwork.class);

  private ArrayList<InetNetwork> _networkList = new ArrayList<InetNetwork>();

  private int _cacheSize = 256;

  private LruCache<String,Boolean> _cache;

  /**
   * Size of the cache used to hold whether or not to allow a certain IP
   * address, default is 256.  The first time a request is received from an ip,
   * the allow and deny rules are checked to determine if the ip is allowed.
   * The result of this check is cached in a an LRU cache.  Subsequent requests
   * can do a cache lookup based on the ip instead of checking the rules.  This
   * is especially important if there are a large number of allow and/or deny
   * rules, and to protect against denial of service attacks.  
   */ 
  @Configurable
  public void setCacheSize(int cacheSize)
  {
    _cacheSize = cacheSize;
  }

  /** 
   * Size of the cache used to hold whether or not to allow a certain IP
   * address.
   */ 
  public int getCacheSize()
  {
    return _cacheSize;
  }

  /**
   * Add an ip network to allow.  If allow is never used, (only deny is used),
   * then all are allowed except those in deny.
   * @throws UnknownHostException 
   */
  @Configurable
  public void addValue(String network) 
    throws UnknownHostException
  {
    if (_networkList == null)
      _networkList = new ArrayList<InetNetwork>();

    _networkList.add(InetNetwork.create(network));
  }

  @PostConstruct
  public void init()
    throws ConfigException
  {
    _cache = new LruCache<String,Boolean>(_cacheSize);
  }

  /**
   * True if the predicate matches.
   *
   * @param request the servlet request to test
   */
  @Override
  public boolean isMatch(HttpServletRequest request)
  {
    String remoteAddr = request.getRemoteAddr();

    if (remoteAddr == null)
      return false;
    
    if (_cache != null) {
      Boolean cacheValue = _cache.get(remoteAddr);

      if (cacheValue != null)
        return cacheValue;
    }

    InetAddress addr = null;
    
    try {
      addr = InetAddress.getByName(remoteAddr);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      return false;
    }

    boolean isMatch = false;
    for (int i = 0; i < _networkList.size(); i++) {
      InetNetwork net = _networkList.get(i);
      
      if (net.isMatch(addr)) {
        isMatch = true;
        break;
      }
    }
    
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " match=" + isMatch + " " + addr);
    }

    // update cache

    if (_cache != null)
      _cache.put(remoteAddr, isMatch);

    return isMatch;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + _networkList;
  }
}
