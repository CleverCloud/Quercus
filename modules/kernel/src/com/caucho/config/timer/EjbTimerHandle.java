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
 * @author Rodrigo Westrupp
 */
package com.caucho.config.timer;

import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Timer;
import javax.ejb.TimerHandle;

import com.caucho.util.L10N;

/**
 * Resin EJB timer handle. This is mostly an adapter/decorator on top of the
 * underlying scheduling system.
 */
public class EjbTimerHandle implements TimerHandle {
  private static final long serialVersionUID = 1L;

  @SuppressWarnings("unused")
  private static final L10N L = new L10N(EjbTimerHandle.class);
  protected static final Logger log = Logger.getLogger(EjbTimerHandle.class
      .getName());

  private long _taskId;

  /**
   * Constructs a new timer handle.
   * 
   * @param taskId
   *          The ID of the underlying scheduled task for the timer.
   */
  EjbTimerHandle(long taskId)
  {
    _taskId = taskId;
  }

  /**
   * Obtain a reference to the timer represented by this handle.
   * 
   * @return A reference to the timer represented by this handle.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws NoSuchObjectLocalException
   *           If invoked on a handle whose associated timer has expired or has
   *           been canceled.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  public Timer getTimer() throws IllegalStateException,
      NoSuchObjectLocalException, EJBException
  {
    // TODO This should probably be a proper lookup/injection of the scheduler
    // via CDI.
    TimerTask timerTask = Scheduler.getTimerTaskById(_taskId);

    if (timerTask == null) {
      throw new NoSuchObjectLocalException("The timer no longer exists.");
    }

    EjbTimer timer = new EjbTimer();
    timer.setScheduledTask(timerTask);
    timer.checkStatus();

    return timer;
  }
}