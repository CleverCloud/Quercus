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

package com.caucho.sql.spy;

import com.caucho.util.L10N;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Spying on a connection.
 */
public class SpyXAResource implements XAResource {
  protected final static Logger log 
    = Logger.getLogger(SpyXAResource.class.getName());
  protected final static L10N L = new L10N(SpyXAResource.class);

  // The underlying resource
  private XAResource _xaResource;

  private String _id;

  /**
   * Creates a new SpyXAResource
   */
  public SpyXAResource(String id, XAResource resource)
  {
    _xaResource = resource;
    _id = id;
  }

  /**
   * Returns the underlying resource.
   */
  public XAResource getXAResource()
  {
    return _xaResource;
  }

  /**
   * Sets the transaction timeout.
   */
  public boolean setTransactionTimeout(int seconds)
    throws XAException
  {
    try {
      boolean ok = _xaResource.setTransactionTimeout(seconds);
      log.fine(_id + ":set-transaction-timeout(" + seconds + ")->" + ok);

      return ok;
    } catch (XAException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    }
  }

  /**
   * Gets the transaction timeout.
   */
  public int getTransactionTimeout()
    throws XAException
  {
    try {
      int seconds = _xaResource.getTransactionTimeout();
      
      log.fine(_id + ":transaction-timeout()->" + seconds);

      return seconds;
    } catch (XAException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    }
  }

  /**
   * Returns true if the underlying RM is the same.
   */
  public boolean isSameRM(XAResource resource)
    throws XAException
  {
    try {
      if (resource instanceof SpyXAResource)
        resource = ((SpyXAResource) resource).getXAResource();

      boolean same = _xaResource.isSameRM(resource);
      
      log.fine(_id + ":is-same-rm(resource=" + resource + ")->" + same);

      return same;
    } catch (XAException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    }
  }

  /**
   * Starts the resource.
   */
  public void start(Xid xid, int flags)
    throws XAException
  {
    try {
      String flagName = "";

      if ((flags & TMJOIN) != 0)
        flagName += ",join";
      if ((flags & TMRESUME) != 0)
        flagName += ",resume";
      
      log.fine(_id + ":start(xid=" + xid + flagName + ")");

      _xaResource.start(xid, flags);
    } catch (XAException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    }
  }

  /**
   * Starts the resource.
   */
  public void end(Xid xid, int flags)
    throws XAException
  {
    try {
      String flagName = "";

      if ((flags & TMFAIL) != 0)
        flagName += ",fail";
      if ((flags & TMSUSPEND) != 0)
        flagName += ",suspend";
      
      log.fine(_id + ":end(xid=" + xid + flagName + ")");

      _xaResource.end(xid, flags);
    } catch (XAException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    }
  }

  /**
   * Rolls the resource back
   */
  public int prepare(Xid xid)
    throws XAException
  {
    try {
      int value = _xaResource.prepare(xid);
      log.fine(_id + ":prepare(xid=" + xid + ")->" + value);

      return value;
    } catch (XAException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    }
  }

  /**
   * Commits the resource
   */
  public void commit(Xid xid, boolean onePhase)
    throws XAException
  {
    try {
      log.fine(_id + ":commit(xid=" + xid + (onePhase ? ",1P)" : ",2P)"));

      _xaResource.commit(xid, onePhase);
    } catch (XAException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    }
  }

  /**
   * Rolls the resource back
   */
  public void rollback(Xid xid)
    throws XAException
  {
    try {
      log.fine(_id + ":rollback(xid=" + xid + ")");

      _xaResource.rollback(xid);
    } catch (XAException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    }
  }

  /**
   * Rolls the resource back
   */
  public Xid []recover(int flags)
    throws XAException
  {
    try {
      String flagString = "";

      if ((flags & XAResource.TMSTARTRSCAN) != 0)
        flagString += "start";

      if ((flags & XAResource.TMENDRSCAN) != 0) {
        if (! flagString.equals(""))
          flagString += ",";

        flagString += "end";
      }
      
      log.fine(_id + ":recover(flags=" + flagString + ")");

      return _xaResource.recover(flags);
    } catch (XAException e) {
      log.fine(e.toString());
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    }
  }

  /**
   * Forgets the transaction
   */
  public void forget(Xid xid)
    throws XAException
  {
    try {
      log.fine(_id + ":forget(xid=" + xid + ")");

      _xaResource.forget(xid);
    } catch (XAException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    } catch (RuntimeException e) {
      log.log(Level.FINE, e.toString(), e);
      throw e;
    }
  }

  public String toString()
  {
    return "SpyXAResource[id=" + _id + ",resource=" + _xaResource + "]";
  }
}
