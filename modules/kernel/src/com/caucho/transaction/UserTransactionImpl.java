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

package com.caucho.transaction;

import com.caucho.util.L10N;

import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;
import javax.transaction.*;
import javax.transaction.xa.Xid;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the UserTransactionImpl for a thread instance.
 */
public class UserTransactionImpl
  implements UserTransaction
{
  private static final Logger log
    = Logger.getLogger(UserTransactionImpl.class.getName());
  private static final L10N L = new L10N(UserTransactionImpl.class);

  private TransactionManagerImpl _transactionManager;

  private ArrayList<UserPoolItem> _resources = new ArrayList<UserPoolItem>();
  private ArrayList<ManagedPoolItem> _poolItems = new ArrayList<ManagedPoolItem>();
  private ArrayList<BeginResource> _beginResources
    = new ArrayList<BeginResource>();
  private ArrayList<CloseResource> _closeResources
    = new ArrayList<CloseResource>();

  private boolean _isInContext;
  private boolean _isTransactionActive;
  
  /**
   * Creates the proxy.
  */
  public UserTransactionImpl(TransactionManagerImpl tm)
  {
    _transactionManager = tm;
  }
  
  /**
   * Sets the transaction's timeout.
   */
  @Override
  public void setTransactionTimeout(int seconds)
    throws SystemException
  {
    _transactionManager.setTransactionTimeout(seconds);
  }
  
  /**
   * Gets the transaction's status
   */
  @Override
  public int getStatus()
    throws SystemException
  {
    return _transactionManager.getStatus();
  }

  /**
   * inContext is valid within a managed UserTransactionImpl context, e.g
   * in a webApp, but not in a cron job.
   */
  public boolean isInContext()
  {
    return _isInContext;
  }

  /**
   * inContext is valid within a managed UserTransactionImpl context, e.g
   * in a webApp, but not in a cron job.
   */
  public void setInContext(boolean isInContext)
  {
    _isInContext = isInContext;
  }

  /**
   * Enlist a resource.
   */
  void enlistResource(UserPoolItem resource)
    throws SystemException, RollbackException
  {
    if (_resources.contains(resource))
      return;
    
    TransactionImpl xa = _transactionManager.getTransaction();
    if (xa != null && xa.isActive()) {
      ManagedPoolItem poolItem = resource.getXAPoolItem();

      enlistPoolItem(xa, poolItem);
    }
    
    _resources.add(resource);
  }

  private void enlistPoolItem(Transaction xa, ManagedPoolItem poolItem)
    throws SystemException, RollbackException
  {
    if (poolItem == null)
      return;
    else if (! poolItem.supportsTransaction()) {
      // server/164j
      return;
    }
    
    // XXX: new
    if (_poolItems.contains(poolItem))
      return;
    
    poolItem.setTransaction(this);

    if (xa instanceof TransactionImpl) {
      TransactionImpl xaImpl = (TransactionImpl) xa;
      
      // server/164l
      if (xaImpl.allowLocalTransactionOptimization())
        poolItem.enableLocalTransactionOptimization(true);
    }

    if (poolItem.getXid() == null)
      xa.enlistResource(poolItem);
    
    _poolItems.add(poolItem);
  }

  /**
   * Delist a pool item
   */
  void delistPoolItem(ManagedPoolItem poolItem, int flags)
    throws SystemException, RollbackException
  {
    Transaction xa = _transactionManager.getTransaction();

    try {
      if (xa != null)
        xa.delistResource(poolItem, flags);
    } finally {
      _poolItems.remove(poolItem);
    }
  }

  /**
   * Delist a resource.
   */
  void delistResource(UserPoolItem resource)
  {
    _resources.remove(resource);
  }

  /**
   * Enlist a resource automatically called when a transaction begins
   */
  public void enlistBeginResource(BeginResource resource)
  {
    _beginResources.add(resource);

    try {
      Transaction xa = _transactionManager.getTransaction();
      if (xa != null)
        resource.begin(xa);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Enlist a resource automatically closed when the context ends.
   */
  public void enlistCloseResource(CloseResource resource)
  {
    _closeResources.add(resource);
  }

  /**
   * Allocates a resource matching the parameters.  If none matches,
   * return null.
   */
  UserPoolItem allocate(ManagedConnectionFactory mcf,
                        Subject subject,
                        ConnectionRequestInfo info)
  {
    if (! _isTransactionActive)
      return null;
    
    ArrayList<ManagedPoolItem> poolItems = _poolItems;
    int length = poolItems.size();
    
    for (int i = 0; i < length; i++) {
      ManagedPoolItem poolItem = poolItems.get(i);

      UserPoolItem item = poolItem.allocateXA(mcf, subject, info);

      if (item != null)
        return item;
    }

    return null;
  }

  /**
   * Finds the pool item joined to this one.
   * return null.
   */
  ManagedPoolItem findJoin(ManagedPoolItem item)
  {
    if (! _isTransactionActive)
      return null;
    
    ArrayList<ManagedPoolItem> poolItems = _poolItems;
    int length = poolItems.size();
    
    for (int i = 0; i < length; i++) {
      ManagedPoolItem poolItem = poolItems.get(i);

      if (poolItem.isJoin(item))
        return poolItem;
    }

    return null;
  }

  /**
   * Returns the XID.
   */
  public Xid getXid()
    throws SystemException, RollbackException
  {
    TransactionImpl xa = (TransactionImpl) _transactionManager.getTransaction();

    if (xa != null)
      return xa.getXid();
    else
      return null;
  }

  /**
   * Returns the XID.
   */
  public int getEnlistedResourceCount()
    throws SystemException, RollbackException
  {
    TransactionImpl xa = (TransactionImpl) _transactionManager.getTransaction();

    if (xa != null)
      return xa.getEnlistedResourceCount();
    else
      return 0;
  }
  
  /**
   * Start the transaction.
   */
  @Override
  public void begin()
    throws NotSupportedException, SystemException
  {
    if (_isTransactionActive)
      throw new NotSupportedException(L.l("UserTransaction.begin() is not allowed because an active transaction already exists.  This may be caused by either a missing commit/rollback or a nested begin().  Nested transactions are not supported."));
    
    _transactionManager.begin();
    _isTransactionActive = true;
    boolean isOkay = false;

    try {
      TransactionImpl xa = (TransactionImpl) _transactionManager.getTransaction();
      xa.setUserTransaction(this);
    
      _poolItems.clear();
    
      // enlist "cached" connections
      int length = _resources.size();

      for (int i = 0; i < length; i++) {
        UserPoolItem userPoolItem = _resources.get(i);

        for (int j = _poolItems.size() - 1; j >= 0; j--) {
          ManagedPoolItem poolItem = _poolItems.get(j);

          if (poolItem.share(userPoolItem)) {
            break;
          }
        }

        ManagedPoolItem xaPoolItem = userPoolItem.getXAPoolItem();
        if (! _poolItems.contains(xaPoolItem))
          _poolItems.add(xaPoolItem);
      }

      for (int i = 0; i < _poolItems.size(); i++) {
        ManagedPoolItem poolItem = _poolItems.get(i);

        poolItem.enableLocalTransactionOptimization(_poolItems.size() == 1);

        try {
          xa.enlistResource(poolItem);
        } catch (Exception e) {
          String message = L.l("Failed to begin UserTransaction due to: {0}", e);
          log.log(Level.SEVERE, message, e);

          throw new SystemException(message);
        }
      }

      // enlist begin resources
      for (int i = 0; i < _beginResources.size(); i++) {
        try {
          BeginResource resource = _beginResources.get(i);

          resource.begin(xa);
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }

      isOkay = true;
    } finally {
      if (! isOkay) {
        Exception e1 = new IllegalStateException(L.l("Rolling back transaction from failed begin."));
        e1.fillInStackTrace();
        log.log(Level.WARNING, e1.toString(), e1);

        // something has gone very wrong
        _isTransactionActive = false;

        ArrayList<ManagedPoolItem> recoveryList = new ArrayList<ManagedPoolItem>(_poolItems);
        _poolItems.clear();
        _resources.clear();

        for (int i = 0; i < recoveryList.size(); i++) {
          try {
            ManagedPoolItem item = recoveryList.get(i);

            item.abortConnection();

            item.destroy();
          } catch (Throwable e) {
            log.log(Level.FINE, e.toString(), e);
          }
        }

        _transactionManager.rollback();
      }
    }
  }

  /**
   * Suspends the transaction.
   */
  public UserTransactionSuspendState userSuspend()
  {
    if (! _isTransactionActive)
      throw new IllegalStateException(L.l("UserTransaction.suspend may only be called in a transaction, but no transaction is active."));

    _isTransactionActive = false;
    
    UserTransactionSuspendState state;
    state = new UserTransactionSuspendState(_poolItems);
    _poolItems.clear();

    return state;
  }

  /**
   * Resumes the transaction.
   */
  public void userResume(UserTransactionSuspendState state)
  {
    if (_isTransactionActive)
      throw new IllegalStateException(L.l("UserTransaction.resume may only be called outside of a transaction, because the resumed transaction must not conflict with an active transaction."));

    _isTransactionActive = true;

    _poolItems.addAll(state.getPoolItems());
  }
  
  /**
   * Marks the transaction as rollback only.
   */
  @Override
  public void setRollbackOnly()
    throws IllegalStateException, SystemException
  {
    _transactionManager.setRollbackOnly();
  }
  
  /**
   * Marks the transaction as rollback only.
   */
  public void setRollbackOnly(Exception e)
    throws IllegalStateException
  {
    _transactionManager.setRollbackOnly(e);
  }
  
  /**
   * Commits the transaction
   */
  @Override
  public void commit()
    throws IllegalStateException, RollbackException, HeuristicMixedException,
    HeuristicRollbackException, SecurityException, SystemException
  {
    try {
      // XXX: interaction with hessian XA
      if (! _isTransactionActive)
        throw new IllegalStateException("UserTransaction.commit() requires an active transaction.  Either the UserTransaction.begin() is missing or the transaction has already been committed or rolled back.");

      _transactionManager.commit();
    } finally {
      _poolItems.clear();

      _isTransactionActive = false;
    }
  }
  
  /**
   * Rolls the transaction back
   */
  @Override
  public void rollback()
    throws IllegalStateException, SecurityException, SystemException
  {
    try {
      _transactionManager.rollback();
    } finally {
      _isTransactionActive = false;
      
      _poolItems.clear();
    }
  }

  /**
   * Aborts the transaction.
   */
  public void abortTransaction()
    throws IllegalStateException
  {
    IllegalStateException exn = null;

    _isInContext = false;
    
    boolean isTransactionActive = _isTransactionActive;
    _isTransactionActive = false;

    if (! isTransactionActive && _poolItems.size() > 0) {
      Exception e = new IllegalStateException("Internal error: user transaction pool broken because poolItems exist, but no transaction is active.");
      log.log(Level.WARNING, e.toString(), e);
    }
    
    _poolItems.clear();

    if (isTransactionActive) {
      try {
        exn = new IllegalStateException(L.l("Transactions must have a commit() or rollback() in a finally block."));

        log.warning("Rolling back dangling transaction.  All transactions must have a commit() or rollback() in a finally block.");

        _transactionManager.rollback();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString());
      }

    }

    _beginResources.clear();

    while (_closeResources.size() > 0) {
      try {
        CloseResource resource;

        resource = _closeResources.remove(_closeResources.size() - 1);
        resource.close();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    boolean hasWarning = false;

    while (_resources.size() > 0) {
      UserPoolItem userPoolItem = _resources.remove(_resources.size() - 1);

      if (! userPoolItem.isCloseDanglingConnections())
        continue;

      if (! hasWarning) {
        hasWarning = true;

        log.warning("Closing dangling connections.  All connections must have a close() in a finally block.");
      }

      try {
        IllegalStateException stackTrace = userPoolItem.getAllocationStackTrace();

        if (stackTrace != null)
          log.log(Level.WARNING, stackTrace.getMessage(), stackTrace);
        else {
          // start saving the allocation stack trace.
          userPoolItem.setSaveAllocationStackTrace(true);
        }

        if (exn == null)
          exn = new IllegalStateException(L.l("Connection {0} was not closed. Connections must have a close() in a finally block.",
                                              userPoolItem.getUserConnection()));

        userPoolItem.abortConnection();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    _poolItems.clear();

    try {
      _transactionManager.setTransactionTimeout(0);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    if (exn != null)
      throw exn;
  }
}

