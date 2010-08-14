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

package com.caucho.jms.queue;

import java.util.*;
import java.util.logging.*;

import javax.jms.*;

import com.caucho.jms.message.*;
import com.caucho.jms.connection.*;

import com.caucho.config.types.Period;
import com.caucho.util.*;

/**
 * Implements an queue which polls the data periodically.
 */
abstract public class PollingQueue<E> extends AbstractQueue<E>
  implements AlarmListener
{
  private static final L10N L = new L10N(PollingQueue.class);
  private static final Logger log
    = Logger.getLogger(PollingQueue.class.getName());

  private long _pollPeriod = 10000;

  private boolean _isPolling;
  private WeakAlarm _alarm;

  protected PollingQueue()
  {
    _alarm = new WeakAlarm(this);
  }

  public void setPollPeriod(Period period)
  {
    _pollPeriod = period.getPeriod();
  }

  @Override
  protected void startPoll()
  {
    _isPolling = true;
    _alarm.queue(_pollPeriod);
  }

  @Override
  protected void stopPoll()
  {
    _isPolling = false;
    _alarm.dequeue();
  }

  public void handleAlarm(Alarm alarm)
  {
    try {
      pollImpl();
    } finally {
      if (_isPolling)
        _alarm.queue(_pollPeriod);
    }
  }

  protected void pollImpl()
  {
  }
}

