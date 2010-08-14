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

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;

import com.caucho.inject.Module;
import com.caucho.util.ThreadTask;

/**
 * Implementation of the work manager.
 */
@Module
public class WorkThread implements ThreadTask {
  private boolean _isStarted;
  private WorkManagerImpl _manager;
  private ClassLoader _classLoader;
  private Work _work;
  private WorkListener _listener;

  /**
   * Constructor.
   */
  WorkThread(WorkManagerImpl manager, Work work,
             ClassLoader classLoader, WorkListener listener)
  {
    _manager = manager;
    _work = work;
    _classLoader = classLoader;
    _listener = listener;
  }

  /**
   * Returns true if it's started.
   */
  public boolean isStarted()
  {
    return _isStarted;
  }

  /**
   * The runnable.
   */
  public void run()
  {
    Thread thread = Thread.currentThread();
    thread.setContextClassLoader(_classLoader);

    _isStarted = true;

    try {
      synchronized (this) {
        notifyAll();
      }
    } catch (Exception e) {
    }

    try {
      _work.run();

      if (_listener != null)
        _listener.workCompleted(new WorkEvent(_manager,
                                              WorkEvent.WORK_COMPLETED,
                                              _work, null, 0));
    } finally {
      _manager.completeWork(_work);
    }
  }
}
