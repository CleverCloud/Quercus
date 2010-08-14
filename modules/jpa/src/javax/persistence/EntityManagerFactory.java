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

package javax.persistence;

import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

/**
 * Factory for getting an entity manager.
 */
public interface EntityManagerFactory {
  /**
   * Create a new EntityManager with TRANSACTION type.
   */
  public EntityManager createEntityManager();

  /**
   * Create a new EntityManager with the given properties.
   */
  @SuppressWarnings("unchecked")
  public EntityManager createEntityManager(Map map);
  
  public CriteriaBuilder getCriteriaBuilder();
  
  public Metamodel getMetamodel();

  /**
   * Returns true if the factory is open.
   */
  public boolean isOpen();

  /**
   * Close the factory an any resources.
   */
  public void close();

  /**
   * Returns the properties and values for the factory
   *
   * @since JPA 2.0
   */
  public Map<String,Object> getProperties();

  /**
   * Returns the entity manager cache
   *
   * @since JPA 2.0
   */
  public Cache getCache();
  
  /**
   * Returns persistence unit utilities.
   *
   * @since JPA 2.0
   */
  public PersistenceUnitUtil getPersistenceUnitUtil();
}
