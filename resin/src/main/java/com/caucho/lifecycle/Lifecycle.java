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

package com.caucho.lifecycle;

import com.caucho.util.Alarm;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lifecycle class.
 */
public final class Lifecycle implements LifecycleState {
  private final Logger _log;
  private String _name;
  
  private Level _level = Level.FINE;
  private Level _lowLevel = Level.FINER;
  
  private final AtomicInteger _state = new AtomicInteger();

  private long _activeCount;
  private long _failCount;

  private long _lastFailTime;
  private long _lastChangeTime;

  private ArrayList<WeakReference<LifecycleListener>> _listeners;

  /**
   * Creates an anonymous lifecycle.
   */
  public Lifecycle()
  {
    _log = null;
  }

  /**
   * Creates an lifecycle with logger and name.
   */
  public Lifecycle(Logger log)
  {
    _log = log;
  }

  /**
   * Creates an lifecycle with logger and name.
   */
  public Lifecycle(Logger log, String name)
  {
    _log = log;
    _name = name;
  }

  /**
   * Creates an lifecycle with logger,  a name, and a level.
   */
  public Lifecycle(Logger log, String name, Level level)
  {
    _log = log;
    _name = name;
    
    setLevel(level);
  }

  /**
   * Gets the lifecycle name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the lifecycle name, and the level to Level.INFO.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the lifecycle logging level.
   */
  public Level getLevel()
  {
    return _level;
  }

  /**
   * Sets the lifecycle logging level.
   */
  public void setLevel(Level level)
  {
    _level = level;

    if (level.intValue() < _lowLevel.intValue())
      _lowLevel = level;
  }

  /**
   * Returns the current state.
   */
  public int getState()
  {
    return _state.get();
  }

  /**
   * Returns the state name for the passed state.
   */
  public static String getStateName(int state)
  {
    switch (state) {
      case IS_NEW:
        return "new";
      case IS_INITIALIZING:
        return "initializing";
      case IS_INIT:
        return "init";
      case IS_STARTING:
        return "starting";
      case IS_ACTIVE:
        return "active";
      case IS_FAILED:
        return "failed";
      case IS_STOPPING:
        return "stopping";
      case IS_STOPPED:
        return "stopped";
      case IS_DESTROYING:
        return "destroying";
      case IS_DESTROYED:
        return "destroyed";
      default:
        return "unknown";
    }
  }

  /**
   * Returns the current state name.
   */
  public String getStateName()
  {
    return getStateName(_state.get());
  }

  /**
   * Returns the last lifecycle change time.
   */
  public long getLastChangeTime()
  {
    return _lastChangeTime;
  }

  /**
   * Returns the last failure time.
   */
  public long getLastFailTime()
  {
    return _lastFailTime;
  }

  /**
   * Returns the number of times the lifecycle has switched to active.
   */
  public long getActiveCount()
  {
    return _activeCount;
  }

  /**
   * Returns the number of times the lifecycle has switched to failing.
   */
  public long getFailCount()
  {
    return _failCount;
  }

  /**
   * Returns true for the initializing state.
   */
  public boolean isInitializing()
  {
    return _state.get() == IS_INITIALIZING;
  }

  /**
   * Returns true for the init state.
   */
  public boolean isInit()
  {
    return _state.get() == IS_INIT;
  }

  /**
   * Returns true for the init state.
   */
  public boolean isBeforeInit()
  {
    return _state.get() < IS_INIT;
  }

  /**
   * Returns true for the init state.
   */
  public boolean isAfterInit()
  {
    return _state.get() >= IS_INIT;
  }

  /**
   * Returns true if the service is starting.
   */
  public boolean isStarting()
  {
    return _state.get() == IS_STARTING;
  }

  /**
   * Returns true if the service is starting.
   */
  public boolean isAfterStarting()
  {
    return _state.get() >= IS_STARTING;
  }

  /**
   * Returns true for the warmup state.
   */
  public boolean isWarmup()
  {
    return _state.get() == IS_WARMUP;
  }

  /**
   * Returns true for the initializing state.
   */
  public boolean isBeforeActive()
  {
    return _state.get() < IS_ACTIVE;
  }

  /**
   * Returns true for the closing states
   */
  public boolean isAfterActive()
  {
    return IS_ACTIVE < _state.get();
  }

  /**
   * Wait for a period of time until the service starts.
   */
  public boolean waitForActive(long timeout)
  {
    int state = _state.get();

    if (state == IS_ACTIVE)
      return true;
    else if (state > IS_ACTIVE)
      return false;
    
    long waitEnd = Alarm.getCurrentTime() + timeout;

    synchronized (this) {
      while ((state = _state.get()) < IS_ACTIVE
             && Alarm.getCurrentTime() < waitEnd) {
        if (state == IS_ACTIVE)
          return true;
        else if (IS_ACTIVE < state)
          return false;
        else if (Alarm.isTest())
          return false;

        try {
          long delta = waitEnd - Alarm.getCurrentTime();
          
          if (delta > 0) {
            wait(delta);
          }
        } catch (InterruptedException e) {
        }
      }
    }
    
    return _state.get() == IS_ACTIVE;
  }

  /**
   * Returns true for the active state.
   */
  public boolean isActive()
  {
    return _state.get() == IS_ACTIVE;
  }

  /**
   * Returns true for the a runnable state, including warmup
   */
  public boolean isRunnable()
  {
    int state = _state.get();
    
    return IS_WARMUP <= state && state <= IS_ACTIVE;
  }

  /**
   * Returns true for the failed state.
   */
  public boolean isError()
  {
    return isFailed();
  }

  /**
   * Returns true for the failed state.
   */
  public boolean isFailed()
  {
    return _state.get() == IS_FAILED;
  }

  /**
   * Returns true if the state is stopping.
   */
  public boolean isStopping()
  {
    return IS_STOPPING <= _state.get();
  }

  /**
   * Returns true if the state is stopping.
   */
  public boolean isStopped()
  {
    return IS_STOPPING <= _state.get();
  }

  /**
   * Returns true if the state is closed
   */
  public boolean isDestroying()
  {
    return IS_DESTROYING <= _state.get();
  }

  /**
   * Returns true if the state is closed
   */
  public boolean isDestroyed()
  {
    return IS_DESTROYED <= _state.get();
  }
  
  /**
   * Changes to the initializing state.
   *
   * @return true if the transition is allowed
   */
  public boolean toInitializing()
  {
    return toNextState(IS_INITIALIZING);
  }

  /**
    * Changes to the init state.
    *
    * @return true if the transition is allowed
    */
   public boolean toInit()
   {
     return toNextState(IS_INIT);
   }
  
  /**
    * Changes to the init from the stopped state.
    *
    * @return true if the transition is allowed
    */
   public boolean toPostInit()
   {
     if (_state.compareAndSet(IS_STOPPED, IS_INIT)) {
       _lastChangeTime = Alarm.getCurrentTime();

       notifyListeners(IS_STOPPED, IS_INIT);

       return true;
     }
     else
       return _state.get() == IS_INIT;
   }

  /**
   * Changes to the starting state.
   *
   * @return true if the transition is allowed
   */
  public boolean toStarting()
  {
    int state;
    
    do {
      state = _state.get();

      if (IS_STARTING <= state && state != IS_STOPPED)
        return false;
    } while (! _state.compareAndSet(state, IS_STARTING));

    _lastChangeTime = Alarm.getCurrentTime();

    if (_log != null && _log.isLoggable(_level) && _log.isLoggable(Level.FINE))
      _log.fine(_name + " starting");

    notifyListeners(state, IS_STARTING);

    return true;
  }
  
  /**
   * Changes to the active state.
   *
   * @return true if the transition is allowed
   */
  public boolean toActive()
  {
    int state;
    
    do {
      state = _state.get();

      if (IS_ACTIVE <= state && state != IS_STOPPED)
        return false;
    } while (! _state.compareAndSet(state, IS_ACTIVE));

    _lastChangeTime = Alarm.getCurrentTime();

    if (_log != null && _log.isLoggable(_level))
      _log.log(_level, _name + " active");

    notifyListeners(state, IS_ACTIVE);

    synchronized (this) {
      notifyAll();
    }

    return true;
  }
  
  /**
   * Changes to the error state.
   *
   * @return true if the transition is allowed
   */
  public boolean toError()
  {
    return toFail();
  }
  
  /**
   * Changes to the failed state.
   *
   * @return true if the transition is allowed
   */
  public boolean toFail()
  {
    int state;
    
    do {
      state = _state.get();
      
      if (IS_DESTROYING <= state)
        return false;
    } while (! _state.compareAndSet(state, IS_FAILED));

    _lastChangeTime = Alarm.getCurrentTime();

    if (_log != null && _log.isLoggable(_level))
      _log.log(_level, _name + " fail");

    notifyListeners(state, IS_FAILED);

    _failCount++;
    
    return true;
  }
  
  /**
   * Changes to the stopping state.
   *
   * @return true if the transition is allowed
   */
  public boolean toStopping()
  {
    int state;
    
    do {
      state = _state.get();
      
      if (IS_STOPPING <= state || state == IS_STARTING)
        return false;
    } while (! _state.compareAndSet(state, IS_STOPPING));

    _lastChangeTime = Alarm.getCurrentTime();

    if (_log != null && _log.isLoggable(_level))
      _log.log(_level, _name + " stopping");

    notifyListeners(state, IS_STOPPING);

    return true;
  }
  
  /**
   * Changes to the stopped state.
   *
   * @return true if the transition is allowed
   */
  public boolean toStop()
  {
    return toNextState(IS_STOPPED);
  }
  
  /**
   * Changes to the destroying state.
   *
   * @return true if the transition is allowed
   */
  public boolean toDestroying()
  {
    return toNextState(IS_DESTROYING);
  }
  
  /**
   * Changes to the closed state.
   *
   * @return true if the transition is allowed
   */
  public boolean toDestroy()
  {
    return toNextState(IS_DESTROYED);
  }
  
  /**
   * Changes to the next state.
   *
   * @return true if the transition is allowed
   */
  private boolean toNextState(int newState)
  {
    int state;
    
    do {
      state = _state.get();
      
      if (newState <= state)
        return false;
    } while (! _state.compareAndSet(state, newState));

    _lastChangeTime = Alarm.getCurrentTime();

    if (_log != null && _log.isLoggable(_lowLevel))
      _log.log(_lowLevel, _name + " " + getStateName(newState));

    notifyListeners(state, newState);

    return true;
  }

  //
  // listeners
  //

  /**
   * Adds a listener to detect lifecycle changes.
   */
  public void addListener(LifecycleListener listener)
  {
    synchronized (this) {
      if (isDestroyed()) {
        IllegalStateException e = new IllegalStateException("attempted to add listener to a destroyed lifecyle " + this);

        if (_log != null)
          _log.log(Level.WARNING, e.toString(), e);
        else
          Logger.getLogger(Lifecycle.class.getName()).log(Level.WARNING, e.toString(), e);

        return;
      }

      if (_listeners == null)
        _listeners = new ArrayList<WeakReference<LifecycleListener>>();

      for (int i = _listeners.size() - 1; i >= 0; i--) {
        LifecycleListener oldListener = _listeners.get(i).get();

        if (listener == oldListener)
          return;
        else if (oldListener == null)
          _listeners.remove(i);
      }

      _listeners.add(new WeakReference<LifecycleListener>(listener));
    }
  }

  /**
   * Removes a listener.
   */
  public void removeListener(LifecycleListener listener)
  {
    synchronized (this) {
      if (_listeners == null)
        return;

      for (int i = _listeners.size() - 1; i >= 0; i--) {
        LifecycleListener oldListener = _listeners.get(i).get();

        if (listener == oldListener) {
          _listeners.remove(i);

          return;
        }
        else if (oldListener == null)
          _listeners.remove(i);
      }
    }
  }

  /**
   * Returns the listeners.
   */
  private void notifyListeners(int oldState, int newState)
  {
    // initial must be outside synchronized to avoid any blocking
    // for fail-safe shutdown
    if (_listeners == null) {
      return;
    }
    
    synchronized (this) {
      if (_listeners != null) {
        for (int i = 0; i < _listeners.size(); i++) {
          LifecycleListener listener = _listeners.get(i).get();

          if (listener != null) {
            listener.lifecycleEvent(oldState, newState);
          }
          else {
            _listeners.remove(i);
            i--;
          }
        }
      }
    }
  }
  
  /**
   * Copies from a target state.
   *
   * @return true if the transition is allowed
   */
  public void copyState(Lifecycle source)
  {
    _state.set(source._state.get());
  }

  /**
   * Debug string value.
   */
  public String toString()
  {
    if (_name != null)
      return "Lifecycle[" + _name + ", " + getStateName() + "]";
    else
      return "Lifecycle[" + getStateName() + "]";
  }
}
