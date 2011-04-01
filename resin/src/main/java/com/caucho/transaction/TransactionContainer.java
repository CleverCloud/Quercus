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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.transaction;

import com.caucho.util.L10N;

import javax.naming.InitialContext;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Transaction identifier implementation.
 */
public class TransactionContainer {
  private static final L10N L = new L10N(TransactionContainer.class);
  private static final Logger log
    = Logger.getLogger(TransactionContainer.class.getName());

  private static TransactionContainer _container;

  private UserTransaction _userTM;
  private TransactionManager _tm;

  public static TransactionContainer getTransactionContainer()
  {
    if (_container == null) {
      _container = new TransactionContainer();

      try {
        InitialContext ic = new InitialContext();

        UserTransaction userTM;
        userTM = (UserTransaction) ic.lookup("java:comp/UserTransaction");

        _container.setUserTransaction(userTM);

        TransactionManager tm;
        tm = (TransactionManager) ic.lookup("java:comp/TransactionManager");

        _container.setTransactionManager(tm);
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    return _container;
  }

  /**
   * Sets the user transaction.
   */
  public void setUserTransaction(UserTransaction userTM)
  {
    _userTM = userTM;
  }

  /**
   * Sets the transaction manager.
   */
  public void setTransactionManager(TransactionManager tm)
  {
    _tm = tm;
  }

  /**
   * Returns a transaction context for the "required" transaction.  If
   * there's already an active transaction, use it.  Otherwise create a
   * new transaction.
   *
   * @return the current transaction context
   */
  public Transaction beginRequired()
  {
    try {
      Transaction currentTrans = _tm.getTransaction();

      if (currentTrans != null)
        return currentTrans;
      
      // _userTransaction.setTransactionTimeout((int) (_transactionTimeout / 1000L));
      _userTM.begin();
      
      return null;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new TransactionRuntimeException(e);
    }
  }

  /**
   * Returns a transaction context for the "RequiresNew" transaction.
   * Always creates a new transaction, suspending any old one.
   *
   * @return the current transaction context
   */
  public Transaction beginRequiresNew()
  {
    try {
      Transaction oldTrans = _tm.getTransaction();

      if (oldTrans != null)
        oldTrans = _tm.suspend();

      // _userTransaction.setTransactionTimeout((int) (_transactionTimeout / 1000L));
      _userTM.begin();
      
      return oldTrans;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new TransactionRuntimeException(e);
    }
  }

  /**
   * Require a transaction, throwing an exception if none exists.
   *
   * @return the current transaction context
   */
  public void beginMandatory()
  {
    try {
      Transaction oldTrans = _tm.getTransaction();

      if (oldTrans == null)
        throw new TransactionRuntimeException(L.l("'Mandatory' transaction boundary requires a transaction."));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new TransactionRuntimeException(e);
    }
  }

  /**
   * Require no transactions, throwing an exception if one exists.
   */
  public void beginNever()
  {
    try {
      Transaction oldTrans = _tm.getTransaction();

      if (oldTrans != null)
        throw new TransactionRuntimeException(L.l("'Never' transaction boundary must not have a transaction."));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new TransactionRuntimeException(e);
    }
  }

  /**
   * Suspends any active transaction.

   * @return the current transaction context
   */
  public Transaction beginSuspend()
  {
    try {
      Transaction oldTrans = _tm.getTransaction();

      if (oldTrans != null)
        oldTrans = _tm.suspend();

      return oldTrans;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new TransactionRuntimeException(e);
    }
  }

  /**
   * Sets a rollback-only transaction.
   */
  public void setRollbackOnly(Throwable e)
  {
  }

  /**
   * Commits the transaction (rolling back if rollback only)
   */
  public void commit(Transaction oldTransaction)
  {
    try {
      Transaction currentTrans = _tm.getTransaction();

      if (currentTrans == null) {
      }
      else if (currentTrans.getStatus() != Status.STATUS_MARKED_ROLLBACK)
        _userTM.commit();
      else
        _userTM.rollback();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new TransactionRuntimeException(e);
    } finally {
      if (oldTransaction != null) {
        try {
          _tm.resume(oldTransaction);
        } catch (Exception e) {
          throw new TransactionRuntimeException(e);
        }
      }
    }
  }

  /**
   * Rolls back any existing transaction.
   */
  public void rollback(Transaction oldTransaction)
  {
    try {
      Transaction currentTrans = _tm.getTransaction();

      if (currentTrans != null)
        _userTM.rollback();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new TransactionRuntimeException(e);
    } finally {
      if (oldTransaction != null) {
        try {
          _tm.resume(oldTransaction);
        } catch (Exception e) {
          throw new TransactionRuntimeException(e);
        }
      }
    }
  }
}
