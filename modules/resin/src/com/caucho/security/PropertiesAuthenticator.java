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

import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;

import com.caucho.config.ConfigException;
import com.caucho.config.Service;
import com.caucho.util.Alarm;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;

/**
 * The Property authenticator reads a properties file for authentication.
 *
 * <code><pre>
 * &lt;sec:PropertiesAuthenticator path="WEB-INF/users.xml"/>
 * </pre></code>
 *
 * <p>The format of the static file is as follows:
 *
 * <code><pre>
 * h.potter=password,user,captain
 * </pre></code>
 *
 * <p>The authenticator can also be configured in the resin-web.xml:
 *
 * <code><pre>
 * &lt;sec:PropertiesAuthenticator password-digest="none">
 *     Harry Potter=quidditch,user,captain
 * &lt;/sec:PropertiesAuthenticator>
 * </pre></code>
 */

@Service
@SuppressWarnings("serial")
public class PropertiesAuthenticator extends AbstractAuthenticator {
  private static final Logger log =
    Logger.getLogger(PropertiesAuthenticator.class.getName());
  
  private Path _path;
  private Hashtable<String,PasswordUser> _userMap
    = new Hashtable<String,PasswordUser>();

  private Depend _depend;
  private long _lastCheck;

  /**
   * Sets the path to the property file.
   */
  public void setPath(Path path)
  {
    _path = path;
  }

  /**
   * Gets the path to the property file.
   */
  public Path getPath()
  {
    return _path;
  }

  /**
   * Sets the properties value
   *
   * <pre>
   * &lt;init value='Harry Potter=quidditch,user,webdav'/>
   * </pre>
   */
  public void setValue(Properties value)
  {
    for (Map.Entry entry : value.entrySet()) {
      String name = (String) entry.getKey();
      String userValue = (String) entry.getValue();

      _userMap.put(name, createUser(name, userValue));
    }
  }
  
  public void addUser(String name, String password)
  {
    _userMap.put(name, createUser(name, password));
  }

  /**
   * Initialize the properties authenticator.
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
    if  (userName == null)
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
  protected void reload()
  {
    if (_path == null)
      return;
    
    synchronized (this) {
      try {
        _lastCheck = Alarm.getCurrentTime();
        _depend = new Depend(_path);

        if (log.isLoggable(Level.FINE))
          log.fine(this + " loading users from " + _path);
      
        _userMap = new Hashtable<String,PasswordUser>();

        Properties props = new Properties();
        InputStream is = _path.openRead();
        try {
          props.load(is);
        } finally {
          is.close();
        }

        setValue(props);
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }
  }

  /**
   * Creates the password user based on a name and a comma-separated value
   */
  protected PasswordUser createUser(String name, String value)
  {
    String []values = value.trim().split("[,]");

    Principal principal = new BasicPrincipal(name);

    if (values.length < 1) {
      return new PasswordUser(principal, new char[0],
                              true, false,
                              new String[0]);
    }

    String password = values[0].trim();
    boolean isDisabled = false;
    boolean isAnonymous = false;
    ArrayList<String> roles = new ArrayList<String>();
      
    for (int i = 1; i < values.length; i++) {
      String item = values[i].trim();

      if (item.equals("disabled"))
        isDisabled = true;
      else if (! item.equals(""))
        roles.add(item);
    }

    if (roles.size() == 0)
      roles.add("user");

    String []roleArray = new String[roles.size()];
    roles.toArray(roleArray);

    return new PasswordUser(principal, password.toCharArray(),
                            isDisabled, isAnonymous,
                            roleArray);
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

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");

    boolean hasValue = false;
    if (getPath() != null) {
      hasValue = true;
      sb.append(getPath());
    }

    if (getPasswordDigest() != null) {
      if (! hasValue)
        sb.append(",");
      
      sb.append(getPasswordDigest());
    }

    sb.append("]");

    return sb.toString();
  }
}
