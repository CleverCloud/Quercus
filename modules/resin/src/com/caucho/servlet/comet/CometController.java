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

package com.caucho.servlet.comet;

/**
 * Public API to control a comet connection.
 * 
 * @deprecated
 */
public interface CometController {
  /**
   * Sets the max idle time.
   */
  public void setMaxIdleTime(long idleTime);
  
  /**
   * Gets the max idle time.
   */
  public long getMaxIdleTime();
  
  /**
   * Gets a request attribute.
   */
  public Object getAttribute(String name);
  
  /**
   * Sets a request attribute.
   */
  public void setAttribute(String name, Object value);
  
  /**
   * Remove a request attribute.
   */
  public void removeAttribute(String name);
  
  /**
   * Wakes the connection.
   */
  public boolean wake();

  /**
   * Returns true if the connection is closed.
   */
  public boolean isClosed();
  
  /**
   * Closes the connection.
   */
  public void close();
}
