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

import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.LruCache;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

/**
 * The main entry point for profiling.  This class is used to obtain instances
 * of {@link ProfilerPoint}, which are then used during execution of code to
 * demarcate the code to be profiled.
 * <p/>
 * A {@link ProfilerManager} for the current {@link ClassLoader} is obtained
 * with {@link #getLocal()}.
 */
public class ProfilerManager {
  private static final EnvironmentLocal<ProfilerManager> _local
    = new EnvironmentLocal<ProfilerManager>();

  private ProfilerPoint _root;

  private boolean _isEnabled = false;

  private ProfilerManager()
  {
    new ProfilerAdmin(this);

    _root = new ProfilerPoint(this, "");
  }

  public static ProfilerManager getLocal()
  {
    synchronized (_local) {
      ProfilerManager local = _local.get();

      if (local == null) {
        local = new ProfilerManager();
        _local.set(local);
      }

      return local;
    }
  }

  public ProfilerPoint getRoot()
  {
    return _root;
  }

  public ProfilerPoint getProfilerPoint(String name)
  {
    return _root.addProfilerPoint(name);
  }

  /**
   * Set to true to enable profiling, default false.
   */
  public void setEnabled(boolean isEnabled)
  {
    _isEnabled = isEnabled;
  }

  public boolean isEnabled()
  {
    return _isEnabled;
  }

  public void enable()
  {
    if (!_isEnabled) {
      reset();
      _isEnabled = true;
    }
  }

  public void disable()
  {
    _isEnabled = false;
  }

  public ProfilerPoint addProfilerPoint(String name)
  {
    return _root.addProfilerPoint(name);
  }

  public ProfilerPoint getCategorizingProfilerPoint(String name)
  {
    return _root.addProfilerPoint(name);
  }

  /**
   * Clear all profiling information.
   */
  public void reset()
  {
    _root.reset();
  }

  public String toString()
  {
    return "ProfilerManager[" + getClass().getClassLoader() + "]";
  }

}
