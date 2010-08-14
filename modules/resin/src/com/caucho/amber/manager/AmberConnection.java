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

import com.caucho.amber.AmberException;
import com.caucho.amber.AmberObjectNotFoundException;
import com.caucho.amber.AmberQuery;
import com.caucho.amber.AmberRuntimeException;
import com.caucho.amber.cfg.EntityResultConfig;
import com.caucho.amber.cfg.NamedNativeQueryConfig;
import com.caucho.amber.cfg.SqlResultSetMappingConfig;
import com.caucho.amber.collection.AmberCollection;
import com.caucho.amber.entity.*;
import com.caucho.amber.query.AbstractQuery;
import com.caucho.amber.query.QueryCacheKey;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.query.ResultSetCacheChunk;
import com.caucho.amber.query.UserQuery;
import com.caucho.amber.table.AmberTable;
import com.caucho.amber.type.EntityType;
import com.caucho.config.ConfigException;
import com.caucho.ejb.EJBExceptionWrapper;
import com.caucho.jdbc.JdbcMetaData;
import com.caucho.transaction.BeginResource;
import com.caucho.transaction.CloseResource;
import com.caucho.transaction.UserTransactionProxy;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;
import javax.persistence.TransactionRequiredException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.metamodel.Metamodel;
import javax.sql.DataSource;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The entity manager from a entity manager proxy.
 */
public class AmberConnection
  implements BeginResource, CloseResource, Synchronization, EntityManager
{
  private static final L10N L = new L10N(AmberConnection.class);
  private static final Logger log
    = Logger.getLogger(AmberConnection.class.getName());

  private static final Entity []NULL_ENTITIES = new Entity[0];

  private AmberPersistenceUnit _persistenceUnit;

  private boolean _isRegistered;
  private boolean _isThreadConnection;

  private Entity []_entities = new Entity[32];
  private int _entitiesTop;

  private Entity []_txEntities = NULL_ENTITIES;
  private int _txEntitiesTop;

  private ArrayList<AmberCompletion> _completionList
    = new ArrayList<AmberCompletion>();

  private ArrayList<AmberCollection> _queries
    = new ArrayList<AmberCollection>();

  private EntityTransaction _trans;

  private long _xid;
  private boolean _isInTransaction;
  private boolean _isXA;

  private boolean _isExtended;

  private boolean _isAppManaged;

  private Connection _conn;
  private Connection _readConn;

  private boolean _isAutoCommit = true;

  private int _depth;

  private LruCache<String,PreparedStatement> _preparedStatementMap
    = new LruCache<String,PreparedStatement>(32);

  private ArrayList<Statement> _statements = new ArrayList<Statement>();

  private EntityKey _entityKey = new EntityKey();
  private QueryCacheKey _queryKey = new QueryCacheKey();

  private ArrayList<Entity> _mergingEntities = new ArrayList<Entity>();

  private boolean _isFlushAllowed = true;

  /**
   * Creates a manager instance.
   */
  AmberConnection(AmberPersistenceUnit persistenceUnit,
                  boolean isExtended,
                  boolean isAppManaged)
  {
    _persistenceUnit = persistenceUnit;
    _isExtended = isExtended;
    _isAppManaged = isAppManaged;
  }

  /**
   * Creates a manager instance.
   */
  AmberConnection(AmberPersistenceUnit persistenceUnit,
                  boolean isExtended)
  {
    this(persistenceUnit, isExtended, false);
  }

  /**
   * Returns the persistence unit.
   */
  public AmberPersistenceUnit getPersistenceUnit()
  {
    return _persistenceUnit;
  }

  /**
   * Returns true for JPA.
   */
  public boolean isJPA()
  {
    return _persistenceUnit.isJPA();
  }

  /**
   * Set true for a threaded connection.
   */
  public void initThreadConnection()
  {
    _isThreadConnection = true;

    initJta();
  }

  public void initJta()
  {
    // non-jta connections do not register with the local transaction
    if (_persistenceUnit.isJta())
      register();
  }

  /**
   * Makes the instance managed.
   */
  public void persist(Object entityObject)
  {
    RuntimeException exn = null;

    try {
      if (entityObject == null)
        return;

      Entity entity = checkEntityType(entityObject, "persist");

      checkTransactionRequired("persist");

      persistInternal(entity);

      // XXX: check spec. for JTA vs. non-JTA behavior and add QA.
      // ejb30/persistence/ee/packaging/ejb/resource_local/test14
      if (! _persistenceUnit.isJta())
        flushInternal();
    } catch (RuntimeException e) {
      exn = e;
    } catch (SQLException e) {
      exn = new IllegalStateException(e);
    } catch (Exception e) {
      exn = new EJBExceptionWrapper(e);
    }

    if (exn != null) {
      if (! _persistenceUnit.isJta()) {
        if (_trans != null)
          _trans.setRollbackOnly();
      }

      throw exn;
    }
  }

  /**
   * Makes the instance managed called
   * from cascading operations.
   */
  public Object persistFromCascade(Object o)
  {
    // jpa/0h25, jpa/0i5e

    try {
      if (o == null)
        return null;

      Entity entity = (Entity) o;

      // jpa/0h25

      return persistInternal(entity);

    } catch (EntityExistsException e) {
      log.log(Level.FINER, e.toString(), e);
      // This is not an issue. It is the cascading
      // operation trying to persist the source
      // entity from the destination end.

      return o;
    } catch (RuntimeException e) {
      throw e;
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Merges the state of the entity into the current context.
   */
  public <T> T merge(T entityT)
  {
    RuntimeException exn = null;

    try {
      flushInternal();

      // Cannot flush before the merge is complete.
      _isFlushAllowed = false;

      entityT = recursiveMerge(entityT);

    } catch (RuntimeException e) {
      exn = e;
    } catch (Exception e) {
      exn = new EJBExceptionWrapper(e);
    } finally {
      _isFlushAllowed = true;

      try {
        flushInternal();
      } catch (RuntimeException e) {
        if (exn == null)
          exn = e;
      } catch (Exception e) {
        if (exn == null)
          exn = new EJBExceptionWrapper(e);
      } finally {
        _mergingEntities.clear();
      }
    }

    // jpa/0o42, jpa/0o44
    if (exn != null)
      throw exn;

    return entityT;
  }

  /**
   * Remove the instance.
   */
  public void remove(Object entity)
  {
    try {
      if (entity == null)
        return;

      Entity instance = checkEntityType(entity, "remove");

      checkTransactionRequired("remove");

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l("removing entity class " + instance.getClass().getName() +
                                 " PK: " + instance.__caucho_getPrimaryKey() +
                                 " state: " + instance.__caucho_getEntityState()));

      EntityState state = instance.__caucho_getEntityState();

      if (EntityState.P_DELETING.ordinal() <= state.ordinal()) {
        if (log.isLoggable(Level.FINER))
          log.log(Level.FINER, L.l("remove is ignoring entity in state " + state));

        return;
      }

      // jpa/0k12
      if (instance.__caucho_getConnection() == null) {
        if (instance.__caucho_getEntityType() == null) {
          if (log.isLoggable(Level.FINER))
            log.log(Level.FINER, L.l("remove is ignoring entity; performing only cascade post-remove"));

          // Ignore this entity; only post-remove child entities.
          instance.__caucho_cascadePostRemove(this);

          // jpa/0ga7
          return;
        }
        else
          throw new IllegalArgumentException(L.l("remove() operation can only be applied to a managed entity. This entity instance '{0}' PK: '{1}' is detached which means it was probably removed or needs to be merged.", instance.getClass().getName(), instance.__caucho_getPrimaryKey()));
      }

      // jpa/0h25, jpa/0i5e
      // Do not flush dependent objects for cascading persistence
      // when this entity is being removed.
      instance.__caucho_setEntityState(EntityState.P_DELETING);

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l("remove is flushing any lazy cascading operation"));

      // jpa/1620
      // In particular, required for cascading persistence, since the cascade
      // is lazy until flush
      // jpa/0h25 flushInternal();
      // Cannot flush since the delete is lazy until flush, i.e.:
      // remove(A); // (*) __caucho_flush()
      // remove(B);
      // (*) would break a FK constraint if B has a reference to A.

      // jpa/0h26
      updateFlushPriority(instance);

      // jpa/0h25, jpa/0i5e
      // Restores original state.
      instance.__caucho_setEntityState(state);

      Object oldEntity;

      oldEntity = getEntity(instance.getClass(),
                            instance.__caucho_getPrimaryKey());

      // jpa/0ga4
      if (oldEntity == null)
        throw new IllegalArgumentException(L.l("remove() operation can only be applied to a managed entity instance."));

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l("remove is performing cascade pre-remove"));

      // Pre-remove child entities.
      instance.__caucho_cascadePreRemove(this);

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l("remove is performing delete on the target entity"));

      delete(instance);

      // jpa/0o30: flushes the owning side delete.
      // XXX: Cannot flush since the delete is lazy until flush.
      // jpa/0h25
      // instance.__caucho_flush();

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l("remove is performing cascade post-remove"));

      // jpa/0o30
      // Post-remove child entities.
      instance.__caucho_cascadePostRemove(this);

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l("DONE successful remove for entity class " +
                                 instance.getClass().getName() +
                                 " PK: " + instance.__caucho_getPrimaryKey()));

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Find by the primary key.
   */
  public <T> T find(Class<T> entityClass,
                    Object primaryKey)
  {
    // Do not flush while an entity is being loaded or merged.
    boolean oldIsFlushAllowed = _isFlushAllowed;

    try {
      // Do not flush while loading an entity.
      // Broken relationships would not pass the flush validation.
      _isFlushAllowed = false;

      T entity = (T) load(entityClass, primaryKey, true);

      // jpa/0j07
      /*
      if (! isActiveTransaction()) {
        // jpa/0o00
        detach();
      }
      */

      return entity;
    } catch (AmberObjectNotFoundException e) {
      // JPA: should not throw at all, returns null only.
      // log.log(Level.FINER, e.toString(), e);
      return null;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    } finally {
      _isFlushAllowed = oldIsFlushAllowed;
    }
  }

  /**
   * Find by the primary key.
   */
  public <T> T getReference(Class<T> entityClass, Object primaryKey)
    throws EntityNotFoundException, IllegalArgumentException
  {
    T reference = null;

    try {
      // XXX: only needs to get a reference.

      reference = (T) load(entityClass, primaryKey, false);

      if (reference == null)
        throw new EntityNotFoundException(L.l("entity with primary key {0} not found in getReference()", primaryKey));

      /*
        if (! (entityClass.isAssignableFrom(Entity.class)))
          throw new IllegalArgumentException(L.l("getReference() operation can only be applied to an entity class"));
      */

      return reference;

    } catch (EntityNotFoundException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(e);
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Clears the connection
   */
  public void clear()
  {
    _entitiesTop = 0;
    _txEntitiesTop = 0;
  }

  /**
   * Creates a query.
   */
  public Query createQuery(String sql)
  {
    try {
      AbstractQuery queryProgram = parseQuery(sql, false);

      return new QueryImpl(queryProgram, this);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(e);
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Creates an instance of the named query
   */
  public Query createNamedQuery(String name)
  {
    String sql = _persistenceUnit.getNamedQuery(name);

    if (sql != null)
      return createQuery(sql);

    NamedNativeQueryConfig nativeQuery
      = _persistenceUnit.getNamedNativeQuery(name);

    sql = nativeQuery.getQuery();

    String resultSetMapping = nativeQuery.getResultSetMapping();

    if (! ((resultSetMapping == null) || "".equals(resultSetMapping)))
      return createNativeQuery(sql, resultSetMapping);

    Class resultClass = nativeQuery.getResultClass();

    AmberEntityHome entityHome
      = _persistenceUnit.getEntityHome(resultClass.getName());

    EntityType entityType = entityHome.getEntityType();

    try {
      return createNativeQuery(sql, entityType.getInstanceClass());
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Creates an instance of the named query
   */
  public Query createNativeQuery(String sql)
  {
    sql = sql.trim();

    char ch = sql.charAt(0);

    if (ch == 'S' || ch == 's')
      throw new UnsupportedOperationException(L.l("createNativeQuery(String sql) is not supported for select statements. Please use createNativeQuery(String sql, String map) or createNativeQuery(String sql, Class cl) to map the result to scalar values or bean classes."));

    return createInternalNativeQuery(sql);
  }

  /**
   * Creates an instance of the named query
   */
  public Query createNativeQuery(String sql, String map)
  {
    // jpa/0y1-

    SqlResultSetMappingConfig resultSet;

    resultSet = _persistenceUnit.getSqlResultSetMapping(map);

    if (resultSet == null)
      throw new IllegalArgumentException(L.l("createNativeQuery() cannot create a native query for a result set named '{0}'", map));

    return createInternalNativeQuery(sql, resultSet);
  }

  /**
   * Creates an instance of the native query
   */
  public Query createNativeQuery(String sql, Class type)
  {
    SqlResultSetMappingConfig resultSet
      = new SqlResultSetMappingConfig();

    EntityResultConfig entityResult
      = new EntityResultConfig();

    entityResult.setEntityClass(type.getName());

    resultSet.addEntityResult(entityResult);

    return createInternalNativeQuery(sql, resultSet);
  }

  /**
   * Refresh the state of the instance from the database.
   */
  public void refresh(Object entity)
  {
    try {
      if (entity == null)
        return;

      if (! (entity instanceof Entity))
        throw new IllegalArgumentException(L.l("refresh() operation can only be applied to an entity instance. This object is of class '{0}'", entity.getClass().getName()));

      checkTransactionRequired("refresh");

      Entity instance = (Entity) entity;

      String className = instance.getClass().getName();
      Object pk = instance.__caucho_getPrimaryKey();

      Entity oldEntity = getEntity(instance.getClass(), pk);

      if (oldEntity != null) {
        EntityState state = instance.__caucho_getEntityState();

        if (state.ordinal() <= EntityState.TRANSIENT.ordinal()
            || EntityState.P_DELETING.ordinal() <= state.ordinal()) {
          throw new IllegalArgumentException(L.l("refresh() operation can only be applied to a managed entity instance. The entity state is '{0}' for object of class '{0}' with PK '{1}'", className, pk, state == EntityState.TRANSIENT ? "TRANSIENT" : "DELETING or DELETED"));
        }
      }
      else
        throw new IllegalArgumentException(L.l("refresh() operation can only be applied to a managed entity instance. There was no managed instance of class '{0}' with PK '{1}'", className, pk));

      // Reset and refresh state.
      instance.__caucho_expire();
      instance.__caucho_makePersistent(this, (EntityType) null);
      instance.__caucho_retrieve_eager(this);
    } catch (SQLException e) {
      throw new AmberRuntimeException(e);
    }
  }

  /**
   * Returns the flush mode.
   */
  public FlushModeType getFlushMode()
  {
    return FlushModeType.AUTO;
  }

  /**
   * Sets the extended type.
   */
  public void setExtended(boolean isExtended)
  {
    _isExtended = isExtended;
  }

  /**
   * Returns the flush mode.
   */
  public void setFlushMode(FlushModeType mode)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Locks the object.
   */
  public void lock(Object entity, LockModeType lockMode)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the transaction.
   */
  public EntityTransaction getTransaction()
  {
    if (_isXA)
      throw new IllegalStateException(L.l("Cannot call EntityManager.getTransaction() inside a distributed transaction."));

    if (_trans == null)
      _trans = new EntityTransactionImpl();

    return _trans;
  }

  /**
   * Returns true if open.
   */
  public boolean isOpen()
  {
    return _persistenceUnit != null;
  }

  /**
   * Registers with the local transaction.
   */
  void register()
  {
    if (! _isRegistered) {
      if (! _isAppManaged)
        UserTransactionProxy.getInstance().enlistCloseResource(this);

      UserTransactionProxy.getInstance().enlistBeginResource(this);
    }

    _isRegistered = true;
  }

  /**
   * Joins the transaction.
   */
  public void joinTransaction()
  {
    // XXX: jpa/0s46, jpa/0s47

    _isInTransaction = true;
  }

  /**
   * Gets the delegate.
   */
  public Object getDelegate()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Closes the context.
   */
  public void close()
  {
    if (_persistenceUnit == null) {
      // jpa/0s45
      throw new IllegalStateException("Entity manager is already closed.");
    }

    try {
      if (_isThreadConnection)
        _persistenceUnit.removeThreadConnection();

      _isRegistered = false;

      cleanup();
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    } finally {
      _persistenceUnit = null;
    }
  }

  /**
   * Returns the amber manager.
   */
  public AmberPersistenceUnit getAmberManager()
  {
    return _persistenceUnit;
  }

  /**
   * Registers a collection.
   */
  public void register(AmberCollection query)
  {
    _queries.add(query);
  }

  /**
   * Adds a completion
   */
  public void addCompletion(AmberCompletion completion)
  {
    if (! _completionList.contains(completion))
      _completionList.add(completion);
  }

  /**
   * Returns true if a transaction is active or
   * this persistence context is extended.
   */
  public boolean isActiveTransaction()
  {
    return _isInTransaction || _isExtended;
  }

  /**
   * Returns true if a transaction is active.
   */
  public boolean isInTransaction()
  {
    return _isInTransaction;
  }

  /**
   * Returns the cache chunk size.
   */
  public int getCacheChunkSize()
  {
    return 32;
  }

  public Object load(Class cl,
                     Object key,
                     boolean isEager)
    throws AmberException
  {
    if (_persistenceUnit == null)
      throw new IllegalStateException(L.l("AmberConnection is closed"));
    
    if (log.isLoggable(Level.FINER))
      log.finer(L.l("{0}[1] amber loading entity class", cl.getSimpleName(), key));

    Entity entity = null;

    if (key == null)
      return null;

    // ejb/0d01, jpa/0gh0, jpa/0g0k, jpa/0j5f
    // if (shouldRetrieveFromCache())
    entity = getEntity(cl, key);

    if (entity != null) {
      // jpa/0s2d: if it contains such entity and we have
      // PersistenceContextType.TRANSACTION, the entity is
      // managed and we can just return it (otherwise it would
      // be detached and not be found in _entities).
      // XXX: for PersistenceContextType.EXTENDED???

      return entity;
    }
    
    _entityKey.init(cl, key);
    
    EntityItem cacheItem = loadCacheItem(cl, key, null);

    if (cacheItem == null)
      return null;

    /*
    boolean isLoad = true;

    // jpa/0h13 as a negative test.
    if (isActiveTransaction())
      isLoad = isEager;
    */
    // jpa/0o03
    boolean isLoad = isEager;

    try {
      entity = cacheItem.createEntity(this, key);

      if (entity == null)
        return null;

      // The entity is added for eager loading
      addInternalEntity(entity);

      boolean isXA = isActiveTransaction();

      // jpa/0l48: inheritance loading optimization.
      // jpa/0h20: no transaction, copy from the existing cache item.
      // jpa/0l42: loading optimization.

      if (isLoad) {
        entity.__caucho_retrieve_eager(this);
      }
      else if (isXA) {
        // jpa/0v33: within a transaction, cannot copy from cache.
        entity.__caucho_retrieve_self(this);
      }
    } catch (SQLException e) {
      if (_persistenceUnit.isJPA()) {
        log.log(Level.FINER, e.toString(), e);

        return null;
      }

      throw new AmberObjectNotFoundException(L.l("{0}[{1}] is an unknown amber object",
                                                 cl.getName(), key),
                                             e);
    } catch (AmberObjectNotFoundException e) {
      // 0g0q: if the entity is not found, removes it from context.
      if (entity != null)
        removeEntity(entity);

      if (_persistenceUnit.isJPA())
        return null;

      throw e;
    }

    Entity txEntity = getTransactionEntity(entity.getClass(),
                                           entity.__caucho_getPrimaryKey());

    // XXX: jpa/0v33
    if (txEntity != null)
      setTransactionalState(txEntity);

    return entity;
  }

  public EntityItem loadCacheItem(Class cl, Object key,
                                  AmberEntityHome entityHome)
    throws AmberException
  {
    _entityKey.init(cl, key);

    EntityItem cacheItem = _persistenceUnit.getEntity(_entityKey);

    if (cacheItem != null)
      return cacheItem;

    if (entityHome == null)
      entityHome = _persistenceUnit.getEntityHome(cl.getName());

    if (entityHome == null) {
      throw new IllegalArgumentException(L.l("'{0}' is an unknown class in persistence-unit '{1}'.  find() operation can only be applied if the entity class is specified in the scope of a persistence unit.",
                                             cl.getName(),
                                             _persistenceUnit.getName()));
    }

    cacheItem = entityHome.findEntityItem(this, key);

    if (cacheItem == null) {
      if (_persistenceUnit.isJPA())
        return null;

      // ejb/0604
      throw new AmberObjectNotFoundException("amber find: no matching object " + cl.getName() + "[" + key + "]");
    }

    if (cacheItem instanceof CacheableEntityItem)
      cacheItem = _persistenceUnit.putEntity(cl, key, cacheItem);

    return cacheItem;
  }

  /**
   * Loads the object based on the class and primary key.
   */
  public Object load(String entityName,
                     Object key)
    throws AmberException
  {
    AmberEntityHome entityHome = _persistenceUnit.getEntityHome(entityName);

    if (entityHome == null)
      return null;

    Entity entity = null;

    // XXX: ejb/0d01
    // jpa/0y14 if (shouldRetrieveFromCache())
    entity = getEntity(entityHome.getJavaClass(), key);

    if (entity != null)
      return entity;

    try {
      entityHome.init();
    } catch (ConfigException e) {
      throw new AmberException(e);
    }

    entity = (Entity) find(entityHome.getEntityType().getInstanceClass(), key);

    // addEntity(entity);

    return entity;
  }

  /**
   * Returns the entity for the connection.
   */
  public Entity getEntity(EntityItem item)
  {
    Entity itemEntity = item.getEntity();

    Class cl = itemEntity.getClass();
    Object pk = itemEntity.__caucho_getPrimaryKey();

    Entity entity = getEntity(cl, pk);

    if (entity != null) {
      if (entity.__caucho_getEntityState().isManaged())
        return entity;
      // else
      // jpa/0g40: the copy object was created at some point in
      // findEntityItem, but it is still not loaded.
    }
    else {
      try {
        entity = item.createEntity(this, pk);
      } catch (SQLException e) {
        throw new AmberRuntimeException(e);
      }

      /*
      // Create a new entity for the given class and primary key.
      try {
        entity = (Entity) cl.newInstance();
      } catch (Exception e) {
        throw new AmberRuntimeException(e);
      }
      */

      // entity.__caucho_setEntityState(EntityState.P_NON_TRANSACTIONAL);
      // entity.__caucho_setPrimaryKey(pk);

      // jpa/1000: avoids extra allocations.
      addInternalEntity(entity);
    }

    // jpa/0l43
    //_persistenceUnit.copyFromCacheItem(this, entity, item);
    // jpa/0l4a
    entity.__caucho_retrieve_eager(this);

    return entity;
  }

  /**
   * Returns the entity for the connection.
   */
  public Entity getEntityLazy(EntityItem item)
  {
    Entity itemEntity = item.getEntity();

    Class cl = itemEntity.getClass();
    Object pk = itemEntity.__caucho_getPrimaryKey();

    Entity entity = getEntity(cl, pk);

    if (entity != null) {
      if (entity.__caucho_getEntityState().isManaged())
        return entity;
      // else
      // jpa/0g40: the copy object was created at some point in
      // findEntityItem, but it is still not loaded.
    }
    else {
      try {
        entity = item.createEntity(this, pk);
      } catch (SQLException e) {
        throw new AmberRuntimeException(e);
      }

      /*
      // Create a new entity for the given class and primary key.
      try {
        entity = (Entity) cl.newInstance();
      } catch (Exception e) {
        throw new AmberRuntimeException(e);
      }
      */

      // entity.__caucho_setEntityState(EntityState.P_NON_TRANSACTIONAL);
      // entity.__caucho_setPrimaryKey(pk);

      // jpa/1000: avoids extra allocations.
      addInternalEntity(entity);
    }

    return entity;
  }

  /**
   * Loads the object based on itself.
   */
  public Object makePersistent(Object obj)
    throws SQLException
  {
    Entity entity = (Entity) obj;

    // check to see if exists

    if (entity == null)
      throw new NullPointerException();

    Class cl = entity.getClass();

    // Entity oldEntity = getEntity(cl, entity.__caucho_getPrimaryKey());

    AmberEntityHome entityHome;
    entityHome = _persistenceUnit.getEntityHome(entity.getClass().getName());

    if (entityHome == null)
      throw new AmberException(L.l("{0}: entity has no matching home",
                                   entity.getClass().getName()));

    entityHome.makePersistent(entity, this, false);

    return entity;
  }

  /**
   * Loads the object with the given class.
   */
  public Entity loadLazy(Class cl, String name, Object key)
  {
    return loadLazy(cl.getName(), name, key);
  }

  /**
   * Loads the object with the given class.
   */
  public Entity loadLazy(String className, String name, Object key)
  {
    if (key == null)
      return null;

    try {
      AmberEntityHome home = _persistenceUnit.getEntityHome(name);

      if (home == null)
        throw new RuntimeException(L.l("no matching home for {0}", className));

      home.init();

      Object obj = load(home.getEntityType().getInstanceClass(), key, false);

      Entity entity = (Entity) obj;

      return entity;
    } catch (SQLException e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    } catch (ConfigException e) {
      throw new AmberRuntimeException(e);
    }
  }

  /**
   * Loads the object with the given class.
   */
  public EntityItem findEntityItem(String name, Object key)
  {
    try {
      AmberEntityHome home = _persistenceUnit.getEntityHome(name);

      if (home == null)
        throw new RuntimeException(L.l("no matching home for {0}", name));

      home.init();

      return loadCacheItem(home.getJavaClass(), key, home);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new AmberRuntimeException(e);
    }
  }

  /**
   * Loads the object with the given class.
   */
  public EntityItem setEntityItem(String name, Object key, EntityItem item)
  {
    try {
      AmberEntityHome home = _persistenceUnit.getEntityHome(name);

      if (home == null)
        throw new RuntimeException(L.l("no matching home for {0}", name));

      home.init();

      return home.setEntityItem(key, item);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new AmberRuntimeException(e);
    }
  }

  /**
   * Loads the object with the given class.
   *
   * @param name the class name.
   * @param key the key.
   * @param notExpiringLoadMask the load mask bit that will not be reset
   *        when the entity is expiring and reloaded to a new transaction.
   *        Normally, the bit is only set in bidirectional one-to-one
   *        relationships where we already know the other side has already
   *        been loaded in the second or new transactions.
   * @param notExpiringGroup the corresponding load group.
   */
  public Entity loadFromHome(String name,
                             Object key)
  {
    try {
      AmberEntityHome home = _persistenceUnit.getEntityHome(name);

      if (home == null)
        throw new RuntimeException(L.l("no matching home for {0}", name));

      home.init();

      // jpa/0ge4, jpa/0o04, jpa/0o0b, jpa/0o0c: bidirectional optimization.
      return (Entity) load(home.getEntityType().getInstanceClass(),
                           key, true);
    } catch (AmberObjectNotFoundException e) {
      if (_persistenceUnit.isJPA()) {
        if (log.isLoggable(Level.FINER))
          log.log(Level.FINER, e.toString(), e);

        // jpa/0h29
        return null;
      }

      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new AmberRuntimeException(e);
    }
  }

  /**
   * Loads the object with the given class.
   */
  public Object loadProxy(String name,
                          Object key)
  {
    if (key == null)
      return null;

    AmberEntityHome home = _persistenceUnit.getEntityHome(name);

    if (home == null)
      throw new RuntimeException(L.l("no matching home for {0}", name));

    return loadProxy(home.getEntityType(), key);
  }

  /**
   * Loads the object with the given class.
   */
  public Object loadProxy(EntityType type,
                          Object key)
  {
    if (key == null)
      return null;

    Entity entity = getEntity(type.getInstanceClass(), key);

    if (entity != null) {
      // jpa/0m30
      return entity;
    }

    try {
      AmberEntityHome home = type.getHome();

      EntityItem item = home.findEntityItem(this, key);

      if (item == null)
        return null;

      EntityFactory factory = home.getEntityFactory();

      Object newEntity = factory.getEntity(this, item);

      if (_persistenceUnit.isJPA()) {
        // jpa/0h29: eager loading.
        Entity instance = (Entity) newEntity;
        setTransactionalState(instance);
      }

      return newEntity;
    } catch (SQLException e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  /**
   * Loads the CMP 2.1 object for the given entityItem
   */
  public Object loadProxy(EntityItem entityItem)
  {
    Entity itemEntity = entityItem.getEntity();

    /*
    Class cl = itemEntity.getClass();
    Object pk = itemEntity.__caucho_getPrimaryKey();

    Entity entity = getEntity(cl.getName(), pk);

    if (entity != null) {
      // jpa/0m30
      return entity;
    }
    */
    
    AmberEntityHome home = entityItem.getEntityHome();

    EntityFactory factory = home.getEntityFactory();

    Object newEntity = factory.getEntity(this, entityItem);

    return newEntity;
  }

  /**
   * Loads the object based on the class and primary key.
   */
  public Object load(Class cl, long intKey)
    throws AmberException
  {
    AmberEntityHome entityHome = _persistenceUnit.getEntityHome(cl.getName());

    if (entityHome == null)
      return null;

    Object key = entityHome.toObjectKey(intKey);

    return load(cl, key, true);
  }

  /**
   * Loads the object based on the class and primary key.
   */
  public Object loadLazy(Class cl, long intKey)
    throws AmberException
  {
    AmberEntityHome entityHome = _persistenceUnit.getEntityHome(cl.getName());

    if (entityHome == null)
      return null;

    Object key = entityHome.toObjectKey(intKey);

    return loadLazy(cl, cl.getName(), key);
  }

  /**
   * Matches the entity.
   */
  public Entity getEntity(Class cl, Object key)
  {
    Entity []entities = _entities;

    for (int i = _entitiesTop - 1; i >= 0; i--) {
      Entity entity = entities[i];

      if (entity.__caucho_match(cl, key)) {
        return entity;
      }
    }

    return null;
  }

  public Entity getEntity(int index)
  {
    return _entities[index];
  }

  /**
   * Returns the context entity that corresponds to the
   * entity passed in. The entity passed in is normally a
   * cache entity but might be the context entity itself
   * when we want to make sure the reference is to an
   * entity in the persistence context.
   */
  public Entity getEntity(Entity entity)
  {
    if (entity == null)
      return null;

    return getEntity(entity.getClass(),
                     entity.__caucho_getPrimaryKey());
  }

  public Entity getSubEntity(Class cl, Object key)
  {
    Entity []entities = _entities;

    // jpa/0l43
    for (int i = _entitiesTop - 1; i >= 0; i--) {
      Entity entity = entities[i];

      if (entity.__caucho_getPrimaryKey().equals(key)) {
        if (cl.isAssignableFrom(entity.getClass()))
          return entity;
      }
    }

    return null;
  }

  /**
   * Gets the cache item referenced by __caucho_item
   * from an entity of class/subclass cl.
   */
  public EntityItem getSubEntityCacheItem(Class cl, Object key)
  {
    Entity []entities = _entities;

    // jpa/0l4a
    for (int i = _entitiesTop - 1; i >= 0; i--) {
      Entity entity = entities[i];

      if (entity.__caucho_getPrimaryKey().equals(key)) {
        if (cl.isAssignableFrom(entity.getClass()))
          return entity.__caucho_getCacheItem();
      }
    }

    return null;
  }

  public Entity getTransactionEntity(Class cl, Object key)
  {
    Entity []entities = _txEntities;

    for (int i = _txEntitiesTop - 1; i >= 0; i--) {
      Entity entity = entities[i];

      if (entity.__caucho_match(cl, key)) {
        return entity;
      }
    }

    return null;
  }

  public Entity getTransactionEntity(int index)
  {
    return _txEntities[index];
  }

  /**
   * Adds a new entity for the given class name and key.
   * The new entity object is supposed to be used as a
   * copy from cache. This avoids the cache entity to
   * be added to the context.
   *
   * @return null - if the entity is already in the context.
   *         otherwise, it returns the new entity added to
   *         the context.
   */
  public Entity addNewEntity(Class cl, Object key)
    throws InstantiationException, IllegalAccessException
  {
    // jpa/0l43
    Entity entity = getSubEntity(cl, key);

    // If the entity is already in the context, it returns null.
    if (entity != null)
      return null;

    if (_persistenceUnit.isJPA()) {
      // XXX: needs to create based on the discriminator with inheritance.
      // Create a new entity for the given class and primary key.
      entity = (Entity) cl.newInstance();

      // jpa/0s2d
      entity.__caucho_setEntityState(EntityState.P_NON_TRANSACTIONAL);
    }
    else {
      // HelperBean__Amber -> HelperBean
      String className = cl.getSuperclass().getName();

      AmberEntityHome entityHome = _persistenceUnit.getEntityHome(className);

      if (entityHome == null) {
        if (log.isLoggable(Level.FINER))
          log.log(Level.FINER, L.l("Amber.addNewEntity: home not found for entity (class: '{0}' PK: '{1}')",
                                   className, key));
        return null;
      }

      EntityFactory factory = entityHome.getEntityFactory();

      // TestBean__EJB
      Object value = factory.getEntity(key);

      Method cauchoGetBeanMethod = entityHome.getCauchoGetBeanMethod();
      if (cauchoGetBeanMethod != null) {
        try {
          // Bean
          entity = (Entity) cauchoGetBeanMethod.invoke(value, new Object[0]);
          // entity.__caucho_makePersistent(aConn, item);
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);
        }
      }

      if (entity == null) {
        throw new IllegalStateException(L.l("AmberConnection.addNewEntity unable to instantiate new entity with cauchoGetBeanMethod"));
      }
    }

    entity.__caucho_setPrimaryKey(key);

    addInternalEntity(entity);

    return entity;
  }

  /**
   * Adds a new entity for the given class name and key.
   */
  public Entity loadEntity(Class cl,
                           Object key,
                           boolean isEager)
  {
    if (key == null)
      return null;

    // jpa/0l43
    Entity entity = getSubEntity(cl, key);

    // If the entity is already in the context, return it
    if (entity != null)
      return entity;

    if (_persistenceUnit.isJPA()) {
      // XXX: needs to create based on the discriminator with inheritance.
      // Create a new entity for the given class and primary key.
      try {
        entity = (Entity) load(cl, key, isEager);
      } catch (AmberException e) {
        throw new AmberRuntimeException(e);
      }
    }
    else {
      // HelperBean__Amber -> HelperBean
      String className = cl.getSuperclass().getName();

      AmberEntityHome entityHome = _persistenceUnit.getEntityHome(className);

      if (entityHome == null) {
        if (log.isLoggable(Level.FINER))
          log.log(Level.FINER, L.l("Amber.addNewEntity: home not found for entity (class: '{0}' PK: '{1}')",
                                   className, key));
        return null;
      }

      EntityFactory factory = entityHome.getEntityFactory();

      // TestBean__EJB
      Object value = factory.getEntity(key);

      Method cauchoGetBeanMethod = entityHome.getCauchoGetBeanMethod();
      if (cauchoGetBeanMethod != null) {
        try {
          // Bean
          entity = (Entity) cauchoGetBeanMethod.invoke(value, new Object[0]);
          // entity.__caucho_makePersistent(aConn, item);
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);
        }
      }

      if (entity == null) {
        throw new IllegalStateException(L.l("AmberConnection.addNewEntity unable to instantiate new entity with cauchoGetBeanMethod"));
      }

      entity.__caucho_setPrimaryKey(key);

      addInternalEntity(entity);
    }

    return entity;
  }

  /**
   * Removes an entity.
   */
  public boolean removeEntity(Entity entity)
  {
    removeEntityImpl(entity);

    if (isActiveTransaction())
      removeTxEntity(entity);

    return true;
  }

  /**
   * Loads the object based on itself.
   */
  public boolean contains(Object obj)
  {
    if (obj == null)
      return false;

    if (! (obj instanceof Entity))
      throw new IllegalArgumentException(L.l("contains() operation can only be applied to an entity instance."));

    Entity entity = (Entity) obj;

    // jpa/11a8
    if (entity.__caucho_getConnection() != this) {
      return false;
    }

    EntityState state = entity.__caucho_getEntityState();
    if (isInTransaction() && ! state.isTransactional()) {
      // jpa/11a6, jpa/1800
      return false;
    }

    // jpa/0j5f
    if (EntityState.P_DELETING.ordinal() <= state.ordinal()) {
      return false;
    }

    return true;
  }

  /**
   * Callback when the user transaction begins
   */
  public void begin(Transaction xa)
  {
    try {
      xa.registerSynchronization(this);

      _isInTransaction = true;
      _isXA = true;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Starts a transaction.
   */
  public void beginTransaction()
    throws SQLException
  {
    _isInTransaction = true;

    if (_conn != null && _isAutoCommit) {
      _isAutoCommit = false;
      _conn.setAutoCommit(false);
    }

    // _xid = _factory.getXid();
  }

  /**
   * Sets XA.
   */
  public void setXA(boolean isXA)
  {
    _isXA = isXA;
    _isInTransaction = isXA;

    if (isXA && ! _isRegistered)
      register();
  }

  /**
   * Commits a transaction.
   */
  public void commit()
    throws SQLException
  {
    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, "AmberConnection.commit");

    try {
      flushInternal();

      _xid = 0;
      if (_conn != null) {
        _conn.commit();
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (SQLException e) {
      throw new IllegalStateException(e);
    }
    catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
    finally {
      if (! _isXA)
        _isInTransaction = false;

      for (int i = 0; i < _txEntitiesTop; i++) {
        Entity entity = _txEntities[i];

        entity.__caucho_afterCommit();
      }

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, "cleaning up txEntities");

      _txEntitiesTop = 0;
    }
  }

  /**
   * Callback before a utrans commit.
   */
  public void beforeCompletion()
  {
    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, this + " beforeCompletion");

    try {
      beforeCommit();
      // XXX: need to figure out how to throw JPA exceptions at commit() time.
      // } catch (SQLException e) {
      //  throw e;
    } catch (RuntimeException e) {
      // jpa/0ga5
      throw e;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Callback after a utrans commit.
   */
  public void afterCompletion(int status)
  {
    if (log.isLoggable(Level.FINER)) {
      if (status == Status.STATUS_COMMITTED)
        log.finer(this + " afterCompletion(commit)");
      else
        log.finer(this + " afterCompletion(rollback)");
    }

    afterCommit(status == Status.STATUS_COMMITTED);
    _isXA = false;
    _isInTransaction = false;
    _isRegistered = false; // ejb/0d19
  }

  /**
   * Called before the commit phase
   */
  public void beforeCommit()
    throws SQLException
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " beforeCommit");

    try {
      flushInternal();
    } catch (SQLException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    /*
    // jpa/0gh0
    for (int i = _txEntities.size() - 1; i >= 0; i--) {
      Entity entity = _txEntities.get(i);

      // jpa/1500
      if (entity.__caucho_getEntityState() == EntityState.P_DELETED) {
        EntityType entityType = entity.__caucho_getEntityType();
        Object key = entity.__caucho_getPrimaryKey();
        EntityItem item = _persistenceUnit.getEntity(entityType, key);

        if (item == null) {
          // jpa/0ga8: entity has been removed and DELETE SQL was already flushed.
          continue;
        }
      }

      entity.__caucho_flush();
    }
    */
  }

  /**
   * Commits a transaction.
   */
  public void afterCommit(boolean isCommit)
  {
    try {
      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, "AmberConnection.afterCommit: " + isCommit);

      if (! _isXA)
        _isInTransaction = false;

      if (isCommit) {
        if (_completionList.size() > 0) {
          _persistenceUnit.complete(_completionList);
        }
      }

      // jpa/0k20: clears the completion list in the
      // finally block so callbacks do not add a completion
      // which has been just removed.
      //
      // _completionList.clear();

      for (int i = 0; i < _txEntitiesTop; i++) {
        Entity entity = _txEntities[i];

        try {
          if (isCommit)
            entity.__caucho_afterCommit();
          else
            entity.__caucho_afterRollback();
        } catch (Exception e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, "cleaning up txEntities");

      _txEntitiesTop = 0;

      // jpa/0s2k
      Entity []entities = _entities;
      for (int i = _entitiesTop - 1; i >= 0; i--) {
        // XXX: needs to check EXTENDED type.
        // jpa/0h07: persistence context TRANSACTION type.
        entities[i].__caucho_detach();
      }

      // jpa/0h60
      _entitiesTop = 0;

      // if (! isCommit) {
      // jpa/0j5c


      /* XXX: jpa/0k11 - avoids double rollback()
         Rollback is done from com.caucho.transaction.TransactionImpl
         to the pool item com.caucho.jca.PoolItem
         try {
           if (_conn != null)
             _conn.rollback();
         } catch (SQLException e) {
           throw new IllegalStateException(e);
         }
      */
      // }
    } finally {
      _completionList.clear();
    }
  }

  public PersistenceException rollback(Exception e)
  {
    try {
      rollback();
    } catch (Exception e1) {
      log.log(Level.FINE, e1.toString(), e1);
    }

    return new PersistenceException(e);
  }

  /**
   * Rollbacks a transaction.
   */
  public void rollback()
    throws SQLException
  {
    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, "AmberConnection.rollback");

    try {
      flushInternal();

      _xid = 0;
      if (_conn != null) {
        _conn.rollback();
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (SQLException e) {
      throw new IllegalStateException(e);
    }
    catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
    finally {
      if (! _isXA)
        _isInTransaction = false;

      _completionList.clear();

      for (int i = 0; i < _txEntitiesTop; i++) {
        Entity entity = _txEntities[i];

        entity.__caucho_afterRollback();
      }

      _txEntitiesTop = 0;
    }
  }

  /**
   * Flushes managed entities.
   */
  public void flush()
  {
    try {
      checkTransactionRequired("flush");

      flushInternal();
    } catch (RuntimeException e) {
      throw e;
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Flushes managed entities.
   */
  public void flushNoChecks()
  {
    try {
      flushInternal();
    } catch (RuntimeException e) {
      throw e;
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Expires the entities
   */
  public void expire()
    throws SQLException
  {
    Entity []entities = _entities;
    for (int i = _entitiesTop - 1; i >= 0; i--) {
      Entity entity = entities[i];

      // jpa/0j5e
      if (! entity.__caucho_getEntityState().isPersist())
        entity.__caucho_expire();
    }
  }

  /**
   * Returns the connection.
   */
  public Connection getConnection()
    throws SQLException
  {
    DataSource readDataSource = _persistenceUnit.getReadDataSource();

    if (! _isXA && ! _isInTransaction && readDataSource != null) {
      if (_readConn == null) {
        _readConn = readDataSource.getConnection();
      }
      else if (_readConn.isClosed()) {
        closeConnectionImpl();
        _readConn = _persistenceUnit.getDataSource().getConnection();
      }

      return _readConn;
    }

    if (_conn == null) {
      _conn = _persistenceUnit.getDataSource().getConnection();
      _isAutoCommit = true;
    }
    else if (_conn.isClosed()) {
      closeConnectionImpl();
      _conn = _persistenceUnit.getDataSource().getConnection();
      _isAutoCommit = true;
    }

    if (_isXA) {
    }
    else if (_isInTransaction && _isAutoCommit) {
      _isAutoCommit = false;
      _conn.setAutoCommit(false);
    }
    else if (! _isInTransaction && ! _isAutoCommit) {
      _isAutoCommit = true;
      _conn.setAutoCommit(true);
    }

    return _conn;
  }

  /**
   * Prepares a statement.
   */
  public PreparedStatement prepareStatement(String sql)
    throws SQLException
  {
    try {
      PreparedStatement pstmt = _preparedStatementMap.get(sql);

      if (pstmt == null) {
        Connection conn = getConnection();

        // XXX: avoids locking issues.
        if (_statements.size() > 0) {
          conn = _statements.get(0).getConnection();
        }

        // XXX: avoids locking issues.
        // See com.caucho.sql.UserConnection
        pstmt = conn.prepareStatement(sql,
                                      ResultSet.TYPE_FORWARD_ONLY,
                                      ResultSet.CONCUR_READ_ONLY);

        _statements.add(pstmt);

        _preparedStatementMap.put(sql, pstmt);
      }

      return pstmt;
    } catch (SQLException e) {
      closeConnectionImpl();

      throw e;
    }
  }

  /**
   * Closes a statement.
   */
  public void closeStatement(String sql)
    throws SQLException
  {
    PreparedStatement pstmt = _preparedStatementMap.remove(sql);

    if (pstmt != null) {
      _statements.remove(pstmt);

      pstmt.close();
    }
  }

  public static void close(ResultSet rs)
  {
    try {
      if (rs != null)
        rs.close();
    } catch (SQLException e) {
      throw new AmberRuntimeException(e);
    }
  }

  /**
   * Prepares an insert statement.
   */
  public PreparedStatement prepareInsertStatement(String sql,
                                                  boolean isGeneratedId)
    throws SQLException
  {
    PreparedStatement pstmt = null;

    try {
      pstmt = _preparedStatementMap.get(sql);

      if (pstmt != null)
        return pstmt;
      
      Connection conn = getConnection();

      // XXX: avoids locking issues.
      if (_statements.size() > 0) {
        conn = _statements.get(0).getConnection();
      }

      if (isGeneratedId && _persistenceUnit.hasReturnGeneratedKeys())
        pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      else {
        // XXX: avoids locking issues.
        // See com.caucho.sql.UserConnection
        pstmt = conn.prepareStatement(sql);
      }

      _statements.add(pstmt);

      _preparedStatementMap.put(sql, pstmt);

      return pstmt;
    } catch (SQLException e) {
      closeStatement(sql);

      throw e;
    }
  }

  /**
   * Updates the database with the values in object.  If the object does
   * not exist, throws an exception.
   *
   * @param obj the object to update
   */
  public void update(Object obj)
  {
  }

  /**
   * Saves the object.
   *
   * @param obj the object to create
   */
  public void create(Object obj)
    throws SQLException
  {
    // ejb/0g22 exception handling
    try {

      createInternal(obj);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Saves the object.
   *
   * @param obj the object to create
   */
  public void create(String homeName, Object obj)
    throws SQLException
  {
    // ejb/0g22 exception handling
    try {

      createInternal(homeName, obj);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Saves the object.
   *
   * @param obj the object to create
   */
  public void create(AmberEntityHome home, Object obj)
    throws SQLException
  {
    // ejb/0g22 exception handling
    try {

      createInternal(home, obj);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Updates the object.
   */
  public void update(Entity entity)
  {
    if (entity == null)
      return;

    // jpa/0g0i
    if (entity.__caucho_getEntityType() == null)
      return;

    // XXX: also needs to check PersistenceContextType.TRANSACTION/EXTENDED.
    // jpa/0k10
    if (! isActiveTransaction())
      return;

    AmberTable table = entity.__caucho_getEntityType().getTable();

    Object key = entity.__caucho_getPrimaryKey();

    addCompletion(new RowInvalidateCompletion(table.getName(), key));

    // jpa/0ga8, jpa/0s2d if (! _txEntities.contains(entity)) {
    Entity oldEntity = getTransactionEntity(entity.getClass(), key);

    if (oldEntity == null) {
      addTxEntity(entity);
    }
    else {
      // XXX:
      /*
      // jpa/0s2d
      Entity oldEntity = _txEntities.get(index);
      _txEntities.set(index, entity);
      */
    }
  }

  /**
   * Deletes the object.
   *
   * @param obj the object to delete
   */
  public void delete(Entity entity)
    throws SQLException
  {
    Entity oldEntity = getEntity(entity.getClass(),
                                 entity.__caucho_getPrimaryKey());

    if (oldEntity == null) {
      throw new IllegalStateException(L.l("AmberEntity[{0}:{1}] cannot be deleted since it is not managed",
                                          entity.getClass().getName(),
                                          entity.__caucho_getPrimaryKey()));
      /*
        EntityType entityType = entity.__caucho_getEntityType();

        if (entityType == null)
          return;
        // throw new AmberException(L.l("entity has no entityType"));

        AmberEntityHome entityHome = entityType.getHome();
        //entityHome = _persistenceUnit.getEntityHome(entity.getClass().getName());

        if (entityHome == null)
          throw new AmberException(L.l("entity has no matching home"));

        // XXX: this makes no sense
        entityHome.makePersistent(entity, this, true);

        addEntity(entity);
      */
    }
    else {
      // XXX: jpa/0k12
      oldEntity.__caucho_setConnection(this);

      entity = oldEntity;
    }

    entity.__caucho_delete();
  }

  /**
   * Creates a query object from a query string.
   *
   * @param query a Hibernate query
   */
  public AmberQuery prepareQuery(String queryString)
    throws AmberException
  {
    return prepareQuery(queryString, false);
  }

  /**
   * Creates a query object from a query string.
   *
   * @param query a Hibernate query
   */
  public AmberQuery prepareLazyQuery(String queryString)
    throws AmberException
  {
    return prepareQuery(queryString, true);
  }

  /**
   * Creates a query object from a query string.
   *
   * @param query a Hibernate query
   */
  public AmberQuery prepareUpdate(String queryString)
    throws AmberException
  {
    return prepareQuery(queryString, true);
  }

  /**
   * Creates a query object from a query string.
   *
   * @param query a Hibernate query
   */
  private AmberQuery prepareQuery(String queryString, boolean isLazy)
    throws AmberException
  {
    AbstractQuery queryProgram = parseQuery(queryString, isLazy);

    UserQuery query = new UserQuery(queryProgram);

    query.setSession(this);

    return query;
  }

  /**
   * Creates a query object from a query string.
   *
   * @param query a Hibernate query
   */
  public AbstractQuery parseQuery(String sql, boolean isLazy)
    throws AmberException
  {
    try {
      _persistenceUnit.initEntityHomes();
    } catch (Exception e) {
      throw AmberRuntimeException.create(e);
    }

    AbstractQuery query = _persistenceUnit.getQueryParseCache(sql);

    if (query == null) {
      QueryParser parser = new QueryParser(sql);

      parser.setPersistenceUnit(_persistenceUnit);
      parser.setLazyResult(isLazy);

      query = parser.parse();

      _persistenceUnit.putQueryParseCache(sql, query);
    }

    return query;
  }

  /**
   * Select a list of objects with a Hibernate query.
   *
   * @param query the hibernate query
   *
   * @return the query results.
   */
  public ResultSet query(String hsql)
    throws SQLException
  {
    AmberQuery query = prepareQuery(hsql);

    return query.executeQuery();
  }

  /**
   * Returns the cache chunk.
   *
   * @param sql the SQL for the cache chunk
   * @param args the filled parameters for the cache chunk
   * @param startRow the starting row for the cache chunk
   */
  public ResultSetCacheChunk getQueryCacheChunk(String sql,
                                                Object []args,
                                                int startRow)
  {
    _queryKey.init(sql, args, startRow);

    return _persistenceUnit.getQueryChunk(_queryKey);
  }

  /**
   * Returns the result set meta data from cache.
   */
  public ResultSetMetaData getQueryMetaData()
  {
    return _persistenceUnit.getQueryMetaData(_queryKey);
  }

  /**
   * Sets the cache chunk.
   *
   * @param sql the SQL for the cache chunk
   * @param args the filled parameters for the cache chunk
   * @param startRow the starting row for the cache chunk
   * @param cacheChunk the new value of the cache chunk
   */
  public void putQueryCacheChunk(String sql,
                                 Object []args,
                                 int startRow,
                                 ResultSetCacheChunk cacheChunk,
                                 ResultSetMetaData cacheMetaData)
  {
    QueryCacheKey key = new QueryCacheKey();
    Object []newArgs = new Object[args.length];

    System.arraycopy(args, 0, newArgs, 0, args.length);

    key.init(sql, newArgs, startRow);

    _persistenceUnit.putQueryChunk(key, cacheChunk);
    _persistenceUnit.putQueryMetaData(key, cacheMetaData);
  }

  /**
   * Updates the database with a query
   *
   * @param query the hibernate query
   *
   * @return the query results.
   */
  public int update(String hsql)
    throws SQLException
  {
    AmberQuery query = prepareUpdate(hsql);

    return query.executeUpdate();
  }

  /**
   * Select a list of objects with a Hibernate query.
   *
   * @param query the hibernate query
   *
   * @return the query results.
   */
  public List find(String hsql)
    throws SQLException
  {
    AmberQuery query = prepareQuery(hsql);

    return query.list();
  }

  /**
   * Cleans up the connection.
   */
  public void cleanup()
  {
    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, "AmberConnection.cleanup");

    try {
      // XXX: also needs to check PersistenceContextType.TRANSACTION/EXTENDED.
      // jpa/0g04
      if (isActiveTransaction()) {
        flushInternal();
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (SQLException e) {
      throw new IllegalStateException(e);
    }
    catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
    finally {
      _depth = 0;

      for (int i = _entitiesTop - 1; i >= 0; i--) {
        _entities[i].__caucho_detach();
      }

      _entitiesTop = 0;
      _txEntitiesTop = 0;
      _completionList.clear();

      freeConnection();
    }
  }

  /**
   * Pushes the depth.
   */
  public void pushDepth()
  {
    // these aren't necessary because the AmberConnection is added as
    // a close callback to the UserTransaction
  }

  /**
   * Pops the depth.
   */
  public void popDepth()
  {
  }

  /**
   * Frees the connection.
   */
  public void freeConnection()
  {
    closeConnectionImpl();
  }

  /**
   * Frees the connection.
   */
  private void closeConnectionImpl()
  {
    Connection conn = _conn;
    _conn = null;

    Connection readConn = _readConn;
    _readConn = null;

    boolean isAutoCommit = _isAutoCommit;
    _isAutoCommit = true;

    try {
      if (conn != null && ! isAutoCommit)
        conn.setAutoCommit(true);
    } catch (SQLException e) {
    }

    for (Statement stmt : _statements) {
      try {
        stmt.close();
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    try {
      _preparedStatementMap.clear();
      _statements.clear();

      if (conn != null)
        conn.close();

      if (readConn != null)
        readConn.close();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  @Override
  public String toString()
  {
    if (_persistenceUnit != null)
      return "AmberConnection[" + _persistenceUnit.getName() + "]";
    else
      return "AmberConnection[closed]";
  }

  /**
   * Finalizer.
   */
  @Override
  public void finalize()
  {
    cleanup();
  }

  /**
   * Returns true when cache items can be used.
   */
  public boolean shouldRetrieveFromCache()
  {
    // ejb/0d01
    return (! isActiveTransaction());
  }

  public void setTransactionalState(Entity entity)
  {
    if (isActiveTransaction()) {
      // jpa/0ga8
      entity.__caucho_setConnection(this);

      // jpa/0j5f
      EntityState state = entity.__caucho_getEntityState();

      //if (state.ordinal() < EntityState.P_DELETING.ordinal())
      if (state == EntityState.P_NON_TRANSACTIONAL)
        entity.__caucho_setEntityState(EntityState.P_TRANSACTIONAL);
    }
  }

  public boolean isCacheEntity(Entity entity)
  {
    return entity == getCacheEntity(entity, true);
  }

  public Entity getCacheEntity(Entity entity)
  {
    return getCacheEntity(entity, false);
  }

  public Entity getCacheEntity(Entity entity,
                               boolean isDebug)
  {
    // jpa/0h0a

    if (entity == null)
      return null;

    // XXX: jpa/0h20, the cache entity is only available after commit.
    Entity cacheEntity = entity.__caucho_getCacheEntity();

    if (cacheEntity != null)
      return cacheEntity;

    return getCacheEntity(entity.getClass(),
                          entity.__caucho_getPrimaryKey(),
                          isDebug);
  }

  public Entity getCacheEntity(Class cl, Object pk)
  {
    return getCacheEntity(cl, pk, false);
  }

  // jpa/0h20
  public Entity getCacheEntity(Class cl, Object pk, boolean isDebug)
  {
    if (pk == null)
      return null;

    String className = cl.getName();

    AmberEntityHome entityHome = _persistenceUnit.getEntityHome(className);

    if (entityHome == null) {
      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l("Home not found for entity (class: '{0}' PK: '{1}')",
                                 className, pk));
      return null;
    }

    EntityType rootType = entityHome.getRootType();

    EntityItem item = _persistenceUnit.getEntity(rootType, pk);

    if (item == null)
      return null;

    // jpa/0o0b
    if (isDebug)
      return item.getEntity();

    // XXX: jpa/0h31, expires the child cache entity.
    if (isActiveTransaction()) {
      Entity txEntity = getTransactionEntity(cl, pk);

      if (txEntity != null)
        txEntity.__caucho_getEntityState();
      else // jpa/0o0b || ! state.isManaged()) {
        item.getEntity().__caucho_expire();

      return null;
    }

    return item.getEntity();
  }

  //
  // private
  //
  // throws Exception (for jpa)
  //
  // ejb/0g22 (cmp) expects exception handling in
  // the public methods. See public void create(Object) above.

  /**
   * Adds an entity to the context, assuming it has not been added yet.
   * Also, if there is a transaction, adds the entity to the list of
   * transactional entities.
   */
  private void addInternalEntity(Entity entity)
  {
    if (log.isLoggable(Level.FINEST)) {
      log.log(Level.FINEST, L.l("amber {0}[{1}] addInternalEntity",
                                entity.getClass().getName(),
                                entity.__caucho_getPrimaryKey()));
    }

    addEntity(entity);

    // jpa/0g06
    if (isActiveTransaction()) {
      addTxEntity(entity);

      // jpa/0s2d: merge()
      setTransactionalState(entity);
    }
  }

  /**
   * Saves the object.
   *
   * @param obj the object to create
   */
  private void createInternal(Object obj)
    throws Exception
  {
    AmberEntityHome home = null;

    Class cl = obj.getClass();

    for (; home == null && cl != null; cl = cl.getSuperclass()) {
      home = _persistenceUnit.getHome(cl);
    }

    if (home == null)
      throw new AmberException(L.l("'{0}' is not a known entity class.",
                                   obj.getClass().getName()));

    createInternal(home, obj);
  }

  /**
   * Saves the object.
   *
   * @param obj the object to create
   */
  private void createInternal(String homeName, Object obj)
    throws Exception
  {
    AmberEntityHome home = _persistenceUnit.getEntityHome(homeName);

    if (home == null)
      throw new AmberException(L.l("'{0}' is not a known entity class.",
                                   obj.getClass().getName()));

    createInternal(home, obj);
  }

  /**
   * Saves the object.
   *
   * @param obj the object to create
   */
  private void createInternal(AmberEntityHome home, Object obj)
    throws Exception
  {
    // XXX: flushing things like delete might be useful?
    // XXX: the issue is a flush can break FK constraints and
    //      fail prematurely (jpa/0h26).
    // commented out: flushInternal();

    if (contains(obj))
      return;

    Entity entity = (Entity) obj;

    // jpa/0g0k: cannot call home.save because of jpa exception handling.
    if (_persistenceUnit.isJPA()) {
      // See persistInternal(): entity.__caucho_cascadePrePersist(this);

      addEntity(entity);

      // jpa/0ga2
      entity.__caucho_lazy_create(this, home.getEntityType());

      // See persistInternal(): entity.__caucho_cascadePostPersist(this);
    }
    else
      home.save(this, entity);

    // jpa/0h25
    // XXX: not correct, since we need to keep the P_PERSIST state around
    // and P_PERSIST is a transactional state
    // setTransactionalState(entity);

    // jpa/0g0i
    AmberTable table = home.getEntityType().getTable();
    addCompletion(new RowInsertCompletion(table.getName()));
  }

  private void checkTransactionRequired(String operation)
    throws TransactionRequiredException, SQLException
  {
    // XXX: also needs to check PersistenceContextType.TRANSACTION/EXTENDED.

    if (! (_isXA || isActiveTransaction()))
      throw new TransactionRequiredException(L.l("{0}() operation can only be executed in the scope of a transaction or with an extended persistence context.", operation));
  }

  private Entity checkEntityType(Object entity, String operation)
  {
    if (! (entity instanceof Entity))
      throw new IllegalArgumentException(L.l("{0}() operation can only be applied to an entity instance. If the argument is an entity, the corresponding class must be specified in the scope of a persistence unit.", operation));

    if (_persistenceUnit.isJPA()) {
      String className = entity.getClass().getName();

      EntityType entityType
        = (EntityType) _persistenceUnit.getEntityType(className);

      // jpa/0m08
      if (entityType == null) {
        throw new IllegalArgumentException(L.l("{0}() operation can only be applied to an entity instance. If the argument is an entity, the class '{1}' must be specified in the orm.xml or annotated with @Entity and must be in the scope of a persistence unit.", operation, className));
      }
    }

    return (Entity) entity;
  }

  /**
   * Detach after non-xa.
   */
  public void detach()
  {
    if (_isXA || _isInTransaction)
      throw new IllegalStateException(L.l("detach cannot be called within transaction"));

    _completionList.clear();

    _txEntitiesTop = 0;

    // jpa/1700
    for (int i = _entitiesTop - 1; i >= 0; i--) {
      _entities[i].__caucho_detach();
    }

    // jpa/0o0d
    _entitiesTop = 0;
  }

  /**
   * Flush managed entities.
   */
  private void flushInternal()
    throws Exception
  {
    // Do not flush within merge() or while loading an entity.
    if (! _isFlushAllowed)
      return;

    /* XXX: moved into __caucho_flush
       for (int i = _txEntities.size() - 1; i >= 0; i--) {
         Entity entity = _txEntities.get(i);

         EntityState state = entity.__caucho_getEntityState();

         // jpa/0i60
         // jpa/0h27: for all entities Y referenced by a *managed*
         // entity X, where the relationship has been annotated
         // with cascade=PERSIST/ALL, the persist operation is
         // applied to Y. It is a lazy cascade as the relationship
         // is not always initialized at the time persist(X) was
         // called but must be at flush time.

         if (state == EntityState.P_PERSIST) {
           entity.__caucho_cascadePrePersist(this);
           entity.__caucho_cascadePostPersist(this);
         }
       }
    */

    // We avoid breaking FK constraints:
    //
    // 1. Assume _txEntities has the following order: A <- B <- C
    //    XXX: Make sure priorities are handled based on owning sides
    //    even when there are cycles in a graph.
    //
    // 2. Persist is done in ascending order: A(0) <- B(1) <- C(2)
    //
    // 3. Delete is done in descending order: C(2) -> B(1) -> A(0)

    // Persists in ascending order.
    for (int i = 0; i < _txEntitiesTop; i++) {
      Entity entity = _txEntities[i];

      if (entity.__caucho_getEntityState().isPersist()) {
        try {
          entity.__caucho_flush();
        } catch (SQLException e) {
          throwPersistException(e, entity);
        }
      }
    }

    // jpa/0h25
    // Deletes in descending order.
    for (int i = _txEntitiesTop - 1; i >= 0; i--) {
      Entity entity = _txEntities[i];

      if (! entity.__caucho_getEntityState().isPersist()) {
        entity.__caucho_flush();
      }
    }

    if (! isInTransaction()) {
      if (_completionList.size() > 0) {
        _persistenceUnit.complete(_completionList);
      }
      _completionList.clear();

      for (int i = 0; i < _txEntitiesTop; i++) {
        Entity entity = _txEntities[i];

        entity.__caucho_afterCommit();
      }

      _txEntitiesTop = 0;
    }
  }

  private void throwPersistException(SQLException e, Entity entity)
    throws SQLException
  {
    log.log(Level.FINER, e.toString(), e);

    String sqlState = e.getSQLState();

    JdbcMetaData metaData = _persistenceUnit.getMetaData();

    if (metaData.isUniqueConstraintSQLState(sqlState)) {
      // jpa/0ga5
      throw new EntityExistsException(L.l("Trying to persist an entity '{0}[{1}]' that already exists. Entity state '{2}'", entity.getClass().getName(), entity.__caucho_getPrimaryKey(), entity.__caucho_getEntityState()));
    }
    else if (metaData.isForeignKeyViolationSQLState(sqlState)) {
      // jpa/0o42
      throw new IllegalStateException(L.l("Trying to persist an entity of class '{0}' with PK '{1}' would break a foreign key constraint. The entity state is '{2}'. Please make sure there are associated entities for all required relationships. If you are merging an entity make sure the association fields are annotated with cascade=MERGE or cascade=ALL.", entity.getClass().getName(), entity.__caucho_getPrimaryKey(), entity.__caucho_getEntityState()));
    }

    throw e;
  }
  
  /**
   * Persists the entity.
   */
  private Entity persistInternal(Entity entity)
    throws Exception
  {
    EntityState state = entity.__caucho_getEntityState();

    if (state == null)
      state = EntityState.TRANSIENT;

    switch (state) {
    case TRANSIENT:
      {
        Entity contextEntity = getEntity(entity.getClass(),
                                         entity.__caucho_getPrimaryKey());

        // jpa/0ga3
        if (contextEntity == null) {
        }
        else if (contextEntity.__caucho_getEntityState().isDeleting()) {
          // jpa/0ga3
          contextEntity.__caucho_flush();
        }
        else if (entity != contextEntity) {
          return contextEntity;
          /*
          // jpa/0ga1: trying to persist a detached entity that already exists.
          throw new EntityExistsException(L.l("Trying to persist a detached entity of class '{0}' with PK '{1}' that already exists. Entity state '{2}'", entity.getClass().getName(), entity.__caucho_getPrimaryKey(), state));
          */
        }

        // jpa/0h24
        // Pre-persist child entities.
        entity.__caucho_cascadePrePersist(this);

        createInternal(entity);
      }
      break;

    case P_DELETING:
    case P_DELETED:
      {
        // jpa/0i60, jpa/1510, jpa/0h25
        // jpa/0h26
        entity.__caucho_cascadePrePersist(this);

        // removed entity instance, reset state and persist.
        entity.__caucho_makePersistent(null, (EntityType) null);
        createInternal(entity);
      }
      break;

    case P_PERSISTING:
    case P_PERSISTED:
      {
        // jpa/0h26
        // Pre-persist child entities.
        entity.__caucho_cascadePrePersist(this);
      }
      break;

    default:
      if (entity.__caucho_getConnection() == this)
        return entity;
      else {
        // jpa/0ga5 (tck):
        // See entitytest.persist.basic.persistBasicTest4 vs.
        //     callback.inheritance.preUpdateTest
        throw new EntityExistsException(L.l("Trying to persist an entity that is detached or already exists. Entity state '{0}'", state));
      }
    }

    // jpa/0j5e
    updateFlushPriority(entity);

    // jpa/0h27, jpa/0i5c, jpa/0j5g
    // Post-persist child entities.
    entity.__caucho_cascadePostPersist(this);

    return entity;
  }

  /**
   * Updates flush priorities.
   */
  private void updateFlushPriority(Entity updateEntity)
  {
    if (! isActiveTransaction())
      return;

    removeTxEntity(updateEntity);

    int updatePriority
      = updateEntity.__caucho_getEntityType().getFlushPriority();

    for (int i = _txEntitiesTop - 1; i >= 0; i--) {
      Entity entity = _txEntities[i];

      int currentPriority = entity.__caucho_getEntityType().getFlushPriority();

      if (currentPriority < updatePriority) {
        addTxEntity(i + 1, updateEntity);
        return;
      }
    }

    addTxEntity(0, updateEntity);
  }

  /**
   * Recursively merges the state of the entity into the current context.
   */
  public <T> T recursiveMerge(T entityT)
  {
    // jpa/0ga3, jpa/0h08, jpa/0i5g, jpa/0s2k

    try {
      if (entityT == null)
        return null;

      Entity entity = checkEntityType(entityT, "merge");

      if (log.isLoggable(Level.FINER)) {
        String className = entity.getClass().getName();
        Object pk = entity.__caucho_getPrimaryKey();
        EntityState state = entity.__caucho_getEntityState();

        log.finer(L.l("recursiveMerge({0}[{1}] state: '{2}'",
                      className, pk, state));
      }

      if (containsMergingEntity(entity))
        return (T) entity;
      else
        return (T) mergeDetachedEntity(entity);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  public Entity mergeDetachedEntity(Entity newEntity)
  {
    try {
      if (newEntity == null)
        return newEntity;

      Class entityClass = newEntity.getClass();
      String className = newEntity.getClass().getName();
      EntityState state = newEntity.__caucho_getEntityState();

      Object pk = newEntity.__caucho_getPrimaryKey();

      if (log.isLoggable(Level.FINER))
        log.finer(L.l("{0}[{1}] amber merge state='{2}'",
                      entityClass.getSimpleName(), pk, state));

      if (state.isDeleting()) {
        // removed entity instance
        throw new IllegalArgumentException(L.l("{0}: merge operation cannot be applied to a removed entity instance",
                                               entityClass));
      }

      // XXX: jpa/0o42 try {

      Entity existingEntity = null;

      try {
        existingEntity = (Entity) load(entityClass, pk, true);
      } catch (AmberObjectNotFoundException e) {
        if (log.isLoggable(Level.FINER))
          log.log(Level.FINER, e.toString(), e);
        // JPA: should not throw at all, returns null only.
      }
      
      if (existingEntity != null) {
        if (containsMergingEntity(existingEntity))
            return existingEntity;

        _mergingEntities.add(existingEntity);
        
        existingEntity.__caucho_mergeFrom(this, newEntity);
              
        return existingEntity;
      }

      // XXX: the original entity should remain detached jpa/0s2k
      // setTransactionalState(entity);

      // new entity instance
      persist(newEntity);
        
      return newEntity;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Creates an instance of the named query
   */
  private Query createInternalNativeQuery(String sql)
  {
    try {
      QueryImpl query = new QueryImpl(this);

      query.setNativeSql(sql);

      return query;
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(e);
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }

  /**
   * Creates an instance of the native query.
   */
  private Query createInternalNativeQuery(String sql,
                                          SqlResultSetMappingConfig map)
  {
    Query query = createInternalNativeQuery(sql);

    QueryImpl queryImpl = (QueryImpl) query;

    queryImpl.setSqlResultSetMapping(map);

    return query;
  }

  private boolean containsMergingEntity(Entity entity)
  {
    if (_mergingEntities.contains(entity))
      return true;

    // jpa/0o42
    int index = getEntityMatch(_mergingEntities,
                               entity.getClass(),
                               entity.__caucho_getPrimaryKey());

    return (index >= 0);
  }

  public void addEntity(Entity entity)
  {
    Entity []entities = _entities;

    if (_entitiesTop == entities.length) {
      entities = new Entity[_entities.length + 32];
      System.arraycopy(_entities, 0, entities, 0, _entities.length);
      _entities = entities;
    }

    entities[_entitiesTop++] = entity;
  }

  private void removeEntityImpl(Entity entity)
  {
    Entity []entities = _entities;

    for (int i = _entitiesTop - 1; i >= 0; i--) {
      if (entities[i] == entity) {
        System.arraycopy(entities, i + 1, entities, i,
                         _entitiesTop - i - 1);

        _entitiesTop -= 1;

        return;
      }
    }
  }

  private void addTxEntity(Entity entity)
  {
    Entity []entities = _txEntities;

    if (_txEntitiesTop == entities.length) {
      entities = new Entity[entities.length + 32];
      System.arraycopy(_txEntities, 0, entities, 0, _txEntities.length);
      _txEntities = entities;
    }

    entities[_txEntitiesTop++] = entity;
  }

  private void removeTxEntity(Entity entity)
  {
    Entity []entities = _txEntities;

    for (int i = _txEntitiesTop - 1; i >= 0; i--) {
      if (entities[i] == entity) {
        System.arraycopy(entities, i + 1, entities, i,
                         _txEntitiesTop - i - 1);

        _txEntitiesTop -= 1;

        return;
      }
    }
  }

  private void addTxEntity(int index, Entity entity)
  {
    Entity []entities = _txEntities;

    if (_txEntitiesTop == entities.length) {
      entities = new Entity[_txEntities.length + 32];
      System.arraycopy(_txEntities, 0, entities, 0, _txEntities.length);
      _txEntities = entities;
    }

    if (index < _txEntitiesTop)
      System.arraycopy(entities, index, entities, index + 1,
                       _txEntitiesTop - index);

    entities[index] = entity;

    _txEntitiesTop += 1;
  }

  private static int getEntityMatch(ArrayList<Entity> list,
                                    Class cl,
                                    Object key)
  {
    // See also: getEntity() and getTransactionEntity().

    // jpa/0o42
    for (int i = list.size() - 1; i >= 0; i--) {
      Entity entity = list.get(i);

      if (entity.__caucho_match(cl, key)) {
        return i;
      }
    }

    return -1;
  }

  private class EntityTransactionImpl implements EntityTransaction {
    private boolean _rollbackOnly;

    /**
     * Starts a resource transaction.
     */
    public void begin()
    {
      // jpa/1522
      if (isActiveTransaction())
        throw new IllegalStateException("begin() cannot be called when the entity transaction is already active.");

      _rollbackOnly = false;

      try {
        AmberConnection.this.beginTransaction();
      } catch (SQLException e) {
        throw new PersistenceException(e);
      }
    }

    /**
     * Commits a resource transaction.
     */
    public void commit()
    {
      // jpa/1523
      if (! isActiveTransaction())
        throw new IllegalStateException("commit() cannot be called when the entity transaction is not active.");

      // jpa/1525
      if (getRollbackOnly())
        throw new RollbackException("commit() cannot be called when the entity transaction is marked for rollback only.");

      try {
        // jpa/11a7
        AmberConnection.this.beforeCommit();

        _isInTransaction = false;

        // jpa/11a7 AmberConnection.this.commit();
        if (AmberConnection.this._conn != null) {
          AmberConnection.this._conn.commit();
        }

        // XXX: missing finally issues if _conn.commit fails

        // jpa/11a7
        AmberConnection.this.afterCommit(true);

        if (AmberConnection.this._conn != null) {
          closeConnectionImpl();
        }
      } catch (SQLException e) {
        throw new PersistenceException(e);
      }
    }

    /**
     * Rolls the current transaction back.
     */
    public void rollback()
    {
      // jpa/1524
      if (! isActiveTransaction())
        throw new IllegalStateException("rollback() cannot be called when the entity transaction is not active.");

      setRollbackOnly();

      PersistenceException exn = null;

      try {
        AmberConnection.this.rollback();
      } catch (Exception e) {
        exn = new PersistenceException(e);
      } finally {
        try {
          // jpa/1501
          AmberConnection.this.afterCommit(false);
        } catch (PersistenceException e) {
          exn = e;
        } catch (Exception e) {
          exn = new PersistenceException(e);
        } finally {
          // jpa/1525
          if (AmberConnection.this._conn != null) {
            closeConnectionImpl();
          }
        }
      }

      if (exn != null)
        throw exn;
    }

    /**
     * Marks the current transaction for rollback only.
     */
    public void setRollbackOnly()
    {
      // jpa/1521
      if (! isActiveTransaction())
        throw new IllegalStateException("setRollbackOnly() cannot be called when the entity transaction is not active.");

      _rollbackOnly = true;
    }

    /**
     * Returns true if the transaction is for rollback only.
     */
    public boolean getRollbackOnly()
    {
      // jpa/1520
      if (! isActiveTransaction())
        throw new IllegalStateException("getRollbackOnly() cannot be called when the entity transaction is not active.");

      return _rollbackOnly;
    }

    /**
     * Test if a transaction is in progress.
     */
    public boolean isActive()
    {
      return _isInTransaction;
    }
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
