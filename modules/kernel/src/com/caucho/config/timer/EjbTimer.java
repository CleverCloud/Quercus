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
package com.caucho.config.timer;

import java.io.Serializable;
import java.util.Date;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerHandle;

import com.caucho.util.Alarm;
import com.caucho.util.L10N;

/**
 * Resin EJB timer. This object is mostly an adapter/decorator over the
 * underlying scheduled task.
 */
public class EjbTimer implements Timer, Runnable {
  @SuppressWarnings("unused")
  private static final L10N L = new L10N(EjbTimer.class);
  @SuppressWarnings("unused")
  private static final Logger log = Logger.getLogger(EjbTimer.class.getName());

  private TimerTask _timerTask;

  /**
   * Creates timer.
   */
  public EjbTimer()
  {
  }

  /**
   * Creates timer.
   */
  public EjbTimer(TimerTask task)
  {
    _timerTask = task;
  }

  /**
   * Sets the underlying scheduled task.
   * 
   * @param scheduledTask
   *          The underlying scheduled task.
   */
  public void setScheduledTask(TimerTask scheduledTask)
  {
    _timerTask = scheduledTask;
  }

  /**
   * Get the schedule expression corresponding to this timer.
   * 
   * @return Schedule expression corresponding to this timer.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method. Also thrown if invoked on a
   *           timer that was created with one of the non-ScheduleExpression
   *           TimerService.createTimer APIs.
   * @throws NoSuchObjectLocalException
   *           If invoked on a timer that has expired or has been canceled.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public ScheduleExpression getSchedule() throws IllegalStateException,
      NoSuchObjectLocalException, EJBException
  {
    checkStatus();

    if (_timerTask.getCronExpression() != null) {
      CronExpression cronExpression = _timerTask.getCronExpression();

      Date start = null;
      Date end = null;
      /*
       * if (_scheduledTask.getStart() != -1) { start = new
       * Date(_scheduledTask.getStart()); }
       * 
       * if (_scheduledTask.getEnd() != -1) { end = new
       * Date(_scheduledTask.getEnd()); }
       */

      return new ScheduleExpression().second(cronExpression.getSecond())
          .minute(cronExpression.getMinute()).hour(cronExpression.getHour())
          .dayOfWeek(cronExpression.getDayOfWeek()).dayOfMonth(
              cronExpression.getDayOfMonth()).month(cronExpression.getMonth())
          .year(cronExpression.getYear()).start(start).end(end);
    } else {
      throw new IllegalStateException(
          "This timer was not created by a schedule expression.");
    }
  }

  /**
   * Get the information associated with the timer at the time of creation.
   * 
   * @return The Serializable object that was passed in at timer creation, or
   *         null if the info argument passed in at timer creation was null.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws NoSuchObjectLocalException
   *           If invoked on a timer that has expired or has been canceled.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public Serializable getInfo() throws IllegalStateException,
      NoSuchObjectLocalException, EJBException
  {
    checkStatus();

    return _timerTask.getData();
  }

  /**
   * Query whether this timer has persistent semantics.
   * 
   * @return true if this timer has persistent guarantees.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws NoSuchObjectLocalException
   *           If invoked on a timer that has expired or has been canceled.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public boolean isPersistent() throws IllegalStateException,
      NoSuchObjectLocalException, EJBException
  {
    checkStatus();

    return false; // Resin does not support persistent timers.
  }

  /**
   * Query whether this timer is a calendar-based timer.
   * 
   * @return true if this timer is a calendar-based timer.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws NoSuchObjectLocalException
   *           If invoked on a timer that has expired or has been cancelled.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  public boolean isCalendarTimer() throws IllegalStateException,
      NoSuchObjectLocalException, EJBException
  {
    checkStatus();

    return (_timerTask.getCronExpression() != null);
  }

  /**
   * Get the point in time at which the next timer expiration is scheduled to
   * occur.
   * 
   * @return The point in time at which the next timer expiration is scheduled
   *         to occur.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws NoSuchObjectLocalException
   *           If invoked on a timer that has expired or has been canceled.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public Date getNextTimeout() throws IllegalStateException,
      NoSuchObjectLocalException, EJBException
  {
    checkStatus();

    return new Date(_timerTask.getNextAlarmTime());
  }

  /**
   * Get the number of milliseconds that will elapse before the next scheduled
   * timer expiration.
   * 
   * @return The number of milliseconds that will elapse before the next
   *         scheduled timer expiration.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws NoSuchObjectLocalException
   *           If invoked on a timer that has expired or has been canceled.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public long getTimeRemaining() throws IllegalStateException,
      NoSuchObjectLocalException, EJBException
  {
    checkStatus();

    long now = Alarm.getExactTime();
    long nextTime = _timerTask.getNextAlarmTime();

    return (nextTime - now);
  }

  /**
   * Get a serializable handle to the timer. This handle can be used at a later
   * time to re-obtain the timer reference.
   * 
   * @return A serializable handle to the timer.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws NoSuchObjectLocalException
   *           If invoked on a timer that has expired or has been canceled.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public TimerHandle getHandle() throws IllegalStateException,
      NoSuchObjectLocalException, EJBException
  {
    checkStatus();

    return new EjbTimerHandle(_timerTask.getTaskId());
  }

  /**
   * Cause the timer and all its associated expiration notifications to be
   * Canceled.
   * 
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws NoSuchObjectLocalException
   *           If invoked on a timer that has expired or has been canceled.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public void cancel() throws IllegalStateException,
      NoSuchObjectLocalException, EJBException
  {
    checkStatus();

    _timerTask.cancel();
  }

  /**
   * Runs the timer task.
   */
  @Override
  public void run()
  {
    if (_timerTask.getStatus() == ScheduledTaskStatus.ACTIVE) {
      _timerTask.invoke(this);
    }
  }

  /**
   * Check if the timer is inactive (expired or cancelled).
   * 
   * @throws NoSuchObjectLocalException
   *           If the timer is expired or cancelled.
   */
  void checkStatus() throws NoSuchObjectLocalException
  {
    if (_timerTask.getStatus() == ScheduledTaskStatus.CANCELLED) {
      throw new NoSuchObjectLocalException("This timer has been cancelled.");
    }

    if (_timerTask.getStatus() == ScheduledTaskStatus.EXPIRED) {
      throw new NoSuchObjectLocalException("This timer has already expired.");
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _timerTask + "]";
  }
}