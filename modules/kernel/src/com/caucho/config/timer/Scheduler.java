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
 * @author Reza Rahman
 */
package com.caucho.config.timer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import com.caucho.util.L10N;

/**
 * Resin scheduler.
 * 
 * @author Reza Rahman
 */
// TODO This should probably be a application/server/cluster managed bean
// itself - would get rid of the boilerplate factory code; I could not figure
// out how to make that happen - tried @ApplicationScoped.
public class Scheduler {
  @SuppressWarnings("unused")
  private static final L10N L = new L10N(Scheduler.class);
  protected static final Logger log = Logger.getLogger(Scheduler.class
      .getName());

  private static List<TimerTask> _scheduledTasks = Collections
      .synchronizedList(new ArrayList<TimerTask>());

  /**
   * Adds a scheduled task.
   * 
   * @param task
   *          The task to add.
   */
  public static void addTimerTask(final TimerTask task)
  {
    _scheduledTasks.add(task);
  }

  /**
   * Gets the scheduled task for a given task ID.
   * 
   * @param taskId
   *          The ID of the task to match.
   * @return The matching scheduled task, null if one is not found.
   */
  public static TimerTask getTimerTaskById(final long taskId)
  {
    for (TimerTask scheduledTask : _scheduledTasks) {
      if (scheduledTask.getTaskId() == taskId) {
        return scheduledTask;
      }
    }

    return null;
  }

  /**
   * Gets the scheduled tasks for a given target bean.
   * 
   * @param targetBean
   *          The target bean to match.
   * @return The scheduled tasks for the target bean.
   */
  @SuppressWarnings("unchecked")
  public static Collection<TimerTask> getTimerTasksByTargetBean(
      final Class targetBean)
  {
    Collection<TimerTask> tasks = new LinkedList<TimerTask>();

    return tasks;
  }

  /**
   * Removes a scheduled task.
   * 
   * @param scheduledTask
   *          The scheduled task to remove.
   */
  public static void removeTimerTask(TimerTask scheduledTask)
  {
    _scheduledTasks.remove(scheduledTask);
  }
}