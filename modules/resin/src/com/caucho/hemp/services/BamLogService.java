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

package com.caucho.hemp.services;

import com.caucho.bam.SimpleActor;
import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;

import java.io.Serializable;

import java.util.*;
import java.util.logging.*;

import javax.jms.TextMessage;
import javax.jms.ObjectMessage;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;


/**
 * log service
 */
public class BamLogService
  extends SimpleActor
{
  private static final L10N L = new L10N(BamLogService.class);
  private static final Logger log
    = Logger.getLogger(BamMailService.class.getName());

  private Logger _log = log;
  private Level _level = Level.INFO;

  /**
   * Sets the logger name.
   */
  public void setName(String name)
  {
    _log = Logger.getLogger(name);
  }

  /**
   * Sets the level
   */
  public void setLevel(String level)
  {
    if ("off".equals(level))
      _level = Level.OFF;
    else if ("all".equals(level))
      _level = Level.ALL;
    else if ("finest".equals(level))
      _level = Level.FINEST;
    else if ("finer".equals(level))
      _level = Level.FINER;
    else if ("fine".equals(level))
      _level = Level.FINE;
    else if ("config".equals(level))
      _level = Level.CONFIG;
    else if ("info".equals(level))
      _level = Level.INFO;
    else if ("warning".equals(level))
      _level = Level.WARNING;
    else if ("severe".equals(level))
      _level = Level.SEVERE;
    else
      throw new ConfigException(L.l("'{0}' is an unknown logging level",
                                    level));
  }

  /**
   * Sends to a mailbox
   */
  @Override
  public void message(String to, String from, Serializable value)
  {
    String text = messageToText(value);

    if (_log.isLoggable(_level))
      _log.log(_level, text);
  }

  protected String messageToText(Serializable value)
  {
    String text = null;
    
    if (value instanceof String) {
      text = value.toString();
    }
    else if (value instanceof TextMessage) {
      try {
        text = ((TextMessage) value).getText();
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
    else if (value instanceof ObjectMessage) {
      try {
        text = String.valueOf(((ObjectMessage) value).getObject());
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
    
    if (text == null)
      text = String.valueOf(value);

    return text;
  }
}
