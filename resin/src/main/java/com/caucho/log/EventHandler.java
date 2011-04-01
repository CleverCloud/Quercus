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

package com.caucho.log;

import com.caucho.config.inject.InjectManager;
import com.caucho.util.L10N;

import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.Filter;
import java.util.logging.Handler;

/**
 * raises a LogRecord as an event
 */
public class EventHandler extends Handler {
  private static final Logger log
    = Logger.getLogger(EventHandler.class.getName());
  private static final L10N L = new L10N(EventHandler.class);

  private InjectManager _webBeans = InjectManager.create();

  /**
   * Publishes the record.
   */
  public void publish(LogRecord record)
  {
    if (! isLoggable(record))
      return;

    Filter filter = getFilter();
    if (filter != null && ! filter.isLoggable(record))
      return;

    _webBeans.fireEvent(record);
  }

  /**
   * Flushes the buffer.
   */
  public void flush()
  {
  }

  /**
   * Closes the handler.
   */
  public void close()
  {
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
