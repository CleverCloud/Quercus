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
 * Managed object which is a notification emitter.
 */
abstract public class AbstractEmitterObject
  extends AbstractManagedObject
  implements NotificationEmitter
{
  private static final Logger log
    = Logger.getLogger(AbstractEmitterObject.class.getName());

  private ArrayList<Listener> _listeners;
  private Listener []_listenerArray;

  protected AbstractEmitterObject()
  {
  }

  protected AbstractEmitterObject(ClassLoader loader)
  {
    super(loader);
  }

  public MBeanNotificationInfo []getNotificationInfo()
  {
    return new MBeanNotificationInfo[0];
  }

  public void addNotificationListener(NotificationListener listener,
                                      NotificationFilter filter,
                                      Object handback)
    throws IllegalArgumentException
  {
    if (listener == null)
      throw new IllegalArgumentException();

    if (_listeners == null)
      _listeners = new ArrayList<Listener>();

    synchronized (_listeners) {
      _listeners.add(new Listener(listener, filter, handback));
      
      _listenerArray = null;
    }
  }

  public void removeNotificationListener(NotificationListener listener)
  {
    if (_listeners != null) {
      synchronized (_listeners) {
        for (int i = _listeners.size() - 1; i >= 0; i--) {
          Listener item = _listeners.get(i);

          if (item.getListener() == listener) {
            _listeners.remove(i);
          }
        }

        _listenerArray = null;
      }
    }
  }

  public void removeNotificationListener(NotificationListener listener,
                                         NotificationFilter filter,
                                         Object handback)
  {
    if (_listeners != null) {
      synchronized (_listeners) {
        for (int i = _listeners.size() - 1; i >= 0; i--) {
          Listener item = _listeners.get(i);

          if (item.getListener() == listener
              && item.getFilter() == filter
              && item.getHandback() == handback) {
            _listeners.remove(i);
          }
        }
      
        _listenerArray = null;
      }
    }
  }

  protected void handleNotification(Notification notification)
  {
    Listener []listeners = null;
    
    if (_listeners != null) {
      synchronized (_listeners) {
        if (_listenerArray == null && _listeners.size() > 0) {
          _listenerArray = new Listener[_listeners.size()];
          _listeners.toArray(_listenerArray);
        }
      }
    }

    if (listeners != null) {
      for (int i = 0; i < listeners.length; i++) {
        listeners[i].handleNotification(notification);
      }
    }
  }

  static class Listener {
    private final NotificationListener _listener;
    private final NotificationFilter _filter;
    private final Object _handback;

    Listener(NotificationListener listener,
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

    void handleNotification(Notification notification)
    {
      if (_filter == null || _filter.isNotificationEnabled(notification))
        _listener.handleNotification(notification, _handback);
    }
  }
}
