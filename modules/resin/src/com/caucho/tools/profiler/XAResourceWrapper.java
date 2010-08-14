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
 * @author Sam
 */


package com.caucho.tools.profiler;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

public class XAResourceWrapper
  implements XAResource
{
  private final ProfilerPoint _profilerPoint;
  private final XAResource _xaResource;

  public XAResourceWrapper(ProfilerPoint profilerPoint, XAResource xaResource)
  {
    _profilerPoint = profilerPoint;
    _xaResource = xaResource;
  }

  public void commit(Xid xid, boolean b)
    throws XAException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _xaResource.commit(xid, b);
    }
    finally {
      profiler.finish();
    }
  }

  public void end(Xid xid, int i)
    throws XAException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _xaResource.end(xid, i);
    }
    finally {
      profiler.finish();
    }
  }

  public void forget(Xid xid)
    throws XAException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _xaResource.forget(xid);
    }
    finally {
      profiler.finish();
    }
  }

  public int getTransactionTimeout()
    throws XAException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _xaResource.getTransactionTimeout();
    }
    finally {
      profiler.finish();
    }
  }

  public boolean isSameRM(XAResource xaResource)
    throws XAException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _xaResource.isSameRM(xaResource);
    }
    finally {
      profiler.finish();
    }
  }

  public int prepare(Xid xid)
    throws XAException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _xaResource.prepare(xid);
    }
    finally {
      profiler.finish();
    }
  }

  public Xid[] recover(int i)
    throws XAException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _xaResource.recover(i);
    }
    finally {
      profiler.finish();
    }
  }

  public void rollback(Xid xid)
    throws XAException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _xaResource.rollback(xid);
    }
    finally {
      profiler.finish();
    }
  }

  public boolean setTransactionTimeout(int i)
    throws XAException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      return _xaResource.setTransactionTimeout(i);
    }
    finally {
      profiler.finish();
    }
  }

  public void start(Xid xid, int i)
    throws XAException
  {
    Profiler profiler = _profilerPoint.start();

    try {
      _xaResource.start(xid, i);
    }
    finally {
      profiler.finish();
    }
  }

  public String toString()
  {
    return "XAResourceWrapper[" + _profilerPoint.getName() + "]";
  }
}
