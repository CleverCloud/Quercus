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

package com.caucho.amber.manager;

import com.caucho.util.L10N;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;

import java.util.Map;
import java.util.logging.Logger;

/**
 * The entity manager from a entity manager proxy.
 */
public class AmberEntityManager extends AmberConnection
  implements EntityManager {
  private static final L10N L = new L10N(AmberEntityManager.class);
  private static final Logger log
    = Logger.getLogger(AmberEntityManager.class.getName());

  /**
   * Creates a manager instance.
   */
  AmberEntityManager(AmberPersistenceUnit persistenceUnit,
                     boolean isAppManaged)
  {
    super(persistenceUnit, false, isAppManaged);

    initJta(); // initThreadConnection(); // ejb/0q00
  }

  public String toString()
  {
    AmberPersistenceUnit persistenceUnit = getPersistenceUnit();

    if (persistenceUnit != null)
      return "AmberEntityManager[" + persistenceUnit.getName() + "]";
    else
      return "AmberEntityManager[closed]";
  }
}
