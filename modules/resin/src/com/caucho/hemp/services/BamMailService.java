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
import com.caucho.config.types.Period;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;

import java.io.Serializable;

import java.util.*;
import java.util.logging.*;

import javax.jms.TextMessage;
import javax.jms.ObjectMessage;

import javax.mail.Address;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.AddressException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

/**
 * mail service
 */
public class BamMailService
  extends SimpleActor
  implements AlarmListener
{
  private static final Logger log
    = Logger.getLogger(BamMailService.class.getName());

  @Inject
  private Session _session;

  private long _delayTime = 60000L;

  private StringBuilder _text;
  private Alarm _alarm;

  private MailService _mailService = new MailService();

  public BamMailService()
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
   * Sets subject
   */
  public void setSubject(String subject)
  {
    _mailService.setSubject(subject);
  }

  /**
   * Adds a 'to'
   */
  public void addTo(String to)
    throws AddressException
  {
    _mailService.addTo(new InternetAddress(to));
  }

  @PostConstruct
  public void init()
  {
    _mailService.init();
  }

  /**
   * Sends to a mailbox
   */
  @Override
  public void message(String to, String from, Serializable value)
  {
    String text = messageToText(value);

    if (_delayTime <= 0) {
      _mailService.send(text);
      return;
    }

    boolean isStartAlarm = false;
    synchronized (this) {
      if (_text == null) {
        isStartAlarm = true;
        _text = new StringBuilder();
      }
    
      _text.append(text).append("\n");
    }

    if (isStartAlarm)
      _alarm.queue(_delayTime);
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

  public void handleAlarm(Alarm alarm)
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
}
