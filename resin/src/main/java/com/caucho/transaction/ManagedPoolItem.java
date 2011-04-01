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

import com.caucho.util.Alarm;
import com.caucho.util.L10N;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages a ManagedConnection in the pool. Each ManagedPoolItem corresponds
 * one-to-one to a ManagedConnection.
 * 
 * The user's view of the pool item is a UserPoolItem. Because of transaction
 * sharing, it's possible for several UserPoolItems to refer to the same
 * ManagedPoolItem.
 */
class ManagedPoolItem implements ConnectionEventListener, XAResource {
  private static final L10N L = new L10N(ManagedPoolItem.class);
  private static final Logger log
    = Logger.getLogger(ManagedPoolItem.class.getName());

  private ConnectionPool _cm;
  
  private ManagedConnectionFactory _mcf;
  private ManagedConnection _mConn;

  private UserTransactionImpl _transaction;

  private String _id;

  private XAResource _xaResource;
  private LocalTransaction _localTransaction;
  private int _defaultTransactionTimeout;
  private int _transactionTimeout;

  private Subject _subject;
  private ConnectionRequestInfo _requestInfo;

  final Object _shareLock = new Object();
  
  // The head shared connection for transaction
  // The UserPoolItem code is responsible for this field
  UserPoolItem _shareHead;
  
  // The other pool items joined transaction
  private ManagedPoolItem _xaHead;
  private ManagedPoolItem _xaNext;
  
  private boolean _hasConnectionError;

  private long _poolStartTime;
  private long _poolEventTime;

  private Xid _xid;
  private int _endFlags = -1;

  // flag forcing an XA transaction (for the local transaction optimization)
  private boolean _isXATransaction = true;
    
  // true if in local transaction
  private boolean _isLocalTransaction;

  private IllegalStateException _allocationStackTrace;

  //
  // statistics
  //

  private long _connectionStartTime;
  private long _idleStartTime;
  private long _activeStartTime;

  public ManagedPoolItem(ConnectionPool cm,
                  ManagedConnectionFactory mcf,
                  ManagedConnection conn)
  {
    _cm = cm;

    _id = _cm.generateId();

    _mcf = mcf;
    _mConn = conn;

    _poolStartTime = Alarm.getCurrentTime();
    _poolEventTime = Alarm.getCurrentTime();

    _connectionStartTime = cm.getConnectionTimeProbe().start();

    // Gets the resource object from the driver
    try {
      if (cm.isXATransaction()) {
        XAResource xaResource = conn.getXAResource();

        try {
          _defaultTransactionTimeout = xaResource.getTransactionTimeout();
        } catch (Throwable e) {
          log.log(Level.FINE, e.toString(), e);
        }

        _xaResource = xaResource;
      }
    } catch (NotSupportedException e) {
      _cm.setXATransaction(false);
      log.log(Level.FINER, e.toString(), e);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    if (_xaResource == null)
      _isXATransaction = false;

    // Gets the local transaction from the driver
    try {
      if (_cm.isLocalTransaction())
        _localTransaction = conn.getLocalTransaction();
    } catch (NotSupportedException e) {
      _cm.setLocalTransaction(false);
      log.log(Level.FINER, e.toString(), e);
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    _mConn.addConnectionEventListener(this);

    if (log.isLoggable(Level.FINE))
      log.fine("create: " + this +
               "(active:" + _cm.getConnectionActiveCount() +
               ", total:" + _cm.getConnectionCount() + ")");
  }

  /**
   * Sets the subject.
   */
  public void setSubject(Subject subject)
  {
    _subject = subject;
  }

  /**
   * Sets the info.
   */
  public void setInfo(ConnectionRequestInfo info)
  {
    _requestInfo = info;
  }

  /**
   * Returns true if the connection is active.
   */
  public boolean isActive()
  {
    return _shareHead != null;
  }

  /**
   * Returns true if the connection is dead
   */
  public boolean isDead()
  {
    return _mConn == null;
  }

  /**
   * Returns the time of the last event.
   */
  public long getEventTime()
  {
    return _poolEventTime;
  }

  /**
   * Returns the time the connection was first used.
   */
  public long getStartTime()
  {
    return _poolStartTime;
  }

  /**
   * Sets the item's transaction.
   */
  void setTransaction(UserTransactionImpl transaction)
  {
    _transaction = transaction;
  }

  /**
   * Make this connection active.
   *
   * @return true if the pool item is valid, false if it should be removed.
   */
  synchronized UserPoolItem toActive(Subject subject,
                                     ConnectionRequestInfo info,
                                     UserPoolItem oldPoolItem)
    throws ResourceException
  {
    long now = Alarm.getCurrentTime();

    long maxIdleTime = _cm.getMaxIdleTime();
    long maxPoolTime = _cm.getMaxPoolTime();

    if (_hasConnectionError)
      return null;
    else if (0 < maxIdleTime && _poolEventTime + maxIdleTime < now)
      return null;
    else if (0 < maxPoolTime && _poolStartTime + maxPoolTime < now)
      return null;
    else if (_shareHead != null)
      throw new IllegalStateException(L.l("trying to activate active pool item."));

    _poolEventTime = now;
    _isXATransaction = _xaResource != null; // disable LT-optim by default

    UserPoolItem userPoolItem = null;

    if (oldPoolItem != null) {
      Object uConn = oldPoolItem.getUserConnection();

      if (uConn != null)
        _mConn.associateConnection(uConn);

      oldPoolItem.associatePoolItem(this);
      
      userPoolItem = oldPoolItem;
    }
    else
      userPoolItem = new UserPoolItem(_cm, this);
    
    if (! isValid(subject, info, userPoolItem))
      return null;

    _subject = subject;
    _requestInfo = info;
    userPoolItem.associate(this, _mcf, subject, info);

    if (log.isLoggable(Level.FINE))
      log.fine("allocate " + this);

    if (_cm.getSaveAllocationStackTrace())
      _allocationStackTrace = new IllegalStateException(L.l("Connection {0} allocation stack trace", this));

    return userPoolItem;
  }

  /**
   * Checks if the pool item is still valid.
   *
   * @return true if the pool item is valid, false if it should be removed.
   */
  boolean isValid()
  {
    synchronized (this) {
      long now = Alarm.getCurrentTime();

      long maxIdleTime = _cm.getMaxIdleTime();
      long maxPoolTime = _cm.getMaxPoolTime();
      long maxActiveTime = _cm.getMaxActiveTime();

      boolean isActive = isActive() || _xid != null;
      boolean isDead = false;

      if (! isActive && _hasConnectionError) {
        isDead = true;
        log.fine("closing pool item from connection error:" + this);
      }
      else if (! isActive &&
               0 < maxIdleTime && _poolEventTime + maxIdleTime < now) {
        isDead = true;
        log.fine("closing pool item from idle timeout:" + this);
      }
      else if (! isActive &&
               0 < maxPoolTime && _poolStartTime + maxPoolTime < now) {
        isDead = true;
        log.fine("closing pool item from pool timeout:" + this);
      }
      else if (isActive &&
               0 < maxActiveTime && _poolEventTime + maxActiveTime < now) {
        isDead = true;
        log.warning("closing pool item from active timeout:" + this);
      }

      if (isDead) {
        _hasConnectionError = true;
        return false;
      }
      else
        return true;
    }
  }
  
  /**
   * Use the item only if it's already been used for the current transaction
   * and is available.  allocateXA returns the same connection for the
   * following case:
   *
   * <pre>
   * UserTransaction.begin();
   *
   * conn = ds.getConnection();
   * ...
   * conn.close();
   *
   * conn = ds.getConnection();
   * ...
   * conn.close();
   * </pre>
   *
   * <p>Nested connections are not reused.
   *
   * @param xid the current transaction id
   *
   * @return true if the pool item has been allocated
   */
  UserPoolItem allocateXA(ManagedConnectionFactory mcf,
                          Subject subject,
                          ConnectionRequestInfo info)
  {
    if (_mConn == null)      // already closed
      return null;
    else if (_subject != subject)
      return null;
    else if (_requestInfo != info)
      return null;
    else if (_mcf != mcf)
      return null;
    else if (_shareHead != null && ! _cm.isShareable()) // is currently in use
      return null;
    /* server/14g9, #2708
    else if (_hasConnectionError) // had a fatal error
      return null;
    */
    
    if (log.isLoggable(Level.FINER))
      log.finer("sharing xa-pool item: " + this);

    UserPoolItem userPoolItem = new UserPoolItem(_cm);
    userPoolItem.associate(this, _mcf, _subject, _requestInfo);

    return userPoolItem;
  }

  /**
   * Returns true if the tested pool item is the Xid leader.
   */
  boolean isJoin(ManagedPoolItem item)
  {
    if (this == item)
      return false;
    else if (_xid != item._xid)
      return false;
    else if (_mcf != item._mcf)
      return false;
    else
      return true;
  }

  /**
   * Try to share the connection.
   */
  boolean share(UserPoolItem userPoolItem)
  {
    if (this == userPoolItem.getOwnPoolItem())
      return true;
    else if (_mConn == null)           // already closed
      return false;
    else if (! _cm.isShareable()) // not shareable
      return false;
    else if (_mcf != userPoolItem.getManagedConnectionFactory())
      return false;
    else if (_subject != userPoolItem.getSubject())
      return false;
    else if (_requestInfo != userPoolItem.getInfo())
      return false;
    else if (_hasConnectionError) // had a fatal error
      return false;

    // skip for now
    if (true)
      return false;
    
    userPoolItem.associate(this, _mcf, _subject, _requestInfo);

    return true;
  }

  /**
   * Returns the managed connection.
   */
  ManagedConnection getManagedConnection()
  {
    return _mConn;
  }

  /**
   * Returns the user connection.
   */
  /*
  Object getUserConnection()
    throws ResourceException
  {
    return _userPoolItem.getUserConnection();
  }
  */

  /**
   * Returns the user connection.
   */
  Object allocateConnection()
    throws ResourceException
  {
    Object conn = _mConn.getConnection(_subject, _requestInfo);
    
    return conn;
  }

  /**
   * Returns true for a valid connection.
   */
  boolean isValid(Subject subject,
                  ConnectionRequestInfo requestInfo,
                  UserPoolItem userPoolItem)
  {
    try {
      ManagedConnection mConn = getManagedConnection();

      if (mConn == null)
        return false;

      Object userConn = userPoolItem.getUserConnection();

      if (userConn == null) {
        userConn = mConn.getConnection(subject, requestInfo);

        userPoolItem.setUserConnection(userConn);
      }

      return userConn != null;
    } catch (ResourceException e) {
      log.log(Level.WARNING, e.toString(), e);

      return false;
    }
  }

  /**
   * Returns the XA resource.
   */
  void enableLocalTransactionOptimization(boolean enableOptimization)
  {
    if (_xaResource == null)
      _isXATransaction = false;
    else if (_localTransaction == null)
      _isXATransaction = true;
    else if (! _cm.isLocalTransactionOptimization())
      _isXATransaction = true;
    else if (! _cm.isShareable())
      _isXATransaction = true;
    else
      _isXATransaction = ! enableOptimization;
  }

  /**
   * Returns true if the pooled connection supports transactions.
   */
  boolean supportsTransaction()
  {
    // server/164j
    return _xaResource != null || _localTransaction != null;
  }

  /**
   * Returns the XA resource.
   */
  XAResource getXAResource()
  {
    return _xaResource;
  }

  /**
   * Returns the Xid resource.
   */
  Xid getXid()
  {
    return _xid;
  }

  /**
   * Notifies that an application has closed the connection.
   * 
   * Remove and close the UserPoolItem associated with the PoolItem. If it's
   * the last UserPoolItem, move to the idle state.
   */
  public void connectionClosed(ConnectionEvent event)
  {
    Object handle = event.getConnectionHandle();

    if (! _hasConnectionError && handle == null && _shareHead != null) {
      log.fine(L.l("JCA close event '{0}' for {1} did not have a connection handle.  Please notify the JCA resource provider.",
                  event, _mConn));
    }
   
    UserPoolItem ptr = _shareHead;
    UserPoolItem prev = null; 

    while (ptr != null) {
      UserPoolItem next = ptr.getShareNext();

      Object userConn = ptr.getUserConnection();

      if (userConn == handle || handle == null) {
        if (prev != null)
          prev.setShareNext(next);
        else
          _shareHead = next;
        
        ptr.close();
      }
      else
        prev = ptr;

      ptr = next;
    }

    if (_shareHead == null) {
      toIdle();
      return;
    }
  }
  
  /**
   * Notifies that a local transaction has started.
   */
  public void localTransactionStarted(ConnectionEvent event)
  {
    if (_isLocalTransaction || _xid != null)
      throw new IllegalStateException(L.l("attempted to start local transaction while transaction is in progress."));

    // server/30c4, #2627
    _isLocalTransaction = true;
  }
  
  /**
   * Notifies that a local transaction has committed.
   */
  public void localTransactionCommitted(ConnectionEvent event)
  {
    if (_isLocalTransaction) {
    }
    else if (_xid != null)
      throw new IllegalStateException(L.l("attempted to commit() local transaction from an active XA transaction."));
    else
      throw new IllegalStateException(L.l("attempted to commit() with no active local transaction."));

    // #2627, server/30c4
    _isLocalTransaction = false;
  }
  
  /**
   * Notifies that a local transaction has rolled back.
   */
  public void localTransactionRolledback(ConnectionEvent event)
  {
    if (_xid != null)
      throw new IllegalStateException(L.l("attempted to rollback() local transaction from an active XA transaction."));
    else if (! _isLocalTransaction)
      throw new IllegalStateException(L.l("attempted to rollback() with no active local transaction."));

    // #2627, server/30c4
    _isLocalTransaction = false;
  }
  
  /**
   * Notifies that a connection error has occurred.
   */
  public void connectionErrorOccurred(ConnectionEvent event)
  {
    _hasConnectionError = true;
  }
  
  /**
   * Notifies that a connection error has occurred.
   */
  public void setConnectionError()
  {
    _hasConnectionError = true;
  }

  /**
   * Returns true if there was a connection error.
   */
  public boolean isConnectionError()
  {
    return _hasConnectionError;
  }

  /**
   * Returns the allocation stack trace.
   */
  public IllegalStateException getAllocationStackTrace()
  {
    return _allocationStackTrace;
  }

  /**
   * Returns true if there is a connection error.
   */

  // XAResource stuff
  
  /**
   * identity of resources
   */
  public boolean isSameRM(XAResource resource)
    throws XAException
  {
    if (! (resource instanceof ManagedPoolItem))
      return false;

    ManagedPoolItem poolItem = (ManagedPoolItem) resource;
    
    //if (_cm == poolItem._cm)
    //  return true;

    if (_xaResource == null)
      return false;

    boolean isSameRM = _xaResource.isSameRM(poolItem._xaResource);
    
    if (log.isLoggable(Level.FINER))
      log.finer("isSameRM->" + isSameRM + " " + _xaResource);

    return isSameRM;
  }
  
  /**
   * starts work on a transaction branch
   */
  public void start(Xid xid, int flags)
    throws XAException
  {
    if (_xid != null) {
      if (log.isLoggable(Level.FINER))
        log.finer("connection pool start XA: rejoin " + this);
      
      return;
    }

    if (flags == TMJOIN && _xid == null) {
      // TMJOIN means the resource manager is managing more than one
      // connection.  The delegates tie the PoolItems managed by
      // the same resource manager together.

      _xid = xid;
      
      UserTransactionImpl trans = _cm.getTransaction();

      if (trans != null) {
        ManagedPoolItem xaHead = trans.findJoin(this);

        if (xaHead != null) {
          _xaNext = xaHead._xaNext;
          _xaHead = xaHead;
          xaHead._xaNext = this;
        }
      }
    }

    // local transaction optimization
    if (! _isXATransaction
        && flags != TMJOIN
        && _localTransaction != null) {
      try {
        if (log.isLoggable(Level.FINER))
          log.finer("begin-local-XA: " + xid + " " + _localTransaction);

        _localTransaction.begin();
      } catch (ResourceException e) {
        throw new XAExceptionWrapper(e);
      }

      _xid = xid;

      return;
    }

    if (_xaResource != null) {
      if (log.isLoggable(Level.FINER))
        log.finer("start-XA: " + xid + " " + _xaResource);

      _xaResource.start(xid, flags);
      _isXATransaction = true;
    }
    else {
      if (log.isLoggable(Level.FINER))
        log.finer("start-XA with non XA resource: " + xid + " " + _xaResource);
    }
    
    _xid = xid;
  }
  
  /**
   * Sets the transaction timeout
   */
  public boolean setTransactionTimeout(int seconds)
    throws XAException
  {
    if (seconds == _transactionTimeout)
      return true;

    XAResource xaResource = _xaResource;
    
    _transactionTimeout = seconds;

    if (xaResource == null)
      return true;
    else if (seconds == 0)
      return xaResource.setTransactionTimeout(_defaultTransactionTimeout);
    else
      return xaResource.setTransactionTimeout(seconds);
  }

  /**
   * Returns the timeout of the underlying resource.
   */
  public int getTransactionTimeout()
    throws XAException
  {
    return _transactionTimeout;
  }

  /**
   * forget about the transaction
   */
  public void forget(Xid xid)
    throws XAException
  {
    try {
      if (_isXATransaction)
        _xaResource.forget(xid);
    } finally {
      clearXid();
    }
  }

  /**
   * Vote using phase-1 of the 2-phase commit.
   */
  public int prepare(Xid xid)
    throws XAException
  {
    if (_endFlags != -1) {
      int endFlags = _endFlags;
      _endFlags = -1;

      if (_isXATransaction)
        endResource(xid, endFlags);
    }

    if (_isXATransaction) {
      try {
        if (log.isLoggable(Level.FINER))
          log.finer("prepare-XA: " + xid + " " + _xaResource);

        int result = _xaResource.prepare(xid);

        if (result == XA_RDONLY) {
          if (_xaResource != null)
            _isXATransaction = true;
      
          clearXid();
        }

        return result;
      } catch (XAException e) {
        if (log.isLoggable(Level.FINER))
          log.finer("failed prepare-XA: " + xid + " " + _xaResource + " " + e);

        throw e;
      }
    }
    else
      return XA_OK;
  }

  /**
   * recover the transaction
   */
  public Xid[]recover(int flag)
    throws XAException
  {
    if (_isXATransaction)
      return _xaResource.recover(flag);
    else
      return null;
  }
  
  /**
   * Ends work with the resource.  Called before commit/rollback.
   */
  public void end(Xid xid, int flags)
    throws XAException
  {
    _endFlags = flags;

    // XXX: In theory, drop the _xid.  The underlying XADataSource
    // can handle combining the connections itself.

    // Don't call the underlying _xaResource.end.  The commit or rollback
    // will handle that automatically.
  }
  
  /**
   * rollback the resource
   */
  public void rollback(Xid xid)
    throws XAException
  {
    try {
      int endFlags = _endFlags;
      _endFlags = -1;

      if (endFlags != -1 && _isXATransaction) {
        boolean isValid = false;
        
        try {
          endResource(xid, endFlags);
          isValid = true;
        } finally {
          if (! isValid)
            _xaResource.rollback(xid);
        }
      }
    
      if (log.isLoggable(Level.FINER))
        log.finer("connection pool rollback XA: " + this);

      if (_isXATransaction)
        _xaResource.rollback(xid);
      else if (_localTransaction != null) {
        try {
          _isLocalTransaction = false;
          _localTransaction.rollback();
        } catch (ResourceException e) {
          throw new XAExceptionWrapper(e);
        }
      }
    } finally {
      if (_xaResource != null)
        _isXATransaction = true;
      
      clearXid();
    }
  }
  
  /**
   * commit the resource
   */
  public void commit(Xid xid, boolean onePhase)
    throws XAException
  {
    boolean logFiner = log.isLoggable(Level.FINER);
    
    try {
      int endFlags = _endFlags;
      _endFlags = -1;
      
      if (endFlags != -1 && _isXATransaction) {
        boolean isValid = false;
        
        try {
          endResource(xid, endFlags);
          isValid = true;
        } finally {
          if (! isValid)
            _xaResource.rollback(xid);
        }
      }

      if (_isXATransaction) {
        if (logFiner) {
          log.finer("commit-XA" + (onePhase ? "-1p: " : ": ")
                    + xid + " " + _xaResource);
        }

        try {
          _xaResource.commit(xid, onePhase);
        } catch (XAException e) {
          if (logFiner)
            log.finer("commit-XA failed: " + _xaResource + " " + e);

          throw e;
        }
      }
      else if (_localTransaction != null) {
        if (logFiner)
          log.finer("commit-local: " + _localTransaction);

        try {
          _localTransaction.commit();
        } catch (ResourceException e) {
          if (logFiner)
            log.finer("commit failed: " + _localTransaction + " " + e);

          throw new XAExceptionWrapper(e);
        } finally {
          _isLocalTransaction = false;
        }
      }
      else {
        if (logFiner)
          log.finer("commit for resource with no XA support: " + this);
      }
    } finally {
      if (_xaResource != null)
        _isXATransaction = true;
      
      clearXid();
    }
  }

  /**
   * Ends the resource.
   */
  private void endResource(Xid xid, int flags)
    throws XAException
  {
    ManagedPoolItem xaPtr = this;

    for (; xaPtr != null; xaPtr = xaPtr._xaNext) {
      if (xaPtr._xaResource != null)
        xaPtr._xaResource.end(xid, flags);
    }
  }
  
  /**
   * Restores the delegation for the entire chain.
   */
  private void clearXid()
  {
    _xid = null;
    
    UserPoolItem shareHead = _shareHead;
    _shareHead = null;

    ManagedPoolItem xaPtr = _xaNext;
    _xaHead = null;
    _xaNext = null;

    UserPoolItem assignedUserItem = null;
    for (UserPoolItem ptr = shareHead; ptr != null; ptr = ptr.getShareNext()) {
      if (ptr.getOwnPoolItem() == this) {
        assignedUserItem = ptr;
        break;
      }
    }
    
    for (UserPoolItem ptr = shareHead; ptr != null; ptr = ptr.getShareNext()) {
      if (assignedUserItem == null && ptr.getOwnPoolItem() == null) {
        ptr.associatePoolItem(this);
        assignedUserItem = ptr;
      }

      try {
        ptr.reassociatePoolItem();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }

    while (xaPtr != null) {
      ManagedPoolItem next = xaPtr._xaNext;
      xaPtr._xaNext = null;
      xaPtr._xaHead = null;

      xaPtr.clearXid();

      xaPtr = next;
    }

    if (assignedUserItem != null) {
      //_shareHead = assignedUserItem;
      //_shareHead.setShareNext(null);
    }
    else if (_hasConnectionError) {
      destroy();
    }
    else {
      toIdle();
    }
  }

  /**
   * Changes the state to idle.
   */
  void toIdle()
  {
    if (_shareHead != null)
      return;
    else if (_xid != null || _isLocalTransaction)
      return;
    else if (_hasConnectionError) {
      destroy();
      return;
    }

    UserTransactionImpl transaction = _transaction;
    _transaction = null;
      
    if (transaction != null) {
      try {
        transaction.delistPoolItem(this, XAResource.TMSUCCESS);
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    _isLocalTransaction = false;

    if (log.isLoggable(Level.FINE))
      log.fine("idle " + this);
    
    _poolEventTime = Alarm.getCurrentTime();
    _cm.toIdle(this);
  }

  /**
   * Closes the connection.
   */
  void abortConnection()
  {
    destroy();
  }

  /**
   * Closes the connection.
   */
  void destroy()
  {
    ManagedConnection mConn = _mConn;
    _mConn = null;

    UserTransactionImpl transaction = _transaction;
    _transaction = null;

    if (mConn == null)
      return;

    _cm.removeItem(this, mConn);

    UserPoolItem userItem = _shareHead;

    if (log.isLoggable(Level.FINE))
      log.fine("connection pool destroy " + this);

    try {
      while (userItem != null) {
        UserPoolItem next = userItem.getShareNext();

        userItem.close();

        userItem = next;
      }

      if (transaction != null)
        transaction.delistPoolItem(this, XAResource.TMFAIL);
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }

    try {
      mConn.destroy();
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      _cm.getConnectionTimeProbe().end(_connectionStartTime);
    }
  }

  public String toString()
  {
    if (_mConn != null) {
      return (getClass().getSimpleName() + "[" + _cm.getName() + "," + _id + ","
              + _mConn.getClass().getSimpleName() + "]");
    }
    else {
      return (getClass().getSimpleName() + "[" + _cm.getName() + "," + _id + ",null]");
    }
  }
}
