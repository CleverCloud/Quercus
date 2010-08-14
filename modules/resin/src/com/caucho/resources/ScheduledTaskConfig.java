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

package com.caucho.resources;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.el.MethodExpression;
import javax.enterprise.context.Dependent;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.Service;
import com.caucho.config.Unbound;
import com.caucho.config.cfg.BeanConfig;
import com.caucho.config.types.Period;
import com.caucho.util.L10N;

/**
 * The cron resources starts application Work tasks at cron-specified
 * intervals.
 *
 * @deprecated
 * @see com.caucho.resources.ScheduledTask
 */

@Service
@Unbound
public class ScheduledTaskConfig extends BeanConfig
{
  private static final L10N L = new L10N(ScheduledTaskConfig.class);
  private static final Logger log
    = Logger.getLogger(ScheduledTaskConfig.class.getName());

  private ScheduledTask _scheduledTask = new ScheduledTask();

  private boolean _isTask = false;

  /**
   * Constructor.
   */
  public ScheduledTaskConfig()
  {
    setBeanConfigClass(Runnable.class);

    setScopeType(Dependent.class);
  }

  @Override
  protected boolean isStartup()
  {
    return false;
  }

  /**
   * Sets the delay
   */
  @Configurable
  public void setDelay(Period delay)
  {
    _scheduledTask.setDelay(delay);
  }

  /**
   * Sets the period
   */
  @Configurable
  public void setPeriod(Period period)
  {
    _scheduledTask.setPeriod(period);
  }

  /**
   * Sets the cron interval.
   */
  public void setCron(String cron)
  {
    _scheduledTask.setCron(cron);
  }

  /**
   * Sets the method expression as a task
   */
  public void setMethod(MethodExpression method)
  {
    _scheduledTask.setMethod(method);

    _isTask = true;
  }

  /**
   * Sets the url expression as a task
   */
  public void setUrl(String url)
  {
    _scheduledTask.setUrl(url);

    _isTask = true;
  }

  /**
   * Sets the work task.
   */
  @Deprecated
  public void setWork(Runnable work)
  {
    _scheduledTask.setTask(work);

    _isTask = true;
  }

  /**
   * Sets the task.
   */
  @Configurable
  public void setTask(Runnable task)
  {
    _scheduledTask.setTask(task);

    _isTask = true;
    // setClass(task.getClass());
  }

  /**
   * Initialization.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (! _isTask) {
      super.init();

      if (_scheduledTask.getTask() == null)
        _scheduledTask.setTask((Runnable) getObject());
    }

    _scheduledTask.init();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _scheduledTask + "]";
  }
}
