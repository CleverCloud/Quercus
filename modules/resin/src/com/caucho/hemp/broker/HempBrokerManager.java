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

package com.caucho.hemp.broker;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.Broker;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.server.cluster.Server;
import com.caucho.server.host.Host;


/**
 * Broker
 */
public class HempBrokerManager
{
  private static final Logger log
    = Logger.getLogger(HempBrokerManager.class.getName());
  
  private static EnvironmentLocal<HempBrokerManager> _localBroker
    = new EnvironmentLocal<HempBrokerManager>();
  
  // brokers
  private final HashMap<String,WeakReference<Broker>> _brokerMap
    = new HashMap<String,WeakReference<Broker>>();

  public HempBrokerManager()
  {
    _localBroker.set(this);
  }

  public static HempBrokerManager getCurrent()
  {
    return _localBroker.get();
  }

  public void addBroker(String name, Broker broker)
  {
    synchronized (_brokerMap) {
      _brokerMap.put(name, new WeakReference<Broker>(broker));
    }

    if (log.isLoggable(Level.FINEST))
      log.finest(this + " add " + broker + " as '" + name + "'");
  }

  public Broker removeBroker(String name)
  {
    WeakReference<Broker> brokerRef = null;
    
    synchronized (_brokerMap) {
      brokerRef = _brokerMap.remove(name);
    }

    if (brokerRef != null) {
      if (log.isLoggable(Level.FINER))
        log.finer(this + " remove " + name);
      
      return brokerRef.get();
    }
    else
      return null;
  }

  public Broker findBroker(String name)
  {
    if (name == null)
      return null;
    
    int p = name.indexOf('@');
    int q = name.indexOf('/');

    if (p >= 0 && q >= 0)
      name = name.substring(p + 1, q);
    else if (p >= 0)
      name = name.substring(p + 1);
    else if (q >= 0)
      name = name.substring(0, q);
    
    WeakReference<Broker> brokerRef = null;
    
    synchronized (_brokerMap) {
      brokerRef = _brokerMap.get(name);
    }

    if (brokerRef != null)
      return brokerRef.get();

    Server server = Server.getCurrent();

    if (server == null || ! server.isActive())
      return null;
    
    Host host = server.getHost(name, 5222);

    if (host == null)
      return null;

    // jms/3f00 vs server/2e06
    if ("default".equals(host.getHostName())
        && ! "localhost".equals(name)) {
      return null;
    }

    Broker broker = host.getBamBroker();

    synchronized (_brokerMap) {
      _brokerMap.put(name, new WeakReference<Broker>(broker));
    }

    return broker;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
