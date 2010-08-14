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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.entity;

import com.caucho.amber.manager.AmberConnection;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages the set of persistent beans.
 */
public class EntityFactory {
  private static final L10N L = new L10N(EntityFactory.class);
  private static final Logger log = Log.open(EntityFactory.class);

  /**
   * Gets the appropriate entity given the key.
   */
  public Object getEntity(Object key)
  {
    return null;
  }

  /**
   * Gets the appropriate entity given the EntityItem.
   */
  public Object getEntity(AmberConnection aConn,
                          EntityItem item)
  {
    return aConn.getEntity(item);
  }

  /**
   * Deletes the proxy.
   */
  public void delete(AmberConnection aConn, Object proxy)
    throws SQLException
  {
    aConn.delete((Entity) proxy);
  }
}
