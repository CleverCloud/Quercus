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
 * A generic XA exception
 */
public class XAException extends Exception {
  public final static int XA_RBBASE = 100;
  public final static int XA_RBROLLBACK = XA_RBBASE;
  public final static int XA_RBCOMMFAIL = XA_RBBASE + 1;
  public final static int XA_RBDEADLOCK = XA_RBBASE + 2;
  public final static int XA_RBINTEGRITY = XA_RBBASE + 3;
  public final static int XA_RBOTHER = XA_RBBASE + 4;
  public final static int XA_RBPROTO = XA_RBBASE + 5;
  public final static int XA_RBTIMEOUT = XA_RBBASE + 6;
  public final static int XA_RBTRANSIENT = XA_RBBASE + 7;
  public final static int XA_RBEND = XA_RBBASE + 8;
  
  public final static int XA_NOMIGRATE = 9;
  public final static int XA_HEURHAZ = 8;
  public final static int XA_HEURCOM = 7;
  public final static int XA_HEURRB = 6;
  public final static int XA_HEURMIX = 5;
  public final static int XA_RDONLY = 3;
  
  public final static int XAER_RMERR = -3;
  public final static int XAER_NOTA = -4;
  public final static int XAER_INVAL = -5;
  public final static int XAER_PROTO = -6;
  public final static int XAER_RMFAIL = -7;
  public final static int XAER_DUPID = -8;
  public final static int XAER_OUTSIDE = -9;
  
  public int errorCode;
  
  /**
   * Creates an exception
   */
  public XAException()
  {
  }
  
  /**
   * Creates an exception
   */
  public XAException(String msg)
  {
    super(msg);
  }
  
  /**
   * Creates an exception
   */
  public XAException(int errCode)
  {
    this.errorCode = errCode;
  }

  /**
   * Creates a wrapped exception.
   */
  public XAException(Throwable rootCause)
  {
    super(rootCause);
  }

  /**
   * Creates a wrapped exception.
   */
  public XAException(String msg, Throwable rootCause)
  {
    super(msg, rootCause);
  }
}
