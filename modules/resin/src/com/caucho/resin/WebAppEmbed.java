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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.caucho.config.ConfigException;
import com.caucho.config.types.InitParam;
import com.caucho.server.dispatch.FilterConfigImpl;
import com.caucho.server.dispatch.FilterMapping;
import com.caucho.server.dispatch.ServletConfigImpl;
import com.caucho.server.dispatch.ServletMapping;
import com.caucho.server.webapp.WebApp;

/**
 * Embeddable version of a Resin web-app.
 *
 * <code><pre>
 * ResinEmbed resin = new ResinEmbed();
 *
 * WebAppEmbed webApp = new WebAppEmbed("/foo", "/var/www/foo");
 *
 * resin.addWebApp(webApp);
 * </pre></code>
 */
public class WebAppEmbed
{
  private String _contextPath = "/";
  private String _rootDirectory = ".";
  private String _archivePath;

  private HashMap<String,String> _contextParamMap
    = new HashMap<String,String>();
  
  private final ArrayList<BeanEmbed> _beanList
    = new ArrayList<BeanEmbed>();

  private final ArrayList<ServletEmbed> _servletList
    = new ArrayList<ServletEmbed>();

  private final ArrayList<ServletMappingEmbed> _servletMappingList
    = new ArrayList<ServletMappingEmbed>();

  private final ArrayList<FilterEmbed> _filterList
    = new ArrayList<FilterEmbed>();

  private final ArrayList<FilterMappingEmbed> _filterMappingList
    = new ArrayList<FilterMappingEmbed>();

  /**
   * Creates a new embedded webapp
   */
  public WebAppEmbed()
  {
  }

  /**
   * Creates a new embedded webapp
   *
   * @param contextPath the URL prefix of the web-app
   */
  public WebAppEmbed(String contextPath)
  {
    setContextPath(contextPath);
  }

  /**
   * Creates a new embedded webapp
   *
   * @param contextPath the URL prefix of the web-app
   * @param rootDirectory the root directory of the web-app
   */
  public WebAppEmbed(String contextPath, String rootDirectory)
  {
    setContextPath(contextPath);
    setRootDirectory(rootDirectory);
  }

  /**
   * The context-path
   */
  public void setContextPath(String contextPath)
  {
    _contextPath = contextPath;
  }

  /**
   * The context-path
   */
  public String getContextPath()
  {
    return _contextPath;
  }

  /**
   * The root directory of the expanded web-app
   */
  public void setRootDirectory(String rootDirectory)
  {
    _rootDirectory = rootDirectory;
  }

  /**
   * The root directory of the expanded web-app
   */
  public String getRootDirectory()
  {
    return _rootDirectory;
  }

  /**
   * The path to the archive war file
   */
  public void setArchivePath(String archivePath)
  {
    _archivePath = archivePath;
  }

  /**
   * The path to the archive war file
   */
  public String getArchivePath()
  {
    return _archivePath;
  }

  /**
   * Adds a servlet definition
   */
  public void addServlet(ServletEmbed servlet)
  {
    if (servlet == null)
      throw new NullPointerException();
    
    _servletList.add(servlet);
  }

  /**
   * Adds a servlet-mapping definition
   */
  public void addServletMapping(ServletMappingEmbed servletMapping)
  {
    if (servletMapping == null)
      throw new NullPointerException();
    
    _servletMappingList.add(servletMapping);
  }

  /**
   * Adds a filter definition
   */
  public void addFilter(FilterEmbed filter)
  {
    if (filter == null)
      throw new NullPointerException();
    
    _filterList.add(filter);
  }

  /**
   * Adds a filter-mapping definition
   */
  public void addFilterMapping(FilterMappingEmbed filterMapping)
  {
    if (filterMapping == null)
      throw new NullPointerException();
    
    _filterMappingList.add(filterMapping);
  }

  /**
   * Adds a web bean.
   */
  public void addBean(BeanEmbed bean)
  {
    _beanList.add(bean);
  }

  /**
   * Sets a context-param.
   */
  public void setContextParam(String name, String value)
  {
    _contextParamMap.put(name, value);
  }

  /**
   * Configures the web-app (for internal use)
   */
  protected void configure(WebApp webApp)
  {
    try {
      for (Map.Entry<String,String> entry : _contextParamMap.entrySet()) {
        InitParam initParam = new InitParam(entry.getKey(), entry.getValue());
        webApp.addContextParam(initParam);
      }

      for (BeanEmbed beanEmbed : _beanList) {
        beanEmbed.configure();
      }

      for (ServletEmbed servletEmbed : _servletList) {
        ServletConfigImpl servlet = webApp.createServlet();

        servletEmbed.configure(servlet);

        webApp.addServlet(servlet);
      }
    
      for (ServletMappingEmbed servletMappingEmbed : _servletMappingList) {
        ServletMapping servletMapping = webApp.createServletMapping();

        servletMappingEmbed.configure(servletMapping);

        webApp.addServletMapping(servletMapping);
      }

      for (FilterEmbed filterEmbed : _filterList) {
        FilterConfigImpl filter = new FilterConfigImpl();

        filterEmbed.configure(filter);

        webApp.addFilter(filter);
      }
    
      for (FilterMappingEmbed filterMappingEmbed : _filterMappingList) {
        FilterMapping filterMapping = new FilterMapping();

        filterMappingEmbed.configure(filterMapping);

        webApp.addFilterMapping(filterMapping);
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}
