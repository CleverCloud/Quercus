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

package com.caucho.server.dispatch;

import com.caucho.server.webapp.WebApp;
import com.caucho.server.session.SessionManager;
import com.caucho.server.http.HttpServletRequestImpl;
import com.caucho.util.L10N;
import com.caucho.util.Alarm;
import com.caucho.vfs.Dependency;

import java.util.logging.Logger;

/**
 * A repository for request information gleaned from the uri.
 */
public class VersionInvocation extends Invocation
{
  static final L10N L = new L10N(Invocation.class);
  private static final Logger log
    = Logger.getLogger(Invocation.class.getName());

  private final Invocation _invocation;
  private final WebApp _webApp;

  private final Invocation _oldInvocation;
  private final WebApp _oldWebApp;

  private final long _expireTime;

  public VersionInvocation(Invocation invocation, WebApp webApp,
                           Invocation oldInvocation, WebApp oldWebApp,
                           long expireTime)
  {
    _invocation = invocation;
    _webApp = webApp;

    setWebApp(webApp);
    
    _oldInvocation = oldInvocation;
    _oldWebApp = oldWebApp;

    _expireTime = expireTime;
  }

  /**
   * Returns true if the invocation has been modified.  Generally only
   * true if the webApp has been modified.
   */
  public boolean isModified()
  {
    long now = Alarm.getCurrentTime();

    if (_expireTime < now)
      return true;
    else
      return _invocation.isModified() || _oldInvocation.isModified();
  }

  /**
   * Log the reason for modification.
   */
  public boolean logModified(Logger log)
  {
    long now = Alarm.getCurrentTime();
    
    if (_expireTime < now) {
      log.info(L.l("{0}: versioning rollover complete.", _webApp));

      return true;
    }
    else
      return _invocation.logModified(log) || _oldInvocation.logModified(log);
  }

  /**
   * Returns the versioned invocation based on this request.
   *
   * @param request the servlet request
   */
  public Invocation getRequestInvocation(HttpServletRequestImpl request)
  {
    if (_expireTime < Alarm.getCurrentTime())
      return _invocation;

    request.setInvocation(this);
    String sessionId = request.getRequestedSessionId();

    if (sessionId == null)
      sessionId = _invocation.getSessionId();

    if (sessionId == null)
      return _invocation;

    SessionManager oldSessionManager = _oldWebApp.getSessionManager();

    if (oldSessionManager != null
        && oldSessionManager.containsSession(sessionId)) {
      return _oldInvocation;
    }
    else
      return _invocation;
  }

  /**
   * Returns the invocation's hash code.
   */
  public int hashCode()
  {
    return _invocation.hashCode();
  }

  /**
   * Checks for equality
   */
  public boolean equals(Object o)
  {
    return _invocation.equals(o);
  }
}
