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

import com.caucho.config.ConfigException;
import com.caucho.config.types.*;
import com.caucho.log.*;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

import javax.annotation.PostConstruct;
import javax.management.*;
import javax.jms.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;

/**
 * Sends formatted messages to JMS queue/topic
 */
public class JmsHandler extends Handler {
  private static final Logger log
    = Logger.getLogger(JmsHandler.class.getName());
  private static final L10N L = new L10N(JmsHandler.class);

  private ConnectionFactory _factory;
  private Connection _conn;
  private Session _session;
  
  private ArrayList<Destination> _destinationList
    = new ArrayList<Destination>();
  
  private ArrayList<BlockingQueue> _queueList
    = new ArrayList<BlockingQueue>();

  public JmsHandler()
  {
  }

  /**
   * Sets the connection factory
   */
  public void setConnectionFactory(ConnectionFactory factory)
  {
    _factory = factory;
  }

  /**
   * Adds a target (queue or jms destination)
   */
  public void addTarget(Destination target)
  {
    if (target instanceof BlockingQueue)
      _queueList.add((BlockingQueue) target);
    else
      _destinationList.add(target);
  }

  /**
   * Initialize the handler
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    try {
      if (_destinationList.size() > 0 && _factory == null)
        throw new ConfigException(L.l("jms: log handler requires a connection-factory"));

      if (_destinationList.size() > 0) {
        _conn = _factory.createConnection();
        _session = _conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
      }
    } catch (JMSException e) {
      throw ConfigException.create(e);
    }
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

      for (BlockingQueue queue : _queueList) {
        queue.offer(value);
      }
      
      if (_session != null) {
        synchronized (_session) {
          //
        }
      }
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
    if (_queueList.size() > 0)
      return getClass().getSimpleName() + _queueList;
    else
      return getClass().getSimpleName() + _destinationList;
  }
}
