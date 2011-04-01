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

package com.caucho.jmx;

import com.caucho.loader.ClassLoaderListener;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;

import java.util.ArrayList;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static convenience methods.
 */
class JobThread implements Runnable {
  private static final L10N L = new L10N(JobThread.class);
  private static final Logger log
    = Logger.getLogger(JobThread.class.getName());

  private static JobThread _job = new JobThread();

  private ArrayList<Job> _jobs = new ArrayList<Job>();
  private ArrayList<Job> _runJobs = new ArrayList<Job>();

  private JobThread()
  {
  }

  /**
   * Queues a task.
   */
  public static void queue(TimerTask task, long time)
  {
    synchronized (_job._jobs) {
      ArrayList<Job> jobs = _job._jobs;

      for (int i = jobs.size() - 1; i >= 0; i--) {
        Job oldJob = jobs.get(i);

        if (oldJob.getTask() == task) {
          if (time < oldJob.getTime())
            oldJob.setTime(time);

          return;
        }
      }
      
      Job job = new Job(task);
      job.setTime(time);

      _job._jobs.add(job);
    }
  }

  /**
   * Dequeues a task.
   */
  public static void dequeue(TimerTask job)
  {
    _job.remove(job);
  }

  void remove(TimerTask task)
  {
    synchronized (_jobs) {
      for (int i = _jobs.size() - 1; i >= 0; i--) {
        Job job = _jobs.get(i);

        if (job.getTask() == task)
          _jobs.remove(i);
      }
    }
  }

  public void run()
  {
    Thread thread = Thread.currentThread();
    
    while (true) {
      long now = Alarm.getCurrentTime();
      
      _runJobs.clear();

      synchronized (_jobs) {
        for (int i = _jobs.size() - 1; i >= 0; i--) {
          Job job = _jobs.get(i);

          if (job.getTime() <= now) {
            _runJobs.add(job);
            _jobs.remove(i);
          }
        }
      }

      for (int i = _runJobs.size() - 1; i >= 0; i--) {
        Job job = _runJobs.get(i);

        try {
          job.run();
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }

      try {
        thread.sleep(500);
      } catch (Throwable e) {
      }
    }
  }

  static class Job implements ClassLoaderListener {
    private TimerTask _task;
    private ClassLoader _loader;
    private long _time;
    private boolean _isDead;

    Job(TimerTask task)
    {
      _task = task;

      _loader = Thread.currentThread().getContextClassLoader();
      

      Environment.addClassLoaderListener(this);
    }

    public TimerTask getTask()
    {
      return _task;
    }

    /**
     * Sets the time the job should execute.
     */
    public void setTime(long time)
    {
      _time = time;
    }

    /**
     * Gets the time the job should execute.
     */
    public long getTime()
    {
      return _time;
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
      _isDead = true;
      JobThread.dequeue(_task);
    }

    public void run()
    {
      if (! _isDead) {
        Thread thread = Thread.currentThread();
        ClassLoader oldLoader = thread.getContextClassLoader();

        try {
          thread.setContextClassLoader(_loader);
          _task.run();
        } finally {
          thread.setContextClassLoader(oldLoader);
        }
      }
    }
  }

  static {
    try {
      Thread thread = new Thread(_job, "jmx-job-thread");
      thread.setDaemon(true);
      thread.start();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
}

