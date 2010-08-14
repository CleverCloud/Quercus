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

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import java.io.Serializable;

import java.util.*;
import java.util.logging.*;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

/**
 * mail service
 */
public class MailService
{
  private static final L10N L = new L10N(MailService.class);

  private static final Logger log
    = Logger.getLogger(MailService.class.getName());

  private ArrayList<Address> _toList = new ArrayList<Address>();
  private Address []_to;

  private String _subject = "Resin MailService messages";

  private Properties _properties = new Properties();

  private Session _session;
  private Transport _smtp;

  /**
   * Sets the mail session
   */
  public void setSession(Session session)
  {
    _session = session;
  }

  /**
   * Gets the mail session
   */
  public Session getSession()
  {
    return _session;
  }

  /**
   * Adds a 'to' value
   */
  public void addTo(Address address)
  {
    _toList.add(address);
  }

  /**
   * Sets a property
   */
  public void setProperty(String key, String value)
  {
    _properties.put(key, value);
  }

  /**
   * Sets properties
   */
  public void setProperties(Properties props)
  {
    _properties.putAll(props);
  }

  /**
   * Sets the subject
   */
  public void setSubject(String subject)
  {
    _subject = subject;
  }

  /**
   * Sends to a mailbox
   */
  public void send(String text)
  {
    try {
      MimeMessage msg = new MimeMessage(getSession());

      msg.addRecipients(RecipientType.TO, _to);
      msg.setSubject(_subject);
      msg.setContent(text, "text/plain");

      send(msg);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sends to a mailbox
   */
  public void send(Message message)
  {
    Transport smtp = null;

    try {
      smtp = _session.getTransport("smtp");

      smtp.connect();

      smtp.send(message, _to);

      log.fine(this + " sent mail to " + _to[0]);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
        if (smtp != null)
          smtp.close();
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
  }

  public void init()
  {
    if (_toList.size() == 0)
      throw new ConfigException(L.l("mail service requires at least one 'to' address"));

    _to = new Address[_toList.size()];
    _toList.toArray(_to);

    try {
      if (_session == null) {
        _session = Session.getInstance(_properties);
      }

      Transport smtp = _session.getTransport("smtp");

      smtp.close();
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + _toList;
  }
}
