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

package com.caucho.server.distlock;

import com.caucho.server.cluster.Server;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages the distributed lock
 */
public class SingleLock implements Lock {
  private String _name;
  private final Lock _lock = new ReentrantLock();
  
  public SingleLock(String name)
  {
    _name = name;
  }

  //
  // Lock API
  //

  public void lock()
  {
    _lock.lock();
  }

  public void lockInterruptibly()
    throws InterruptedException
  {
    _lock.lockInterruptibly();
  }

  public boolean tryLock()
  {
    return _lock.tryLock();
  }

  public boolean tryLock(long time, TimeUnit unit)
    throws InterruptedException
  {
    return _lock.tryLock(time, unit);
  }

  public void unlock()
  {
    _lock.unlock();
  }

  public Condition newCondition()
  {
    return _lock.newCondition();
  }
}
