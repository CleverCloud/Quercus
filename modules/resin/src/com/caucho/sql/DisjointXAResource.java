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

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * Represents an XA resource which should have isSameRM return false.
 */
public class DisjointXAResource implements XAResource {
  // The underlying resource
  private XAResource _xaResource;

  /**
   * Creates a new DisjointXAResource
   */
  public DisjointXAResource(XAResource resource)
  {
    _xaResource = resource;
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
  @Override
  public boolean setTransactionTimeout(int seconds)
    throws XAException
  {
    return _xaResource.setTransactionTimeout(seconds);
  }

  /**
   * Gets the transaction timeout.
   */
  @Override
  public int getTransactionTimeout()
    throws XAException
  {
    return _xaResource.getTransactionTimeout();
  }

  /**
   * Returns true if the underlying RM is the same.
   */
  @Override
  public boolean isSameRM(XAResource resource)
    throws XAException
  {
    return this == resource;
  }

  /**
   * Starts the resource.
   */
  @Override
  public void start(Xid xid, int flags)
    throws XAException
  {
    _xaResource.start(xid, flags);
  }

  /**
   * Starts the resource.
   */
  @Override
  public void end(Xid xid, int flags)
    throws XAException
  {
    _xaResource.end(xid, flags);
  }

  /**
   * Rolls the resource back
   */
  @Override
  public int prepare(Xid xid)
    throws XAException
  {
    return _xaResource.prepare(xid);
  }

  /**
   * Commits the resource
   */
  @Override
  public void commit(Xid xid, boolean onePhase)
    throws XAException
  {
    _xaResource.commit(xid, onePhase);
  }

  /**
   * Rolls the resource back
   */
  @Override
  public void rollback(Xid xid)
    throws XAException
  {
    _xaResource.rollback(xid);
  }

  /**
   * Rolls the resource back
   */
  @Override
  public Xid []recover(int flags)
    throws XAException
  {
    return _xaResource.recover(flags);
  }

  /**
   * Forgets the transaction
   */
  @Override
  public void forget(Xid xid)
    throws XAException
  {
    _xaResource.forget(xid);
  }

  @Override
  public String toString()
  {
    return "DisjointXAResource[" + _xaResource.getClass().getName() + "]";
  }
}
