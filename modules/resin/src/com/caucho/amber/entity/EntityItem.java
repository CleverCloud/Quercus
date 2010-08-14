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

package com.caucho.amber.entity;

import com.caucho.amber.manager.AmberConnection;

import java.util.Map;

/**
 * An entity item handles the living entities.
 */
abstract public class EntityItem {

  private int _numberOfLoadingColumns;

  abstract public Entity getEntity();

  public int getNumberOfLoadingColumns()
  {
    return _numberOfLoadingColumns;
  }

  public void setNumberOfLoadingColumns(int number)
  {
    _numberOfLoadingColumns = number;
  }

  public Entity loadEntity(int index)
  {
    return getEntity();
  }

  public Entity loadEntity(AmberConnection aConn,
                           int index)
  {
    return getEntity();
  }

  public Entity createEntity(AmberConnection aConn, Object key)
    throws java.sql.SQLException
  {
    throw new UnsupportedOperationException();
  }

  public AmberEntityHome getEntityHome()
  {
    return null;
  }

  abstract public void save(Entity item);

  abstract public void savePart(Entity item);

  abstract public void expire();

  public Entity load(AmberConnection aConn)
  {
    return aConn.getEntity(this);
  }

  abstract Class getInstanceClass();
}
