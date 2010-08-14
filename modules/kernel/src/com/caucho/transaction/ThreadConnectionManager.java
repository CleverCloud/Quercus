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

package com.caucho.transaction;

import com.caucho.util.L10N;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.logging.Logger;


/**
 * Manages the connections for a thread.  Needed to support the
 * "cached" connection, i.e. elisting only when work is actually done.
 */
public class ThreadConnectionManager {
  private static final L10N L = new L10N(ThreadConnectionManager.class);
  private static final Logger log
    = Logger.getLogger(ThreadConnectionManager.class.getName());

  private static final ThreadLocal<ThreadConnectionManager> _threadManager
    = new ThreadLocal<ThreadConnectionManager>();

  private ArrayList<SoftReference<ManagedPoolItem>> _activeConnections
    = new ArrayList<SoftReference<ManagedPoolItem>>();

  /**
   * Returns the manager for the current thread.
   */
  public static ThreadConnectionManager getThreadManager()
  {
    return _threadManager.get();
  }

  /**
   * Sets the manager for the current thread.
   */
  public static void setThreadManager(ThreadConnectionManager manager)
  {
    _threadManager.set(manager);
  }

  /**
   * Returns the manager for the current thread, creating if necessary.
   */
  public static ThreadConnectionManager createThreadManager()
  {
    ThreadConnectionManager cm = _threadManager.get();

    if (cm == null) {
      cm = new ThreadConnectionManager();
      _threadManager.set(cm);
    }
      
    return cm;
  }

  /**
   * Adds a pooled connection to the current thread.
   */
  public static void addConnection(ManagedPoolItem conn)
  {
    createThreadManager().add(conn);
  }

  /**
   * Removes a pooled connection from the current thread.
   */
  public static void removeConnection(ManagedPoolItem conn)
  {
    createThreadManager().remove(conn);
  }

  /**
   * Adds the connection.
   */
  private void add(ManagedPoolItem conn)
  {
    _activeConnections.add(new SoftReference<ManagedPoolItem>(conn));
  }

  /**
   * Removes the connection.
   */
  private void remove(ManagedPoolItem conn)
  {
    for (int i = _activeConnections.size() - 1; i >= 0; i--) {
      ManagedPoolItem aConn = _activeConnections.get(i).get();

      if (aConn == null || aConn == conn)
        _activeConnections.remove(i);
    }
  }
}
