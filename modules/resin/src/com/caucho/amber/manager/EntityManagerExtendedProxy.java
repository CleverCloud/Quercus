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
import com.caucho.util.L10N;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The Entity manager
 */
public class EntityManagerExtendedProxy
  implements EntityManager
{
  private static final L10N L = new L10N(EntityManagerProxy.class);
  private static final Logger log
    = Logger.getLogger(EntityManagerProxy.class.getName());

  private AmberPersistenceUnit _persistenceUnit;
  private AmberConnection _aConn;

  public EntityManagerExtendedProxy(AmberPersistenceUnit persistenceUnit)
  {
    _persistenceUnit = persistenceUnit;
    _aConn = new AmberConnection(persistenceUnit, true, true);
  }

  /**
   * Makes the instance managed.
   */
  public void persist(Object entity)
  {
    _aConn.persist(entity);
  }

  /**
   * Merges the state of the entity into the current context.
   */
  public <T> T merge(T entity)
  {
    return _aConn.merge(entity);
  }

  /**
   * Remove the instance.
   */
  public void remove(Object entity)
  {
    _aConn.remove(entity);
  }

  /**
   * Find by the primary key.
   */
  /*
    public Object find(String entityName, Object primaryKey)
    {
    return _aConn.find(entityName, primaryKey);
    }
  */

  /**
   * Find by the primary key.
   */
  public <T> T find(Class<T> entityClass, Object primaryKey)
  {
    return _aConn.find(entityClass, primaryKey);
  }

  /**
   * Find by the primary key.
   */
  public <T> T getReference(Class<T> entityClass, Object primaryKey)
  {
    return _aConn.getReference(entityClass, primaryKey);
  }

  /**
   * Returns the flush mode.
   */
  public FlushModeType getFlushMode()
  {
    return _aConn.getFlushMode();
  }

  /**
   * Sets the flush mode.
   */
  public void setFlushMode(FlushModeType mode)
  {
    _aConn.setFlushMode(mode);
  }

  /**
   * Sets the extended type.
   */
  public void setExtended(boolean isExtended)
  {
    _aConn.setExtended(isExtended);
  }

  /**
   * Locks the object.
   */
  public void lock(Object entity, LockModeType lockMode)
  {
    _aConn.lock(entity, lockMode);
  }

  /**
   * Clears the manager.
   */
  public void clear()
  {
    _aConn.clear();
  }

  /**
   * Synchronize with the database.
   */
  public void flush()
  {
    _aConn.flush();
  }

  /**
   * Joins the transaction.
   */
  public void joinTransaction()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the delegate.
   */
  public Object getDelegate()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Clears the manager.
   */
  public void close()
  {
    AmberConnection aConn = _aConn;
    _aConn = null;

    if (aConn != null)
      aConn.close();
  }
  
  /**
   * Creates a query.
   */
  public Query createQuery(String sql)
  {
    return _aConn.createQuery(sql);
  }

  /**
   * Creates an instance of the named query
   */
  public Query createNamedQuery(String sql)
  {
    return _aConn.createNamedQuery(sql);
  }

  /**
   * Creates a query.
   */
  public Query createNativeQuery(String sql)
  {
    return _aConn.createNativeQuery(sql);
  }

  /**
   * Creates a query.
   */
  public Query createNativeQuery(String sql, String map)
  {
    return _aConn.createNativeQuery(sql, map);
  }

  /**
   * Creates a query.
   */
  public Query createNativeQuery(String sql, Class retType)
  {
    return _aConn.createNativeQuery(sql, retType);
  }

  /**
   * Refresh the state of the instance from the database.
   */
  public void refresh(Object entity)
  {
    _aConn.refresh(entity);
  }

  /**
   * Returns true if the entity belongs to the current context.
   */
  public boolean contains(Object entity)
  {
    return _aConn.contains(entity);
  }

  /**
   * Returns the entity manager transaction.
   */
  public EntityTransaction getTransaction()
  {
    return _aConn.getTransaction();
  }

  /**
   * Returns true if open.
   */
  public boolean isOpen()
  {
    return _aConn != null;
  }

  @Override
  public String toString()
  {
    if (_aConn != null)
      return "EntityManagerExtendedProxy[" + _aConn + "]";
    else
      return "EntityManagerProxy[closed]";
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#createNamedQuery(java.lang.String, java.lang.Class)
   */
  @Override
  public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#createQuery(javax.persistence.criteria.CriteriaQuery)
   */
  @Override
  public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#createQuery(javax.persistence.criteria.CriteriaQuery, java.lang.Class)
   */
  @Override
  public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery,
                                       Class<T> resultClass)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#detach(java.lang.Object)
   */
  @Override
  public void detach(Object entity)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#find(java.lang.Class, java.lang.Object, java.util.Map)
   */
  @Override
  public <T> T find(Class<T> entityCLass, Object primaryKey,
                    Map<String, Object> properties)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#find(java.lang.Class, java.lang.Object, javax.persistence.LockModeType)
   */
  @Override
  public <T> T find(Class<T> entityCLass, Object primaryKey,
                    LockModeType lockMode)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#find(java.lang.Class, java.lang.Object, javax.persistence.LockModeType, java.util.Map)
   */
  @Override
  public <T> T find(Class<T> entityCLass, Object primaryKey,
                    LockModeType lockMode, Map<String, Object> properties)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#getCriteriaBuilder()
   */
  @Override
  public CriteriaBuilder getCriteriaBuilder()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#getEntityManagerFactory()
   */
  @Override
  public EntityManagerFactory getEntityManagerFactory()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#getLockMode(java.lang.Object)
   */
  @Override
  public LockModeType getLockMode(Object entity)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#getMetamodel()
   */
  @Override
  public Metamodel getMetamodel()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#getProperties()
   */
  @Override
  public Map<String, Object> getProperties()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#getSupportedProperties()
   */
  @Override
  public Set<String> getSupportedProperties()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#lock(java.lang.Object, javax.persistence.LockModeType, java.util.Map)
   */
  @Override
  public void lock(Object entity, LockModeType lockMode,
                   Map<String, Object> properties)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#refresh(java.lang.Object, java.util.Map)
   */
  @Override
  public void refresh(Object entity, Map<String, Object> properties)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#refresh(java.lang.Object, javax.persistence.LockModeType)
   */
  @Override
  public void refresh(Object entity, LockModeType lockMode)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#refresh(java.lang.Object, javax.persistence.LockModeType, java.util.Map)
   */
  @Override
  public void refresh(Object entity, LockModeType lockMode,
                      Map<String, Object> properties)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#setProperty(java.lang.String, java.lang.Object)
   */
  @Override
  public void setProperty(String propertyName, Object value)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see javax.persistence.EntityManager#unwrap(java.lang.Class)
   */
  @Override
  public <T> T unwrap(Class<T> cls)
  {
    // TODO Auto-generated method stub
    return null;
  }
}
