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

package com.caucho.server.security;

import com.caucho.config.ConfigException;
import com.caucho.util.InetNetwork;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Allow or deny requests based on the ip address of the client.
 *
 * <pre>
 * &lt;security-constraint&gt;
 *   &lt;ip-constraint&gt;
 *     &lt;allow&gt;192.168.17.0/24&lt;/allow&gt;
 *   &lt;/ip-constraint&gt;
 * 
 *   &lt;web-resource-collection&gt;
 *     &lt;url-pattern&gt;/admin/*&lt;/url-pattern&gt;
 *   &lt;/web-resource-collection&gt;
 * &lt;/security-constraint&gt;
 * </pre>
 * 
 * <pre> 
 * &lt;security-constraint&gt;
 *   &lt;ip-constraint&gt;
 *     &lt;deny&gt;205.11.12.3&lt;/deny&gt;
 *     &lt;deny&gt;213.43.62.45&lt;/deny&gt;
 *     &lt;deny&gt;123.4.45.6&lt;/deny&gt;
 *     &lt;deny&gt;233.15.25.35&lt;/deny&gt;
 *     &lt;deny&gt;233.14.87.12&lt;/deny&gt;
 *   &lt;/ip-constraint&gt;
 * 
 *   &lt;web-resource-collection&gt;
 *     &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 *   &lt;/web-resource-collection&gt;
 * &lt;/security-constraint&gt;
 * </pre>
 */
public class IPConstraint extends AbstractConstraint {
  private static final Logger log
    = Logger.getLogger(IPConstraint.class.getName());
  static L10N L = new L10N(IPConstraint.class);

  private ArrayList<InetNetwork> _allowNetworkList;
  private ArrayList<InetNetwork> _denyNetworkList;

  private int _cacheSize = 256;
  private int _errorCode = HttpServletResponse.SC_FORBIDDEN;
  private String _errorMessage = L.l("Forbidden IP Address");

  private LruCache<String,Boolean> _cache;

  /** see method SecurityConstraint.addIPConstraint() for explanation */
  private boolean _oldStyle = false;

  public IPConstraint()
  {
  }

  /**
   * The error code to send with response.sendError, default is 403.
   */
  public void setErrorCode(int errorCode)
  {
    _errorCode = errorCode;
  }

  /**
   * The error code to send with response.sendError, default is 403.
   */
  public int getErrorCode()
  {
    return _errorCode;
  }

  /**
   * The error message to send with response.sendError, default is 
   * "Forbidden IP Address" 
   */
  public void setErrorMessage(String errorMessage)
  {
    _errorMessage = errorMessage;
  }

  /**
   * The error message to send with response.sendError, default is 
   * "Forbidden IP Address" 
   */
  public String getErrorMessage()
  {
    return _errorMessage;
  }

  /**
   * Size of the cache used to hold whether or not to allow a certain IP
   * address, default is 256.  The first time a request is received from an ip,
   * the allow and deny rules are checked to determine if the ip is allowed.
   * The result of this check is cached in a an LRU cache.  Subsequent requests
   * can do a cache lookup based on the ip instead of checking the rules.  This
   * is especially important if there are a large number of allow and/or deny
   * rules, and to protect against denial of service attacks.  
   */ 
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
  public void addAllow(String network) throws UnknownHostException
  {
    if (_allowNetworkList == null)
      _allowNetworkList = new ArrayList<InetNetwork>();

    _allowNetworkList.add(InetNetwork.create(network));
  }

  /**
   * Add an ip network to deny.
   * @throws UnknownHostException 
   */
  public void addDeny(String network) throws UnknownHostException
  {
    if (_denyNetworkList == null)
      _denyNetworkList = new ArrayList<InetNetwork>();

    _denyNetworkList.add(InetNetwork.create(network));
  }

  /** backwards compatibility, same as addAllow() 
   * @throws UnknownHostException */
  public void addText(String network) throws UnknownHostException
  {
    _oldStyle = true;
    addAllow(network);
  }

  /** backwards compatibility, used by SecurityConstraint.addIPConstraint() */
  boolean isOldStyle()
  {
    return _oldStyle;
  }

  /** backwards compatibility, used by SecurityConstraint.addIPConstraint() */
  void copyInto(IPConstraint target)
  {
    if (_allowNetworkList != null) {
      for (int i = 0; i < _allowNetworkList.size(); i++) {
        target.addAllowInetNetwork(_allowNetworkList.get(i));
      }
    }
    if (_denyNetworkList != null) {
      for (int i = 0; i < _denyNetworkList.size(); i++) {
        target.addDenyInetNetwork(_denyNetworkList.get(i));
      }
    }
  }

  private void addAllowInetNetwork(InetNetwork a)
  {
    if (_allowNetworkList == null)
      _allowNetworkList = new ArrayList<InetNetwork>();
    _allowNetworkList.add(a);
  }

  private void addDenyInetNetwork(InetNetwork d)
  {
    if (_denyNetworkList == null)
      _denyNetworkList = new ArrayList<InetNetwork>();
    _denyNetworkList.add(d);
  }

  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_allowNetworkList == null && _denyNetworkList == null)
      throw new ConfigException(L.l("<ip-constraint> either '<allow>' or '<deny>' or both are expected"));

    if (_allowNetworkList != null)
      _allowNetworkList.trimToSize();

    if (_denyNetworkList != null)
      _denyNetworkList.trimToSize();

    int rules = _allowNetworkList == null ? 0 : _allowNetworkList.size();
    rules += _denyNetworkList == null ? 0 : _denyNetworkList.size();

    _cache = new LruCache<String,Boolean>(_cacheSize);
  }

  /**
   * Returns true if the user is authorized for the resource.
   */
  public AuthorizationResult isAuthorized(HttpServletRequest request,
                                          HttpServletResponse response,
                                          ServletContext application)
    throws ServletException, IOException
  {
    String remoteAddr = request.getRemoteAddr();
    boolean allow =  false;
    InetAddress addr = null;

    if (remoteAddr != null) {
      if (_cache != null) {
        Boolean cacheValue = _cache.get(remoteAddr);
        if (cacheValue != null) {
          allow = cacheValue.booleanValue();

          if (! allow)
            response.sendError(_errorCode, _errorMessage);

          return (allow
                  ? AuthorizationResult.ALLOW
                  : AuthorizationResult.DENY_SENT_RESPONSE);
        }
      }
      
      addr = InetAddress.getByName(remoteAddr);
    }

    // check allow

    if (_allowNetworkList == null) {
      // if no allow specified, then allow all
      allow = true;
    }
    else {
      for (int i = 0; i < _allowNetworkList.size(); i++) {
        InetNetwork net = _allowNetworkList.get(i);

        if (net.isMatch(addr)) {
          allow = true;
          break;
        }
      }
    }

    // check deny

    if (allow && _denyNetworkList != null) {
      for (int i = 0; i < _denyNetworkList.size(); i++) {
        InetNetwork net = _denyNetworkList.get(i);

        if (net.isMatch(addr)) {
          allow = false;
          break;
        }
      }
    }

    // update cache

    if (_cache != null)
      _cache.put(remoteAddr, allow ? Boolean.TRUE : Boolean.FALSE);

    // respond accordingly

    if (! allow)
      response.sendError(_errorCode, _errorMessage);


    return (allow
            ? AuthorizationResult.ALLOW
            : AuthorizationResult.DENY_SENT_RESPONSE);
  }

}
