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

import java.util.concurrent.locks.LockSupport;

import com.caucho.util.Alarm;

/**
 * A generic pool of threads available for Alarms and Work tasks.
 */
final class ThreadTask {
  private final Runnable _runnable;
  private final ClassLoader _loader;
  private volatile Thread _thread;

  ThreadTask(Runnable runnable, ClassLoader loader, Thread thread)
  {
    _runnable = runnable;
    _loader = loader;
    _thread = thread;
  }

  final Runnable getRunnable()
  {
    return _runnable;
  }

  final ClassLoader getLoader()
  {
    return _loader;
  }
  
  void clearThread()
  {
    _thread = null;
  }

  final void wake()
  {
    Thread thread = _thread;
    _thread = null;

    if (thread != null)
      LockSupport.unpark(thread);
  }

  final void park(long expires)
  {
    Thread thread = _thread;

    while (_thread != null
           && Alarm.getCurrentTimeActual() < expires) {
      try {
        Thread.interrupted();
        LockSupport.parkUntil(thread, expires);
      } catch (Exception e) {
      }
    }

    /*
      if (_thread != null) {
        System.out.println("TIMEOUT:" + thread);
        Thread.dumpStack();
      }
     */

    _thread = null;
  }
}
