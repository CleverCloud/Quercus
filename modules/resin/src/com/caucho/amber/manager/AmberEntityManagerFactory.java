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

import com.caucho.config.inject.HandleAware;
import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Amber's EntityManagerFactory container.
 */
public class AmberEntityManagerFactory
  implements EntityManagerFactory, java.io.Serializable,
             HandleAware
{
  private static final Logger log
    = Logger.getLogger(AmberEntityManagerFactory.class.getName());

  private AmberPersistenceUnit _unit;
  private boolean _isOpen = true;

  private Object _serializationHandle;

  AmberEntityManagerFactory(AmberPersistenceUnit unit)
  {
    _unit = unit;
  }

  /**
   * Create a new EntityManager with TRANSACTION type.
   */
  public EntityManager createEntityManager()
  {
    return createEntityManager(null);
  }

  /**
   * Create a new EntityManager with the given properties.
   */
  public EntityManager createEntityManager(Map map)
  {
    return new AmberEntityManager(_unit, true);
  }

  /**
   * Close the factory an any resources.
   */
  public void close()
  {
    // jpa/0s20
    // _isOpen = false;
  }

  /**
   * Returns true if the factory is open.
   */
  public boolean isOpen()
  {
    return _isOpen;
  }

  /**
   * Returns the properties and values for the factory
   *
   * @since JPA 2.0
   */
  public Map getProperties()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the supported properties
   *
   * @since JPA 2.0
   */
  public Set<String> getSupportedProperties()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the entity manager cache
   *
   * @since JPA 2.0
   */
  public Cache getCache()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets the serializable handle
   */
  public void setSerializationHandle(Object handle)
  {
    _serializationHandle = handle;
  }

  /**
   * Serializes to the handle
   */
  private Object writeReplace()
  {
    return _serializationHandle;
  }

  public String toString()
  {
    return "AmberEntityManagerFactory[" + _unit.getName() + "]";
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManagerFactory#getCriteriaBuilder()
   */
  @Override
  public CriteriaBuilder getCriteriaBuilder()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManagerFactory#getMetamodel()
   */
  @Override
  public Metamodel getMetamodel()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManagerFactory#getPersistenceUnitUtil()
   */
  @Override
  public PersistenceUnitUtil getPersistenceUnitUtil()
  {
    // TODO Auto-generated method stub
    return null;
  }
}


