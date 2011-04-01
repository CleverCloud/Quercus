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

package com.caucho.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.thread.TaskWorker;
import com.caucho.env.thread.ThreadPool;
import com.caucho.loader.ClassLoaderListener;
import com.caucho.loader.DynamicClassLoader;

/**
 * The alarm class provides a lightweight event scheduler.  This allows
 * an objects to schedule a timeout without creating a new thread.
 *
 * <p>A separate thread periodically tests the queue for alarms ready.
 */
public class Alarm implements ThreadTask, ClassLoaderListener {
  private static final Logger log
    = Logger.getLogger(Alarm.class.getName());

  private static final ClassLoader _systemLoader;

  private static final AlarmThread _alarmThread;
  private static final CoordinatorThread _coordinatorThread;

  private static volatile long _currentTime;
  private static volatile boolean _isCurrentTimeUsed;
  private static volatile boolean _isSlowTime;

  private static final AlarmHeap _heap = new AlarmHeap();

  private static final AtomicInteger _runningAlarmCount
    = new AtomicInteger();

  private static final boolean _isStressTest;

  private static long _testTime;
  private static long _testNanoDelta;
  
  private long _wakeTime;
  private AlarmListener _listener;
  private ClassLoader _contextLoader;
  private String _name;

  private boolean _isPriority = true;

  private int _heapIndex = 0;

  private volatile boolean _isRunning;

  /**
   * Create a new wakeup alarm with a designated listener as a callback.
   * The alarm is not scheduled.
   */
  protected Alarm()
  {
    this("alarm");
  }

  protected Alarm(String name)
  {
    _name = name;

    addEnvironmentListener();
  }

  /**
   * Create a new wakeup alarm with a designated listener as a callback.
   * The alarm is not scheduled.
   */
  public Alarm(AlarmListener listener)
  {
    this("alarm[" + listener + "]", listener);
  }

  /**
   * Create a new wakeup alarm with a designated listener as a callback.
   * The alarm is not scheduled.
   */
  public Alarm(String name, AlarmListener listener)
  {
    this(name, listener, Thread.currentThread().getContextClassLoader());
  }

  /**
   * Create a new wakeup alarm with a designated listener as a callback.
   * The alarm is not scheduled.
   */
  public Alarm(String name, AlarmListener listener, ClassLoader loader)
  {
    this(name);

    setListener(listener);
    setContextLoader(loader);
  }

  /**
   * Create a new wakeup alarm with a designated listener as a callback.
   * The alarm is not scheduled.
   */
  public Alarm(String name,
               AlarmListener listener,
               long delta,
               ClassLoader loader)
  {
    this(name);

    setListener(listener);
    setContextLoader(loader);

    queue(delta);
  }

  /**
   * Creates a named alarm and schedules its wakeup.
   *
   * @param name the object prepared to receive the callback
   * @param listener the object prepared to receive the callback
   * @param delta the time in milliseconds to wake up
   */
  public Alarm(String name, AlarmListener listener, long delta)
  {
    this(name, listener);

    queue(delta);
  }

  /**
   * Creates a new alarm and schedules its wakeup.
   *
   * @param listener the object prepared to receive the callback
   * @param delta the time in milliseconds to wake up
   */
  public Alarm(AlarmListener listener, long delta)
  {
    this(listener);

    queue(delta);
  }

  /**
   * Returns the alarm name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the alarm name.
   */
  protected void setName(String name)
  {
    _name = name;
  }
  
  public static boolean isActive()
  {
    return _testTime == 0 && _alarmThread != null;
  }

  /**
   * Returns the approximate current time in milliseconds.
   * Convenient for minimizing system calls.
   */
  public static long getCurrentTime()
  {
    // test avoids extra writes on multicore machines
    if (! _isCurrentTimeUsed) {
      if (_testTime > 0)
        return _testTime;
      
      if (_alarmThread == null)
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
   * Gets current time, handling test
   */
  public static long getCurrentTimeActual()
  {
    if (_testTime > 0)
      return System.currentTimeMillis();
    else
      return getCurrentTime();
  }

  /**
   * Returns the exact current time in milliseconds.
   */
  public static long getExactTime()
  {
    if (_testTime > 0)
      return _testTime;
    else {
      return System.currentTimeMillis();
    }
  }

  /**
   * Returns the exact current time in nanoseconds.
   */
  public static long getExactTimeNanoseconds()
  {
    if (_testTime > 0) {
      // php/190u
      // System.nanoTime() is not related to currentTimeMillis(), so return
      // a different offset.  See System.nanoTime() javadoc

      return (_testTime - 10000000) * 1000000L + _testNanoDelta;
    }

    return System.nanoTime();
  }

  /**
   * Returns true for testing.
   */
  public static boolean isTest()
  {
    return _testTime > 0;
  }

  /**
   * Yield if in test mode to maintain ordering
   */
  public static void yieldIfTest()
  {
    if (_testTime > 0) {
      // Thread.yield();
    }
  }

  /**
   * Returns the wake time of this alarm.
   */
  public long getWakeTime()
  {
    return _wakeTime;
  }
  
  void setWakeTime(long wakeTime)
  {
    _wakeTime = wakeTime;
  }
  
  int getHeapIndex()
  {
    return _heapIndex;
  }
  
  void setHeapIndex(int index)
  {
    _heapIndex = index;
  }

  /**
   * Return the alarm's listener.
   */
  public AlarmListener getListener()
  {
    return _listener;
  }

  /**
   * Sets the alarm's listener.
   */
  public void setListener(AlarmListener listener)
  {
    _listener = listener;
  }

  /**
   * Sets the alarm's context loader
   */
  public void setContextLoader(ClassLoader loader)
  {
    _contextLoader = loader;
  }

  /**
   * Sets the alarm's context loader
   */
  public ClassLoader getContextLoader()
  {
    return _contextLoader;
  }

  /**
   * Returns true if the alarm is currently queued.
   */
  public boolean isQueued()
  {
    return _heapIndex != 0;
  }

  /**
   * Returns true if the alarm is currently running
   */
  boolean isRunning()
  {
    return _isRunning;
  }

  /**
   * True for a priority alarm (default)
   */
  public void setPriority(boolean isPriority)
  {
    _isPriority = isPriority;
  }

  /**
   * True for a priority alarm (default)
   */
  public boolean isPriority()
  {
    return _isPriority;
  }

  /**
   * Registers the alarm with the environment listener for auto-close
   */
  protected void addEnvironmentListener()
  {
    // Environment.addClassLoaderListener(this);
  }

  /**
   * Queue the alarm for wakeup.
   *
   * @param delta time in milliseconds to wake
   */
  public void queue(long delta)
  {
    long now = getCurrentTime();
    
    boolean isNotify = _heap.queueAt(this, now + delta);

    if (isNotify) {
      _coordinatorThread.wake();
    }
  }

  /**
   * Queue the alarm for wakeup.
   *
   * @param delta time in milliseconds to wake
   */
  public void queueAt(long wakeTime)
  {
    boolean isNotify = _heap.queueAt(this, wakeTime);

    if (isNotify) {
      _coordinatorThread.wake();
    }
  }

  /**
   * Remove the alarm from the wakeup queue.
   */
  public void dequeue()
  {
    if (_heapIndex > 0)
      _heap.dequeue(this);
  }

  /**
   * Runs the alarm.  This is only called from the worker thread.
   */
  public void run()
  {
    try {
      handleAlarm();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      _isRunning = false;
      _runningAlarmCount.decrementAndGet();
    }
  }

  /**
   * Handles the alarm.
   */
  private void handleAlarm()
  {
    AlarmListener listener = getListener();

    if (listener == null)
      return;

    Thread thread = Thread.currentThread();
    ClassLoader loader = getContextLoader();

    if (loader != null)
      thread.setContextClassLoader(loader);
    else
      thread.setContextClassLoader(_systemLoader);

    try {
      listener.handleAlarm(this);
    } finally {
      thread.setContextClassLoader(_systemLoader);
    }
  }

  /**
   * Handles the case where a class loader has completed initialization
   */
  public void classLoaderInit(DynamicClassLoader loader)
  {
  }

  /**
   * Handles the case where a class loader is dropped.
   */
  public void classLoaderDestroy(DynamicClassLoader loader)
  {
    close();
  }

  /**
   * Closes the alarm instance
   */
  public void close()
  {
    dequeue();

    // server/16a{0,1}
    /*
    _listener = null;
    _contextLoader = null;
    */
  }

  // test

  static void testClear()
  {
    _heap.testClear();
  }
  
  static void setTestTime(long time)
  {
    _testTime = time;

    if (_testTime > 0) {
      if (time < _currentTime) {
        testClear();
      }

      _currentTime = time;
    }
    else {
      _currentTime = System.currentTimeMillis();
    }

    Alarm alarm;

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      while ((alarm = _heap.extractAlarm(getCurrentTime())) != null) {
        alarm.run();
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  static void setTestNanoDelta(long delta)
  {
    _testNanoDelta = delta;
  }

  public String toString()
  {
    return "Alarm[" + _name + "]";
  }

  static class AlarmThread extends Thread {
    AlarmThread()
    {
      super("resin-timer");

      setDaemon(true);
      setPriority(Thread.MAX_PRIORITY);
    }

    public void run()
    {
      int idleCount = 0;

      while (true) {
        try {
          if (_testTime > 0) {
            _currentTime = _testTime;

            LockSupport.park();
            
            continue;
          }
          
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

  static class CoordinatorThread extends TaskWorker {
    @Override
    protected boolean isPermanent()
    {
      return true;
    }
    
    /**
     * Runs the coordinator task.
     */
    @Override
    public long runTask()
    {
      try {
        Alarm alarm;
        
        if ((alarm = _heap.extractAlarm(getCurrentTime())) != null) {
          // throttle alarm invocations by 5ms so quick alarms don't need
          // extra threads
          /*
            if (_concurrentAlarmThrottle < _runningAlarmCount.get()) {
              try {
                Thread.sleep(5);
              } catch (Throwable e) {
              }
            }
           */

          _runningAlarmCount.incrementAndGet();

          long now;

          if (_isStressTest)
            now = Alarm.getExactTime();
          else
            now = Alarm.getCurrentTime();

          long delta = now - alarm._wakeTime;

          if (delta > 10000) {
            log.warning(this + " slow alarm " + alarm + " " + delta + "ms");
          }
          else if (_isStressTest && delta > 100) {
            System.out.println(this + " slow alarm " + alarm + " " + delta);
          }

          if (alarm.isPriority())
            ThreadPool.getThreadPool().schedulePriority(alarm);
          else
            ThreadPool.getThreadPool().schedule(alarm);
        }
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }

      long next = _heap.nextAlarmTime();
      // #3548 - getCurrentTime for consistency
      long now = getCurrentTime();

      long delta;
      
      if (next < 0)
        delta = 120000L;
      else
        delta = next - now;
      
      return delta;
    }
  }

  static {
    _currentTime = System.currentTimeMillis();

    ClassLoader systemLoader = null;
    AlarmThread alarmThread = null;
    CoordinatorThread coordinator = null;

    try {
      systemLoader = ClassLoader.getSystemClassLoader();
    } catch (Throwable e) {
    }

    try {
      ClassLoader loader = Alarm.class.getClassLoader();

      if (loader == null
          || loader == systemLoader
          || systemLoader != null && loader == systemLoader.getParent()) {
        alarmThread = new AlarmThread();
        alarmThread.start();

        coordinator = new CoordinatorThread();
        Thread coordinatorThread = new Thread(coordinator);
        coordinatorThread.setDaemon(true);
        coordinatorThread.setPriority(Thread.MAX_PRIORITY);
        coordinatorThread.setName("alarm-coordinator");
        coordinatorThread.start();
      }
    } catch (Throwable e) {
      // should display for security manager issues
      log.fine("Alarm not started: " + e);
    }

    _systemLoader = systemLoader;
    _alarmThread = alarmThread;
    _coordinatorThread = coordinator;

    _isStressTest = System.getProperty("caucho.stress.test") != null;
  }
}
