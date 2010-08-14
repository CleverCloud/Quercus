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
 * @author Sam
 */

package com.caucho.rewrite;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.IfMBeanEnabledMXBean;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

/**
 * Matches if the MBean is enabled and does not match if it's not enabled.
 * 
 * The MBean is registered as resin:name=&lt;name>,type=IfMBean
 *
 * <pre>
 * &lt;resin:Allow url-pattern="/admin/*"&gt;
 *                  xmlns:resin="urn:java:com.caucho.resin"&gt;
 *   &lt;resin:IfMBeanEnabled name="admin"/>
 * &lt;/resin:Allow>
 * </pre>
 *
 * <p>RequestPredicates may be used for security and rewrite actions.
 */
@Configurable
public class IfMBeanEnabled implements RequestPredicate
{
  private static final L10N L = new L10N(IfMBeanEnabled.class);

  private String _name;
  private boolean _isEnabledDefault = true;
  
  private Boolean _isEnabled;
  
  private Admin _admin;

  /**
   * Sets the MBean name.
   */
  @Configurable
  public void setName(String name)
  {
    _name = name;
  }
  
  /**
   * Sets true if the default is to enable.
   */
  @Configurable
  public void setEnabled(boolean isEnabled)
  {
    _isEnabledDefault = isEnabled;
  }

  /**
   * True if the predicate matches.
   *
   * @param request the servlet request to test
   */
  @Override
  public boolean isMatch(HttpServletRequest request)
  {
    if (_isEnabled != null)
      return _isEnabled;
    
    return _isEnabledDefault;
  }
  
  @PostConstruct
  private void init()
  {
    if (_name == null)
      throw new ConfigException(L.l("resin:IfMBeanEnabled requires a 'name' attribute."));
    
    _admin = new Admin();
    _admin.register();
  }
  
  class Admin extends AbstractManagedObject implements IfMBeanEnabledMXBean {
    protected void register()
    {
      registerSelf();
    }
    
    @Override
    public String getName()
    {
      return _name;
    }

    @Override
    public void disable()
    {
      _isEnabled = false;
    }

    @Override
    public void enable()
    {
      _isEnabled = true;
    }

    @Override
    public String getState()
    {
      if (_isEnabled != null)
        return String.valueOf(_isEnabled);
      else
        return "default";
    }

    @Override
    public void reset()
    {
      _isEnabled = null;
    }
  }
}
