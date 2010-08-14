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

import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import com.caucho.server.util.CauchoSystem;

/**
 * The main interface for retrieving and managing JMX objects.
 */
public class MBeanServerDelegateImpl extends MBeanServerDelegate {
  private static final ObjectName DELEGATE_NAME;
  
  private String _agentId;
  private long _seq;

  MBeanServerDelegateImpl(String agentId)
  {
    _agentId = agentId;
  }

  /**
   * Return the MBean server agent id.
   */
  public String getMBeanServerId()
  {
    return _agentId;
  }

  /**
   * Returns the implementation vendor.
   */
  public String getImplementationName()
  {
    return "Resin-JMX";
  }

  /**
   * Returns the implementation vendor.
   */
  public String getImplementationVendor()
  {
    return "Caucho Technology";
  }

  /**
   * Returns the implementation version.
   */
  @Override
  public String getImplementationVersion()
  {
    return "Resin-" + CauchoSystem.getVersion();
  }

  /**
   * Sends the register notification.
   */
  public void sendRegisterNotification(ObjectName name)
  {
    serverNotification(name, MBeanServerNotification.REGISTRATION_NOTIFICATION);
  }

  /**
   * Sends the register notification.
   */
  public void sendUnregisterNotification(ObjectName name)
  {
    serverNotification(name, MBeanServerNotification.UNREGISTRATION_NOTIFICATION);
  }

  public void addNotificationListener(NotificationListener listener,
                                      NotificationFilter filter,
                                      Object handback)
  {
    super.addNotificationListener(listener, filter, handback);
  }

  /**
   * Sends the notification
   */
  private void serverNotification(ObjectName name, String type)
  {
    MBeanServerNotification notif;

    notif = new MBeanServerNotification(type, DELEGATE_NAME, _seq++, name);

    sendNotification(notif);
  }

  public MBeanNotificationInfo []getNotificationInfo()
  {
    MBeanNotificationInfo []notifs = super.getNotificationInfo();

    return notifs;
  }

  static {
    ObjectName name = null;
    
    try {
      name = new ObjectName("JMImplementation:type=MBeanServerDelegate");
    } catch (Throwable e) {
      e.printStackTrace();
    }
    
    DELEGATE_NAME = name;
  }
}
