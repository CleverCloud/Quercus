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

package com.caucho.server.session;

import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import java.util.logging.Logger;

/**
 * A factory for creating sessions.
 */
public class SessionFactory  {
  private static final Logger log
    = Logger.getLogger(SessionFactory.class.getName());
  static final L10N L = new L10N(SessionFactory.class);

  private SessionManager _manager;

  /**
   * Sets the session manager.
   */
  public void setSessionManager(SessionManager manager)
  {
    _manager = manager;
  }

  /**
   * Gets the session manager.
   */
  public SessionManager getSessionManager()
  {
    return _manager;
  }

  /**
   * Initialize after all the attributes are set.
   */
  @PostConstruct
  public void init()
    throws ServletException
  {
  }

  /**
   * Creates a new session.
   *
   * @param id the session's id
   * @param creationTime the current time
   *
   * @return the new session
   */
  public SessionImpl create(String id, long creationTime)
    throws ServletException
  {
    return new SessionImpl(_manager, id, creationTime);
  }
}



