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

import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.annotation.PostConstruct;

import com.caucho.bam.ActorClient;
import com.caucho.bam.SimpleActorClient;
import com.caucho.config.ConfigException;
import com.caucho.hemp.broker.HempBroker;
import com.caucho.util.L10N;

/**
 * Sends formatted messages to HMTP target
 */
public class HmtpHandler extends Handler {
  private static final L10N L = new L10N(HmtpHandler.class);

  private String _to;
  private ActorClient _conn;

  public HmtpHandler()
  {
  }

  /**
   * Sets the destination
   */
  public void setTo(String to)
  {
    _to = to;
  }

  /**
   * Initialize the handler
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_to == null)
      throw new ConfigException(L.l("BamHandler needs a 'to' attribute"));

    HempBroker broker = HempBroker.getCurrent();
    
    _conn = new SimpleActorClient(broker, "log@localhost", null);
  }

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

    try {
      String value;

      Formatter formatter = getFormatter();
      if (formatter != null)
        value = formatter.format(record);
      else
        value = record.getMessage();

      _conn.message(_to, value);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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
    return getClass().getSimpleName() + "[" + _to + "]";
  }
}
