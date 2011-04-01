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

package com.caucho.make;

import com.caucho.loader.DynamicClassLoader;
import com.caucho.util.Alarm;
import com.caucho.util.Log;
import com.caucho.vfs.Dependency;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Contains a set of dependencies.
 */
public class DependencyContainer implements Dependency
{
  private static Logger _log;
  
  private ArrayList<Dependency> _dependencyList = new ArrayList<Dependency>();

  // Marks if the last check returned modified
  private volatile boolean _isModified;
  // Marks if the modification has been logged
  private boolean _isModifiedLog;

  // The interval for checking for a dependency.
  private long _checkInterval = 2000L;

  // When the dependency check last occurred
  private volatile long _checkExpiresTime = 0;

  private final AtomicBoolean _isChecking = new AtomicBoolean();

  public DependencyContainer()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    _checkInterval = DynamicClassLoader.getGlobalDependencyCheckInterval();
    
    for (; loader != null; loader = loader.getParent()) {
      if (loader instanceof DynamicClassLoader) {
        _checkInterval = ((DynamicClassLoader) loader).getDependencyCheckInterval();
        break;
      }
    }
  }
  
  /**
   * Adds a dependency.
   */
  public DependencyContainer add(Dependency dependency)
  {
    if (dependency == this)
      throw new IllegalArgumentException("Can't add self as a dependency.");
    
    if (! _dependencyList.contains(dependency))
      _dependencyList.add(dependency);

    // server/1d0w
    // XXX: _lastCheckTime = 0;

    return this;
  }
  
  public DependencyContainer addAll(DependencyContainer container)
  {
    for (Dependency depend : container._dependencyList) {
      add(depend);
    }
    
    return this;
  }
  
  public DependencyContainer addAll(ArrayList<Dependency> dependencyList)
  {
    for (Dependency depend : dependencyList) {
      add(depend);
    }
    
    return this;
  }
  
  public ArrayList<Dependency> getDependencies()
  {
    return _dependencyList;
  }
  
   /**
   * Removes a dependency.
   */
  public DependencyContainer remove(Dependency dependency)
  {
    if (dependency == this)
      throw new IllegalArgumentException("Can't remove self as a dependency.");
    
    _dependencyList.remove(dependency);

    return this;
  }

  public int size()
  {
    return _dependencyList.size();
  }

  /**
   * Sets the check modification check interval in milliseconds.
   * Negative values mean never check. 0 means always check.
   *
   * @param checkInterval how often the dependency should be checked
   */
  public void setCheckInterval(long checkInterval)
  {
    if (checkInterval < 0 || checkInterval > Long.MAX_VALUE / 2)
      _checkInterval = Long.MAX_VALUE / 2;
    else
      _checkInterval = checkInterval;

    _checkExpiresTime = 0;
  }

  /**
   * Gets the check modification check interval.
   * Negative values mean never check. 0 means always check.
   */
  public long getCheckInterval()
  {
    return _checkInterval;
  }

  /**
   * Sets the modified.
   */
  public void setModified(boolean isModified)
  {
    _isModified = isModified;

    if (_isModified)
      _checkExpiresTime = Long.MAX_VALUE / 2;
    else
      _checkExpiresTime = Alarm.getCurrentTime() + _checkInterval;
    
    if (! isModified)
      _isModifiedLog = false;
  }
      
  /**
   * Resets the check interval.
   */
  public void resetDependencyCheckInterval()
  {
    _checkExpiresTime = 0;
  }

  /**
   * Clears the modified flag and sets the last check time to now.
   */
  public void clearModified()
  {
    setModified(false);
  }

  /**
   * Returns true if the underlying dependencies have changed.
   */
  public boolean isModified()
  {
    long now = Alarm.getCurrentTime();

    if (now < _checkExpiresTime)
      return _isModified;
    
    if (_isChecking.getAndSet(true))
      return _isModified;

    try {
      _checkExpiresTime = now + _checkInterval;

      for (int i = _dependencyList.size() - 1; i >= 0; i--) {
        Dependency dependency = _dependencyList.get(i);

        if (dependency.isModified()) {
          setModified(true);
        
          return _isModified;
        }
      }
      
      return _isModified;
    } finally {
      _isChecking.set(false);
    }
  }

  /**
   * Logs the reason for modification.
   */
  public boolean logModified(Logger log)
  {
    if (_isModifiedLog)
      return true;
    
    for (int i = _dependencyList.size() - 1; i >= 0; i--) {
      Dependency dependency = _dependencyList.get(i);

      if (dependency.logModified(log)) {
        _isModifiedLog = true;
        return true;
      }
    }
      
    return false;
  }

  /**
   * Returns true if the underlying dependencies have changed, forcing a check.
   */
  public boolean isModifiedNow()
  {
    _checkExpiresTime = 0;

    return isModified();
  }

  private Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(DependencyContainer.class.getName());

    return _log;
  }
  
  public String toString()
  {
    return "DependencyContainer" + _dependencyList;
  }
}
