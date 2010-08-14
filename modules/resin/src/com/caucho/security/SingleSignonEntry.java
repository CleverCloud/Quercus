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

package com.caucho.security;

import java.lang.ref.SoftReference;
import java.security.Principal;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpSession;

import com.caucho.server.session.SessionImpl;

public class SingleSignonEntry {
  private static final Logger log
    = Logger.getLogger(SingleSignonEntry.class.getName());
  
  private Principal _principal;
  private ArrayList<SoftReference<SessionImpl>> _sessions;

  SingleSignonEntry(Principal principal)
  {
    _principal = principal;
  }

  /**
   * Returns the single signon entry
   */
  public Principal getPrincipal()
  {
    return _principal;
  }

  protected void addSession(SessionImpl session)
  {
    if (_sessions == null)
      _sessions = new ArrayList<SoftReference<SessionImpl>>();
      
    _sessions.add(new SoftReference<SessionImpl>(session));
  }

  /**
   * Logout only the given session, returning true if it's the
   * last session to logout.
   */
  protected boolean logoutSession(HttpSession timeoutSession)
  {
    ArrayList<SoftReference<SessionImpl>> sessions = _sessions;

    if (sessions == null)
      return true;

    boolean isEmpty = true;
    for (int i = sessions.size() - 1; i >= 0; i--) {
      SoftReference<SessionImpl> ref = sessions.get(i);
      SessionImpl session = ref.get();

      try {
        if (session == timeoutSession) {
          sessions.remove(i);
          // session.logout();
          // XXX: invalidate?
        }
        else if (session == null)
          sessions.remove(i);
        else
          isEmpty = false;
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    return isEmpty;
  }

  /**
   * Logout all sessions
   */
  protected void logout()
  {
    ArrayList<SoftReference<SessionImpl>> sessions = _sessions;
    _sessions = null;
      
    for (int i = 0; sessions != null && i < sessions.size(); i++) {
      SoftReference<SessionImpl> ref = sessions.get(i);
      SessionImpl session = ref.get();

      try {
        if (session != null) {
          // session.logout();
          session.invalidateLogout();  // #599,  server/12i3
        }
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }
}
