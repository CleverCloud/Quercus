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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.hessian;

import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resource
 */
public class HessianXAResource implements XAResource {
  private static final Logger log
    = Logger.getLogger(HessianXAResource.class.getName());
  
  private String _url;
  private Path _path;
  
  public HessianXAResource(String url)
  {
    _url = url;

    _path = Vfs.lookup(url);
  }

  public boolean isSameRM(XAResource xares)
  {
    if (! (xares instanceof HessianXAResource))
      return false;
    
    HessianXAResource rm = (HessianXAResource) xares;

    return _url.equals(rm._url);
  }

  public void start(Xid xid, int flags)
    throws XAException
  {
  }
  
  public void end(Xid xid, int flags)
    throws XAException
  {
  }
  
  public boolean setTransactionTimeout(int seconds)
    throws XAException
  {
    return true;
  }
  
  public int getTransactionTimeout()
    throws XAException
  {
    return 0;
  }

  public int prepare(Xid xid)
    throws XAException
  {
    try {
      //Object value = MetaStub.call(_path, "prepare", xidToString(xid));
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);

      throw new XAException(e.toString());
    }
    
    // also rdonly
    
    return XA_OK;
  }

  public Xid[]recover(int flag)
    throws XAException
  {
    // XXX: should query
    
    return null;
  }

  public void forget(Xid xid)
    throws XAException
  {
  }

  public void rollback(Xid xid)
    throws XAException
  {
    try {
      //MetaStub.call(_path, "rollback", xidToString(xid));
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);

      throw new XAException(e.toString());
    }
  }

  public void commit(Xid xid, boolean onephase)
    throws XAException
  {
    try {
      /*
      if (onephase)
        MetaStub.call(_path, "commitOnePhase", xidToString(xid));
      else
        MetaStub.call(_path, "commit", xidToString(xid));
        */
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);

      throw new XAException(e.toString());
    }
  }

  private static String xidToString(Xid xid)
  {
    byte []id = xid.getGlobalTransactionId();

    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < id.length; i++) {
      byte b = id[i];

      sb.append(toHex((b >> 4) & 0xf));
      sb.append(toHex(b & 0xf));
    }

    return sb.toString();
  }

  private static char toHex(int d)
  {
    if (d < 10)
      return (char) ('0' + d);
    else
      return (char) ('a' + d - 10);
  }
}
