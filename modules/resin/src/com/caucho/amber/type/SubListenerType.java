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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.type;

import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * Represents a listener type with a parent listener:
 * either a default listener declared in orm.xml meta-data or
 * an entity listener specified in @EntityListeners entity
 * annotation.
 */
public class SubListenerType extends ListenerType {
  private static final Logger log = Logger.getLogger(SubListenerType.class.getName());
  private static final L10N L = new L10N(SubListenerType.class);

  private ListenerType _parent;

  public SubListenerType(AmberPersistenceUnit amberPersistenceUnit,
                         ListenerType parent)
  {
    super(amberPersistenceUnit);

    _parent = parent;
  }

  /**
   * Returns the parent type.
   */
  public ListenerType getParentType()
  {
    return _parent;
  }

  /**
   * Printable version of the listener.
   */
  public String toString()
  {
    if (getBeanClass() == null)
      return "SubListenerType[]";
    else
      return "SubListenerType[" + getBeanClass().getName() + "]";
  }
}
