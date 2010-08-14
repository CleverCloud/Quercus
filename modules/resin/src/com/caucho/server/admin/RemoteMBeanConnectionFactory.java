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

package com.caucho.server.admin;

import java.lang.reflect.*;
import java.util.logging.*;
import javax.management.MBeanServerConnection;

import com.caucho.config.*;
import com.caucho.util.*;

/**
 * Proxy implementation for Hessian clients.  Applications will generally
 * use HessianProxyFactory to create proxy clients.
 */
public class RemoteMBeanConnectionFactory {
  private static final L10N L = new L10N(RemoteMBeanConnectionFactory.class);
  private static final Logger log
    = Logger.getLogger(RemoteMBeanConnectionFactory.class.getName());
  
  private static Constructor _constructor;
  
  public static MBeanServerConnection create(String serverId)
  {
    try {
      if (_constructor == null) {
        Class cl = Class.forName("com.caucho.server.admin.RemoteMBeanServerConnection");

        _constructor = cl.getConstructor(new Class[] { String.class });
      }

      return (MBeanServerConnection)  _constructor.newInstance(serverId);
    } catch (RuntimeException e) {
      throw e;
    } catch (InvocationTargetException e) {
      throw ConfigException.create(e);
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
      
      throw new ConfigException(L.l("remote mbeans require Resin Professional"));
    }
  }
}
