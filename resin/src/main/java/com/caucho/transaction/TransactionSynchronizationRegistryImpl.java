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
 */

package com.caucho.transaction;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.xa.XAResource;

import com.caucho.util.L10N;

/**
 * Resin transaction synchronization registry implementation.
 * 
 * @author Reza Rahman
 */
public class TransactionSynchronizationRegistryImpl
  implements TransactionSynchronizationRegistry
{
  private static final Logger log
    = Logger.getLogger(TransactionSynchronizationRegistryImpl.class.getName());
  private static final L10N L
    = new L10N(TransactionSynchronizationRegistryImpl.class);

  private final TransactionManagerImpl _transactionManager;

  public TransactionSynchronizationRegistryImpl(TransactionManagerImpl transactionManager)
  {
    _transactionManager = transactionManager;
  }

  @Override
  public Object getResource(Object key)
  {
    TransactionImpl transaction = _transactionManager.getTransaction();

    if (transaction != null) {
      return transaction.getResource(key);
    } else {
      throw new IllegalStateException(L.l("Thread {0} does not have an active transaction.",
                                          Thread.currentThread()));
    }
  }

  @Override
  public boolean getRollbackOnly()
  {
    TransactionImpl transaction = _transactionManager.getTransaction();

    if (transaction != null) {
      return transaction.isRollbackOnly();
    } else {
      throw new IllegalStateException(L.l("This {0} does not have an active transaction.",
                                          Thread.currentThread()));
    }
  }

  @Override
  public Object getTransactionKey()
  {
    TransactionImpl transaction = _transactionManager.getTransaction();

    if (transaction != null) {
      return transaction.getXid();
    } else {
      return null;
    }
  }

  @Override
  public int getTransactionStatus()
  {
    TransactionImpl transaction = _transactionManager.getTransaction();

    if (transaction != null) {
      return transaction.getStatus();
    } else {
      return Status.STATUS_NO_TRANSACTION;
      /*
      throw new IllegalStateException(L.l("Thread {0} does not have an active transaction.",
                                          Thread.currentThread()));
                                          */
    }
  }

  // TODO This seems like a strange signature for a transaction resource.
  // Besides XAResource what other resource type could be used in Resin?
  @Override
  public void putResource(Object key, Object value)
  {
    if (!(value instanceof XAResource)) {
      throw new IllegalArgumentException(L.l("{0} is not an XA resource.",
          value));
    }

    TransactionImpl transaction = _transactionManager.getTransaction();

    if (transaction != null) {
      try {
        transaction.putResource(key, (XAResource) value);
      } catch (RollbackException e) {
        log.log(Level.WARNING, L.l("Error adding resoure to transaction: {0}",
            e.getMessage()), e);
      } catch (SystemException e) {
        log.log(Level.WARNING, L.l("Error adding resoure to transaction: {0}",
            e.getMessage()), e);
      }
    } else {
      throw new IllegalStateException(L.l("No active transaction."));
    }
  }

  @Override
  public void registerInterposedSynchronization(Synchronization synchronization)
  {
    TransactionImpl transaction = _transactionManager.getTransaction();

    if (transaction != null) {
      transaction.registerInterposedSynchronization(synchronization);
    } else {
      throw new IllegalStateException(L.l("No active transaction."));
    }
  }

  @Override
  public void setRollbackOnly()
  {
    TransactionImpl transaction = _transactionManager.getTransaction();

    if (transaction != null) {
      try {
        transaction.setRollbackOnly();
      } catch (SystemException e) {
        log.log(Level.WARNING, 
                L.l("Error setting roll-back: {0}", e.getMessage()), 
                e);
      }
    } else {
      throw new IllegalStateException(L.l("No active transaction."));
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}