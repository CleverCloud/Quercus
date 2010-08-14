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
package com.caucho.ejb.timer;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import com.caucho.config.timer.CronExpression;
import com.caucho.config.timer.CronTrigger;
import com.caucho.config.timer.EjbTimer;
import com.caucho.config.timer.TimeoutInvoker;
import com.caucho.config.timer.TimerTask;
import com.caucho.config.types.Trigger;
import com.caucho.ejb.server.AbstractEjbBeanManager;
import com.caucho.resources.TimerTrigger;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;

/**
 * Resin EJB timer service.
 */
// TODO This should probably be a application/server/cluster managed bean
// itself - would get rid of the boilerplate factory code; I could not figure
// out how to make that happen - tried @ApplicationScoped.
public class EjbTimerService implements TimerService {
  private static final L10N L = new L10N(EjbTimerService.class);
  @SuppressWarnings("unused")
  private static final Logger log = Logger.getLogger(EjbTimerService.class
      .getName());

  private final AbstractEjbBeanManager _server;
  private final TimeoutInvoker _timeout;

  private final LinkedList<TimerTask> _timers = new LinkedList<TimerTask>();

  /**
   * Creates a new timer service.
   * 
   * @param context
   *          EJB context.
   */
  public EjbTimerService(AbstractEjbBeanManager server)
  {
    _server = server;
    _timeout = new EjbTimerInvocation(server);
  }

  /**
   * Create a single-action timer that expires after a specified duration.
   * 
   * @param duration
   *          The number of milliseconds that must elapse before the timer
   *          expires.
   * @param info
   *          Application information to be delivered along with the timer
   *          expiration notification. This can be null.
   * @return The newly created Timer.
   * @throws IllegalArgumentException
   *           If duration is negative.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method fails due to a system-level failure.
   */
  @Override
  public Timer createTimer(long duration, Serializable info)
      throws IllegalArgumentException, IllegalStateException, EJBException
  {
    if (duration < 0) {
      throw new IllegalArgumentException("Timer duration must not be negative.");
    }

    long expiration = Alarm.getCurrentTime() + duration;

    return createOneTimeTimer(expiration, info);
  }

  /**
   * Create a single-action timer that expires after a specified duration.
   * 
   * @param duration
   *          The number of milliseconds that must elapse before the timer
   *          expires.
   * @param timerConfig
   *          Timer configuration.
   * @return The newly created Timer.
   * @throws IllegalArgumentException
   *           If duration is negative.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method fails due to a system-level failure.
   */
  @Override
  public Timer createSingleActionTimer(long duration, TimerConfig timerConfig)
      throws IllegalArgumentException, IllegalStateException, EJBException
  {
    if (duration < 0) {
      throw new IllegalArgumentException(L
          .l("Timer duration must not be negative."));
    }

    long expiration = Alarm.getCurrentTime() + duration;

    Serializable info = null;

    if (timerConfig != null)
      info = timerConfig.getInfo();

    return createOneTimeTimer(expiration, info);
  }

  /**
   * Create an interval timer whose first expiration occurs after a specified
   * duration, and whose subsequent expirations occur after a specified
   * interval.
   * 
   * @param initialDuration
   *          The number of milliseconds that must elapse before the first timer
   *          expiration notification.
   * @param intervalDuration
   *          The number of milliseconds that must elapse between timer
   *          expiration notifications. Expiration notifications are scheduled
   *          relative to the time of the first expiration. If expiration is
   *          delayed (e.g. due to the interleaving of other method calls on the
   *          bean) two or more expiration notifications may occur in close
   *          succession to "catch up".
   * @param info
   *          Application information to be delivered along with the timer
   *          expiration. This can be null.
   * @return The newly created Timer.
   * @throws IllegalArgumentException
   *           If initialDuration is negative, or intervalDuration is negative.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public Timer createTimer(long initialDuration, long intervalDuration,
      Serializable info) throws IllegalArgumentException,
      IllegalStateException, EJBException
  {
    if (initialDuration < 0) {
      throw new IllegalArgumentException(L
          .l("Timer initial duration must not be negative."));
    }

    if (intervalDuration < 0) {
      throw new IllegalArgumentException(L
          .l("Timer interval duration must not be negative."));
    }

    Date expiration = new Date(Alarm.getCurrentTime() + initialDuration);

    return createRepeatingTimer(expiration, intervalDuration, info);
  }

  /**
   * Create an interval timer whose first expiration occurs after a specified
   * duration, and whose subsequent expirations occur after a specified
   * interval.
   * 
   * @param initialDuration
   *          The number of milliseconds that must elapse before the first timer
   *          expiration notification.
   * @param intervalDuration
   *          The number of milliseconds that must elapse between timer
   *          expiration notifications. Expiration notifications are scheduled
   *          relative to the time of the first expiration. If expiration is
   *          delayed (e.g. due to the interleaving of other method calls on the
   *          bean) two or more expiration notifications may occur in close
   *          succession to "catch up".
   * @param timerConfig
   *          Timer configuration.
   * @return The newly created Timer.
   * @throws IllegalArgumentException
   *           If initialDuration is negative, or intervalDuration is negative.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public Timer createIntervalTimer(long initialDuration, long intervalDuration,
      TimerConfig timerConfig) throws IllegalArgumentException,
      IllegalStateException, EJBException
  {
    if (initialDuration < 0) {
      throw new IllegalArgumentException(L
          .l("Timer initial duration must not be negative."));
    }

    if (intervalDuration < 0) {
      throw new IllegalArgumentException(L
          .l("Timer interval duration must not be negative."));
    }

    Date expiration = new Date(Alarm.getCurrentTime() + initialDuration);

    Serializable info = null;

    if (timerConfig != null)
      info = timerConfig.getInfo();

    return createRepeatingTimer(expiration, intervalDuration, info);
  }

  /**
   * Create a single-action timer that expires at a given point in time.
   * 
   * @param expiration
   *          The point in time at which the timer must expire.
   * @param info
   *          Application information to be delivered along with the timer
   *          expiration notification. This can be null.
   * @return The newly created Timer.
   * @throws IllegalArgumentException
   *           If expiration is null, or expiration.getTime() is negative.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public Timer createTimer(Date expiration, Serializable info)
      throws IllegalArgumentException, IllegalStateException, EJBException
  {
    if (expiration == null) {
      throw new IllegalArgumentException(L
          .l("Timer expiration must not be null."));
    }

    if (expiration.getTime() < 0) {
      throw new IllegalArgumentException(L
          .l("Timer expiration must not be negative."));
    }

    return createOneTimeTimer(expiration.getTime(), info);
  }

  /**
   * Create a single-action timer that expires at a given point in time.
   * 
   * @param expiration
   *          The point in time at which the timer must expire.
   * @param timerConfig
   *          Timer configuration.
   * @return The newly created Timer.
   * @throws IllegalArgumentException
   *           If expiration is null, or expiration.getTime() is negative.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public Timer createSingleActionTimer(Date expiration, TimerConfig timerConfig)
      throws IllegalArgumentException, IllegalStateException, EJBException
  {
    if (expiration == null) {
      throw new IllegalArgumentException(L
          .l("Timer expiration must not be null."));
    }

    if (expiration.getTime() < 0) {
      throw new IllegalArgumentException(L
          .l("Timer expiration must not be negative."));
    }

    Serializable info = null;

    if (timerConfig != null)
      info = timerConfig.getInfo();

    return createOneTimeTimer(expiration.getTime(), info);
  }

  /**
   * Create an interval timer whose first expiration occurs at a given point in
   * time and whose subsequent expirations occur after a specified interval.
   * 
   * @param initialExpiration
   *          The point in time at which the first timer expiration must occur.
   * @param intervalDuration
   *          The number of milliseconds that must elapse between timer
   *          expiration notifications. Expiration notifications are scheduled
   *          relative to the time of the first expiration. If expiration is
   *          delayed (e.g. due to the interleaving of other method calls on the
   *          bean) two or more expiration notifications may occur in close
   *          succession to "catch up".
   * @param info
   *          Application information to be delivered along with the timer
   *          expiration. This can be null.
   * @return The newly created Timer.
   * @throws IllegalArgumentException
   *           If initialExpiration is null, or initialExpiration.getTime() is
   *           negative, or intervalDuration is negative.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public Timer createTimer(Date initialExpiration, long intervalDuration,
      Serializable info) throws IllegalArgumentException,
      IllegalStateException, EJBException
  {
    if (initialExpiration == null) {
      throw new IllegalArgumentException(L
          .l("Timer initial expiration must not be null."));
    }

    if (initialExpiration.getTime() < 0) {
      throw new IllegalArgumentException(L
          .l("Timer initial expiration must not be negative."));
    }

    if (intervalDuration < 0) {
      throw new IllegalArgumentException(L
          .l("Timer interval duration must not be negative."));
    }

    return createRepeatingTimer(initialExpiration, intervalDuration, info);
  }

  /**
   * Create an interval timer whose first expiration occurs at a given point in
   * time and whose subsequent expirations occur after a specified interval.
   * 
   * @param initialExpiration
   *          The point in time at which the first timer expiration must occur.
   * @param intervalDuration
   *          The number of milliseconds that must elapse between timer
   *          expiration notifications. Expiration notifications are scheduled
   *          relative to the time of the first expiration. If expiration is
   *          delayed (e.g. due to the interleaving of other method calls on the
   *          bean) two or more expiration notifications may occur in close
   *          succession to "catch up".
   * @param timerConfig
   *          Timer configuration.
   * @return The newly created Timer.
   * @throws IllegalArgumentException
   *           If initialExpiration is null, or initialExpiration.getTime() is
   *           negative, or intervalDuration is negative.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public Timer createIntervalTimer(Date initialExpiration,
      long intervalDuration, TimerConfig timerConfig)
      throws IllegalArgumentException, IllegalStateException, EJBException
  {
    if (initialExpiration == null) {
      throw new IllegalArgumentException(L
          .l("Timer initial expiration must not be null."));
    }

    if (initialExpiration.getTime() < 0) {
      throw new IllegalArgumentException(L
          .l("Timer initial expiration must not be negative."));
    }

    if (intervalDuration < 0) {
      throw new IllegalArgumentException(L
          .l("Timer interval duration must not be negative."));
    }

    Serializable info = null;

    if (timerConfig != null)
      info = timerConfig.getInfo();

    return createRepeatingTimer(initialExpiration, intervalDuration, info);
  }

  /**
   * Create a calendar-based timer based on the input schedule expression.
   * 
   * @param schedule
   *          A schedule expression describing the timeouts for this timer.
   * @param info
   *          Application information to be delivered along with the timer
   *          expiration. This can be null.
   * @return The newly created Timer.
   * @throws IllegalArgumentException
   *           If Schedule represents an invalid schedule expression.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public Timer createCalendarTimer(ScheduleExpression schedule,
      Serializable info) throws IllegalArgumentException,
      IllegalStateException, EJBException
  {
    return createScheduledTimer(schedule, info);
  }

  /**
   * Create a calendar-based timer based on the input schedule expression.
   * 
   * @param schedule
   *          A schedule expression describing the timeouts for this timer.
   * @param timerConfig
   *          Timer configuration.
   * @return The newly created Timer.
   * @throws IllegalArgumentException
   *           If Schedule represents an invalid schedule expression.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public Timer createCalendarTimer(ScheduleExpression schedule,
      TimerConfig timerConfig) throws IllegalArgumentException,
      IllegalStateException, EJBException
  {
    Serializable info = null;

    if (timerConfig != null)
      info = timerConfig.getInfo();

    return createScheduledTimer(schedule, info);
  }

  /**
   * Get all the active timers associated with this bean.
   * 
   * @return A collection of javax.ejb.Timer objects.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public Collection<Timer> getTimers() throws IllegalStateException,
      EJBException
  {
    Collection<Timer> timers = new LinkedList<Timer>();

    synchronized (_timers) {
      for (TimerTask task : _timers) {
        timers.add(new EjbTimer(task));
      }
    }

    return timers;
  }

  /**
   * Create a single-action timer that expires at a given point in time.
   * 
   * @param expiration
   *          The point in time at which the timer must expire.
   * @param info
   *          Application information to be delivered along with the timer
   *          expiration. This can be null.
   * @return The newly created Timer.
   */
  private Timer createOneTimeTimer(long expiration, Serializable info)
  {
    Trigger trigger = new TimerTrigger(expiration);

    return createTimer(trigger, info);
  }

  /**
   * Create a single-action timer that expires at a given point in time.
   * 
   * @param expiration
   *          The point in time at which the timer must expire.
   * @param info
   *          Application information to be delivered along with the timer
   *          expiration. This can be null.
   * @return The newly created Timer.
   */
  private Timer createTimer(Trigger trigger, Serializable info)
  {
    EjbTimer timer = new EjbTimer();
    TimerTask scheduledTask
      = new TimerTask(_timeout, timer, null, trigger, info);
    timer.setScheduledTask(scheduledTask);

    synchronized (_timers) {
      _timers.add(scheduledTask);
    }

    scheduledTask.start();

    return timer;
  }

  /**
   * Create an interval timer whose first expiration occurs at a given point in
   * time and whose subsequent expirations occur after a specified interval.
   * 
   * @param expiration
   *          The point in time at which the first timer expiration must occur.
   * @param interval
   *          The number of milliseconds that must elapse between timer
   *          expiration notifications.
   * @param info
   *          Application information to be delivered along with the timer
   *          expiration. This can be null.
   * @return The newly created Timer.
   */
  private Timer createRepeatingTimer(Date expiration, long interval,
      Serializable info)
  {
    Trigger trigger = new TimerTrigger(expiration.getTime(), interval);
    EjbTimer timer = new EjbTimer();

    TimerTask scheduledTask = new TimerTask(_timeout, timer, null, trigger,
        info);
    timer.setScheduledTask(scheduledTask);

    synchronized (_timers) {
      _timers.add(scheduledTask);
    }

    scheduledTask.start();

    return timer;
  }

  /**
   * Create a calendar-based timer based on the input schedule expression.
   * 
   * @param schedule
   *          A schedule expression describing the timeouts for this timer.
   * @param info
   *          Application information to be delivered along with the timer
   *          expiration. This can be null.
   * @return The newly created Timer.
   */
  private Timer createScheduledTimer(ScheduleExpression schedule,
      Serializable info)
  {
    CronExpression cronExpression = new CronExpression(schedule.getSecond(),
        schedule.getMinute(), schedule.getHour(), schedule.getDayOfWeek(),
        schedule.getDayOfMonth(), schedule.getMonth(), schedule.getYear());

    TimeZone timezone = null;

    if (!schedule.getTimezone().trim().equals("")) {
      timezone = TimeZone.getTimeZone(schedule.getTimezone());
    }

    long start = -1;
    long end = -1;

    if (schedule.getStart() != null) {
      start = schedule.getStart().getTime();
    }

    if (schedule.getEnd() != null) {
      end = schedule.getEnd().getTime();
    }

    Trigger trigger = new CronTrigger(cronExpression, start, end, timezone);
    EjbTimer timer = new EjbTimer();
    TimerTask scheduledTask = new TimerTask(_timeout, timer, cronExpression,
        trigger, info);
    timer.setScheduledTask(scheduledTask);

    synchronized (_timers) {
      _timers.add(scheduledTask);
    }

    scheduledTask.start();

    return timer;
  }

  /**
   * Returns a string representation of the object.
   * 
   * @return String representation of the object.
   */
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _server + "]";
  }
}