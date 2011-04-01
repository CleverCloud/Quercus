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

package com.caucho.jmx;

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import java.io.InvalidObjectException;
import java.io.NotActiveException;
import java.io.ObjectStreamException;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serialization handle for mbeans.
 */
public class MBeanHandle implements java.io.Serializable {
  private static final Logger log
    = Logger.getLogger(MBeanHandle.class.getName());
  
  private static SoftReference<MBeanServer> _globalMBeanServer;

  private final ObjectName _name;

  public MBeanHandle(ObjectName name)
  {
    _name = name;
  }

  /**
   * Convert the handle to a proxy for deserialization.
   */
  private Object readResolve()
    throws ObjectStreamException
  {
    try {
      MBeanServer server = getMBeanServer();

      ObjectInstance instance = server.getObjectInstance(_name);

      String className = instance.getClassName();

      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      Class cl = Class.forName(className, false, loader);

      return MBeanServerInvocationHandler.newProxyInstance(server,
                                                           _name,
                                                           cl,
                                                           false);
    } catch (ObjectStreamException e) {
      e.printStackTrace();
      
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      
      log.log(Level.FINE, e.toString(), e);
      
      throw new InvalidObjectException(e.toString());
    }
  }

  private static MBeanServer getMBeanServer()
    throws ObjectStreamException
  {
    MBeanServer server = null;

    if (_globalMBeanServer != null)
      server = _globalMBeanServer.get();

    if (server != null)
      return server;

    try {
      InitialContext ic = new InitialContext();
      server = (MBeanServer) ic.lookup("java:comp/env/jmx/GlobalMBeanServer");

      if (server != null) {
        _globalMBeanServer = new SoftReference<MBeanServer>(server);

        return server;
      }
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    try {
      Class cl = Class.forName("java.lang.Management.ManagementFactory");

      Method method = cl.getMethod("getPlatformMBeanServer", new Class[0]);

      server = (MBeanServer) method.invoke(null, new Object[0]);

      if (server != null) {
        _globalMBeanServer = new SoftReference<MBeanServer>(server);

        return server;
      }
    } catch (Throwable e) {
    }

    log.warning("Can't load global mbean server for proxy deserialization.");

    throw new NotActiveException("Can't load global mbean server for proxy deserialization.");
  }
}

