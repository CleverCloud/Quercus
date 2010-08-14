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

package com.caucho.security;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.ServletException;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.types.InitParam;
import com.caucho.server.security.RolePrincipal;
import com.caucho.util.L10N;

/**
 * The JAAS authenticator uses an existing JAAS LoginModule.  Applications
 * which have existing JAAS modules can use the JaasAuthenticator to
 * log users in based on the old login.
 *
 * <code><pre>
 * &lt;authenticator url="jaas:">
 *   &lt;init login-module="example.MyLogin"/>
 * &lt;/authenticator>
 */
@SuppressWarnings("serial")
public class JaasAuthenticator extends AbstractAuthenticator {
  private static final L10N L = new L10N(JaasAuthenticator.class);
  
  private static final Logger log
    = Logger.getLogger(JaasAuthenticator.class.getName());
  
  private Class<?> _loginModuleClass;

  private HashMap<String,String> _options
    = new HashMap<String,String>();

  public JaasAuthenticator()
  {
    setPasswordDigest(null);
  }

  /**
   * Sets the JAAS spi login module class.
   */
  public void setLoginModule(Class<?> loginModuleClass)
    throws ConfigException
  {
    _loginModuleClass = loginModuleClass;

    Config.checkCanInstantiate(loginModuleClass);

    if (! LoginModule.class.isAssignableFrom(loginModuleClass))
      throw new ConfigException(L.l("'{0}' must implement javax.security.auth.spi.LoginModule",
                                    loginModuleClass.getName()));
  }

  public void setInitParam(InitParam init)
  {
    _options.putAll(init.getParameters());
  }

  public void setOptions(InitParam init)
  {
    _options.putAll(init.getParameters());
  }

  /**
   * Initialize the authenticator.
   */
  @PostConstruct
  public synchronized void init()
    throws ServletException
  {
    super.init();
    
    if (_loginModuleClass == null)
      throw new ServletException(L.l("JaasAuthenticator requires login-module"));
  }
  
  /**
   * Authenticate (login) the user.
   */
  @Override
  protected Principal authenticate(Principal principal,
                                   PasswordCredentials cred,
                                   Object details)
  {
    try {
      String userName = principal.getName();
      String password = new String(cred.getPassword());
      
      LoginModule login = (LoginModule) _loginModuleClass.newInstance();
      Subject subject = new Subject();

      HashMap<String,String> state = new HashMap<String,String>();

      state.put("javax.security.auth.login.name", userName);
      state.put("javax.security.auth.login.password", password);

      login.initialize(subject,
                       new Handler(userName, password),
                       state, _options);

      try {
        login.login();
      } catch (Exception e) {
        login.abort();
      }

      login.commit();

      Set principals = subject.getPrincipals();

      if (principals == null || principals.size() == 0)
        return null;

      Iterator iter = principals.iterator();
      if (iter.hasNext())
        return (Principal) iter.next();

      return null;
    } catch (LoginException e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns true if the user plays the named role.
   *
   * @param request the servlet request
   * @param user the user to test
   * @param role the role to test
   */
  public boolean isUserInRole(Principal principal, String role)
  {
    if (principal == null)
      return false;

    if (principal instanceof RolePrincipal)
      return ((RolePrincipal) principal).isUserInRole(role);
    else
      return "user".equals(role);
  }

  static class Handler implements CallbackHandler {
    private String _userName;
    private String _password;

    Handler(String userName, String password)
    {
      _userName = userName;
      _password = password;
    }
    
    public void handle(Callback []callbacks)
      throws IOException, UnsupportedCallbackException
    {
      for (int i = 0; i < callbacks.length; i++) {
        Callback cb = callbacks[i];

        if (cb instanceof NameCallback) {
          NameCallback name = (NameCallback) cb;

          name.setName(_userName);
        }
        else if (cb instanceof PasswordCallback) {
          PasswordCallback password = (PasswordCallback) cb;

          password.setPassword(_password.toCharArray());
        }
      }
    }
  }
}
