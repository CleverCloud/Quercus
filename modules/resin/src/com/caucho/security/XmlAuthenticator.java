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

package com.caucho.security;

import java.security.Principal;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.servlet.ServletException;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.util.Alarm;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;

/**
 * The XML authenticator reads a static file for authentication.
 *
 * <code><pre>
 * &lt;security:XmlAuthenticator path="WEB-INF/users.xml"/>
 * </pre></code>
 *
 * <p>The format of the static file is as follows:
 *
 * <code><pre>
 * &lt;users>
 *   &lt;user name="h.potter" password="quidditch" roles="user,captain"/>
 *   ...
 * &lt;/users>
 * </pre></code>
 *
 * <p>The authenticator can also be configured in the resin-web.xml:
 *
 * <code><pre>
 * &lt;security:XmlAuthenticator password-digest="none">
 *   &lt;user name="Harry Potter" password="quidditch" roles="user,captain"/>
 * &lt;/security:XmlAuthenticator>
 * </pre></code>
 */
@Singleton
@SuppressWarnings("serial")
public class XmlAuthenticator extends AbstractAuthenticator
{
  private static final Logger log =
    Logger.getLogger(XmlAuthenticator.class.getName());
  
  private Path _path;
  private Hashtable<String,PasswordUser> _userMap
    = new Hashtable<String,PasswordUser>();

  private Depend _depend;
  private long _lastCheck;

  /**
   * Sets the path to the XML file.
   */
  public void setPath(Path path)
  {
    _path = path;
  }

  /**
   * Gets the path to the XML file.
   */
  public Path getPath()
  {
    return _path;
  }
  
  /**
   * Returns the default group for a user
   */
  protected String getDefaultGroup()
  {
    return "user";
  }

  /**
   * Adds a user from the configuration.
   *
   * <pre>
   * &lt;init user='Harry Potter:quidditch:user,webdav'/>
   * </pre>
   */
  public User createUser()
  {
    return new User();
  }

  /**
   * Adds a user from the configuration.
   *
   * <pre>
   * &lt;init user='Harry Potter:quidditch:user,webdav'/>
   * </pre>
   */
  public void addUser(User user)
  {
    _userMap.put(user.getName(), user.getPasswordUser());
  }

  /**
   * Returns the user map
   */
  protected Hashtable<String,PasswordUser> getUserMap()
  {
    return _userMap;
  }

  /**
   * Initialize the XML authenticator.
   */
  @PostConstruct
  public synchronized void init()
    throws ServletException
  {
    super.init();

    reload();
  }
  
  /**
   * Returns the PasswordUser
   */
  @Override
  protected PasswordUser getPasswordUser(String userName)
  {
    if (userName == null)
      return null;
    
    if (isModified())
      reload();

    PasswordUser user = _userMap.get(userName);

    if (user != null)
      return user.copy();
    else
      return null;
  }

  /**
   * Reload the authenticator.
   */
  public synchronized void reload()
  {
    if (_path == null)
      return;
    
    try {
      _lastCheck = Alarm.getCurrentTime();
      _depend = new Depend(_path);

      if (log.isLoggable(Level.FINE))
        log.fine(this + " loading users from " + _path);
      
      _userMap = new Hashtable<String,PasswordUser>();
      
      new Config().configureBean(this, _path);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  private boolean isModified()
  {
    if (_path == null)
      return false;
    else if (_depend == null)
      return true;
    else if (Alarm.getCurrentTime() < _lastCheck + 5000)
      return false;
    else {
      _lastCheck = Alarm.getCurrentTime();
      return _depend.isModified();
    }
  }

  public class User {
    private String _name;
    private String _password;
    
    private Principal _principal;
    private String []_roles = new String[0];

    private boolean _isDisabled;

    public User()
    {
    }
    
    User(String name, String password, Principal principal)
    {
      _name = name;
      _password = password;
      _principal = principal;
    }

    public void setName(String name)
    {
      _name = name;

      if (_principal == null)
        _principal = new BasicPrincipal(name);
    }

    public String getName()
    {
      return _name;
    }

    public void setPassword(String password)
    {
      _password = password;
    }

    public void setPrincipal(Principal principal)
    {
      _principal = principal;
    }

    Principal getPrincipal()
    {
      return _principal;
    }

    public void addRoles(String roles)
    {
      for (String role : roles.split("[ ,]")) {
        addRole(role);
      }
    }

    public void setEnable(boolean isEnabled)
    {
      _isDisabled = ! isEnabled;
    }

    public void setDisable(boolean isDisabled)
    {
      _isDisabled = isDisabled;
    }

    public void addGroup(String role)
    {
      addRole(role);
    }
    
    public void addRole(String role)
    {
      if ("disabled".equals(role))
        _isDisabled = true;
      
      String []newRoles = new String[_roles.length + 1];
      System.arraycopy(_roles, 0, newRoles, 0, _roles.length);
      newRoles[_roles.length] = role;

      _roles = newRoles;
    }

    String []getRoles()
    {
      return _roles;
    }

    public void addText(String userParam)
    {
      int p1 = userParam.indexOf(':');

      if (p1 < 0)
        return;

      String name = userParam.substring(0, p1);
      int p2 = userParam.indexOf(':', p1 + 1);
      String password;
      String roles;

      if (p2 < 0) {
        password = userParam.substring(p1 + 1);
        roles = getDefaultGroup();
      }
      else {
        password = userParam.substring(p1 + 1, p2);
        roles = userParam.substring(p2 + 1);
      }

      setName(name);
      setPassword(password);
      addRoles(roles);
    }

    @PostConstruct
    public void init()
    {
      if ((_roles == null || _roles.length == 0) && getDefaultGroup() != null)
        _roles = new String[] { getDefaultGroup() };
    }

    public PasswordUser getPasswordUser()
    {
      boolean isAnonymous = false;

      return new PasswordUser(_principal, _password.toCharArray(),
                              _isDisabled, isAnonymous,
                              _roles);
    }
  }
}
