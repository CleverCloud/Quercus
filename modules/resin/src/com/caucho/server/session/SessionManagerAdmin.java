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


package com.caucho.server.session;

import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.PersistentStoreMXBean;
import com.caucho.management.server.SessionManagerMXBean;
import com.caucho.management.server.WebAppMXBean;

import java.util.logging.Logger;

/**
 * Implementation of the SessionManager's administration mbean.
 */
public class SessionManagerAdmin extends AbstractManagedObject
  implements SessionManagerMXBean
{
  private static final Logger log
    = Logger.getLogger(SessionManagerAdmin.class.getName());
  
  private final SessionManager _manager;

  public SessionManagerAdmin(SessionManager manager)
  {
    _manager = manager;

    registerSelf();
  }

  /**
   * The SessionManager doesn't have a name.
   */
  public String getName()
  {
    return null;
  }

  /**
   * Returns the owning web-app's
   */
  public WebAppMXBean getWebApp()
  {
    return _manager.getWebApp().getAdmin();
  }

  //
  // Configuration attributes
  //

  /**
   * True if the session should be serialized for storage, even if
   * no attributes in the session have been set.
   */
  public boolean isAlwaysSaveSession()
  {
    return _manager.getAlwaysSaveSession();
  }

  /**
   * If true, the server's cluster index is appended to the cookie value.
   */
  public boolean isCookieAppendServerIndex()
  {
    return _manager.isCookieAppendServerIndex();
  }

  /**
   * The host domain used for session cookies
   */
  public String getCookieDomain()
  {
    return _manager.getCookieDomain();
  }

  /**
   * True if the cookie should only be used for non-secure sessions.
   */
  public boolean isCookieHttpOnly()
  {
    return _manager.isCookieHttpOnly();
  }

  /**
   * The length of the generated cookie
   */
  public long getCookieLength()
  {
    return _manager.getCookieLength();
  }

  /**
   * The cookie max-age sent to the browser.
   */
  public long getCookieMaxAge()
  {
    return _manager.getCookieMaxAge();
  }

  /**
   * Returns the cookie name for sessions.
   */
  public String getCookieName()
  {
    return _manager.getCookieName();
  }

  /**
   * Returns the cookie port for sessions.
   */
  public String getCookiePort()
  {
    return _manager.getCookiePort();
  }

  /**
   * True if the cookie should only be used for secure sessions.
   */
  public boolean isCookieSecure()
  {
    return _manager.getCookieSecure();
  }

  /**
   * Returns the cookie version number.
   */
  public int getCookieVersion()
  {
    return _manager.getCookieVersion();
  }

  /**
   * Returns true if cookies are enabled.
   */
  public boolean isEnableCookies()
  {
    return _manager.enableSessionCookies();
  }

  /**
   * Returns true if url-rewriting is enabled.
   */
  public boolean isEnableURLRewriting()
  {
    return _manager.enableSessionUrls();
  }

  /**
   * Returns true if persistent sessions should ignore serialization errors
   */
  public boolean isIgnoreSerializationErrors()
  {
    return _manager.getIgnoreSerializationErrors();
  }

  /**
   * True if sessions should invalidate only after calling listeners.
   */
  public boolean isInvalidateAfterListener()
  {
    return _manager.isInvalidateAfterListener();
  }

  /**
   * True if sessions should reuse available session cookie values.
   */
  public boolean isReuseSessionId()
  {
    return _manager.getReuseSessionId() != 0;
  }
  
  /**
   * Returns the save mode.
   */
  public String getSaveMode()
  {
    return _manager.getSaveMode();
  }
  
  /**
   * Returns the maximum number of sessions.
   */
  public int getSessionMax()
  {
    return _manager.getSessionMax();
  }

  /**
   * Returns session timeout (in ms)
   */
  public long getSessionTimeout()
  {
    return _manager.getSessionTimeout();
  }

  /**
   * Returns the object name for the persistent store
   */
  public PersistentStoreMXBean getPersistentStore()
  {
    return null;
  }

  /**
   * Returns the active sessions.
   */
  public int getActiveSessionCount()
  {
    return _manager.getActiveSessionCount();
  }

  /**
   * Returns the active sessions.
   */
  public long getSessionActiveCount()
  {
    return _manager.getSessionActiveCount();
  }

  /**
   * Returns the session create count
   */
  public long getSessionCreateCountTotal()
  {
    return _manager.getSessionCreateCount();
  }

  /**
   * Returns the session invalidate count
   */
  public long getSessionInvalidateCountTotal()
  {
    return _manager.getSessionInvalidateCount();
  }

  /**
   * Returns the session timeout count
   */
  public long getSessionTimeoutCountTotal()
  {
    return _manager.getSessionTimeoutCount();
  }

  /**
   * Returns the session store type
   */
  public String getSessionStoreType()
  {
    /*
    Store store = _manager.getSessionStore();

    if (store == null)
      return null;

    String className = store.getStoreManager().getClass().getName();

    int p = className.lastIndexOf('.');

    return className.substring(p + 1);
    */
    return "unknown";
  }

  /**
   * Returns a debug string for the session
   */
  public String getSessionSerializationDebug(String id)
  {
    return _manager.getSessionSerializationDebug(id);
  }
  
  /**
   * Unregisters the object with JMX.
   */
  public void unregister()
  {
    unregisterSelf();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getObjectName() + "]";
  }
}
