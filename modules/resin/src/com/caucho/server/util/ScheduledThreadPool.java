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
package com.caucho.server.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.inject.SingletonBindingHandle;
import com.caucho.env.thread.ThreadPool;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;

/**
 * Scheduler for custom services.
 */
public class ScheduledThreadPool implements ScheduledExecutorService,
    EnvironmentListener, java.io.Serializable {
  private static final long serialVersionUID = 1L;

  private static Logger log
    = Logger.getLogger(ScheduledThreadPool.class.getName());
  private static L10N L = new L10N(ScheduledThreadPool.class);

  private static EnvironmentLocal<ScheduledThreadPool> _local = new EnvironmentLocal<ScheduledThreadPool>();

  private ThreadPool _threadPool;

  private boolean _isShutdown;
  private boolean _isTerminated;

  private ClassLoader _loader;

  @SuppressWarnings("unchecked")
  private final Set<Future> _futureSet = new HashSet<Future>();

  private ScheduledThreadPool()
  {
    _loader = Thread.currentThread().getContextClassLoader();
    _threadPool = ThreadPool.getThreadPool();

    Environment.addEnvironmentListener(this);
  }

  public static ScheduledThreadPool getLocal()
  {
    synchronized (_local) {
      ScheduledThreadPool pool = _local.getLevel();

      if (pool == null) {
        pool = new ScheduledThreadPool();
        _local.set(pool);
      }

      return pool;
    }
  }

  //
  // Executor
  //

  /**
   * Launches a thread to execute a command.
   */
  @SuppressWarnings("unchecked")
  public void execute(Runnable command)
  {
    if (_isShutdown)
      throw new IllegalStateException("ThreadPool has closed");

    TaskFuture future = new TaskFuture(_loader, command, null);

    synchronized (_futureSet) {
      _futureSet.add(future);

      _threadPool.scheduleExecutorTask(future);
    }
  }

  //
  // ExecutorService
  //

  /**
   * Blocks until the tasks complete.
   */
  public boolean awaitTermination(long timeout, TimeUnit unit)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Invokes a set of tasks.
   */
  @SuppressWarnings("unchecked")
  public List invokeAll(Collection tasks)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Invokes a set of tasks.
   */
  @SuppressWarnings("unchecked")
  public List invokeAll(Collection tasks, long timeout, TimeUnit unit)
  {
    // XXX: todo
    throw new UnsupportedOperationException();
  }

  /**
   * Invokes a set of tasks.
   */
  @SuppressWarnings("unchecked")
  public Object invokeAny(Collection tasks)
  {
    // XXX: todo
    throw new UnsupportedOperationException();
  }

  /**
   * Invokes a set of tasks.
   */
  @SuppressWarnings("unchecked")
  public Object invokeAny(Collection tasks, long timeout, TimeUnit unit)
  {
    // XXX: todo
    throw new UnsupportedOperationException();
  }

  /**
   * Return true if the executor is shut down.
   */
  public boolean isShutdown()
  {
    return _isShutdown;
  }

  /**
   * Return true if the executor has completed shutting down.
   */
  public boolean isTerminated()
  {
    return _isTerminated;
  }

  /**
   * Starts the shutdown.
   */
  public void shutdown()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Starts the shutdown.
   */
  public List<Runnable> shutdownNow()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Submits a task for execution.
   */
  public <T> Future<T> submit(Callable<T> task)
  {
    if (_isShutdown)
      throw new IllegalStateException("ThreadPool has closed");

    TaskFuture<T> future = new TaskFuture<T>(_loader, task);

    synchronized (_futureSet) {
      _futureSet.add(future);

      _threadPool.scheduleExecutorTask(future);
    }

    return future;
  }

  /**
   * Submits a task for execution.
   */
  @SuppressWarnings("unchecked")
  public Future<?> submit(Runnable command)
  {
    if (_isShutdown)
      throw new IllegalStateException(L.l("Can't submit after ThreadPool has closed"));

    TaskFuture future = new TaskFuture(_loader, command, null);

    synchronized (_futureSet) {
      _futureSet.add(future);

      _threadPool.scheduleExecutorTask(future);
    }

    return future;
  }

  /**
   * Submits a task for execution.
   */
  public <T> Future<T> submit(Runnable task, T result)
  {
    if (_isShutdown)
      throw new IllegalStateException(L.l("Can't submit after ThreadPool has closed"));

    TaskFuture<T> future = new TaskFuture<T>(_loader, task, result);

    synchronized (_futureSet) {
      _futureSet.add(future);

      _threadPool.scheduleExecutorTask(future);
    }

    return future;
  }

  //
  // ScheduledExecutorService
  //

  /**
   * Schedules a future task.
   */
  @SuppressWarnings("unchecked")
  public <V> ScheduledFuture<V> schedule(Callable<V> callable,
                                         long delay,
                                         TimeUnit unit)
  {
    if (_isShutdown)
      throw new IllegalStateException(L.l("Can't submit after ThreadPool has closed"));

    long initialExpires = Alarm.getCurrentTime() + unit.toMillis(delay);

    AlarmFuture future = new AlarmFuture(_loader, callable,
                                         initialExpires, 0, 0);

    synchronized (_futureSet) {
      _futureSet.add(future);
    }

    future.queue();

    return future;
  }

  /**
   * Schedules a future task.
   */
  @SuppressWarnings("unchecked")
  public ScheduledFuture<?> schedule(Runnable command,
                                     long delay,
                                     TimeUnit unit)
  {
    if (_isShutdown)
      throw new IllegalStateException(L.l("Can't submit after ThreadPool has closed"));

    long initialExpires = Alarm.getCurrentTime() + unit.toMillis(delay);

    AlarmFuture future = new AlarmFuture(_loader, command, initialExpires, 0, 0);

    synchronized (_futureSet) {
      _futureSet.add(future);
    }

    future.queue();

    return future;
  }

  /**
   * Schedules a future task.
   */
  @SuppressWarnings("unchecked")
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                                long initialDelay,
                                                long period,
                                                TimeUnit unit)
  {
    if (_isShutdown)
      throw new IllegalStateException(L.l("Can't submit after ThreadPool has closed"));

    long initialExpires = Alarm.getExactTime() + unit.toMillis(initialDelay);

    AlarmFuture future = new AlarmFuture(_loader, command, initialExpires,
                                         unit.toMillis(period), 0);

    synchronized (_futureSet) {
      _futureSet.add(future);
    }

    future.queue();

    return future;
  }

  /**
   * Schedules with fixed delay
   */
  @SuppressWarnings("unchecked")
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                                   long initialDelay,
                                                   long delay,
                                                   TimeUnit unit)
  {
    if (_isShutdown)
      throw new IllegalStateException(L.l("Can't submit after ThreadPool has closed"));

    long initialExpires = Alarm.getCurrentTime() + unit.toMillis(initialDelay);

    AlarmFuture future = new AlarmFuture(_loader, command,
                                         initialExpires, 0, unit.toMillis(delay));

    synchronized (_futureSet) {
      _futureSet.add(future);
    }

    future.queue();

    return future;
  }

  //
  // Timer
  //

  /**
   * Returns the Timer for this pool.
   */
  public Timer getTimer()
  {
    throw new UnsupportedOperationException();
  }

  //
  // lifecycle
  //

  /**
   * Stops the pool on environment shutdown.
   */
  @SuppressWarnings("unchecked")
  private void stop()
  {
    _isShutdown = true;

    while (true) {
      Future future = null;

      synchronized (_futureSet) {
        Iterator<Future> iter = _futureSet.iterator();

        if (iter.hasNext()) {
          future = iter.next();

          _futureSet.remove(future);
        }
      }

      if (future == null)
        break;

      future.cancel(true);
    }
  }

  @SuppressWarnings("unchecked")
  void removeFuture(Future future)
  {
    synchronized (_futureSet) {
      _futureSet.remove(future);
    }
  }

  //
  // Environment callbacks.
  //

  /**
   * Called when the environment config phase
   */
  public void environmentConfigure(EnvironmentClassLoader loader)
  {
  }

  /**
   * Called when the environment bind phase
   */
  public void environmentBind(EnvironmentClassLoader loader)
  {
  }

  /**
   * Called when the environment starts.
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
  }

  /**
   * Called when the environment stops.
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
    stop();
  }

  /**
   * Serialize to a webbeans handle
   */
  public Object writeReplace()
  {
    return new SingletonBindingHandle(ScheduledExecutorService.class);
  }

  @Override
  public String toString()
  {
    if (_loader instanceof EnvironmentClassLoader) {
      return (getClass().getSimpleName()
              + "[" + ((EnvironmentClassLoader) _loader).getId() + "]");
    }
    else
      return getClass().getSimpleName() + "[" + _loader + "]";
  }

  class TaskFuture<T> implements Future<T>, Runnable {
    private final ClassLoader _loader;
    private final Callable<T> _callable;
    private final Runnable _runnable;

    private Thread _thread;

    private boolean _isCancelled;
    private boolean _isDone;

    private Exception _exception;
    private T _value;

    TaskFuture(ClassLoader loader, Callable<T> callable)
    {
      _loader = loader;
      _callable = callable;
      _runnable = null;
    }

    TaskFuture(ClassLoader loader, Runnable runnable, T result)
    {
      _loader = loader;
      _callable = null;
      _runnable = runnable;
      _value = result;
    }

    public boolean isCancelled()
    {
      return _isCancelled;
    }

    public boolean isDone()
    {
      return _isDone;
    }

    public boolean cancel(boolean mayInterrupt)
    {
      synchronized (this) {
        removeFuture(this);

        if (_isCancelled || _isDone)
          return false;

        _isCancelled = true;

        notifyAll();
      }

      Thread thread = _thread;

      if (mayInterrupt && thread != null)
        thread.interrupt();

      return true;
    }

    public T get()
      throws InterruptedException, ExecutionException
    {
      try {
        return get(Long.MAX_VALUE / 2, TimeUnit.MILLISECONDS);
      } catch (TimeoutException e) {
        throw new IllegalStateException(e);
      }
    }

    public T get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException
    {
      long expire = Alarm.getCurrentTime() + unit.toMillis(timeout);

      synchronized (this) {
        while (!_isDone && !_isCancelled && Alarm.getCurrentTime() < expire
               && !Thread.currentThread().isInterrupted()) {
          if (! Alarm.isTest()) {
            long delta = expire - Alarm.getCurrentTime();
            
            if (delta > 0) {
              wait(expire - Alarm.getCurrentTime());
            }
          }
          else {
            wait(1000);
            break;
          }
        }
      }

      if (_exception != null)
        throw new ExecutionException(_exception);
      else if (_isDone)
        return _value;
      else if (_isCancelled)
        throw new CancellationException();
      else
        throw new TimeoutException();
    }

    public void run()
    {
      _thread = Thread.currentThread();
      ClassLoader oldLoader = _thread.getContextClassLoader();

      try {
        if (_isCancelled || _isDone || _isShutdown)
          return;

        _thread.setContextClassLoader(_loader);

        if (_callable != null)
          _value = _callable.call();
        else
          _runnable.run();
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        _exception = e;
      } finally {
        _thread.setContextClassLoader(oldLoader);
        _thread = null;
        _isDone = true;

        _threadPool.completeExecutorTask();

        // alarm

        removeFuture(this);

        synchronized (this) {
          notifyAll();
        }
      }
    }

    public String toString()
    {
      Object task = _callable != null ? _callable : _runnable;

      if (_isDone)
        return "TaskFuture[" + task + ",done]";
      else if (_thread != null) {
        if (Alarm.isTest())
          return "TaskFuture[" + task + ",active]";
        else
          return "TaskFuture[" + task + "," + _thread + "]";
      }
      else if (_isCancelled)
        return "TaskFuture[" + task + ",cancelled]";
      else
        return "TaskFuture[" + task + ",pending]";
    }
  }

  class AlarmFuture<T> implements ScheduledFuture<T>, AlarmListener {
    private final String _name;

    private final ClassLoader _loader;
    private final Callable<T> _callable;
    private final Runnable _runnable;

    private final Alarm _alarm;

    private final long _initialExpires;
    private final long _period;
    private final long _delay;

    private long _nextTime;

    private Thread _thread;

    private boolean _isCancelled;
    private boolean _isDone;
    private int _alarmCount;

    private Exception _exception;
    private T _value;

    AlarmFuture(ClassLoader loader, Callable<T> callable, long initialExpires,
        long period, long delay)
    {
      _name = "Scheduled[" + callable + "]";

      _loader = loader;
      _callable = callable;
      _runnable = null;

      _initialExpires = initialExpires;
      _period = period;
      _delay = delay;
      _nextTime = initialExpires;

      _alarm = new Alarm(_name, this, loader);
    }

    AlarmFuture(ClassLoader loader, Runnable runnable, long initialExpires,
        long period, long delay)
    {
      _name = "Scheduled[" + runnable + "]";

      _loader = loader;
      _callable = null;
      _runnable = runnable;

      _initialExpires = initialExpires;
      _period = period;
      _delay = delay;

      _alarm = new Alarm(_name, this, loader);
    }

    void queue()
    {
      _alarm.queueAt(_initialExpires);
    }

    public boolean isCancelled()
    {
      return _isCancelled;
    }

    public boolean isDone()
    {
      return _isDone;
    }

    public long getDelay(TimeUnit unit)
    {
      long delay = _nextTime - Alarm.getCurrentTime();

      return TimeUnit.MILLISECONDS.convert(delay, unit);
    }

    public int compareTo(Delayed b)
    {
      long delta = (getDelay(TimeUnit.MILLISECONDS) - b
          .getDelay(TimeUnit.MILLISECONDS));

      if (delta < 0)
        return -1;
      else if (delta > 0)
        return 1;
      else
        return 0;
    }

    public boolean cancel(boolean mayInterrupt)
    {
      synchronized (this) {
        if (_isCancelled || _isDone)
          return false;

        _isCancelled = true;

        _alarm.dequeue();

        notifyAll();
      }

      removeFuture(this);

      Thread thread = _thread;

      if (mayInterrupt && thread != null)
        thread.interrupt();

      return true;
    }

    public T get() throws InterruptedException, ExecutionException
    {
      try {
        return get(Long.MAX_VALUE / 2, TimeUnit.MILLISECONDS);
      } catch (TimeoutException e) {
        throw new IllegalStateException(e);
      }
    }

    public T get(long timeout, TimeUnit unit) throws InterruptedException,
        ExecutionException, TimeoutException
    {
      long expire = Alarm.getCurrentTimeActual() + unit.toMillis(timeout);
      int count = _alarmCount;

      while (!_isDone && !_isCancelled && count == _alarmCount
          && Alarm.getCurrentTimeActual() < expire
          && !Thread.currentThread().isInterrupted()) {
        synchronized (this) {
          long delta = expire - Alarm.getCurrentTimeActual();
          
          if (delta > 0)
            wait(delta);
        }
      }

      if (_exception != null)
        throw new ExecutionException(_exception);
      else if (_isDone || count != _alarmCount)
        return _value;
      else if (_isCancelled)
        throw new CancellationException();
      else
        throw new TimeoutException();
    }

    @Override
    public void handleAlarm(Alarm alarm)
    {
      if (_isCancelled || _isDone || _isShutdown)
        return;

      _thread = Thread.currentThread();
      ClassLoader oldLoader = _thread.getContextClassLoader();
      String oldName = _thread.getName();

      try {
        _thread.setContextClassLoader(_loader);
        _thread.setName(_name);

        if (_callable != null)
          _value = _callable.call();
        else
          _runnable.run();
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);

        _exception = e;
        _isCancelled = true;
      } finally {
        _thread.setContextClassLoader(oldLoader);
        _thread.setName(oldName);
        _thread = null;

        synchronized (this) {
          _alarmCount++;

          if (_isCancelled || _isDone) {
            removeFuture(this);
          }
          else if (_delay > 0) {
            _nextTime = Alarm.getCurrentTime() + _delay;

            _alarm.queue(_delay);
          }
          else if (_period > 0) {
            long now = Alarm.getCurrentTime();
            long next;

            do {
              next = _initialExpires + _alarmCount * _period;

              if (next < now)
                _alarmCount++;
            } while (next < now);

            _alarm.queueAt(next);
          }
          else {
            _isDone = true;
            removeFuture(this);
          }

          notifyAll();
        }
      }
    }

    public String toString()
    {
      Object task = _callable != null ? _callable : _runnable;

      if (_isDone)
        return "AlarmFuture[" + task + ",done]";
      else if (_thread != null) {
        if (Alarm.isTest())
          return "AlarmFuture[" + task + ",active]";
        else
          return "AlarmFuture[" + task + "," + _thread + "]";
      } else if (_isCancelled)
        return "AlarmFuture[" + task + ",cancelled]";
      else
        return "AlarmFuture[" + task + ",pending]";
    }
  }
}
