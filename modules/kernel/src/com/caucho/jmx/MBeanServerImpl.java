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

import com.caucho.util.L10N;

import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerDelegateMBean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main interface for retrieving and managing JMX objects.
 */
class MBeanServerImpl extends AbstractMBeanServer {
  private static final L10N L = new L10N(MBeanServerImpl.class);  
  private static final Logger log
    = Logger.getLogger(MBeanServerImpl.class.getName());

  private MBeanContext _context;

  /**
   * Creats a new MBeanServer implementation.
   */
  public MBeanServerImpl(String domain, MBeanServerDelegate delegate)
  {
    super(domain);

    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    MBeanContext globalContext = null;
    MBeanContext context
      = new MBeanContext(this, loader, delegate, globalContext);

    // MBeanContext should set _context automatically
    if (context != _context)
      throw new IllegalStateException();

    try {
      IntrospectionMBean mbean;
      mbean = new IntrospectionMBean(delegate, MBeanServerDelegateMBean.class);

      registerMBean(mbean, SERVER_DELEGATE_NAME);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Returns the context.
   */
  protected MBeanContext createContext(ClassLoader loader)
  {
    return _context;
  }

  /**
   * Returns the context.
   */
  @Override
  protected MBeanContext getCurrentContext(ClassLoader loader)
  {
    return _context;
  }

  /**
   * Returns the context.
   */
  @Override
  protected void setCurrentContext(MBeanContext context, ClassLoader loader)
  {
    if (_context != null)
      throw new IllegalStateException(L.l("MBeanServerImpl cannot reassign the server"));
    
    _context = context;
  }

  /**
   * Returns the context.
   */
  protected MBeanContext getContext(ClassLoader loader)
  {
    return _context;
  }
}
