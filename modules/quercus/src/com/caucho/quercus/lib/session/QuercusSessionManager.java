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
 * @author Emil Ong
 */

package com.caucho.quercus.lib.session;

import com.caucho.config.ConfigException;
import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.env.SessionArrayValue;
import com.caucho.util.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stripped down version of com.caucho.server.session.SessionManager,
 * customized to PHP instead of J2EE sessions.
 */
public class QuercusSessionManager
  implements AlarmListener
{
  private static final L10N L = new L10N(QuercusSessionManager.class);
  private static final Logger log
    = Logger.getLogger(QuercusSessionManager.class.getName());

  private static int FALSE = 0;
  private static int COOKIE = 1;
  private static int TRUE = 2;

  private static int UNSET = 0;
  private static int SET_TRUE = 1;
  private static int SET_FALSE = 2;
  
  // active sessions
  protected LruCache<String,SessionArrayValue> _sessions;
  // total sessions
  private int _totalSessions;

  // iterator to purge sessions (to reduce gc)
  protected Iterator<SessionArrayValue> _sessionIter;
  // array list for session timeout
  protected ArrayList<SessionArrayValue> _sessionList 
    = new ArrayList<SessionArrayValue>();

  // maximum number of sessions
  protected int _sessionMax = 4096;
  private long _sessionTimeout = 30 * 60 * 1000L;

  private int _reuseSessionId = COOKIE;
  private int _cookieLength = 18;

  private int _alwaysLoadSession;
  private boolean _alwaysSaveSession;
  private boolean _saveOnlyOnShutdown;

  private boolean _isModuloSessionId = false;
  private boolean _isAppendServerIndex = false;
  private boolean _isTwoDigitSessionIndex = false;
  
  protected boolean _isClosed;

  //private Alarm _alarm = new Alarm(this);

  private Map _persistentStore;

  // statistics
  protected Object _statisticsLock = new Object();
  protected long _sessionCreateCount;
  protected long _sessionTimeoutCount;

  /**
   * Creates and initializes a new session manager.
   */
  public QuercusSessionManager(QuercusContext quercus)
  {
    _sessions = new LruCache<String,SessionArrayValue>(_sessionMax);
    _sessionIter = _sessions.values();

    _persistentStore = quercus.getSessionCache();
  }

  /**
   * True if sessions should always be saved.
   */
  boolean getAlwaysSaveSession()
  {
    return _alwaysSaveSession;
  }

  /**
   * True if sessions should always be saved.
   */
  public void setAlwaysSaveSession(boolean save)
  {
    _alwaysSaveSession = save;
  }

  /**
   * True if sessions should always be loadd.
   */
  boolean getAlwaysLoadSession()
  {
    return _alwaysLoadSession == SET_TRUE;
  }

  /**
   * True if sessions should always be loadd.
   */
  public void setAlwaysLoadSession(boolean load)
  {
    _alwaysLoadSession = load ? SET_TRUE : SET_FALSE;
  }

  /**
   * True if sessions should only be saved on shutdown.
   */
  public boolean getSaveOnlyOnShutdown()
  {
    return _saveOnlyOnShutdown;
  }

  /**
   * True if sessions should only be saved on shutdown.
   */
  public void setSaveOnlyOnShutdown(boolean save)
  {
    _saveOnlyOnShutdown = save;
  }

  /**
   * True if sessions should only be saved on shutdown.
   */
  public void setSaveOnShutdown(boolean save)
  {
    log.warning
        ("<save-on-shutdown> is deprecated.  "
            + "Use <save-only-on-shutdown> instead");

    setSaveOnlyOnShutdown(save);
  }

  /**
   * Sets the cookie length
   */
  public void setCookieLength(int cookieLength)
  {
    if (cookieLength < 7)
      cookieLength = 7;

    _cookieLength = cookieLength;
  }

  protected void setSessionTimeout(long sessionTimeout)
  {
    _sessionTimeout = sessionTimeout;
  }

  /**
   * Returns the current number of active sessions.
   */
  public int getActiveSessionCount()
  {
    if (_sessions == null)
      return -1;
    else
      return _sessions.size();
  }

  /**
   * Returns the created sessions.
   */
  public long getSessionCreateCount()
  {
    return _sessionCreateCount;
  }

  /**
   * Returns the timeout sessions.
   */
  public long getSessionTimeoutCount()
  {
    return _sessionTimeoutCount;
  }

  /**
   * True if the server should reuse the current session id if the
   * session doesn't exist.
   */
  public int getReuseSessionId()
  {
    return _reuseSessionId;
  }

  /**
   * True if the server should reuse the current session id if the
   * session doesn't exist.
   */
  public void setReuseSessionId(String reuse)
    throws ConfigException
  {
    if (reuse == null)
      _reuseSessionId = COOKIE;
    else if (reuse.equalsIgnoreCase("true")
             || reuse.equalsIgnoreCase("yes")
             || reuse.equalsIgnoreCase("cookie"))
      _reuseSessionId = COOKIE;
    else if (reuse.equalsIgnoreCase("false") || reuse.equalsIgnoreCase("no"))
      _reuseSessionId = FALSE;
    else if (reuse.equalsIgnoreCase("all"))
      _reuseSessionId = TRUE;
    else
      throw new ConfigException(
          L.l("'{0}' is an invalid value for reuse-session-id.  "
              + "'true' or 'false' are the allowed values.",
                                    reuse));
  }

  /**
   * Returns true if the sessions are closed.
   */
  public boolean isClosed()
  {
    return _isClosed;
  }

  /**
   * Returns the maximum number of sessions.
   */
  public int getSessionMax()
  {
    return _sessionMax;
  }

  /**
   * Returns the maximum number of sessions.
   */
  public void setSessionMax(int max)
  {
    _sessionMax = max;
  }
  
  /**
   * Removes a session from the cache and deletes it from the backing store,
   * if applicable.
   */
  public void removeSession(String sessionId)
  {
    _sessions.remove(sessionId);
    
    if (_persistentStore != null)
      _persistentStore.remove(sessionId);

    remove(sessionId);
  }

  protected void remove(String sessionId)
  {
  }

  /**
   * Loads the session.
   *
   * @param in the input stream containing the serialized session
   * @param obj the session object to be deserialized
   */
  public void load(ObjectInputStream in, Object obj)
    throws IOException
  {
    SessionArrayValue session = (SessionArrayValue) obj;

    session.load(null, in);
  }

  /**
   * Checks if the session is empty.
   */
  public boolean isEmpty(Object obj)
  {
    SessionArrayValue session = (SessionArrayValue) obj;

    return session.isEmpty();
  }

  /**
   * Sets module session id generation.
   */
  public void setCookieAppendServerIndex(boolean isAppend)
  {
    _isAppendServerIndex = isAppend;
  }

  /**
   * Create a new session.
   *
   * @param oldId the id passed to the request.  Reuse if possible.
   * @param now the current date
   *
   */
  public SessionArrayValue createSession(Env env, String oldId, long now)
  {
    String id = oldId;

    if (id == null || id.length() < 4)
      id = createSessionId(env);

    SessionArrayValue session = create(env, id, now);

    if (session == null)
      return null;
    
    synchronized (_statisticsLock) {
      _sessionCreateCount++;
    }
    
    return session;
  }

  /**
   * Creates a pseudo-random session id.  If there's an old id and the
   * group matches, then use it because different applications on the
   * same matchine should use the same cookie.
   */
  public String createSessionId(Env env)
  {
    String id;

    do {
      CharBuffer sb = new CharBuffer();

      Base64.encode(sb, RandomUtil.getRandomLong());
      Base64.encode(sb, env.getCurrentTime());

      id = sb.toString();
    } while (getSession(env, id, 0) != null);

    if (id == null || id.equals(""))
      throw new RuntimeException();

    return id;
  }

  /**
   * Returns a session from the session store, returning null if there's
   * no cached session.
   *
   * @param key the session id
   * @param now the time in milliseconds.  now == 0 implies
   * that we're just checking for the existence of such a session in
   * the cache and do not intend actually to load it if it is not.
   *
   * @return the cached session.
   * 
   */
  public SessionArrayValue getSession(Env env, String key, long now)
  {
    SessionArrayValue session;
    boolean isNew = false;
    boolean killSession = false;

    if (_sessions == null)
      return null;

    // Check the cache first
    session = _sessions.get(key);

    if (session != null && ! session.getId().equals(key))
      throw new IllegalStateException(key + " != " + session.getId());

    if (session != null) {
      if (session.inUse()) {
        return (SessionArrayValue)session.copy(env);
      }
    }

    if (session == null)
      return null;

    if (isNew) {
      isNew = ! load(env, session, now);
    }
    else if (! getSaveOnlyOnShutdown() && ! session.load()) {
      // if the load failed, then the session died out from underneath
      session.reset(now);
      isNew = true;
    }

    if (! isNew)
      session.setAccess(now);
    
    return (SessionArrayValue)session.copy(env);
  }

  public void saveSession(Env env, SessionArrayValue session)
  {
    SessionArrayValue copy = (SessionArrayValue) session.copy(env);

    _sessions.put(session.getId(), copy);
    
    session.finish();

    if (_persistentStore != null) {
      _persistentStore.put(session.getId(), copy.encode(env));
    }
  }

  /**
   * Creates a session.  It's already been established that the
   * key does not currently have a session.
   */
  protected SessionArrayValue create(Env env, String key, long now)
  {
    SessionArrayValue session
      = createSessionValue(key, now, _sessionTimeout);

    load(env, session, now);

    // If another thread has created and stored a new session,
    // putIfNew will return the old session
    session = _sessions.putIfNew(key, session);

    if (! key.equals(session.getId()))
      throw new IllegalStateException(key + " != " + session.getId());

    return (SessionArrayValue)session.copy(env);
  }

  /**
   * Creates a new SessionArrayValue instance.
   */
  protected SessionArrayValue createSessionValue(String key, long now,
                                                 long sessionTimeout)
  {
    return new SessionArrayValue(key, now, _sessionTimeout);
  }
  
  /**
   * Loads the session from the backing store.  
   *
   * @param session the session to load.
   * @param now current time in milliseconds.  now == 0 implies
   * that we're just checking for the existence of such a session in
   * the cache and do not intend actually to load it if it is not.
   *
   */
  protected boolean load(Env env, SessionArrayValue session, long now)
  {
    try {
      if (session.inUse()) {
        return true;
      }
      else if (now <= 0) {
        return false;
      }

      if (_persistentStore != null) {
        String encoded = (String) _persistentStore.get(session.getId());

        if (encoded != null) {
          session.decode(env, new StringBuilderValue(encoded));
        }
      }
      
      if (session.load()) {
        session.setAccess(now);
        return true;
      }
      else {
        session.reset(now);
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      session.reset(now);
    }

    return false;
  }

  /**
   * Timeout for reaping old sessions.
   */
  public void handleAlarm(Alarm alarm)
  {
    try {
      _sessionList.clear();

      int liveSessions = 0;

      if (_isClosed)
        return;

      long now = Alarm.getCurrentTime();

      synchronized (_sessions) {
        _sessionIter = _sessions.values(_sessionIter);

        while (_sessionIter.hasNext()) {
          SessionArrayValue session = _sessionIter.next();

          long maxIdleTime = session.getMaxInactiveInterval();

          if (session.inUse())
            liveSessions++;
          else if (session.getAccessTime() + maxIdleTime < now)
            _sessionList.add(session);
          else
            liveSessions++;
        }
      }

      synchronized (_statisticsLock) {
        _sessionTimeoutCount += _sessionList.size();
      }

      for (int i = 0; i < _sessionList.size(); i++) {
        SessionArrayValue session = _sessionList.get(i);

        try {
          long maxIdleTime = session.getMaxInactiveInterval();
          _sessions.remove(session.getId());

          session.invalidate();
        } catch (Throwable e) {
          log.log(Level.FINER, e.toString(), e);
        }
      }
    } finally {
      if (! _isClosed)
        alarm.queue(60000);
    }
  }

  /**
   * Cleans up the sessions when the Application shuts down gracefully.
   */
  public void close()
  {
    synchronized (this) {
      if (_isClosed)
        return;
      _isClosed = true;
    }

    if (_sessions == null)
      return;

    //_alarm.dequeue();

    _sessionList.clear();

    ArrayList<SessionArrayValue> list = new ArrayList<SessionArrayValue>();

    boolean isError = false;

    synchronized (_sessions) {
      _sessionIter = _sessions.values(_sessionIter);

      while (_sessionIter.hasNext()) {
        SessionArrayValue session = _sessionIter.next();

        if (session.isValid())
          list.add(session);
      }
    }

    for (int i = list.size() - 1; i >= 0; i--) {
      SessionArrayValue session = list.get(i);

      try {
        if (session.isValid()) {
          synchronized (session) {
            if (! session.isEmpty())
              session.storeOnShutdown();
          }
        }

        _sessions.remove(session.getId());
      } catch (Exception e) {
        if (! isError)
          log.log(Level.WARNING, "Can't store session: " + e, e);
        isError = true;
      }
    }
  }

  /**
   * Notification from the cluster.
   */
  public void notifyRemove(String id)
  {
    SessionArrayValue session = _sessions.remove(id);

    if (session != null)
      session.invalidate();
  }

  /**
   * Notification from the cluster.
   */
  public void notifyUpdate(String id)
  {
  }

  /**
   * Saves the session.
   */
  public void store(OutputStream out, Object obj)
    throws IOException
  {
    SessionArrayValue session = (SessionArrayValue) obj;

    session.store(Env.getInstance(), out);
  }
}
