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

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;
import javax.transaction.RollbackException;
import java.util.logging.Logger;

/**
 * Pool item representing the user connection, i.e. matching to a Connection
 * object.
 * 
 * Because of XA sharing of connections, it's possible for multiple
 * UserPoolItems to be associated with a single ManagedPoolItem during the
 * XA. When the XA completes, the UserPoolItem will be reassociated with
 * the original ManagedPoolItem.
 *
 * The UserPoolItem is responsible for the lifecycle of an
 * associated PoolItem (ownPoolItem).
 *
 * The UserPoolItem also has the current XA PoolItem, which may be
 * shared with other UserPoolItems.
 *
 * When the XA completes, the UserPoolItem restores
 * its current XA PoolItem to the ownPoolItem.
 */
class UserPoolItem {
  private static final L10N L = new L10N(UserPoolItem.class);
  private static final Logger log
    = Logger.getLogger(UserPoolItem.class.getName());

  private ConnectionPool _cm;
  private String _id;

  // the pool item associated with this connection
  private ManagedPoolItem _ownPoolItem;

  private ManagedConnectionFactory _mcf;
  private Subject _subject;
  private ConnectionRequestInfo _info;

  // The owning transaction
  private UserTransactionImpl _transaction;
  
  // The head PoolItem for the transaction
  private ManagedPoolItem _sharePoolItem;
  
  // The next shared UserPoolItem in the chain
  private UserPoolItem _shareNext;
  // The previous shared UserPoolItem in the chain
  private UserPoolItem _sharePrev;

  private Object _userConn;
  
  private boolean _hasConnectionError;

  private IllegalStateException _allocationStackTrace;

  public UserPoolItem(ConnectionPool cm)
  {
    _cm = cm;
    _id = _cm.generateId();

    _transaction = _cm.getTransaction();

    if (cm.getSaveAllocationStackTrace()) {
      _allocationStackTrace = new IllegalStateException(L.l("unclosed connection: " + this + " was allocated at"));
      _allocationStackTrace.fillInStackTrace();
    }
  }

  public UserPoolItem(ConnectionPool cm, ManagedPoolItem poolItem)
  {
    this(cm);

    _ownPoolItem = poolItem;
    _sharePoolItem = poolItem;
  }

  /**
   * Sets the managed connection factory
   */
  public void setManagedConnectionFactory(ManagedConnectionFactory mcf)
  {
    _mcf = mcf;
  }

  /**
   * Gets the managed connection factory
   */
  public ManagedConnectionFactory getManagedConnectionFactory()
  {
    return _mcf;
  }

  /**
   * Sets the subject.
   */
  public void setSubject(Subject subject)
  {
    _subject = subject;
  }

  /**
   * Sets the subject.
   */
  public Subject getSubject()
  {
    return _subject;
  }

  /**
   * Sets the info.
   */
  public void setInfo(ConnectionRequestInfo info)
  {
    _info = info;
  }

  /**
   * Gets the info.
   */
  public ConnectionRequestInfo getInfo()
  {
    return _info;
  }

  /**
   * Returns true if the connection is active.
   */
  public boolean isActive()
  {
    return _userConn != null;
  }
  
  /**
   * Notifies that a connection error has occurred.
   */
  public void connectionErrorOccurred(ConnectionEvent event)
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
   * Enables saving of the allocation stack traces.
   */
  public void setSaveAllocationStackTrace(boolean enable)
  {
    _cm.setSaveAllocationStackTrace(enable);
  }

  /**
   * Return true if connections should be closed automatically.
   */
  public boolean isCloseDanglingConnections()
  {
    return _cm.isCloseDanglingConnections();
  }
  
  /**
   * Returns the pool item.
   */
  public ManagedPoolItem getOwnPoolItem()
  {
    return _ownPoolItem;
  }
  
  /**
   * Returns the pool item.
   */
  void setOwnPoolItem(ManagedPoolItem poolItem)
  {
    assert(_ownPoolItem == null);
    
    _ownPoolItem = poolItem;
    _sharePoolItem = poolItem;
  }
  
  /**
   * Returns the xa item.
   */
  public ManagedPoolItem getXAPoolItem()
  {
    return _sharePoolItem;
  }

  /**
   * Gets the user connection.
   */
  public Object getUserConnection()
  {
    return _userConn;
  }

  /**
   * Gets the user connection.
   */
  Object allocateUserConnection()
    throws ResourceException
  {
    if (_userConn == null)
      _userConn = _sharePoolItem.allocateConnection();
    
    return _userConn;
  }

  /**
   * Sets the user connection.
   */
  public void setUserConnection(Object userConn)
  {
    _userConn = userConn;
  }

  /**
   * Returns the next share.
   */
  UserPoolItem getShareNext()
  {
    return _shareNext;
  }

  /**
   * Returns the next share.
   */
  void setShareNext(UserPoolItem next)
  {
    _shareNext = next;
  }

  /**
   * Associates the UserPoolItem with a pool item
   */
  void associatePoolItem(ManagedPoolItem poolItem)
  {
    if (_ownPoolItem != null)
      throw new IllegalStateException(L.l("associating with old pool item."));
    
    _ownPoolItem = poolItem;

    if (_sharePoolItem != null)
      removeFromShareList();

    addToShareList(poolItem);
  }

  /**
   * Associates the UserPoolItem with a pool item
   */
  void associate(ManagedPoolItem poolItem,
                 ManagedConnectionFactory mcf,
                 Subject subject,
                 ConnectionRequestInfo info)
  {
    if (_sharePoolItem != null)
      removeFromShareList();

    _mcf = mcf;
    _subject = subject;
    _info = info;

    addToShareList(poolItem);

    if (_transaction != null) {
      try {
        _transaction.enlistResource(this);
      } catch (RollbackException e) {
        removeFromShareList();

        poolItem.toIdle();

        throw new RuntimeException(e);
      } catch (Throwable e) {
        removeFromShareList();

        poolItem.setConnectionError();
        poolItem.toIdle();

        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Reassociates with the own pool item.
   */
  void reassociatePoolItem()
    throws ResourceException
  {
    if (_ownPoolItem == null) {
      UserPoolItem item
        = _cm.allocatePoolConnection(_mcf, _subject, _info, this);

      assert(item == this);
      
      _ownPoolItem = item.getOwnPoolItem();
    }

    if (_sharePoolItem != null)
      removeFromShareList();

    addToShareList(_ownPoolItem);
  }

  /**
   * Close the connection, called from UserTransactionImpl.
   */
  void abortConnection()
    throws ResourceException
  {
    ManagedPoolItem poolItem = _ownPoolItem;
    _ownPoolItem = null;

    removeFromShareList();

    if (poolItem != null)
      poolItem.abortConnection();
  }

  /**
   * Closes the item.
   */
  void close()
  {
    ManagedPoolItem ownPoolItem = _ownPoolItem;
    _ownPoolItem = null;

    _userConn = null;

    if (_transaction != null)
      _transaction.delistResource(this);

    removeFromShareList();
  }

  /**
   * Removes from the current list.
   */
  void removeFromShareList()
  {
    ManagedPoolItem poolItem = _sharePoolItem;
    _sharePoolItem = null;
    
    if (poolItem == null)
      return;

    synchronized (poolItem._shareLock) {
      UserPoolItem prev = _sharePrev;
      UserPoolItem next = _shareNext;
      
      _sharePrev = null;
      _shareNext = null;

      if (prev != null)
        prev._shareNext = next;
      else
        poolItem._shareHead = next;

      if (next != null)
        next._sharePrev = prev;
    }
  }

  /**
   * Removes from the current list.
   */
  void addToShareList(ManagedPoolItem poolItem)
  {
    if (_sharePoolItem != null)
      throw new IllegalStateException();

    synchronized (poolItem._shareLock) {
      _sharePoolItem = poolItem;
      
      _sharePrev = null;
      _shareNext = poolItem._shareHead;

      if (poolItem._shareHead != null)
        poolItem._shareHead._sharePrev = this;

      poolItem._shareHead = this;
    }
  }

  /**
   * Returns true for a closed connection.
   */
  boolean isClosed()
  {
    return _sharePoolItem == null;
  }

  // XAResource stuff

  public String toString()
  {
    return "UserPoolItem[" + _cm.getName() + "," + _id + "]";
  }
}
