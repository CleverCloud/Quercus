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

package com.caucho.management.server;

import com.caucho.jmx.Description;
import com.caucho.jmx.Jmx;

import javax.management.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parent mbean of all Resin's managed objects.
 */
abstract public class AbstractNotificationObject extends AbstractManagedObject
  implements NotificationEmitter
{
  private static final Logger log
    = Logger.getLogger(AbstractNotificationObject.class.getName());

  private ArrayList<Notif> _notifList
    = new ArrayList<Notif>();

  protected AbstractNotificationObject()
  {
  }

  protected AbstractNotificationObject(ClassLoader loader)
  {
    super(loader);
  }

  /**
   * Adds a new listener
   */
  public void addNotificationListener(NotificationListener listener,
                                      NotificationFilter filter,
                                      Object handback)
  {
    _notifList.add(new Notif(listener, filter, handback));
  }

  public void removeNotificationListener(NotificationListener listener)
    throws ListenerNotFoundException
  {
    boolean isMatch = false;

    for (int i = _notifList.size() - 1; i >= 0; i--) {
      Notif notif = _notifList.get(i);

      if (listener == notif.getListener()) {
        isMatch = true;
        _notifList.remove(i);
      }
    }

    if (! isMatch)
      throw new ListenerNotFoundException(String.valueOf(listener));
  }

  public void removeNotificationListener(NotificationListener listener,
                                         NotificationFilter filter,
                                         Object handback)
    throws ListenerNotFoundException
  {
    boolean isMatch = false;

    for (int i = _notifList.size() - 1; i >= 0; i--) {
      Notif notif = _notifList.get(i);

      if (listener == notif.getListener()
          && filter == notif.getFilter()
          && handback == notif.getHandback()) {
        isMatch = true;
        _notifList.remove(i);
      }
    }

    if (! isMatch)
      throw new ListenerNotFoundException(String.valueOf(listener));
  }

  protected void handleNotification(NotificationListener listener,
                                    Notification notif,
                                    Object handback)
  {
    listener.handleNotification(notif, handback);
  }

  public void sendNotification(Notification notification)
  {
    for (int i = 0; i < _notifList.size(); i++) {
      Notif notif = _notifList.get(i);

      if (notif.getFilter() == null
          || notif.getFilter().isNotificationEnabled(notification)) {
        handleNotification(notif.getListener(),
                           notification,
                           notif.getHandback());
      }
    }
  }

  static class Notif {
    private final NotificationListener _listener;
    private final NotificationFilter _filter;
    private final Object _handback;

    Notif(NotificationListener listener,
          NotificationFilter filter,
          Object handback)
    {
      _listener = listener;
      _filter = filter;
      _handback = handback;
    }

    NotificationListener getListener()
    {
      return _listener;
    }

    NotificationFilter getFilter()
    {
      return _filter;
    }

    Object getHandback()
    {
      return _handback;
    }
  }
}
