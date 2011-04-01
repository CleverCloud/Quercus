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

package com.caucho.jmx;

import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resin implementation for an MBeanServer factory.
 */
public class EnvironmentMBeanServerBuilder { // extends MBeanServerBuilder {
  private static final Logger log
    = Logger.getLogger(EnvironmentMBeanServerBuilder.class.getName());

  private static MBeanServer _globalServer;

  private MBeanServer _mbeanServer;
  private boolean _isInit;
  
  public EnvironmentMBeanServerBuilder()
  {
  }
  
  /**
   * Creates the delegate.
   */
  public MBeanServerDelegate newMBeanServerDelegate()
  {
    return new MBeanServerDelegateImpl("Resin-JMX");
  }
  
  /**
   * Creates the mbean server
   */
  public MBeanServer newMBeanServer(String defaultDomain,
                                    MBeanServer outer,
                                    MBeanServerDelegate delegate)
  {
    if (! _isInit) {
      _isInit = true;

      try {
        Class cl = Class.forName("java.lang.management.ManagementFactory");

        Method method = cl.getMethod("getPlatformMBeanServer", new Class[0]);

        _mbeanServer = (MBeanServer) method.invoke(null, new Object[0]);

        return _mbeanServer;
      } catch (ClassNotFoundException e) {
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    if (_mbeanServer == null) {
      if (defaultDomain == null)
        defaultDomain = "resin";
    
      //_mbeanServer = new EnvironmentMBeanServer(defaultDomain);
    }

    return _mbeanServer;
  }
  
  /**
   * Creates the mbean server
   */
  public static MBeanServer getGlobal(String defaultDomain)
  {
    if (_globalServer == null) {
      if (defaultDomain == null)
        defaultDomain = "resin";

      MBeanServerDelegateImpl delegate;
      delegate = new MBeanServerDelegateImpl("Resin-JMX");
      _globalServer = new EnvironmentMBeanServer(defaultDomain, delegate);
    }

    return _globalServer;
  }
}
