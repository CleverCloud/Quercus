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

package com.caucho.jca.cfg;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import com.caucho.config.ConfigException;
import com.caucho.config.cfg.AbstractBeanConfig;
import com.caucho.config.inject.BeanBuilder;
import com.caucho.config.inject.InjectManager;

/**
 * Configuration for a javamail.
 */
public class JavaMailConfig extends AbstractBeanConfig {
  private Properties _props = new Properties();
  private Authenticator _auth;

  private String _user;
  private String _password;
  private Session _session;
  
  public JavaMailConfig()
  {
    setClass(Session.class);
  }

  /**
   * Sets the authenticator
   */
  public void setAuthenticator(Authenticator auth)
  {
    _auth = auth;
  }

  //
  // well-known attributes
  //

  /**
   * mail.from
   */
  public void setFrom(String from)
  {
    setProperty("mail.from", from);
  }

  /**
   * mail.host
   */
  public void setHost(String host)
  {
    setProperty("mail.host", host);
  }

  /**
   * mail.imap.host
   */
  public void setImapHost(String host)
  {
    setProperty("mail.imap.host", host);
  }

  /**
   * mail.imap.user
   */
  public void setImapUser(String user)
  {
    setProperty("mail.imap.user", user);
  }

  /**
   * mail.pop3.host
   */
  public void setPop3Host(String host)
  {
    setProperty("mail.pop3.host", host);
  }

  /**
   * mail.pop3.user
   */
  public void setPop3User(String user)
  {
    setProperty("mail.pop3.user", user);
  }

  /**
   * mail.smtp.auth
   */
  public void setSmtpAuth(boolean isEnable)
  {
    setProperty("mail.smtp.auth", isEnable ? "true" : "false");
  }

  /**
   * mail.smtp.host
   */
  public void setSmtpHost(String host)
  {
    setProperty("mail.smtp.host", host);
  }

  /**
   * mail.smtp.ssl
   */
  public void setSmtpSsl(boolean ssl)
  {
    setProperty("mail.smtp.ssl", String.valueOf(ssl));
  }

  /**
   * mail.smtp.port
   */
  public void setSmtpPort(int port)
  {
    setProperty("mail.smtp.port", String.valueOf(port));
  }
  
  /**
   * mail.smtp.user
   */
  public void setSmtpUser(String user)
  {
    setProperty("mail.smtp.user", user);
  }

  /**
   * mail.store.protocol
   */
  public void setStoreProtocol(String protocol)
  {
    setProperty("mail.store.protocol", protocol);
  }

  /**
   * mail.transport.protocol
   */
  public void setTransportProtocol(String protocol)
  {
    setProperty("mail.transport.protocol", protocol);
  }

  /**
   * mail.user
   */
  public void setUser(String user)
  {
    _user = user;
    
    setProperty("mail.user", user);
  }

  /**
   * password
   */
  public void setPassword(String password)
  {
    _password = password;
  }

  /**
   * Sets an attribute.
   */
  public void setProperty(String name, String value)
  {
    _props.put(name, value);
  }

  public void setProperties(Properties props)
  {
    _props.putAll(props);
  }

  public void setValue(Properties props)
  {
    _props.putAll(props);
  }

  @Override
  public void initImpl()
    throws ConfigException
  {
    super.initImpl();
    
    try {
      if (getInit() != null)
        getInit().configure(this);

      Authenticator auth = _auth;

      if (auth == null && _user != null && _password != null)
        auth = new StandardAuthenticator(_user, _password);
      
      if (auth != null)
        _session = Session.getInstance(_props, auth);
      else
        _session = Session.getInstance(_props);

      deploy();
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  @Override
  public Object replaceObject()
  {
    return _session;
  }

  static class StandardAuthenticator extends Authenticator {
    private final String _userName;
    private final PasswordAuthentication _passwordAuth;

    StandardAuthenticator(String userName, String password)
    {
      _userName = userName;
      _passwordAuth = new PasswordAuthentication(userName, password);
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication()
    {
      return _passwordAuth;
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _userName + "]";
    }
  }
}
