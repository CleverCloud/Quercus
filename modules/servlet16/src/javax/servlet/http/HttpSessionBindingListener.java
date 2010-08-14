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

import java.util.EventListener;

/**
 * This listener lets session values know when they've been added to
 * or removed from a session.  This particularly useful when a session
 * value needs to free resources at the session end.
 */
public interface HttpSessionBindingListener extends EventListener {
  /**
   * Called when the object is added to a session.
   *
   * @param event session event object
   */
  public void valueBound(HttpSessionBindingEvent event);
  
  /**
   * Called when the object is removed from a session or the session
   * is invalidated.
   *
   * @param event session event object
   */
  public void valueUnbound(HttpSessionBindingEvent event);
}
