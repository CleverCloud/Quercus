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

package com.caucho.resources;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

/**
 * The task resource starts a background task on initialization.
 * intervals.
 */
public class TaskResource {
  private static final L10N L = new L10N(TaskResource.class);
  private static final Logger log
    = Logger.getLogger(TaskResource.class.getName());

  @Resource
  private Executor _executor;
  
  private Runnable _work;

  /**
   * Sets the work task.
   */
  public void setWork(Runnable work)
  {
    _work = work;
  }

  /**
   * Initialization.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_work == null)
      throw new ConfigException(L.l("TaskResource needs a <work> task."));
  }

  /**
   * Starting.
   */
  public void start()
  {
    _executor.execute(_work);
  }
}
