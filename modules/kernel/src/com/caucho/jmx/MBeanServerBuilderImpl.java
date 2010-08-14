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
import javax.management.MBeanServerBuilder;
import javax.management.MBeanServerDelegate;
import java.util.logging.Logger;

/**
 * Resin implementation for an MBeanServer factory.
 */
public class MBeanServerBuilderImpl extends MBeanServerBuilder {
  private static final Logger log
    = Logger.getLogger(MBeanServerBuilderImpl.class.getName());

  private static boolean _isInit;
  private static boolean _isJdkManagementInit;

  public MBeanServerBuilderImpl()
  {
  }
  
  /**
   * Creates the delegate.
   */
  public MBeanServerDelegate newMBeanServerDelegate()
  {
    return new MBeanServerDelegateImpl("Resin");
  }
  
  /**
   * Creates the mbean server
   */
  public MBeanServer newMBeanServer(String defaultDomain,
                                    MBeanServer outer,
                                    MBeanServerDelegate delegate)
  {
    // return EnvironmentMBeanServerBuilder.getGlobal(defaultDomain);
    EnvironmentMBeanServerBuilder.getGlobal(defaultDomain);

    if (! _isJdkManagementInit) {
      Exception e = new Exception();
      e.fillInStackTrace();
      StackTraceElement []stackTrace = e.getStackTrace();

      for (int i = 0; i < stackTrace.length; i++) {
        if (stackTrace[i].getClassName().equals("java.lang.management.ManagementFactory")) {
          _isJdkManagementInit = true;

          return Jmx.getGlobalMBeanServer();
        }
      }
    }

    if (! _isInit) {
      _isInit = true;
      
      Jmx.getContextMBeanServer();
    }
    
    if (defaultDomain == null)
      defaultDomain = "resin";

    /*
    if ("resin".equals(defaultDomain))
      return Jmx.getContextMBeanServer();
    else
      return new MBeanServerImpl(defaultDomain, delegate);
    */
    return new MBeanServerImpl(defaultDomain, delegate);
  }
}
