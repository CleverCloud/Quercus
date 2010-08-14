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

package com.caucho.env.thread;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import com.caucho.util.Alarm;

/**
 * A generic pool of threads available for Alarms and Work tasks.
 */
abstract public class TaskWorker implements Runnable {
  private static final int TASK_PARK = 0;
  private static final int TASK_SLEEP = 1;
  private static final int TASK_READY = 2;
  
  private static final AtomicLong _idGen = new AtomicLong();
  
  private final AtomicInteger _taskState = new AtomicInteger();
  private final AtomicBoolean _isActive = new AtomicBoolean();

  private final ThreadPool _threadPool;
  private final ClassLoader _classLoader;
  private long _idleTimeout = 30000L;
  private boolean _isClosed;

  private volatile Thread _thread;

  protected TaskWorker()
  {
    _classLoader = Thread.currentThread().getContextClassLoader();
    _threadPool = ThreadPool.getCurrent();
  }

  protected boolean isPermanent()
  {
    return false;
  }
  
  public boolean isTaskActive()
  {
    return _isActive.get();
  }
  
  public boolean isClosed()
  {
    return _isClosed;
  }
  
  abstract public long runTask();

  public void destroy()
  {
    _isClosed = true;

    wake();
    
    Thread thread = _thread;

    if (thread != null)
      LockSupport.unpark(thread);
  }

  public final void wake()
  {
    int oldState = _taskState.getAndSet(TASK_READY);

    if (! _isActive.getAndSet(true)) {
      _threadPool.schedulePriority(this);
    }

    if (oldState == TASK_PARK) {
      Thread thread = _thread;

      if (thread != null) {
        LockSupport.unpark(thread);
      }
    }
  }

  protected String getThreadName()
  {
    // return getClass().getSimpleName() + "-" + _idGen.incrementAndGet();
    return toString() + "-" + _idGen.incrementAndGet();
  }
  
  protected void onThreadStart()
  {
  }
  
  protected void onThreadComplete()
  {
  }

  @Override
  public final void run()
  {
    String oldName = null;
    try {
      _thread = Thread.currentThread();
      _thread.setContextClassLoader(_classLoader);
      oldName = _thread.getName();
      _thread.setName(getThreadName());
      
      onThreadStart();

      long expires;

      do {
        expires = Alarm.getCurrentTimeActual() + _idleTimeout;
        while (_taskState.getAndSet(TASK_SLEEP) == TASK_READY) {
          long delta = runTask();
          
          if (delta < 0) {
            expires = Alarm.getCurrentTimeActual() + _idleTimeout;
          }
          else {
            expires = Alarm.getCurrentTimeActual() + delta;
          }
        }

        if (isClosed())
          return;
        
        if (_taskState.compareAndSet(TASK_SLEEP, TASK_PARK)) {
          Thread.interrupted();
          LockSupport.parkUntil(expires);
          
          if (isPermanent())
            _taskState.set(TASK_READY);
        }
      } while (_taskState.get() == TASK_READY
               || Alarm.getCurrentTimeActual() < expires
               || isPermanent());
    } finally {
      Thread thread = _thread;
      _thread = null;

      _isActive.set(false);

      if (_taskState.get() == TASK_READY)
        wake();

      onThreadComplete();
      
      if (thread != null && oldName != null)
        thread.setName(oldName);
    }
  }
}
