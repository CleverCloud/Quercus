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
import com.caucho.config.program.*;
import com.caucho.config.types.*;
import com.caucho.server.cluster.*;
import com.caucho.server.dispatch.*;
import com.caucho.server.webapp.*;

import java.util.*;

/**
 * Embeddable version of a servlet-mapping
 *
 * <code><pre>
 * ResinEmbed resin = new ResinEmbed();
 *
 * WebAppEmbed webApp = new WebAppEmbed("/foo", "/var/www/foo");
 *
 * ServletMappingEmbed myServlet
 *   = new ServletMappingEmbed("my-servlet", "/my-servlet", "qa.MyServlet");
 *
 * webApp.addServletMapping(myServlet);
 *
 * resin.addWebApp(webApp);
 * </pre></code>
 */
public class ServletMappingEmbed
{
  private String _urlPattern;
  private String _servletName;
  private String _servletClass;
  private int _loadOnStartup = -1;

  private HashMap<String,String> _initParamMap = new HashMap<String,String>();
  private ContainerProgram _init = new ContainerProgram();

  private ServletProtocolEmbed _protocol;

  /**
   * Creates a new embedded servlet-mapping
   */
  public ServletMappingEmbed()
  {
  }

  /**
   * Creates a new embedded servlet-mapping
   *
   * @param servletName the servlet-name
   */
  public ServletMappingEmbed(String servletName)
  {
    setServletName(servletName);
  }

  /**
   * Creates a new embedded servlet-mapping
   *
   * @param servletName the servlet-name
   * @param urlPattern the url-pattern
   */
  public ServletMappingEmbed(String servletName, String urlPattern)
  {
    setServletName(servletName);
    setUrlPattern(urlPattern);
  }

  /**
   * Creates a new embedded servlet-mapping
   *
   * @param urlPattern the url-pattern
   * @param servletName the servlet-name
   * @param servletClass the servlet-class
   */
  public ServletMappingEmbed(String servletName,
                             String urlPattern,
                             String servletClass)
  {
    setUrlPattern(urlPattern);
    setServletName(servletName);
    setServletClass(servletClass);
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
   * The url-pattern
   */
  public void setUrlPattern(String urlPattern)
  {
    _urlPattern = urlPattern;
  }

  /**
   * The url-pattern
   */
  public String getUrlPattern()
  {
    return _urlPattern;
  }

  /**
   * Sets the load-on-startup parameter.
   */
  public void setLoadOnStartup(int loadOnStartup)
  {
    _loadOnStartup = loadOnStartup;
  }

  /**
   * Sets an init-param value.
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

  protected void configure(ServletMapping servletMapping)
  {
    try {
      if (_urlPattern != null)
        servletMapping.addURLPattern(_urlPattern);
    
      servletMapping.setServletName(_servletName);

      if (_servletClass != null)
        servletMapping.setServletClass(_servletClass);

      for (Map.Entry<String,String> entry : _initParamMap.entrySet()) {
        servletMapping.setInitParam(entry.getKey(), entry.getValue());
      }

      servletMapping.setInit(_init);

      if (_loadOnStartup >= 0)
        servletMapping.setLoadOnStartup(_loadOnStartup);

      if (_protocol != null) {
        servletMapping.setProtocol(_protocol.createProtocol());
      }

      servletMapping.init();
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}
