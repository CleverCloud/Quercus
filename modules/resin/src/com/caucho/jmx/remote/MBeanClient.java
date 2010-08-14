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

package com.caucho.jmx.remote;

import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.jmx.JMXSerializerFactory;

import javax.management.MBeanInfo;
import javax.management.ObjectName;

/**
 * Client for mbeans.
 */
public class MBeanClient {
  private String _url;
  private RemoteJMX _jmxProxy;
  
  /**
   * Creates the MBeanClient.
   */
  public MBeanClient()
  {
  }
  
  /**
   * Creates the MBeanClient.
   */
  public MBeanClient(String url)
  {
    _url = url;
  }

  /**
   * Sets the proxy
   */
  public void setProxy(RemoteJMX proxy)
  {
    _jmxProxy = proxy;
  }

  /**
   * Returns the mbean info
   */
  public MBeanInfo getMBeanInfo(ObjectName objectName)
    throws Exception
  {
    return getProxy().getMBeanInfo(objectName.getCanonicalName());
  }

  /**
   * Gets an attribute.
   */
  public Object getAttribute(ObjectName objectName, String attrName)
    throws Exception
  {
    return getProxy().getAttribute(objectName.getCanonicalName(), attrName);
  }

  private RemoteJMX getProxy()
  {
    if (_jmxProxy == null) {
      try {
        HessianProxyFactory proxy = new HessianProxyFactory();
        proxy.getSerializerFactory().addFactory(new JMXSerializerFactory());
        _jmxProxy = (RemoteJMX) proxy.create(_url);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    return _jmxProxy;
  }
}
