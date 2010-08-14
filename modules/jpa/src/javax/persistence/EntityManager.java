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
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;

/**
 * The main application interface to the persistence context.
 */
public interface EntityManager {
  /**
   * Makes an object managed and persistent.
   */
  public void persist(Object entity);

  /**
   * Merge the state of the entity to the current context.
   */
  public <T> T merge(T entity);

  /**
   * Removes the instance.
   */
  public void remove(Object entity);

  /**
   * Find based on the primary key.
   */
  public <T> T find(Class<T> entityClass, Object primaryKey);

  /**
   * Find based on the primary key.
   *
   * @since JPA 2.0
   */
  public <T> T find(Class<T> entityCLass,
                    Object primaryKey,
                    Map<String,Object> properties);

  /**
   * Find based on the primary key.
   *
   * @since JPA 2.0
   */
  public <T> T find(Class<T> entityCLass,
                    Object primaryKey,
                    LockModeType lockMode);

  /**
   * Find based on the primary key.
   *
   * @since JPA 2.0
   */
  public <T> T find(Class<T> entityCLass,
                    Object primaryKey,
                    LockModeType lockMode,
                    Map<String,Object> properties);

  /**
   * Gets an instance whose state may be lazily fetched.
   */
  public <T> T getReference(Class<T> entityClass, Object primaryKey);

  /**
   * Synchronize the context with the database.
   */
  public void flush();

  /**
   * Sets the flush mode for all objects in the context.
   */
  public void setFlushMode(FlushModeType flushMode);

  /**
   * Returns the flush mode for the objects in the context.
   */
  public FlushModeType getFlushMode();

  /**
   * Sets the lock mode for an entity.
   */
  public void lock(Object entity, LockModeType lockMode);

  /**
   * Sets the lock mode for an entity.
   *
   * @since JPA 2.0
   */
  public void lock(Object entity,
                   LockModeType lockMode,
                   Map<String,Object> properties);

  /**
   * Update the state of the instance from the database.
   */
  public void refresh(Object entity);

  /**
   * Update the state of the instance from the database.
   *
   * @since JPA 2.0
   */
  public void refresh(Object entity,
                      Map<String,Object> properties);

  /**
   * Update the state of the instance from the database.
   *
   * @since JPA 2.0
   */
  public void refresh(Object entity, LockModeType lockMode);

  /**
   * Update the state of the instance from the database.
   *
   * @since JPA 2.0
   */
  public void refresh(Object entity,
                      LockModeType lockMode,
                      Map<String,Object> properties);

  /**
   * Clears the context, causing all entities to become detached.
   */
  public void clear();

  /**
   * Clears the entity
   *
   * @since JPA 2.0
   */
  public void detach(Object entity);

  /**
   * Check if the instance belongs to the current context.
   */
  public boolean contains(Object entity);

  /**
   * Returns the lock mode for the entity
   *
   * @since JPA 2.0
   */
  public LockModeType getLockMode(Object entity);

  /**
   * Sets properties for the entity manager
   *
   * @since JPA 2.0
   */
  public void setProperty(String propertyName, Object value);

  /**
   * Returns the properties for the entity manager
   *
   * @since JPA 2.0
   */
  public Map<String,Object> getProperties();

  /**
   * Returns the supported properties for the entity manager
   *
   * @since JPA 2.0
   */
  public Set<String> getSupportedProperties();

  /**
   * Creates a new query.
   */
  public Query createQuery(String ql);
  
  /**
   * Creates a TypedQuery for a criteria
   * 
   * @since JPA 2.0
   */
  public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery);
  
  /**
   * Creates a TypedQuery for a criteria
   * 
   * @since JPA 2.0
   */
  public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery,
                                       Class<T> resultClass);

  /**
   * Creates a named query.
   */
  public Query createNamedQuery(String name);

  /**
   * Creates a named query.
   * 
   * @since JPA 2.0
   */
  public <T> TypedQuery<T> createNamedQuery(String name,
                                            Class<T> resultClass);

  /**
   * Creates a native SQL query.
   */
  public Query createNativeQuery(String sql);

  /**
   * Creates a native SQL query.
   */
  @SuppressWarnings("unchecked")
  public Query createNativeQuery(String sql, Class resultClass);

  /**
   * Creates a query for SQL.
   */
  public Query createNativeQuery(String sql, String resultSetMapping);

  /**
   * Joins the transaction.
   */
  public void joinTransaction();
  
  /**
   * Returns the object of the type for a provider-specific API
   */
  public <T> T unwrap(Class<T> cls);

  /**
   * Gets the delegate.
   */
  public Object getDelegate();

  /**
   * Closes the entity manager.
   */
  public void close();

  /**
   * Returns true if the entity manager is open.
   */
  public boolean isOpen();

  /**
   * Returns the transaction manager object.
   */
  public EntityTransaction getTransaction();

  /**
   * Returns the owning factory
   *
   * @since JPA 2.0
   */
  public EntityManagerFactory getEntityManagerFactory();
  
  /**
   * Returns a CriteriaBuilder to create CriteriaQuery objects.
   * 
   * @since JPA 2.0
   */
  public CriteriaBuilder getCriteriaBuilder();
  
  /**
   * Returns the Metamodel interface for the persistence unit.
   * 
   * @since JPA 2.0
   */
  public Metamodel getMetamodel();
}
