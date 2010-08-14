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

package com.caucho.resin;

import com.caucho.config.*;
import com.caucho.config.types.*;
import com.caucho.config.program.*;
import com.caucho.server.cluster.*;
import com.caucho.server.dispatch.*;
import com.caucho.util.*;

import java.util.*;

/**
 * Embeddable version of a servlet
 *
 * <code><pre>
 * ResinEmbed resin = new ResinEmbed();
 *
 * WebAppEmbed webApp = new WebAppEmbed("/foo", "/var/www/foo");
 *
 * ServletEmbed myServlet = new ServletEmbed("my-servlet", "qa.MyServlet");
 * webApp.addServlet(myServlet);
 *
 * resin.addWebApp(webApp);
 * </pre></code>
 */
public class ServletEmbed
{
  private static final L10N L = new L10N(ServletEmbed.class);
  
  private String _servletName;
  private String _servletClass;
  private int _loadOnStartup = -1;
  
  private HashMap<String,String> _initParamMap = new HashMap<String,String>();
  private ContainerProgram _init = new ContainerProgram();

  private ServletProtocolEmbed _protocol;

  /**
   * Creates a new embedded servlet
   */
  public ServletEmbed()
  {
  }

  /**
   * Creates a new embedded servlet
   *
   * @param servletClass the servlet-class
   */
  public ServletEmbed(String servletClass)
  {
    setServletClass(servletClass);
  }

  /**
   * Creates a new embedded servlet
   *
   * @param servletClass the servlet-class
   * @param servletName the servlet-name
   */
  public ServletEmbed(String servletClass, String servletName)
  {
    setServletClass(servletClass);
    setServletName(servletName);
  }

  /**
   * The servlet-name
   */
  public void setServletName(String servletName)
  {
    _servletName = servletName;
  }

  /**
   * The servlet-name
   */
  public String getServletName()
  {
    return _servletName;
  }

  /**
   * The servlet-class
   */
  public void setServletClass(String servletClass)
  {
    _servletClass = servletClass;
  }

  /**
   * The servlet-class
   */
  public String getServletClass()
  {
    return _servletClass;
  }

  /**
   * Sets the load-on-startup parameter.
   */
  public void setLoadOnStartup(int loadOnStartup)
  {
    _loadOnStartup = loadOnStartup;
  }

  /**
   * Sets an init-param.
   */
  public void setInitParam(String name, String value)
  {
    _initParamMap.put(name, value);
  }

  /**
   * Adds an init/ioc property.
   */
  public void addProperty(String name, Object value)
  {
    _init.addProgram(new PropertyValueProgram(name, value));
  }

  /**
   * Sets the remoting protocol
   */
  public void setProtocol(ServletProtocolEmbed protocol)
  {
    _protocol = protocol;
  }

  protected void configure(ServletConfigImpl servletConfig)
  {
    try {
      if (_servletClass == null)
        throw new ConfigException(L.l("servlet-class is required for ServletEmbed."));
      
      servletConfig.setServletClass(_servletClass);

      if (_servletName != null)
        servletConfig.setServletName(_servletName);
      else
        servletConfig.setServletName(_servletClass);

      for (Map.Entry<String,String> entry : _initParamMap.entrySet()) {
        servletConfig.setInitParam(entry.getKey(), entry.getValue());
      }

      servletConfig.setInit(_init);

      if (_loadOnStartup >= 0)
        servletConfig.setLoadOnStartup(_loadOnStartup);

      if (_protocol != null) {
        servletConfig.setProtocol(_protocol.createProtocol());
      }

      servletConfig.init();
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}
