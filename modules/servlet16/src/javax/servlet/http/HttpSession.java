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

package javax.servlet.http;

import javax.servlet.ServletContext;
import java.util.Enumeration;

/**
 * Sessions are a convenient way to connect users to web pages.  Because
 * HTTP requests are intrinsically stateless, cookies and sessions are
 * needed to implement more sophisticated interfaces like user preferences.
 *
 * <p>Because a web site might easily have thousands of simultaneous
 * sessions, session attributes generally store small chunks of data
 * rather than large objects.
 *
 * <p>The servlet engine controls the number of active sessions through
 * two methods: a time limit on inactive sessions, and
 * a cap on the number of active sessions.  The cap on the number of
 * sessions is controlled by an LRU mechanism, so active sessions will not
 * be culled.
 *
 * Session configuration is per-application.  It looks like:
 * <code><pre>
 * &lt;session-config session-max='4096'
 *                 session-timeout='30'/>
 * </pre></code>
 *
 * <h4>Load balancing</h4>
 *
 * When using load balancing with Apache, sessions will always go to the
 * same JVM.  The session id encodes the JVM which first created the session.
 */
public interface HttpSession {
  /**
   * Returns the id for the session.  The session variable name is
   * 'jsessionid'.  <code>getId</code> returns the randomly generated
   * value.
   */
  public String getId();
  /**
   * Returns true if the session is new.  If the servlet engine found the
   * session from the client's request, <code>isNew</code> is false.
   */
  public boolean isNew();
  /**
   * Returns the time when the session was created.
   */
  public long getCreationTime();
  /**
   * Returns the time of last request associated with a session
   * before the current request
   */
  public long getLastAccessedTime();
  /**
   * Sets the maximum inactive interval.  Sessions have a limited lifetime.
   * When the lifetime ends, the session will be invalidated.
   *
   * @param interval the new inactive interval in seconds.
   */
  public void setMaxInactiveInterval(int interval);
  /*
   * @return the new inactive interval in seconds.
   */
  public int getMaxInactiveInterval();
  /**
   * Returns a session value.
   *
   * @param name of the attribute.
   * @return stored value
   */
  public Object getAttribute(String name);
  /**
   * Returns an enumeration of all the attribute names.
   */
  public Enumeration<String> getAttributeNames();
  /**
   * Sets an attribute value.  Because servlets are multithreaded,
   * setting HttpSession attributes will generally need synchronization.
   * Remember, users may open multiple browsers to the same page.
   *
   * <p>A typical initialization of an session attribute might look like:
   * <code><pre>
   * HttpSession session = request.getSession();
   * String user;
   * synchronized (session) {
   *   user = (String) session.getAttribute("user");
   *   if (user == null) {
   *     user = lookupUser(request);
   *     sesion.setAttribute("user", user);
   *   }
   * }
   * </pre></code>
   *
   * @param name of the attribute.
   * @param value value to store
   */
  public void setAttribute(String name, Object value);
  /**
   * Removes an attribute.  If the attribute value implements
   * HttpSessionBindingListener, it will receive a notice when
   * it is removed. Because servlets are multithreaded,
   * removing ServletContext attributes will generally need synchronization.
   *
   * @param name of the attribute.
   */
  public void removeAttribute(String name);
  /**
   * Invalidates the current session.  Calling most of the session methods
   * after invalidation will throw an IllegalStateException.
   *
   * <p>All attribute values which implement HttpSessionBindingListener,
   * will receive a notice when they're removed at invalidation.
   */
  public void invalidate();
  /**
   * @deprecated
   */
  public HttpSessionContext getSessionContext();
  /**
   * Returns the owning servlet context.
   */
  public ServletContext getServletContext();
  /**
   * @deprecated
   */
  public Object getValue(String name);
  /**
   * @deprecated
   */
  public String []getValueNames();
  /**
   * @deprecated
   */
  public void putValue(String name, Object value);
  /**
   * @deprecated
   */
  public void removeValue(String name);

  /**
   * logs the user out and invalidates the sessions.
   */
  // public void logout();
}
