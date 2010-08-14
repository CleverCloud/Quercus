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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.caucho.transaction.xalog.AbstractXALogStream;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

/**
 * Implementation of the transaction. Transactions are normally associated with
 * a single thread.
 */
public class TransactionImpl implements Transaction, AlarmListener {
  private static final Logger log = Logger.getLogger(TransactionImpl.class
      .getName());
  private static final L10N L = new L10N(TransactionImpl.class);

  // private final static long EXTRA_TIMEOUT = 60000;
  private final static long EXTRA_TIMEOUT = 0;
  private final static long MAX_TIMEOUT = 24 * 3600 * 1000L;

  // flag when the resource is active (between getConnection() and close())
  private final static int RES_ACTIVE = 0x1;
  // flag when the resource shares another resource manager
  private final static int RES_SHARED_RM = 0x2;
  // flag when the resource is suspended
  private final static int RES_SUSPENDED = 0x4;
  // flag when the resource wants to commit
  private final static int RES_COMMIT = 0x8;

  private TransactionManagerImpl _transactionManager;
  private UserTransactionImpl _userTransaction;
  private UserTransactionSuspendState _suspendState;

  // The transaction id for the resource
  private XidImpl _xid;

  /** Transaction resources **/

  // TODO Should these be re-factored to lists?
  /**
   * How many resources are available in the transaction.
   */
  private int _resourceCount;

  /**
   * The current resources in the transaction
   */
  private XAResource [] _resources;

  /**
   * The xids for the resources.
   */
  private XidImpl [] _resourceXids;

  /**
   * Whether the resources are active (between begin/end) or not.
   */
  private int [] _resourceStates;

  /**
   * Transaction resources that a client API such 
   * as JPA may store and retrieve by key.
   */
  private Map<Object, Object> _mappedResources;

  private long _timeout = 0;

  /** Transaction synchronization **/

  /**
   * Synchronizations interposed for container resources like the JPA provider.
   */
  private ArrayList<Synchronization> _interposedSynchronizations;

  /**
   * Transaction synchronization list for resources like stateful EJBs.
   */
  private ArrayList<Synchronization> _synchronizations;

  /** Transaction status **/

  // State of the transaction
  private int _status;
  // True if the transaction is suspended.
  private boolean _isSuspended;
  // True if the transaction was closed.
  private boolean _isDead;

  private Throwable _rollbackException;

  private AbstractXALogStream _xaLog;

  private HashMap<String, Object> _properties;

  private Alarm _alarm;

  /**
   * Creates a new transaction.
   * 
   * @param manager
   *          the owning transaction manager
   */
  TransactionImpl(TransactionManagerImpl manager)
  {
    _transactionManager = manager;
    _timeout = _transactionManager.getTimeout();
    _status = Status.STATUS_NO_TRANSACTION;
    _alarm = new Alarm("xa-timeout", this, ClassLoader.getSystemClassLoader());
  }

  public static TransactionImpl getCurrent()
  {
    return TransactionManagerImpl.getLocal().getCurrent();
  }

  /**
   * Sets the user transaction.
   */
  public void setUserTransaction(UserTransactionImpl ut)
  {
    _userTransaction = ut;
  }

  public Xid getXid()
  {
    return _xid;
  }

  /**
   * Returns true if the transaction has any associated resources.
   */
  boolean hasResources()
  {
    return _resourceCount > 0;
  }

  public boolean isActive()
  {
    return _status == Status.STATUS_ACTIVE;
  }

  /**
   * Returns true if the transaction is currently suspended.
   */
  boolean isSuspended()
  {
    return _isSuspended;
  }

  /**
   * Returns true if the transaction is dead, i.e. failed for some reason.
   */
  boolean isDead()
  {
    return _isDead;
  }

  /**
   * Puts a resource into a map of resources and adds the resource to the
   * transaction.
   * 
   * @param key
   *          User defined key for the Resource.
   * @param resource
   *          The Resource to enlist in the transaction.
   * @throws RollbackException
   *           If a roll-back occurs.
   * @throws SystemException
   *           If an unexpected problem occurs.
   */
  public void putResource(Object key, XAResource resource)
      throws RollbackException, SystemException
  {
    enlistResource(resource);

    if (_mappedResources == null) {
      _mappedResources = new HashMap<Object, Object>();
    }

    _mappedResources.put(key, resource);
  }

  public void putResource(Object key, Object resource)
  {
    if (_mappedResources == null) {
      _mappedResources = new HashMap<Object, Object>();
    }

    _mappedResources.put(key, resource);
  }

  /**
   * Gets a Resource from the underlying map.
   * 
   * @param key
   *          User defined key for the resource.
   * @return The Resource mapped to the key.
   */
  public Object getResource(Object key)
  {
    Map<Object, Object> mappedResources = _mappedResources;
    
    if (mappedResources != null)
      return mappedResources.get(key);
    else
      return null;
  }

  /**
   * Enlists a resource with the current transaction. Example resources are
   * database or JMS connections.
   * 
   * @return true if successful
   */
  @Override
  public boolean enlistResource(XAResource resource) throws RollbackException,
      SystemException
  {
    if (resource == null) {
      throw new IllegalArgumentException(L
          .l("Resource must not be null in enlistResource"));
    }

    if (_isSuspended) {
      throw new IllegalStateException(L.l(
          "Can't enlist resource {0} because the transaction is suspended.",
          resource));
    }

    if (_status == Status.STATUS_ACTIVE) {
      // normal
    } else {
      // validate the status
      if (_status != Status.STATUS_MARKED_ROLLBACK) {
      } else if (_rollbackException != null) {
        throw RollbackExceptionWrapper.create(_rollbackException);
      } else
        throw new RollbackException(
            L.l("Can't enlist resource {0} because the transaction is marked rollback-only.",
                resource));

      if (_status == Status.STATUS_NO_TRANSACTION)
        throw new IllegalStateException(L.l(
            "Can't enlist resource {0} because the transaction is not active",
            resource));

      throw new IllegalStateException(
          L.l("Can't enlist resource {0} because the transaction is not in an active state.  state='{1}'",
              resource, xaState(_status)));
    }

    // creates enough space in the arrays for the resource
    if (_resources == null) {
      _resources = new XAResource[16];
      _resourceXids = new XidImpl[16];
      _resourceStates = new int[16];
    } else if (_resources.length <= _resourceCount) {
      int oldLength = _resources.length;
      int newLength = 2 * oldLength;

      XAResource [] resources = new XAResource[newLength];
      XidImpl [] resourceXids = new XidImpl[newLength];
      int [] resourceStates = new int[newLength];

      System.arraycopy(_resources, 0, resources, 0, oldLength);
      System.arraycopy(_resourceStates, 0, resourceStates, 0, oldLength);
      System.arraycopy(_resourceXids, 0, resourceXids, 0, oldLength);

      _resources = resources;
      _resourceStates = resourceStates;
      _resourceXids = resourceXids;
    }

    int flags = XAResource.TMNOFLAGS;

    // Active transaction will call the XAResource.start() method
    // to let the resource manager know that the resource is managed.
    //
    // If the resource uses the same resource manager as one of the
    // current resources, issue a TMJOIN message.
    XidImpl xid = _xid;
    boolean hasNewResource = true;

    for (int i = 0; i < _resourceCount; i++) {
      if (_resources[i] != resource) {
      } else if ((_resourceStates[i] & RES_ACTIVE) != 0) {
        IllegalStateException exn;
        exn = new IllegalStateException(
            L.l("Can't enlist same resource {0} twice. "
                + "Delist is required before calling enlist with an old resource.",
                resource));

        setRollbackOnly(exn);
        throw exn;
      }

      try {
        if (_resources[i].isSameRM(resource)) {
          flags = XAResource.TMJOIN;
          xid = _resourceXids[i];

          if ((_resourceStates[i] & RES_ACTIVE) == 0) {
            _resources[i] = resource;
            _resourceStates[i] |= RES_ACTIVE;
            hasNewResource = false;
          }

          break;
        }
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    if (_resourceCount > 0 && flags != XAResource.TMJOIN)
      xid = new XidImpl(_xid, _resourceCount + 1);

    try {
      if (_timeout > 0)
        resource.setTransactionTimeout((int) (_timeout / 1000L));

      if (log.isLoggable(Level.FINER)) {
        if (flags == XAResource.TMJOIN)
          log.finer(this + " join-XA " + resource);
        else
          log.finer(this + " start-XA " + resource);
      }

      resource.start(xid, flags);
    } catch (XAException e) {
      setRollbackOnly(e);

      String message = L.l("Failed to enlist resource {0} in transaction because of exception:\n{1}",
              resource, e);

      log.log(Level.SEVERE, message, e);

      throw new SystemException(message);
    }

    if (hasNewResource) {
      _resources[_resourceCount] = resource;
      _resourceStates[_resourceCount] = RES_ACTIVE;

      if (flags == XAResource.TMJOIN)
        _resourceStates[_resourceCount] |= RES_SHARED_RM;

      _resourceXids[_resourceCount] = xid;
      _resourceCount++;
    }

    return true;
  }

  /**
   * De-lists a resource from the current transaction
   * 
   * @param resource
   *          the resource to delist
   * @param flag
   *          XXX: ???
   * 
   * @return true if successful
   */
  public boolean delistResource(XAResource resource, int flag)
      throws SystemException
  {
    if (_isSuspended)
      throw new IllegalStateException(L.l("transaction is suspended"));

    if (_resourceCount == 0)
      return true;

    int index;

    for (index = _resourceCount - 1; index >= 0; index--) {
      if (_resources[index].equals(resource))
        break;
    }

    if (index < 0)
      return false;

    // If there is no current transaction,
    // remove it from the resource list entirely.
    if (_status == Status.STATUS_NO_TRANSACTION) {
      for (; index + 1 < _resourceCount; index++) {
        _resources[index] = _resources[index + 1];
        _resourceStates[index] = _resourceStates[index + 1];
        _resourceXids[index] = _resourceXids[index + 1];
      }

      _resourceCount--;

      return true;
    }

    if (_status == Status.STATUS_MARKED_ROLLBACK)
      flag = XAResource.TMFAIL;

    _resourceStates[index] &= ~RES_ACTIVE;

    try {
      resource.end(_resourceXids[index], flag);
    } catch (XAException e) {
      setRollbackOnly(e);

      String message = L.l("Failed to delist resource due to: {0}", e);
      log.log(Level.SEVERE, message, e);

      throw new SystemException(message);
    }

    return true;
  }

  /**
   * Returns the current number of resources.
   */
  public int getEnlistedResourceCount()
  {
    return _resourceCount;
  }

  /**
   * Return true if the transaction has no resources.
   */
  public boolean isEmpty()
  {
    if (_isDead)
      return true;
    else if (_resourceCount > 0)
      return false;
    // XXX: ejb/3692 because TransactionContext adds itself
    else if (_synchronizations != null && _synchronizations.size() > 1)
      return false; // TODO Should the interposed transactions be added here as
    // well?
    else
      return true;
  }

  /**
   * Returns true if the local transaction optimization would be allowed.
   */
  public boolean allowLocalTransactionOptimization()
  {
    // XXX: can also check if all are non-local
    return _resourceCount == 0;
  }

  /**
   * sets the timeout for the transaction
   */
  public void setTransactionTimeout(int seconds) throws SystemException
  {
    if (seconds == 0)
      _timeout = _transactionManager.getTimeout();
    else if (seconds < 0)
      _timeout = MAX_TIMEOUT;
    else {
      _timeout = 1000L * (long) seconds;
    }

    if (_status == Status.STATUS_ACTIVE && _timeout > 0) {
      _alarm.queue(_timeout + EXTRA_TIMEOUT);
    } else {
      _alarm.dequeue();
    }
  }

  /**
   * sets the timeout for the transaction
   */
  public int getTransactionTimeout() throws SystemException
  {
    if (_timeout < 0)
      return -1;
    else
      return (int) (_timeout / 1000L);
  }

  /**
   * Adds an attribute.
   */
  public void setAttribute(String var, Object value)
  {
    if (_properties == null)
      _properties = new HashMap<String, Object>();

    _properties.put(var, value);
  }

  /**
   * Gets an attribute.
   */
  public Object getAttribute(String var)
  {
    if (_properties != null)
      return _properties.get(var);
    else
      return null;
  }

  /**
   * Registers synchronization interposed by container resources such as the JPA
   * persistence provider.
   * 
   * @param synchronization
   *          Interposed synchronization.
   */
  public void registerInterposedSynchronization(Synchronization synchronization)
  {
    if (_interposedSynchronizations == null) {
      _interposedSynchronizations = new ArrayList<Synchronization>();
    }

    _interposedSynchronizations.add(synchronization);
  }

  /**
   * Register a synchronization callback
   */
  @Override
  public void registerSynchronization(Synchronization synchronization)
  {
    if (_synchronizations == null)
      _synchronizations = new ArrayList<Synchronization>();

    _synchronizations.add(synchronization);
    
    if (log.isLoggable(Level.FINER))
      log.finer(this + " registerSync " + synchronization);
  }

  /**
   * Returns the status of this transaction
   */
  @Override
  public int getStatus()
  {
    return _status;
  }

  /**
   * Start a transaction.
   */
  void begin() 
    throws SystemException, NotSupportedException
  {
    if (_status != Status.STATUS_NO_TRANSACTION) {
      int status = _status;

      // env/0691
      /*
      try {
        rollback();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
      */

      throw new NotSupportedException(
          L.l("Nested transactions are not supported. "
              + "The previous transaction for this thread did not commit() or rollback(). "
              + "Check that every UserTransaction.begin() has its commit() or rollback() in a finally block.\nStatus was {0}.",
              xaState(status)));
    }

    if (_isDead)
      throw new IllegalStateException(L
          .l("Error trying to use dead transaction."));

    _status = Status.STATUS_ACTIVE;

    _rollbackException = null;

    if (_xid == null)
      _xid = _transactionManager.createXID();

    if (log.isLoggable(Level.FINE))
      log.fine(this + " begin");

    if (_timeout > 0) {
      _alarm.queue(_timeout + EXTRA_TIMEOUT);
    }
  }

  /**
   * Suspend the transaction. The timeouts are stopped.
   */
  void suspend() throws SystemException
  {
    if (_isSuspended)
      throw new IllegalStateException(L.l("can't suspend already-suspended transaction"));

    // _alarm.dequeue();

    _isSuspended = true;

    for (int i = _resourceCount - 1; i >= 0; i--) {
      if ((_resourceStates[i] & (RES_ACTIVE | RES_SUSPENDED)) == RES_ACTIVE) {
        try {
          XAResource resource = _resources[i];

          resource.end(_resourceXids[i], XAResource.TMSUSPEND);
        } catch (Exception e) {
          setRollbackOnly(e);
        }
      }
    }

    if (_userTransaction != null)
      _suspendState = _userTransaction.userSuspend();

    if (log.isLoggable(Level.FINE))
      log.fine(this + " suspended");
  }

  /**
   * Resume the transaction and requeue the timeout.
   */
  void resume() throws SystemException
  {
    if (!_isSuspended)
      throw new IllegalStateException(L
          .l("can't resume non-suspended transaction"));

    if (_timeout > 0)
      _alarm.queue(_timeout + EXTRA_TIMEOUT);

    for (int i = _resourceCount - 1; i >= 0; i--) {
      if ((_resourceStates[i] & (RES_ACTIVE | RES_SUSPENDED)) == RES_ACTIVE) {
        try {
          XAResource resource = _resources[i];

          resource.start(_resourceXids[i], XAResource.TMRESUME);
        } catch (Exception e) {
          setRollbackOnly(e);
        }
      }
    }

    if (_userTransaction != null)
      _userTransaction.userResume(_suspendState);

    _isSuspended = false;

    if (log.isLoggable(Level.FINE))
      log.fine(this + " resume");
  }

  /**
   * Force any completion to be a roll-back.
   */
  @Override
  public void setRollbackOnly() throws SystemException
  {
    if (_status != Status.STATUS_ACTIVE
        && _status != Status.STATUS_MARKED_ROLLBACK) {
      throw new IllegalStateException(
          L.l("Can't set rollback-only because the transaction is not active, state={0}.",
              xaState(_status)));
    }

    _status = Status.STATUS_MARKED_ROLLBACK;

    _alarm.dequeue();
    _timeout = 0;

    if (log.isLoggable(Level.FINE))
      log.fine(this + " rollback-only");

    if (_rollbackException == null) {
      _rollbackException = new RollbackException(L.l("Transaction marked rollback-only"));
      _rollbackException.fillInStackTrace();
    }
  }

  /**
   * Force any completion to be a rollback.
   */
  public void setRollbackOnly(Throwable exn)
  {
    if (_status != Status.STATUS_ACTIVE
        && _status != Status.STATUS_MARKED_ROLLBACK) {
      throw new IllegalStateException(
          L.l("Can't set rollback-only because the transaction is not active, state={0}.",
              xaState(_status)));
    }

    _status = Status.STATUS_MARKED_ROLLBACK;

    if (_rollbackException == null) {
      _rollbackException = exn;
    }

    if (log.isLoggable(Level.FINER)) {
      log.log(Level.FINER, this + " rollback-only: " + exn.toString(), exn);
    } else if (log.isLoggable(Level.FINE)) {
      log.fine(this + " rollback-only: " + exn.toString());
    }
  }

  public boolean isRollbackOnly()
  {
    return _status == Status.STATUS_MARKED_ROLLBACK;
  }

  /**
   * Commit the transaction.
   */
  public void commit() throws RollbackException, HeuristicMixedException,
      HeuristicRollbackException, SystemException
  {
    _alarm.dequeue();

    Exception heuristicExn = null;

    try {
      switch (_status) {
      case Status.STATUS_ACTIVE:
        if (log.isLoggable(Level.FINE))
          log.fine(this + " commit (active)");
        break;

      case Status.STATUS_MARKED_ROLLBACK:
        if (log.isLoggable(Level.FINE))
          log.fine(this + " commit (marked rollback)");
        break;

      case Status.STATUS_NO_TRANSACTION:
        if (log.isLoggable(Level.FINE))
          log.fine(this + " commit (no transaction)");

        throw new IllegalStateException(
            L.l("Can't commit outside of a transaction.  Either the UserTransaction.begin() is missing or the transaction has already been committed or rolled back."));

      default:
        if (log.isLoggable(Level.FINE))
          log.fine(this + " commit (unknown: " + _status + ")");

        rollbackInt();
        throw new IllegalStateException(L.l(
            "Can't commit because the transaction state is {0}", String
                .valueOf(_status)));
      }

      try {
        callBeforeCompletion();
      } catch (RollbackException e) {
        rollbackInt();

        throw e;
      } catch (Throwable e) {
        setRollbackOnly(e);

        rollbackInt();

        RollbackException newException = new RollbackException(e.toString());
        newException.initCause(e);

        throw newException;
      }

      if (_status == Status.STATUS_MARKED_ROLLBACK) {
        rollbackInt();

        if (_rollbackException != null)
          throw new RollbackExceptionWrapper(
              L.l("Transaction can't commit because it has been marked rolled back\n  {0}",
                  _rollbackException), _rollbackException);
        else
          throw new RollbackException(
              L.l("Transaction can't commit because it has been marked rolled back."));
      }

      if (_resourceCount > 0) {
        _status = Status.STATUS_PREPARING;

        AbstractXALogStream xaLog = _transactionManager.getXALogStream();
        boolean hasPrepare = false;
        boolean allowSinglePhase = false;

        for (int i = _resourceCount - 1; i >= 0; i--) {
          XAResource resource = (XAResource) _resources[i];

          if (i == 0 && (xaLog == null || !hasPrepare)) {
            // server/1601
            _resourceStates[0] |= RES_COMMIT;

            allowSinglePhase = true;
            break;
          }

          if ((_resourceStates[i] & RES_SHARED_RM) == 0) {
            try {
              int prepare = resource.prepare(_resourceXids[i]);

              if (prepare == XAResource.XA_RDONLY) {
              } else if (prepare == XAResource.XA_OK) {
                hasPrepare = true;
                _resourceStates[i] |= RES_COMMIT;
              } else {
                log.finer(this + " unexpected prepare result " + prepare);
                rollbackInt();
              }
            } catch (XAException e) {
              heuristicExn = heuristicException(heuristicExn, e);
              rollbackInt();
              throw new RollbackExceptionWrapper(L.l("all commits rolled back"), 
                                                 heuristicExn);
            }
          }
        }

        if (hasPrepare && xaLog != null) {
          _xaLog = xaLog;

          xaLog.writeTMCommit(_xid);
        }

        _status = Status.STATUS_COMMITTING;

        if (allowSinglePhase) {
          try {
            XAResource resource = (XAResource) _resources[0];

            if ((_resourceStates[0] & RES_COMMIT) != 0)
              resource.commit(_xid, true);

            if (_timeout > 0)
              resource.setTransactionTimeout(0);
          } catch (XAException e) {
            log.log(Level.FINE, e.toString(), e);

            heuristicExn = heuristicException(heuristicExn, e);
          }
        }

        for (int i = 0; i < _resourceCount; i++) {
          XAResource resource = (XAResource) _resources[i];

          if (i == 0 && allowSinglePhase)
            continue;

          if ((_resourceStates[i] & RES_SHARED_RM) != 0)
            continue;
          if ((_resourceStates[i] & RES_COMMIT) == 0)
            continue;

          if (heuristicExn == null) {
            try {
              resource.commit(_resourceXids[i], false);

              if (_timeout > 0)
                resource.setTransactionTimeout(0);
            } catch (XAException e) {
              heuristicExn = e;
              log.log(Level.FINE, e.toString(), e);
            }
          } else {
            try {
              resource.rollback(_resourceXids[i]);

              if (_timeout > 0)
                resource.setTransactionTimeout(0);
            } catch (XAException e) {
              log.log(Level.FINE, e.toString(), e);
            }
          }
        }
      }

      if (heuristicExn != null && log.isLoggable(Level.FINE))
        log.fine(this + " " + heuristicExn);

      if (heuristicExn == null)
        _status = Status.STATUS_COMMITTED;
      else if (heuristicExn instanceof RollbackException) {
        _status = Status.STATUS_ROLLEDBACK;
        throw (RollbackException) heuristicExn;
      } else if (heuristicExn instanceof HeuristicMixedException) {
        _status = Status.STATUS_ROLLEDBACK;
        throw (HeuristicMixedException) heuristicExn;
      } else if (heuristicExn instanceof HeuristicRollbackException) {
        _status = Status.STATUS_ROLLEDBACK;
        throw (HeuristicRollbackException) heuristicExn;
      } else if (heuristicExn instanceof SystemException) {
        _status = Status.STATUS_ROLLEDBACK;
        throw (SystemException) heuristicExn;
      } else {
        _status = Status.STATUS_ROLLEDBACK;
        throw RollbackExceptionWrapper.create(heuristicExn);
      }
    } finally {
      callAfterCompletion();
    }
  }

  /**
   * Rollback the transaction.
   */
  @Override
  public void rollback()
  {
    _alarm.dequeue();

    try {
      callBeforeCompletion();
    } catch (Throwable e) {
      setRollbackOnly(e);
    }

    try {
      switch (_status) {
      case Status.STATUS_ACTIVE:
      case Status.STATUS_MARKED_ROLLBACK:
        // fall through to normal completion
        break;

      case Status.STATUS_NO_TRANSACTION:
        throw new IllegalStateException(
            L.l("Can't rollback outside of a transaction.  Either the UserTransaction.begin() is missing or the transaction has already been committed or rolled back."));

      default:
        rollbackInt();
        throw new IllegalStateException(L.l("Can't rollback in state: {0}",
            String.valueOf(_status)));
      }

      _status = Status.STATUS_MARKED_ROLLBACK;

      rollbackInt();
    } finally {
      callAfterCompletion();
    }
  }

  /**
   * Calculates the heuristic exception based on the resource manager's
   * exception.
   */
  private Exception heuristicException(Exception oldException,
      XAException xaException)
  {
    switch (xaException.errorCode) {
    case XAException.XA_HEURHAZ:
    case XAException.XA_HEURCOM:
      return oldException;

    case XAException.XA_HEURRB:
      if (oldException instanceof HeuristicMixedException)
        return oldException;
      else if (oldException instanceof HeuristicRollbackException)
        return oldException;
      else if (oldException instanceof RollbackException)
        return oldException;
      else
        return new HeuristicRollbackException(getXAMessage(xaException));

    default:
      if (oldException instanceof SystemException)
        return oldException;
      else
        return new SystemExceptionWrapper(getXAMessage(xaException),
            xaException);
    }
  }

  /**
   * Rollback the transaction.
   */
  private void rollbackInt()
  {
    _status = Status.STATUS_ROLLING_BACK;

    if (log.isLoggable(Level.FINE))
      log.fine(this + " rollback");

    for (int i = 0; i < _resourceCount; i++) {
      XAResource resource = (XAResource) _resources[i];

      if ((_resourceStates[i] & RES_SHARED_RM) != 0)
        continue;

      try {
        resource.rollback(_resourceXids[i]);

        if (_timeout > 0)
          resource.setTransactionTimeout(0);
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    _status = Status.STATUS_ROLLEDBACK;
  }

  /**
   * Call all the Synchronization listeners before the commit()/rollback()
   * starts.
   */
  private void callBeforeCompletion() throws RollbackException
  {
    _alarm.dequeue();
    
    // server/16h2
    for (int i = 0; _synchronizations != null && i < _synchronizations.size(); i++) {
      Synchronization synchronization = _synchronizations.get(i);
      
      if (log.isLoggable(Level.FINEST))
        log.finest(this + " beforeCompletion " + synchronization);

      try {
        synchronization.beforeCompletion();
      } catch (RuntimeException e) {
        setRollbackOnly(e);

        RollbackException newException = new RollbackException(e.toString());
        newException.initCause(e);

        throw newException;
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    if (_interposedSynchronizations != null) {
      for (Synchronization interposedSynchronization : _interposedSynchronizations) {
        try {
          interposedSynchronization.beforeCompletion();
        } catch (RuntimeException e) {
          setRollbackOnly(e);

          RollbackException newException = new RollbackException(e.toString());
          newException.initCause(e);

          throw newException;
        } catch (Throwable e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }

    // tell the resources everything's done
    for (int i = _resourceCount - 1; i >= 0; i--) {
      XAResource resource = _resources[i];

      int flag;

      if (_status == Status.STATUS_MARKED_ROLLBACK)
        flag = XAResource.TMFAIL;
      else
        flag = XAResource.TMSUCCESS;

      try {
        if ((_resourceStates[i] & RES_ACTIVE) != 0)
          resource.end(_resourceXids[i], flag);
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
        setRollbackOnly(e);
      }
    }
  }

  /**
   * Call all the Synchronization listeners after the commit()/rollback()
   * complete.
   */
  private void callAfterCompletion()
  {
    ArrayList<Synchronization> interposedSynchronizations = _interposedSynchronizations;
    _interposedSynchronizations = null;

    ArrayList<Synchronization> synchronizations = _synchronizations;
    _synchronizations = null;

    _userTransaction = null;

    XidImpl xid = _xid;
    _xid = null;

    int status = _status;
    _status = Status.STATUS_NO_TRANSACTION;

    _rollbackException = null;

    // remove the resources which have officially delisted
    for (int i = _resourceCount - 1; i >= 0; i--)
      _resources[i] = null;
    
    _resourceCount = 0;
    
    _mappedResources = null;

    AbstractXALogStream xaLog = _xaLog;
    _xaLog = null;

    if (xaLog != null) {
      try {
        xaLog.writeTMFinish(xid);
      } catch (Throwable e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }

    int length = (interposedSynchronizations == null 
                  ? 0
                  : interposedSynchronizations.size());
    for (int i = 0; i < length; i++) {
      Synchronization sync
        = (Synchronization) interposedSynchronizations.get(i);

      try {
        if (log.isLoggable(Level.FINEST))
          log.finest(this + " afterCompletion " + sync);

        sync.afterCompletion(status);
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    length = synchronizations == null ? 0 : synchronizations.size();
    for (int i = 0; i < length; i++) {
      Synchronization sync = (Synchronization) synchronizations.get(i);

      try {
        if (log.isLoggable(Level.FINEST))
          log.finest(this + " afterCompletion " + sync);

        sync.afterCompletion(status);
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    if (_properties != null)
      _properties.clear();
  }

  @Override
  public void handleAlarm(Alarm alarm)
  {
    try {
      String msg = L.l("{0}: timed out after {1} seconds.", this, 
                       String.valueOf(getTransactionTimeout()));
                       
      log.warning(msg);
      
      RuntimeException exn = new RuntimeException(msg);

      setRollbackOnly(exn);

      // should not close at this point because there could be following
      // statements that also need to be rolled back
      // server/16a7
      // close();
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Close the transaction, rolling back everything and removing all enlisted
   * resources.
   */
  public void close()
  {
    _isDead = true;
    _alarm.dequeue();

    try {
      if (_status != Status.STATUS_NO_TRANSACTION)
        rollback();
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }

    if (_synchronizations != null)
      _synchronizations.clear();

    if (_interposedSynchronizations != null)
      _interposedSynchronizations.clear();

    for (int i = _resourceCount - 1; i >= 0; i--)
      _resources[i] = null;

    _resourceCount = 0;

    Map<Object,Object> mappedResources = _mappedResources;
    _mappedResources = null;
    
    if (mappedResources != null)
      mappedResources.clear();

    _xid = null;
  }

  /**
   * Printable version of the transaction.
   */
  public String toString()
  {
    if (_xid == null)
      return "Transaction[]";

    CharBuffer cb = new CharBuffer();

    cb.append("Transaction[");

    byte [] branch = _xid.getBranchQualifier();

    addByte(cb, branch[0]);

    cb.append(":");

    byte [] global = _xid.getGlobalTransactionId();
    for (int i = 24; i < 28; i++)
      addByte(cb, global[i]);

    cb.append("]");

    return cb.toString();
  }

  /**
   * Adds hex for debug
   */
  private void addByte(CharBuffer cb, int b)
  {
    int h = (b / 16) & 0xf;
    int l = b & 0xf;

    if (h >= 10)
      cb.append((char) ('a' + h - 10));
    else
      cb.append((char) ('0' + h));

    if (l >= 10)
      cb.append((char) ('a' + l - 10));
    else
      cb.append((char) ('0' + l));
  }

  /**
   * Converts XA error code to a message.
   */
  private static String getXAMessage(XAException xaException)
  {
    if (xaException.getMessage() != null
        && !xaException.getMessage().equals(""))
      return xaException.getMessage();
    else
      return (xaName(xaException.errorCode) + ": " + xaMessage(xaException.errorCode));
  }

  /**
   * Converts XA state code to string.
   */
  private static String xaState(int xaState)
  {
    switch (xaState) {
    case Status.STATUS_ACTIVE:
      return "ACTIVE";
    case Status.STATUS_MARKED_ROLLBACK:
      return "MARKED-ROLLBACK";
    case Status.STATUS_PREPARED:
      return "PREPARED";
    case Status.STATUS_COMMITTED:
      return "COMITTED";
    case Status.STATUS_ROLLEDBACK:
      return "ROLLEDBACK";
    case Status.STATUS_PREPARING:
      return "PREPARING";
    case Status.STATUS_COMMITTING:
      return "COMMITTING";
    case Status.STATUS_ROLLING_BACK:
      return "ROLLING_BACK";
    case Status.STATUS_NO_TRANSACTION:
      return "NO_TRANSACTION";
    default:
      return "XA-STATE(" + xaState + ")";
    }
  }

  /**
   * Converts XA error code to string.
   */
  private static String xaName(int xaCode)
  {
    switch (xaCode) {
    // rollback codes
    case XAException.XA_RBROLLBACK:
      return "XA_RBROLLBACK";
    case XAException.XA_RBCOMMFAIL:
      return "XA_RBCOMMFAIL";
    case XAException.XA_RBDEADLOCK:
      return "XA_RBDEADLOCK";
    case XAException.XA_RBINTEGRITY:
      return "XA_RBINTEGRITY";
    case XAException.XA_RBOTHER:
      return "XA_RBOTHER";
    case XAException.XA_RBPROTO:
      return "XA_RBPROTO";
    case XAException.XA_RBTIMEOUT:
      return "XA_RBTIMEOUT";
    case XAException.XA_RBTRANSIENT:
      return "XA_RBTRANSIENT";

      // suspension code
    case XAException.XA_NOMIGRATE:
      return "XA_NOMIGRATE";

      // heuristic completion codes
    case XAException.XA_HEURHAZ:
      return "XA_HEURHAZ";
    case XAException.XA_HEURCOM:
      return "XA_HEURCOM";
    case XAException.XA_HEURRB:
      return "XA_HEURRB";
    case XAException.XA_HEURMIX:
      return "XA_HEURMIX";
    case XAException.XA_RDONLY:
      return "XA_RDONLY";

      // errors
    case XAException.XAER_RMERR:
      return "XA_RMERR";
    case XAException.XAER_NOTA:
      return "XA_NOTA";
    case XAException.XAER_INVAL:
      return "XA_INVAL";
    case XAException.XAER_PROTO:
      return "XA_PROTO";
    case XAException.XAER_RMFAIL:
      return "XA_RMFAIL";
    case XAException.XAER_DUPID:
      return "XA_DUPID";
    case XAException.XAER_OUTSIDE:
      return "XA_OUTSIDE";

    default:
      return "XA(" + xaCode + ")";
    }
  }

  /**
   * Converts XA error code to a message.
   */
  private static String xaMessage(int xaCode)
  {
    switch (xaCode) {
    // rollback codes
    case XAException.XA_RBROLLBACK:
    case XAException.XA_RBOTHER:
      return L.l("Resource rolled back for an unspecified reason.");
    case XAException.XA_RBCOMMFAIL:
      return L.l("Resource rolled back because of a communication failure.");
    case XAException.XA_RBDEADLOCK:
      return L.l("Resource rolled back because of a deadlock.");
    case XAException.XA_RBINTEGRITY:
      return L.l("Resource rolled back because of an integrity check failure.");
    case XAException.XA_RBPROTO:
      return L
          .l("Resource rolled back because of a protocol error in the resource manager.");
    case XAException.XA_RBTIMEOUT:
      return L.l("Resource rolled back because of a timeout.");
    case XAException.XA_RBTRANSIENT:
      return L.l("Resource rolled back because of transient error.");

      // suspension code
    case XAException.XA_NOMIGRATE:
      return L.l("Resumption must occur where the suspension occurred.");

      // heuristic completion codes
    case XAException.XA_HEURHAZ:
      return L.l("Resource may have been heuristically completed.");
    case XAException.XA_HEURCOM:
      return L.l("Resource has been heuristically committed.");
    case XAException.XA_HEURRB:
      return L.l("Resource has been heuristically rolled back.");
    case XAException.XA_HEURMIX:
      return L.l("Resource has been heuristically committed and rolled back.");
    case XAException.XA_RDONLY:
      return L
          .l("Resource was read-only and has been heuristically committed.");

      // errors
    case XAException.XAER_RMERR:
      return L.l("Resource manager error.");
    case XAException.XAER_NOTA:
      return L.l("The XID (transaction identifier) was invalid.");
    case XAException.XAER_INVAL:
      return L.l("Invalid arguments were given.");
    case XAException.XAER_PROTO:
      return L.l("Method called in an invalid context.");
    case XAException.XAER_RMFAIL:
      return L.l("Resource manager is unavailable.");
    case XAException.XAER_DUPID:
      return L.l("Duplicate XID (transaction identifier).");
    case XAException.XAER_OUTSIDE:
      return L.l("Resource manager called outside of transaction.");

    default:
      return "";
    }
  }
}
