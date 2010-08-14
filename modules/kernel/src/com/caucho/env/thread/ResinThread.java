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

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A generic pool of threads available for Alarms and Work tasks.
 */
final class ResinThread extends Thread {
  private static final Logger log 
    = Logger.getLogger(ResinThread.class.getName());
  
  private final ThreadPool _pool;
  private final int _id;
  private final String _name;
  
  private volatile ResinThread _next;
  private boolean _isClose;
  
  private volatile ClassLoader _taskLoader;
  
  private final AtomicReference<Runnable> _taskRef
    = new AtomicReference<Runnable>();

  ResinThread(ThreadPool pool, int id)
  {
    _pool = pool;
    _id = id;
    _name = "resin-" + _id;
    
    setDaemon(true);
  }

  /**
   * Returns the name.
   */
  public String getDebugName()
  {
    return _name;
  }

  /**
   * Returns the thread id.
   */
  public long getThreadId()
  {
    return getId();
  }
  
  final ResinThread getNext()
  {
    return _next;
  }
  
  final void setNext(ResinThread thread)
  {
    _next = thread;
  }

  /**
   * Sets the thread's task.  Must be called inside _idleLock
   */
  final boolean scheduleTask(Runnable task, ClassLoader loader)
  {
    if (_isClose)
      return false;
    
    _taskLoader = loader;
    if (_taskRef.getAndSet(task) != null) {
      System.out.println("BAD: getandset");
    }
    LockSupport.unpark(this);

    return true;
  }

  /**
   * Wake the thread.  Called outside of _idleLock
   */
  final void close()
  {
    _isClose = true;
    LockSupport.unpark(this);
  }

  /**
   * The main thread execution method.
   */
  @Override
  public void run()
  {
    try {
      _pool.onThreadStart();
      
      runTasks();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      _pool.onThreadEnd();
    }
  }

  private void runTasks()
  {
    ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
    
    
    Thread thread = this;
    setName(_name);
    
    while (! _isClose) {
      Runnable task = null;
      ClassLoader classLoader = null;

      ThreadTask taskItem = _pool.nextTask(this);

      if (taskItem != null) {
        task = taskItem.getRunnable();
        classLoader = taskItem.getLoader();
        
        taskItem.wake();
      }
      else if (_pool.isIdleExpire()){
        return;
      }
      else if ((task = waitForTask()) != null) {
        classLoader = _taskLoader;
        _taskLoader = null;
      }
      else {
        return;
      }

      try {
        // if the task is available, run it in the proper context
        thread.setContextClassLoader(classLoader);

        task.run();
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      } finally {
        thread.setContextClassLoader(systemClassLoader);
      }
    }
  }
  
  private Runnable waitForTask()
  {
    _pool.pushIdleThread(this);
    
    /*
    for (int i = 1000; i >= 0; i--) {
      Runnable task = _taskRef.getAndSet(null);

      if (task != null)
        return task;
    }
    */
    
    while (! _isClose) {
      Runnable task = _taskRef.getAndSet(null);

      if (task != null)
        return task;

      setName(_name);
      Thread.interrupted();
      LockSupport.park();
    }
    
    return null;
  }
}
