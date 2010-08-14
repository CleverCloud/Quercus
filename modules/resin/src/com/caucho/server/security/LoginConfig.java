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

package com.caucho.server.security;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.inject.InjectManager;
import com.caucho.security.Authenticator;
import com.caucho.security.AbstractLogin;
import com.caucho.security.BasicLogin;
import com.caucho.security.DigestLogin;
import com.caucho.security.Login;
import com.caucho.util.L10N;

import javax.servlet.ServletException;
import javax.enterprise.inject.spi.InjectionTarget;
import java.util.logging.Logger;

/**
 * Configuration for the login-config.
 */
public class LoginConfig {
  private static final Logger log
    = Logger.getLogger(LoginConfig.class.getName());
  private static final L10N L = new L10N(LoginConfig.class);

  private String _authMethod = "basic";
  private String _realmName;
  private Class _customType;
  private ContainerProgram _formLoginConfig;
  private ContainerProgram _init;

  private Authenticator _authenticator;

  /**
   * Creates the login-config.
   */
  public LoginConfig()
  {
  }

  /**
   * Sets the auth-method
   */
  public void setAuthMethod(String method)
  {
    _authMethod = method;
  }

  /**
   * Gets the auth-method
   */
  public String getAuthMethod()
  {
    return _authMethod;
  }

  /**
   * Sets the authenticator.
   */
  public void setAuthenticator(Authenticator auth)
  {
    _authenticator = auth;
  }

  /**
   * Sets the custom type
   */
  public void setType(Class type)
    throws ConfigException
  {
    _customType = type;

    Config.validate(type, AbstractLogin.class);
  }

  /**
   * Sets the realm-name
   */
  public void setRealmName(String realmName)
  {
    _realmName = realmName;
  }

  /**
   * Gets the realm-name
   */
  public String getRealmName()
  {
    return _realmName;
  }

  /**
   * Creates the form-login-config
   */
  public ContainerProgram createFormLoginConfig()
  {
    if (_formLoginConfig == null)
      _formLoginConfig = new ContainerProgram();

    return _formLoginConfig;
  }

  /**
   * Creates the init
   */
  public ContainerProgram createInit()
  {
    if (_init == null)
      _init = new ContainerProgram();

    return _init;
  }

  /**
   * Returns the login.
   */
  public Login getLogin()
  {
    try {
      /*
        if (auth == null)
        throw new ServletException(L.l("Login needs an authenticator resource with JNDI name java:comp/env/caucho/auth"));
      */

      AbstractLogin login;

      if (_customType != null) {
        login = (AbstractLogin) _customType.newInstance();

        if (_init != null)
          _init.configure(login);
      }
      else if (_authMethod.equalsIgnoreCase("basic")) {
        BasicLogin basicLogin = new BasicLogin();
        basicLogin.setRealmName(_realmName);
        login = basicLogin;
      }
      else if (_authMethod.equalsIgnoreCase("digest")) {
        DigestLogin digestLogin = new DigestLogin();
        digestLogin.setRealmName(_realmName);
        login = digestLogin;
      }
      else if (_authMethod.equalsIgnoreCase("client-cert")) {
        ClientCertLogin certLogin = new ClientCertLogin();
        login = certLogin;
      }
      else if (_authMethod.equalsIgnoreCase("form")) {
        login = new FormLogin();

        if (_formLoginConfig == null)
          throw new ConfigException(L.l("'form' authentication requires form-login"));

        _formLoginConfig.configure(login);
      }
      else
        throw new ConfigException(L.l("'{0}' is an unknown auth-type.",
                                       _authMethod));

      if (_authenticator != null)
        login.setAuthenticator(_authenticator);

      InjectManager manager = InjectManager.create();
      InjectionTarget inject = manager.createInjectionTarget(login.getClass());
      inject.inject(login, manager.createCreationalContext(null));

      login.init();

      return login;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}
