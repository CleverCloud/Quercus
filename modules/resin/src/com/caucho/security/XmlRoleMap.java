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

import java.security.Principal;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.util.Alarm;
import com.caucho.vfs.Depend;
import com.caucho.vfs.Path;

/**
 * The XML role-map reads a static file for authentication.
 *
 * <code><pre>
 * &lt;role-map url="xml:path=WEB-INF/role-map.xml"/>
 * </pre></code>
 *
 * <p>The format of the static file is as follows:
 *
 * <code><pre>
 * &lt;role-map>
 *   &lt;role name="admin" user="Harry Potter"/>
 *   ...
 * &lt;/users>
 * </pre></code>
 */
@com.caucho.config.Service
public class XmlRoleMap extends AbstractRoleMap
{
  private static final Logger log =
    Logger.getLogger(XmlRoleMap.class.getName());

  private Path _path;
  
  private Hashtable<String,Role> _roleMap
    = new Hashtable<String,Role>();

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
   * Adds a user from the configuration.
   *
   * <pre>
   * &lt;init user='Harry Potter:quidditch:user,webdav'/>
   * </pre>
   */
  public void addRole(Role role)
  {
    _roleMap.put(role.getName(), role);
  }

  /**
   * Initialize the XML authenticator.
   */
  @PostConstruct
  public synchronized void init()
  {
    super.init();

    reload();
  }
  
  /**
   * Returns the PasswordUser
   */
  public Boolean isUserInRole(String roleName, Principal user)
  {
    Role role = _roleMap.get(roleName);

    if (role == null)
      return null;
    
    String name = user.getName();

    if (role.containsUser(name))
      return Boolean.TRUE;

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
      Alarm.getCurrentTime();
      new Depend(_path);

      if (log.isLoggable(Level.FINE))
        log.fine(this + " loading users from " + _path);
      
      _roleMap = new Hashtable<String,Role>();
      
      new Config().configureBean(this, _path);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public static class Role {
    private String _name;
    private HashSet<String> _userSet = new HashSet<String>();
    private HashSet<String> _groupSet = new HashSet<String>();

    public Role()
    {
    }
    
    public void setName(String name)
    {
      _name = name;
    }

    public String getName()
    {
      return _name;
    }
    
    public void addUser(String user)
    {
      _userSet.add(user);
    }

    public boolean containsUser(String user)
    {
      return _userSet.contains(user);
    }
    
    public void addGroup(String group)
    {
      _groupSet.add(group);
    }
  }
}
