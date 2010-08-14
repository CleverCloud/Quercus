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

import com.caucho.network.listen.AbstractSelectManager;

public class ProtocolDispatchServer extends DispatchServer {
  private boolean _isIgnoreClientDisconnect = true;
  private AbstractSelectManager _selectManager;

  /**
   * Sets the ignore-client-disconnect
   */
  public void setIgnoreClientDisconnect(boolean ignore)
  {
    _isIgnoreClientDisconnect = ignore;
  }

  /**
   * Gets the ignore-client-disconnect
   */
  @Override
  public boolean isIgnoreClientDisconnect()
  {
    return _isIgnoreClientDisconnect;
  }

  /**
   * Returns true if the select manager is enabled.
   */
  public boolean isSelectManagerEnabled()
  {
    return false;
  }

  /**
   * Gets the select manager.
   */
  public AbstractSelectManager getSelectManager()
  {
    return null;
  }

  /**
   * Returns the configured keepalive max.
   */
  public int getKeepaliveMax()
  {
    return Integer.MAX_VALUE / 2;
  }

  /**
   * Returns the number of available keepalive connections.
   */
  public int getFreeSelectKeepalive()
  {
    return Integer.MAX_VALUE / 2;
  }

  /**
   * Returns true if the server is active.
   */
  public boolean isActive()
  {
    return true;
  }

  /**
   * Stops the server.
   */
  public void stop()
  {
    AbstractSelectManager manager = _selectManager;
    _selectManager = null;

    if (manager != null)
      manager.stop();
  }
}
