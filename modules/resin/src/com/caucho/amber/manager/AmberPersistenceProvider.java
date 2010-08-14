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

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;

import java.util.Map;
import java.util.logging.Logger;

/**
 * The persistence provider implementation.
 */
public class AmberPersistenceProvider implements PersistenceProvider {
  private static final Logger log
    = Logger.getLogger(AmberPersistenceProvider.class.getName());
  private static final L10N L
    = new L10N(AmberPersistenceProvider.class);

  /**
   * Create an return an EntityManagerFactory for the named unit.
   */
  public EntityManagerFactory createEntityManagerFactory(String name,
                                                         Map map)
  {
    if (map != null) {
      String providerClass = (String) map.get("javax.persistence.provider");

      if (providerClass != null
          && ! getClass().getName().equals(providerClass))
        return null;
    }
    
    AmberContainer container = AmberContainer.getCurrent();

    if (container == null)
      return null;

    AmberPersistenceUnit pUnit = container.getPersistenceUnit(name);

    if (pUnit != null)
      return new AmberEntityManagerFactory(pUnit);
    else
      return null;
  }

  /**
   * Create an return an EntityManagerFactory for the named unit.
   */
  public EntityManagerFactory
    createContainerEntityManagerFactory(PersistenceUnitInfo info,
                                        Map map)
  {
    AmberContainer container = AmberContainer.create();
    
    if (container == null)
      return null;
    
    return container.createEntityManagerFactory(info);
  }

  /* (non-Javadoc)
   * @see javax.persistence.spi.PersistenceProvider#getProviderUtil()
   */
  @Override
  public ProviderUtil getProviderUtil()
  {
    // TODO Auto-generated method stub
    return null;
  }
}
