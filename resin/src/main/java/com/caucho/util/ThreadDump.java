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

import javax.annotation.*;
import javax.management.*;
import java.lang.management.*;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Configuration for management.
 */
public class ThreadDump
{
  private static Logger log = Logger.getLogger(ThreadDump.class.getName());

  private static final ThreadDump _threadDump = new ThreadDump();
  private final AtomicLong _lastDump = new AtomicLong();

  private ThreadDump()
  {
  }

  public static void dumpThreads()
  {
    long timeout = 3600L * 1000L;

    _threadDump.threadDump(timeout);
  }

  private void threadDump(long timeout)
  {
    long now = Alarm.getCurrentTime();
    long lastDump = _lastDump.get();

    if (lastDump + timeout < now
        && _lastDump.compareAndSet(lastDump, now)) {
      threadDumpImpl();
    }
  }

  private void threadDumpImpl()
  {
    ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    long []ids = threadBean.getAllThreadIds();
    ThreadInfo []info = threadBean.getThreadInfo(ids, 32);

    StringBuilder sb = new StringBuilder();
    sb.append("Thread Dump:\n");

    Arrays.sort(info, new ThreadCompare());
    buildThreads(sb, info, Thread.State.RUNNABLE, false);
    buildThreads(sb, info, Thread.State.RUNNABLE, true);
    buildThreads(sb, info, Thread.State.BLOCKED, false);
    buildThreads(sb, info, Thread.State.WAITING, false);
    buildThreads(sb, info, Thread.State.TIMED_WAITING, false);
    buildThreads(sb, info, null, false);

    log.info(sb.toString());
  }

  private void buildThreads(StringBuilder sb,
                            ThreadInfo []infoArray,
                            Thread.State matchState,
                            boolean isNative)
  {
    for (ThreadInfo info : infoArray) {
      if (info == null)
        continue;

      Thread.State state = info.getThreadState();

      if (matchState == Thread.State.RUNNABLE
          && (isNative != info.isInNative())) {
        continue;
      }

      if (state == matchState)
        buildThread(sb, info);
      else if (state == null
               && matchState != Thread.State.RUNNABLE
               && matchState != Thread.State.BLOCKED
               && matchState != Thread.State.WAITING
               && matchState != Thread.State.TIMED_WAITING) {
        buildThread(sb, info);
      }
    }
  }

  private void buildThread(StringBuilder sb, ThreadInfo info)
  {
    sb.append("\n\"");
    sb.append(info.getThreadName());
    sb.append("\" id=" + info.getThreadId());
    sb.append(" " + info.getThreadState());

    if (info.isInNative())
      sb.append(" (in native)");

    if (info.isSuspended())
      sb.append(" (suspended)");

    String lockName = info.getLockName();
    if (lockName != null) {
      sb.append("\n    waiting on ");
      sb.append(lockName);

      if (info.getLockOwnerName() != null) {
        sb.append("\n    owned by \"");
        sb.append(info.getLockOwnerName());
        sb.append("\"");
      }
    }

    sb.append("\n");

    /*
    Server server = Server.getCurrent();

    if (server != null) {
      TcpConnection conn = server.findConnectionByThreadId(info.getThreadId());

      if (conn != null && conn.getRequest() instanceof AbstractHttpRequest) {
        AbstractHttpRequest req = (AbstractHttpRequest) conn.getRequest();

        if (req.getRequestURI() != null) {
          sb.append("   ").append(req.getRequestURI()).append("\n");
        }
      }
    }
    */

    StackTraceElement []stackList = info.getStackTrace();
    if (stackList == null)
      return;

    for (StackTraceElement stack : stackList) {
      sb.append("  at ");
      sb.append(stack.getClassName());
      sb.append(".");
      sb.append(stack.getMethodName());

      if (stack.getFileName() != null) {
        sb.append(" (");
        sb.append(stack.getFileName());
        if (stack.getLineNumber() > 0) {
          sb.append(":");
          sb.append(stack.getLineNumber());
        }
        sb.append(")");
      }

      if (stack.isNativeMethod())
        sb.append(" (native)");

      sb.append("\n");
    }
  }

  static class ThreadCompare implements Comparator<ThreadInfo> {
    public int compare(ThreadInfo a, ThreadInfo b)
    {
      if (a == b)
        return 0;
      else if (a == null)
        return -1;
      else if (b == null)
        return 1;
      else if (a.getThreadState() != b.getThreadState())
        return a.getThreadState().ordinal() - b.getThreadState().ordinal();
      else if (a.isInNative() && ! b.isInNative())
        return 1;
      else if (b.isInNative() && ! a.isInNative())
        return -1;
      else
        return a.getThreadName().compareTo(b.getThreadName());
    }
  }
}
