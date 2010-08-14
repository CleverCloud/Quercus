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

package javax.transaction.xa;

/**
 * The interface for a transaction resource.
 */
public interface XAResource {
  // end a recovery scan
  public final static int TMENDRSCAN   = 0x00800000;
  // disassociates the transaction and mark rollback-only
  public final static int TMFAIL       = 0x20000000;
  // join existing branch
  public final static int TMJOIN       = 0x00200000;
  // no flags
  public final static int TMNOFLAGS    = 0x00000000;
  // using one-phase
  public final static int TMONEPHASE   = 0x40000000;
  // resuming suspended branch
  public final static int TMRESUME     = 0x08000000;
  // start a recovery scan
  public final static int TMSTARTRSCAN = 0x01000000;
  // disassociate from transaction branch
  public final static int TMSUCCESS    = 0x04000000;
  // suspend from transaction branch
  public final static int TMSUSPEND    = 0x02000000;

  public final static int XA_OK = 0;
  public final static int XA_RDONLY = 3;
  
  /**
   * Returns true if the specified resource has the same RM.
   */
  public boolean isSameRM(XAResource xa)
    throws XAException;
  
  /**
   * Sets the transaction timeout in seconds.
   */
  public boolean setTransactionTimeout(int timeout)
    throws XAException;
  
  /**
   * Gets the transaction timeout in seconds.
   */
  public int getTransactionTimeout()
    throws XAException;
  
  /**
   * Called when the resource is associated with a transaction.
   */
  public void start(Xid xid, int flags)
    throws XAException;
  
  /**
   * Called when the resource is is done with a transaction.
   */
  public void end(Xid xid, int flags)
    throws XAException;
  
  /**
   * Called to start the first phase of the commit.
   */
  public int prepare(Xid xid)
    throws XAException;
  
  /**
   * Called to commit.
   */
  public void commit(Xid xid, boolean onePhase)
    throws XAException;
  
  /**
   * Called to roll back.
   */
  public void rollback(Xid xid)
    throws XAException;
  
  /**
   * Called to forget an Xid that had a heuristic commit.
   */
  public void forget(Xid xid)
    throws XAException;
  
  /**
   * Called to find Xid's that need recovery.
   */
  public Xid[] recover(int flag)
    throws XAException;
}
