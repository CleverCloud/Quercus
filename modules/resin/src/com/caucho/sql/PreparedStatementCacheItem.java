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

package com.caucho.sql;

import com.caucho.util.CacheListener;

import java.lang.ref.SoftReference;
import java.sql.PreparedStatement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represtents a prepared statement.
 */
class PreparedStatementCacheItem implements CacheListener {
  private final static Logger log
    = Logger.getLogger(PreparedStatementCacheItem.class.getName());

  private PreparedStatementKey _key;
  private SoftReference<PreparedStatement> _pStmtRef;
  private ManagedConnectionImpl _mConn;

  private boolean _isActive;
  private boolean _isRemoved;

  PreparedStatementCacheItem(PreparedStatementKey key,
                             PreparedStatement pStmt,
                             ManagedConnectionImpl mConn)
  {
    if (pStmt == null)
      throw new NullPointerException();

    _key = key;
    _pStmtRef = new SoftReference<PreparedStatement>(pStmt);
    _mConn = mConn;
  }

  /**
   * Activates the cache item.
   */
  UserPreparedStatement toActive(UserConnection conn)
  {
    SoftReference<PreparedStatement> ref = _pStmtRef;

    if (ref == null)
      return null;
    
    PreparedStatement pStmt = ref.get();

    if (pStmt == null) {
      _mConn.remove(_key);
      return null;
    }
    
    synchronized (this) {
      if (_isActive)
        return null;
      _isActive = true;
    }

    return new UserPreparedStatement(conn, pStmt, this);
  }

  void toIdle()
  {
    boolean doClose = false;

    synchronized (this) {
      if (_isRemoved) {
        _isRemoved = true;
        doClose = _isActive;
      }
      _isActive = false;
    }

    if (doClose) {
      try {
        PreparedStatement pStmt = _pStmtRef.get();
        _pStmtRef = null;

        if (pStmt != null)
          pStmt.close();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }

  /**
   * Returns true for a removed item.
   */
  boolean isRemoved()
  {
    return _isRemoved;
  }

  /**
   * Called when removed from the cache.
   */
  public void removeEvent()
  {
    boolean doClose = false;

    synchronized (this) {
      if (! _isRemoved) {
        _isRemoved = true;
        doClose = ! _isActive;
      }
    }

    if (doClose) {
      try {
        PreparedStatement pStmt = _pStmtRef.get();
        _pStmtRef = null;

        if (pStmt != null)
          pStmt.close();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
  }

  void destroy()
  {
    _isRemoved = true;
    
    SoftReference<PreparedStatement> ref = _pStmtRef;
    _pStmtRef = null;

    if (ref != null) {
      PreparedStatement pStmt = ref.get();

      if (pStmt != null) {
        try {
          pStmt.close();
        } catch (Throwable e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }
  }
}
