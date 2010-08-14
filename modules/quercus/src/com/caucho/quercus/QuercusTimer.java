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
 * @author Nam Nguyen
 */

package com.caucho.quercus;

import java.util.concurrent.locks.LockSupport;
import java.util.logging.Logger;

public class QuercusTimer
{
  private static final Logger log
    = Logger.getLogger(QuercusTimer.class.getName());

  private TimerThread _timerThread;
  
  private volatile long _currentTime;
  private volatile boolean _isCurrentTimeUsed;
  private volatile boolean _isSlowTime;
  
  private volatile boolean _isRunnable = true;
  
  public QuercusTimer()
  {
    _currentTime = System.currentTimeMillis();

    TimerThread timerThread = null;

    try {
      timerThread = new TimerThread();
      timerThread.start();
    } catch (Throwable e) {
      // should display for security manager issues
    }

    _timerThread = timerThread;
  }
  
  /**
   * Returns the approximate current time in milliseconds.
   * Convenient for minimizing system calls.
   */
  public long getCurrentTime()
  {
    // test avoids extra writes on multicore machines
    if (! _isCurrentTimeUsed) {
      if (_timerThread == null)
        return System.currentTimeMillis();
      
      if (_isSlowTime) {
        return System.currentTimeMillis();
      }
      else {
        _isCurrentTimeUsed = true;
      }
    }

    return _currentTime;
  }
  
  /**
   * Returns the exact current time in milliseconds.
   */
  public long getExactTime()
  {
    return System.currentTimeMillis();
  }
  
  /**
   * Returns the exact current time in nanoseconds.
   */
  public long getExactTimeNanoseconds()
  {
    return System.nanoTime();
  }
  
  public void shutdown()
  {
    _isRunnable = false;
  }
  
  class TimerThread extends Thread {
    TimerThread()
    {
      super("quercus-timer");

      setDaemon(true);
      setPriority(Thread.MAX_PRIORITY);
    }

    public void run()
    {
      int idleCount = 0;

      while (_isRunnable) {
        try {
          long now = System.currentTimeMillis();
            
          _currentTime = now;
            
          boolean isCurrentTimeUsed = _isCurrentTimeUsed;
          _isCurrentTimeUsed = false;
          
          if (isCurrentTimeUsed) {
            _isSlowTime = false;
          }
          else {
            idleCount++;

            if (idleCount == 10) {
              _isSlowTime = true;
            }
          }

          long sleepTime = _isSlowTime ? 1000L : 5L;
              
          LockSupport.parkNanos(sleepTime * 1000000L);
        } catch (Throwable e) {
        }
      }
    }
  }
}
