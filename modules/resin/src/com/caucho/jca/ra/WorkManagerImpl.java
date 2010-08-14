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
 * @author Scott Ferguson
 */

package com.caucho.jca.ra;

import com.caucho.env.thread.ThreadPool;
import com.caucho.inject.Module;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;
import java.util.ArrayList;

/**
 * Implementation of the work manager.
 */
@Module
public class WorkManagerImpl implements WorkManager {
  private static final L10N L = new L10N(WorkManagerImpl.class);
  private ArrayList<Work> _activeTasks = new ArrayList<Work>();

  private volatile boolean _isClosed;

  /**
   * Constructor.
   */
  WorkManagerImpl()
  {
  }

  /**
   * Accepts a work instance for processing.  The call blocks until
   * the work instance completes.
   */
  public void doWork(Work work)
    throws WorkException
  {
    doWork(work, INDEFINITE, null, null);
  }

  /**
   * Accepts a work instance for processing.  The call blocks until
   * the work instance completes.
   */
  public void doWork(Work work,
                     long startTimeout,
                     ExecutionContext context,
                     WorkListener listener)
    throws WorkException
  {
    boolean isStart = false;
    
    try {
      WorkException exn = null;
      
      synchronized (this) {
        if (_isClosed)
          exn = new WorkException(L.l("Work task can't be started from closed context."));
        else if (_activeTasks.contains(work))
          exn = new WorkException(L.l("Reentrant Work tasks are not allowed."));
        else {
          isStart = true;

          _activeTasks.add(work);
        }
      }

      if (listener == null) {
      }
      else if (isStart)
        listener.workAccepted(new WorkEvent(this, WorkEvent.WORK_ACCEPTED,
                                            work, null, 0));
      else {
        listener.workRejected(new WorkEvent(this, WorkEvent.WORK_REJECTED,
                                            work, exn, 0));
      }
      
      if (exn != null)
        throw exn;

      if (listener != null)
        listener.workStarted(new WorkEvent(this, WorkEvent.WORK_STARTED,
                                           work, null, 0));
      
      work.run();

      if (listener != null)
        listener.workCompleted(new WorkEvent(this, WorkEvent.WORK_COMPLETED,
                                             work, null, 0));
    } finally {
      synchronized (this) {
        _activeTasks.remove(work);
      }
    }
  }

  /**
   * Accepts a work instance for processing.  The call blocks until
   * the work instance starts, but does not wait not until the completion.
   */
  public long startWork(Work work)
    throws WorkException
  {
    return startWork(work, INDEFINITE, null, null);
  }

  /**
   * Accepts a work instance for processing.  The call blocks until
   * the work instance starts, but does not wait not until the completion.
   */
  public long startWork(Work work,
                        long startTimeout,
                        ExecutionContext context,
                        WorkListener listener)
    throws WorkException
  {
    long start = Alarm.getCurrentTime();
    
    startWork(work, startTimeout, context, listener, true);

    return Alarm.getCurrentTime() - start;
  }

  /**
   * Schedules a work instance.
   */
  public void scheduleWork(Work work)
    throws WorkException
  {
    // XXX: since there's no delay in start work, currently, 
    scheduleWork(work, INDEFINITE, null, null);
  }

  /**
   * Schedules a work instance.
   */
  public void scheduleWork(Work work,
                           long startTimeout,
                           ExecutionContext context,
                           WorkListener listener)
    throws WorkException
  {
    startWork(work, startTimeout, context, listener, false);
  }

  /**
   * Accepts a work instance for processing.  The call blocks until
   * the work instance starts, but does not wait not until the completion.
   */
  private long startWork(Work work,
                         long startTimeout,
                         ExecutionContext context,
                         WorkListener listener,
                         boolean waitForStart)
    throws WorkException
  {
    boolean isStart = false;
    
    WorkException exn = null;

    try {
      synchronized (this) {
        if (_isClosed)
          exn = new WorkException(L.l("Work task can't be started from closed context."));
        else if (_activeTasks.contains(work))
          exn = new WorkException(L.l("Reentrant Work tasks are not allowed."));
        else
          _activeTasks.add(work);
      }

      if (exn != null) {
        if (listener != null)
          listener.workRejected(new WorkEvent(this, WorkEvent.WORK_REJECTED,
                                              work, exn, 0));
        throw exn;
      }
      else if (listener != null)
        listener.workAccepted(new WorkEvent(this, WorkEvent.WORK_ACCEPTED,
                                            work, null, 0));

      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      WorkThread workThread = new WorkThread(this, work, loader, listener);

      if (listener != null)
        listener.workStarted(new WorkEvent(this, WorkEvent.WORK_STARTED,
                                           work, null, 0));

      if (waitForStart)
        isStart = ThreadPool.getThreadPool().start(workThread, startTimeout);
      else
        isStart = ThreadPool.getThreadPool().schedule(workThread, startTimeout);
    } finally {
      synchronized (this) {
        if (! isStart)
          _activeTasks.remove(work);
      }
    }

    return 0;
  }

  void completeWork(Work work)
  {
    synchronized (this) {
      _activeTasks.remove(work);
    }
  }

  /**
   * Closes the work manager.
   */
  public void destroy()
  {
    synchronized (this) {
      if (_isClosed)
        return;

      _isClosed = true;
    }

    ArrayList<Work> activeTasks = new ArrayList<Work>();

    synchronized (this) {
      activeTasks.addAll(_activeTasks);
    }

    for (int i = 0; i < activeTasks.size(); i++)
      activeTasks.get(i).release();
  }
}
