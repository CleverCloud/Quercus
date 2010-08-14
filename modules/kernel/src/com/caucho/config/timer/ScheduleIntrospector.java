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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.TimeZone;

import javax.ejb.Schedule;
import javax.ejb.Schedules;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.types.Trigger;

/**
 * Processes EJB declarative scheduling. The scheduling functionality can be
 * applied to bean types that are not EJBs.
 * 
 * @author Reza Rahman
 */
public class ScheduleIntrospector {
  /**
   * Introspects the method for scheduling attributes.
   */
  public ArrayList<TimerTask> introspect(TimeoutCaller caller,
                                         AnnotatedType<?> type)
  {
    ArrayList<TimerTask> timers = null;

    for (AnnotatedMethod<?> method : type.getMethods()) {
      Schedules schedules = method.getAnnotation(Schedules.class);

      if (schedules != null) {
        if (timers == null)
          timers = new ArrayList<TimerTask>();

        for (Schedule schedule : schedules.value()) {
          addSchedule(timers, schedule, caller, getScheduledMethod(method));
        }
      }

      Schedule schedule = method.getAnnotation(Schedule.class);

      if (schedule != null) {
        if (timers == null)
          timers = new ArrayList<TimerTask>();

        addSchedule(timers, schedule, caller, getScheduledMethod(method));
      }
    }

    return timers;
  }
  
  /**
   * Returns the method to call when the schedule event occurs.
   */
  protected Method getScheduledMethod(AnnotatedMethod<?> method)
  {
    return method.getJavaMember();
  }

  private void addSchedule(ArrayList<TimerTask> timers, Schedule schedule,
                           TimeoutCaller caller, Method method)
  {
    CronExpression cronExpression
      = new CronExpression(schedule.second(),
                           schedule.minute(),
                           schedule.hour(), 
                           schedule.dayOfWeek(),
                           schedule.dayOfMonth(), 
                           schedule.month(),
                           schedule.year());

    TimeZone timezone = null;

    if (!schedule.timezone().trim().equals("")) {
      timezone = TimeZone.getTimeZone(schedule.timezone());
    }

    Trigger trigger = new CronTrigger(cronExpression, -1, -1, timezone);
    EjbTimer ejbTimer = new EjbTimer();

    TimeoutInvoker timeoutInvoker
      = new MethodTimeoutInvoker(caller, method);

    TimerTask timerTask 
      = new TimerTask(timeoutInvoker, ejbTimer,
                      cronExpression, trigger, schedule.info());

    ejbTimer.setScheduledTask(timerTask);

    timers.add(timerTask);
  }
}