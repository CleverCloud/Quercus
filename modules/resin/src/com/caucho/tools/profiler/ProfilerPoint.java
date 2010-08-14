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

import java.util.*;
import com.caucho.util.*;

/**
 * Represents a unique point at which profiling is performed. Obtained from a
 * {@link ProfilerManager}. Equality between two instances of ProfilerPoint is
 * based on the name only.
 */
public class ProfilerPoint
  implements Comparable<ProfilerPoint>
{
  private static final Profiler NOOP_PROFILER;

  private final ProfilerManager _profilerManager;
  private final String _name;

  private LruCache<String,ProfilerPoint> _children;

  private long _time;
  private long _invocationCount;

  private long _minTime = Long.MAX_VALUE;
  private long _maxTime = Long.MIN_VALUE;

  ProfilerPoint(ProfilerManager profilerManager, String name)
  {
    assert profilerManager != null;
    assert name != null;

    _profilerManager = profilerManager;
    _name = name;
  }

  protected ProfilerManager getProfilerManager()
  {
    return _profilerManager;
  }

  public String getName()
  {
    return _name;
  }

  public ProfilerPoint addProfilerPoint(String name)
  {
    synchronized (this) {
      if (_children == null)
        _children = new LruCache<String,ProfilerPoint>(1024);
    
      ProfilerPoint child = _children.get(name);

      if (child == null) {
        child = create(name);
        _children.put(name, child);
      }

      return child;
    }
  }

  protected ProfilerPoint create(String name)
  {
    return new ProfilerPoint(getProfilerManager(), name);
  }

  public Profiler start()
  {
    if (!_profilerManager.isEnabled())
      return NOOP_PROFILER;

    ThreadProfiler profiler = ThreadProfiler.current();

    profiler.start(this);

    return profiler;
  }

  protected Profiler start(ProfilerPoint parent)
  {
    if (!getProfilerManager().isEnabled())
      return NOOP_PROFILER;

    ThreadProfiler profiler = ThreadProfiler.current();

    profiler.start(parent, this);

    return profiler;
  }

  /**
   * Caller must synchronize on this ProfilerPoint while it uses the returned
   * map.
   */
  List<ProfilerPoint> getChildren()
  {
    if (_children == null)
      return Collections.emptyList();
    else {
      ArrayList<ProfilerPoint> children = new ArrayList<ProfilerPoint>();

      Iterator<ProfilerPoint> iter = _children.values();
      while (iter.hasNext())
        children.add(iter.next());
      
      return children;
    }
  }

  /**
   * Increment the invocation count and add time.
   *
   * @param totalTime
   */
  void update(long totalTime)
  {
    synchronized (this) {
      _invocationCount++;

      if (_invocationCount > 0) {
        _time += totalTime;
      }

      if (totalTime < _minTime)
        _minTime = totalTime;

      if (_maxTime < totalTime)
        _maxTime = totalTime;
    }
  }

  /**
   * Time for this node in nanoseconds, does not include the time for child
   * nodes.
   */
  public long getTime()
  {
    return _time;
  }

  /**
   * Minimum time for this node in nanoseconds, does not include
   * the time for child nodes.
   */
  public long getMinTime()
  {
    return _minTime;
  }

  /**
   * Minimum time for this node in nanoseconds, does not include
   * the time for child nodes.
   */
  public long getMaxTime()
  {
    return _maxTime;
  }

  void incrementInvocationCount()
  {
    synchronized (this) {
      _invocationCount++;
    }
  }

  public long getInvocationCount()
  {
    return _invocationCount;
  }

  /**
   * Drop all of the children
   */
  void reset()
  {
    _children = null;

    _time = 0;
    _invocationCount = 0;

    _minTime = Long.MAX_VALUE;
    _maxTime = Long.MIN_VALUE;
  }

  public boolean equals(Object o)
  {
    if (o == this)
      return true;

    if (!(o instanceof ProfilerPoint))
      return false;

    ProfilerPoint point = (ProfilerPoint) o;

    return getName().equals(point.getName());
  }

  public int compareTo(ProfilerPoint point)
  {
    return getName().compareTo(point.getName());
  }

  public int hashCode()
  {
    return getName().hashCode();
  }

  public String toString()
  {
    return "ProfilerPoint[" + getName() + "]";
  }

  static {
    NOOP_PROFILER = new Profiler() {
        public void finish()
        {
        }

        public String toString()
        {
          return "NoopProfiler[]";
        }
      };
  }
}
