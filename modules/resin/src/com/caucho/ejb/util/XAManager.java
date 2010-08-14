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
package com.caucho.ejb.util;

import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRequiredException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.ejb.SessionSynchronization;
import javax.ejb.TransactionRolledbackLocalException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import com.caucho.ejb.manager.EjbManager;
import com.caucho.transaction.TransactionImpl;
import com.caucho.transaction.TransactionManagerImpl;
import com.caucho.transaction.UserTransactionProxy;
import com.caucho.util.L10N;

/**
 * Manages XA for bean methods.
 */
public class XAManager {
  private static L10N L = new L10N(XAManager.class);

  private UserTransactionProxy _ut;
  private EjbManager _ejbManager;

  public XAManager()
  {
    _ut = UserTransactionProxy.getInstance();
    _ejbManager = EjbManager.getCurrent();
  }

  /**
   * Enlists a resource
   */
  public void enlist(XAResource xaResource)
  {
    try {
      TransactionManagerImpl tm = TransactionManagerImpl.getLocal();

      Transaction xa = tm.getTransaction();

      if (xa != null && xaResource != null)
        xa.enlistResource(xaResource);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBException(e);
    }
  }

  /**
   * Enlists a resource
   */
  public void registerSynchronization(SessionSynchronization sync)
  {
    try {
      TransactionManagerImpl tm = TransactionManagerImpl.getLocal();

      Transaction xa = tm.getTransaction();

      if (xa != null && sync != null) {
        sync.afterBegin();

        xa.registerSynchronization(new SynchronizationAdapter(sync));
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBException(e);
    }
  }

  /**
   * Begins a mandatory transaction.
   */
  public void beginMandatory()
  {
    try {
      TransactionManagerImpl tm = TransactionManagerImpl.getLocal();

      Transaction xa = tm.getTransaction();

      if (xa == null)
        throw new EJBTransactionRequiredException(L
            .l("Transaction required for 'Mandatory' transaction attribute"));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBException(e);
    }
  }

  /**
   * Begins a never transaction.
   */
  public void beginNever()
  {
    try {
      TransactionManagerImpl tm = TransactionManagerImpl.getLocal();

      Transaction xa = tm.getTransaction();

      if (xa != null)
        throw new EJBException(L
            .l("Transaction forbidden for 'Never' transaction attribute"));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBException(e);
    }
  }

  /**
   * Begins a required transaction.
   * 
   * @return the current transaction if it exists
   */
  public Transaction beginRequired()
  {
    try {
      TransactionManagerImpl tm = TransactionManagerImpl.getLocal();

      Transaction xa = tm.getTransaction();

      if (xa != null)
        return xa;

      _ut.begin();

      return null;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBException(e);
    }
  }

  /**
   * Begins a requires-new transaction.
   * 
   * @return the current transaction if it exists
   */
  public Transaction beginRequiresNew()
  {
    try {
      TransactionManagerImpl tm = TransactionManagerImpl.getLocal();

      Transaction xa = tm.suspend();

      _ut.begin();

      return xa;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBException(e);
    }
  }

  /**
   * Begins a requires-new transaction.
   * 
   * @return the current transaction if it exists
   */
  public void endRequiresNew(Transaction parent)
  {
    try {
      TransactionImpl xa = getTransaction();
      
      if (xa != null && xa.isRollbackOnly())
        _ut.rollback();
      else
        _ut.commit();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBException(e);
    } finally {
      try {
        if (parent != null) {
          TransactionManagerImpl tm = TransactionManagerImpl.getLocal();

          tm.resume(parent);
        }
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new EJBException(e);
      }
    }
  }

  /**
   * Begins a not-supported transaction, i.e. suspend any current transaction.
   * 
   * @return the current transaction if it exists
   */
  public Transaction beginNotSupported()
  {
    try {
      TransactionManagerImpl tm = TransactionManagerImpl.getLocal();

      return tm.suspend();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBException(e);
    }
  }

  /**
   * Gets the active transaction.
   * 
   * @return The current transaction if it exists.
   */
  public TransactionImpl getTransaction()
  {
    try {
      TransactionManagerImpl tm = TransactionManagerImpl.getLocal();

      return tm.getTransaction();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBException(e);
    }
  }

  public boolean systemException(RuntimeException e)
  {
    TransactionImpl xa = getTransaction();
    
    AppExceptionItem appExn = null;
    
    if (_ejbManager != null)
      appExn = _ejbManager.getSystemException(e.getClass());

    if (appExn == null || appExn.isRollback()) {
      if (xa != null)
        xa.setRollbackOnly(e);
      
      return appExn == null;
    }
    else
      return false;
  }

  public void applicationException(Exception e)
  {
    TransactionImpl xa = getTransaction();
    
    if (xa == null)
      return;
    
    AppExceptionItem appExn = null;
    
    if (_ejbManager != null)
      appExn = _ejbManager.getApplicationException(e.getClass());
    
    if (appExn != null && appExn.isRollback()) {
      xa.setRollbackOnly(e);
    }
  }
  
  /**
   * Mark the transaction for rollback
   */
  public void markRollback(Exception e)
  {
    _ut.setRollbackOnly(e);
  }

  /**
   * Mark the transaction for rollback
   */
  public void markRollback()
  {
    try {
      if (_ut.getStatus() == Status.STATUS_ACTIVE)
        _ut.setRollbackOnly();
    } catch (SystemException e) {
      throw new IllegalStateException(e);
    }
  }
  
  public void rethrowEjbException(Exception e, boolean isClientXa)
  {
    if (isClientXa) {
      throw new EJBTransactionRolledbackException(e.getMessage(), e);
    }
    else
      throw new EJBException(e);
  }

  /**
   * Commits transaction.
   */
  public void commit()
  {
    try {
      TransactionImpl xa = getTransaction();
      
      if (xa != null && xa.isRollbackOnly())
        _ut.rollback();
      else
        _ut.commit();
    } catch (RuntimeException e) {
      throw e;
    } catch (RollbackException e) {
      throw new TransactionRolledbackLocalException(e.getMessage(), e);
    } catch (HeuristicMixedException e) {
      throw new TransactionRolledbackLocalException(e.getMessage(), e);
    } catch (HeuristicRollbackException e) {
      throw new TransactionRolledbackLocalException(e.getMessage(), e);
    } catch (Exception e) {
      throw new EJBException(e);
    }
  }

  /**
   * Commits transaction.
   */
  public void rollback()
  {
    try {
      TransactionImpl xa = getTransaction();
      
      if (xa != null)
        _ut.rollback();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBException(e);
    }
  }

  /**
   * Commits transaction.
  public void commit(boolean isCommit)
  {
    try {
      if (isCommit)
        _ut.commit();
      else
        _ut.rollback();
    } catch (RuntimeException e) {
      throw e;
    } catch (RollbackException e) {
      throw new TransactionRolledbackLocalException(e.getMessage(), e);
    } catch (HeuristicMixedException e) {
      throw new TransactionRolledbackLocalException(e.getMessage(), e);
    } catch (HeuristicRollbackException e) {
      throw new TransactionRolledbackLocalException(e.getMessage(), e);
    } catch (Exception e) {
      throw new EJBException(e);
    }
  }*/

  /**
   * Resumes transaction.
   */
  public void resume(Transaction xa)
  {
    try {
      TransactionManagerImpl tm = TransactionManagerImpl.getLocal();

      tm.resume(xa);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new EJBException(e);
    }
  }

  public static class SynchronizationAdapter implements Synchronization {
    private final SessionSynchronization _sync;

    SynchronizationAdapter(SessionSynchronization sync)
    {
      _sync = sync;
    }

    public void beforeCompletion()
    {
      try {
        _sync.beforeCompletion();
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new EJBException(e);
      }
    }

    public void afterCompletion(int status)
    {
      try {
        _sync.afterCompletion(status == Status.STATUS_COMMITTED);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new EJBException(e);
      }
    }

    public String toString()
    {
      return getClass().getSimpleName() + "[" + _sync + "]";
    }
  }

  public String toString()
  {
    return "XAManager[]";
  }
}