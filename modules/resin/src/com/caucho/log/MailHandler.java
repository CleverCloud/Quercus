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

import com.caucho.hemp.services.MailService;
import com.caucho.config.ConfigException;
import com.caucho.config.types.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.mail.*;
import javax.mail.internet.*;
import javax.management.*;

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
 * Sends formatted messages to mail
 */
public class MailHandler extends Handler implements AlarmListener
{
  private static final Logger log
    = Logger.getLogger(MailHandler.class.getName());
  private static final L10N L = new L10N(MailHandler.class);

  private long _delayTime = 60000L;
  private long _timeIntervalMin = 3 * 60 * 60000L;

  private long _lastMailTime;

  private StringBuilder _text;
  private Alarm _alarm;

  private MailService _mailService = new MailService();

  public MailHandler()
  {
    _alarm = new Alarm(this);
  }

  /**
   * Sets the delay time, i.e. how long the service should accumulate
   * messages before sending them.
   */
  public void setDelayTime(Period period)
  {
    _delayTime = period.getPeriod();
  }

  /**
   * Sets the delay time, i.e. how long the service should accumulate
   * messages before sending them.
   */
  public void setMailIntervalMin(Period period)
  {
    _timeIntervalMin = period.getPeriod();
  }

  /**
   * Sets the mail session
   */
  public void setMailSession(Session session)
  {
    _mailService.setSession(session);
  }

  /**
   * Sets a property
   */
  public void setProperty(String key, String value)
  {
    _mailService.setProperty(key, value);
  }

  /**
   * Sets properties
   */
  public void setProperties(Properties props)
  {
    _mailService.setProperties(props);
  }

  /**
   * Adds a 'to'
   */
  public void addTo(String to)
    throws AddressException
  {
    _mailService.addTo(new InternetAddress(to));
  }

  /**
   * Initialize the handler
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    _mailService.init();
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
      else {
        value = record.getMessage();

        Throwable thrown = record.getThrown();
        if (thrown != null) {
          java.io.StringWriter writer = new java.io.StringWriter();
          PrintWriter out = new PrintWriter(writer);
          thrown.printStackTrace(out);
          out.close();

          value += "\n" + out;
        }
      }

      boolean isStartAlarm = false;
      synchronized (this) {
        if (_text == null) {
          isStartAlarm = true;
          _text = new StringBuilder();
        }

        _text.append(value).append("\n");
      }

      if (isStartAlarm) {
        long delta = _lastMailTime + _timeIntervalMin - Alarm.getCurrentTime();

        if (delta < _delayTime)
          delta = _delayTime;

        _alarm.queue(delta);
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

  public void handleAlarm(Alarm alarm)
  {
    String text = null;

    synchronized (this) {
      if (_text != null)
        text = _text.toString();
      _text = null;
    }

    _lastMailTime = Alarm.getCurrentTime();

    if (text != null)
      _mailService.send(text);
  }

  @PreDestroy()
  public void close()
  {
    String text = null;

    synchronized (this) {
      if (_text != null)
        text = _text.toString();
      _text = null;
    }

    if (text != null)
      _mailService.send(text);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
