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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.security;

import com.caucho.util.InetNetwork;
import com.caucho.util.L10N;
import com.caucho.util.LongKeyMap;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class to forbid hosts by IP.
 */
public class ForbidHost {
  static final protected Logger log
    = Logger.getLogger(ForbidHost.class.getName());
  static final L10N L = new L10N(ForbidHost.class);

  private LongKeyMap _forbiddenHosts;
  private ArrayList _forbiddenNets;

  /**
   * Adds a forbidden host.
   */
  public void addForbidIP(String addrName)
  {
    try {
      InetAddress addr = InetAddress.getByName(addrName);

      if (_forbiddenHosts == null)
        _forbiddenHosts = new LongKeyMap();
      
      _forbiddenHosts.put(inetAddressToLong(addr), "true");
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Removes a forbidden host.
   */
  public void removeForbidIP(String addrName)
  {
    try {
      InetAddress addr = InetAddress.getByName(addrName);

      if (_forbiddenHosts != null)
        _forbiddenHosts.remove(inetAddressToLong(addr));
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Adds a forbidden net.
   */
  public void addForbidNet(String netmask)
  {
    try {
      InetNetwork net = InetNetwork.create(netmask);

      if (net == null)
        return;

      if (_forbiddenNets == null)
        _forbiddenNets = new ArrayList();
      
      _forbiddenNets.add(net);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Removes a forbidden net.
   */
  public void removeForbidNet(String netmask)
  {
    try {
      InetNetwork net = InetNetwork.create(netmask);

      if (net == null)
        return;

      if (_forbiddenNets != null)
        _forbiddenNets.remove(net);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Initialize the forbidden host.
   */
  @PostConstruct
  public void init()
  {
  }

  /**
   * Returns true if the host is forbidden.
   */
  /*
  public boolean isForbidden(long addr)
  {
    if (_forbiddenHosts != null) {
      if (_forbiddenHosts.get(addr) != null)
        return true;
    }

    if (_forbiddenNets != null) {
      for (int i = _forbiddenNets.size(); i >= 0; i--) {
        InetNetwork net = (InetNetwork) _forbiddenNets.get(i);

        if (net.isMatch(addr))
          return true;
      }
    }

    return false;
  }
  */

  /**
   * Returns true if the host is forbidden.
   */
  public boolean isForbidden(InetAddress addr)
  {
    if (_forbiddenHosts == null && _forbiddenNets == null)
      return false;
    
    long ip = inetAddressToLong(addr);
    if (_forbiddenHosts != null) {
      if (_forbiddenHosts.get(ip) != null)
        return true;
    }

    if (_forbiddenNets != null) {
      for (int i = _forbiddenNets.size(); i >= 0; i--) {
        InetNetwork net = (InetNetwork) _forbiddenNets.get(i);

        if (net.isMatch(addr))
          return true;
      }
    }

    return false;
  }

  private static long inetAddressToLong(InetAddress addr)
  {
    byte []bytes = addr.getAddress();

    long address = 0;
    for (int i = 0; i < bytes.length; i++)
      address = 256 * address + (bytes[i] & 0xff);

    return address;
  }
}
