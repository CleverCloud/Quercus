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

import com.caucho.util.Alarm;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

final class ThreadProfiler
  implements Profiler
{
  private static final Logger log = Logger.getLogger(ThreadProfiler.class.getName());

  private static ThreadLocal<ThreadProfiler> _current
    = new ThreadLocal<ThreadProfiler>();

  private final ArrayList<ProfilerPoint> _nodeStack
    = new ArrayList<ProfilerPoint>();
  private long[] _cumulativeTimeStack = new long[16];
  private boolean[] _unwindStack = new boolean[16];
  private long[] _startTimeStack = new long[16];

  static ThreadProfiler current()
  {
    ThreadProfiler current = _current.get();

    if (current == null) {
      current = new ThreadProfiler();
      _current.set(current);
    }

    return current;
  }

  private long currentTimeNanoseconds()
  {
    return Alarm.getExactTimeNanoseconds();
  }

  void start(ProfilerPoint profilerPoint)
  {
    start(profilerPoint, false);
  }

  void start(ProfilerPoint guaranteedParent, ProfilerPoint profilerPoint)
  {
    int stackLen = _nodeStack.size();

    boolean isParentFound = false;

    for (int i = 0; i < stackLen; i++) {
      if (_nodeStack.get(i) == guaranteedParent) {
        isParentFound = true;
        break;
      }
    }

    if (!isParentFound)
      start(guaranteedParent, true);

    start(profilerPoint, false);
  }

  private void start(ProfilerPoint node, boolean isUnwind)
  {
    int stackLen = _nodeStack.size();
    int topOfStack = stackLen - 1;

    ProfilerPoint parentNode;

    /** XXX:>>
    for (int i = 0; i < stackLen; i++)
      System.out.print("  ");
    System.out.println(">>start " + profilerPoint);
    (new Exception()).printStackTrace(System.out);
     */

    if (stackLen == 0) {
      parentNode = null;
    }
    else {
      // if there is parent, update it's cumulativeTimeStack so that
      // when the stack unwinds past it in finish() the time is added to the total
      parentNode = _nodeStack.get(topOfStack);

      long parentStartTime = _startTimeStack[topOfStack];

      long parentTime = currentTimeNanoseconds() - parentStartTime;

      _cumulativeTimeStack[topOfStack] += parentTime;
    }

    // ensure capacity

    int stackCapacity = _startTimeStack.length;
    int newStackCapacity = stackLen + 2;

    if (newStackCapacity > stackCapacity) {
      long[] newStartTimeStack = new long[stackCapacity * 3 / 2 + 1];
      System.arraycopy(_startTimeStack, 0, newStartTimeStack, 0, stackCapacity);
      _startTimeStack = newStartTimeStack;

      long[] newCumulativeTimeStack = new long[stackCapacity * 3 / 2 + 1];
      System.arraycopy(_cumulativeTimeStack, 0, newCumulativeTimeStack, 0, stackCapacity);
      _cumulativeTimeStack = newCumulativeTimeStack;

      boolean[] newUnwindStack = new boolean[stackCapacity * 3 / 2 + 1];
      System.arraycopy(_unwindStack, 0, newUnwindStack, 0, stackCapacity);
      _unwindStack = newUnwindStack;
    }

    // push a new node onto the stack

    long currentTime = currentTimeNanoseconds();

    _nodeStack.add(node);

    _unwindStack[stackLen] = isUnwind;
    _startTimeStack[stackLen] = currentTime;
    _cumulativeTimeStack[stackLen] = 0;

    /** XXX:>>
    for (int i = 0; i < stackLen; i++)
      System.out.print("  ");
    System.out.println(">>startNode " + node + " currentTime " + currentTime);
     */

    if (log.isLoggable(Level.FINEST)) {
      log.finest("[" + stackLen + "] start "  + node + " isUnwind=" + isUnwind);
      log.log(Level.FINEST, "", new Exception());
    }
  }

  public void finish()
  {
    int removeIndex = _nodeStack.size() - 1;

    ProfilerPoint node = _nodeStack.remove(removeIndex);
    long startTime = _startTimeStack[removeIndex];

    long currentTime = currentTimeNanoseconds();

    long time = currentTime - startTime;

    long totalTime = _cumulativeTimeStack[removeIndex] + time;

    node.update(totalTime);

    /** XXX:>>
    for (int i = 0; i < removeIndex; i++)
      System.out.print("  ");
    System.out.println(">>finishNode " + node +
      " startTime " + startTime +
      " currentTime " + currentTime +
      " totalTime" + time +
      " time"  + totalTime);
     */

    int parentIndex = removeIndex - 1;

    if (parentIndex >= 0) {
      _startTimeStack[parentIndex] = currentTimeNanoseconds();

      boolean isUnwind = _unwindStack[parentIndex];

      if (log.isLoggable(Level.FINEST)) {
        log.finest("[" + removeIndex + "] finish "  + node + " isUnwind=" + isUnwind);
      }

      if (isUnwind)
        finish();
    }
    else {
      if (log.isLoggable(Level.FINEST)) {
        log.finest("[" + removeIndex + "] finish "  + node);
      }
    }

    /** XXX:>>
    for (int i = 0; i < removeIndex; i++)
      System.out.print("  ");
    System.out.println(">>finish " + node + " " + totalTime);
     */
  }

  public String toString()
  {
    return "Profiler[" + Thread.currentThread().getName() + "]";
  }
}
