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

package javax.transaction;

import javax.transaction.xa.XAResource;

/**
 * The transaction interface.  Transactions are normally
 * associated with a single thread.
 */
public interface Transaction {
 
  /**
   * Returns the transaction's status.
   */
  public int getStatus()
    throws SystemException;
  
  /**
   * Enlists a resource with the transaction.
   */
  public boolean enlistResource(XAResource xaRes)
    throws IllegalStateException, RollbackException, SystemException;
  
  /**
   * Delists a resource from the transaction.
   */
  public boolean delistResource(XAResource xaRes, int flag)
    throws IllegalStateException, SystemException;

  /**
   * Registers a synchronization.
   */
  public void registerSynchronization(Synchronization sync)
    throws IllegalStateException, RollbackException, SystemException;

  /**
   * Marks the transaction as rollback only.
   */
  public void setRollbackOnly()
    throws IllegalStateException, SystemException;
  
  /**
   * Commits the transaction.
   */
  public void commit()
    throws IllegalStateException, RollbackException, HeuristicMixedException,
           HeuristicRollbackException, SecurityException, SystemException;
  
  /**
   * Rolls the transaction back.
   */
  public void rollback()
    throws IllegalStateException, SystemException;
}
