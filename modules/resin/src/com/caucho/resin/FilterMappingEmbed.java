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
 * Embeddable version of a filter-mapping
 *
 * <code><pre>
 * ResinEmbed resin = new ResinEmbed();
 *
 * WebAppEmbed webApp = new WebAppEmbed("/foo", "/var/www/foo");
 *
 * FilterMappingEmbed myFilter
 *   = new FilterMappingEmbed("/my-filter", "*.jsp", "qa.MyFilter");
 *
 * webApp.addFilterMapping(myFilter);
 *
 * resin.addWebApp(webApp);
 * </pre></code>
 */
public class FilterMappingEmbed
{
  private String _urlPattern;
  private String _servletName;
  
  private String _filterName;
  private String _filterClass;

  private HashMap<String,String> _initParamMap = new HashMap<String,String>();
  private ContainerProgram _init = new ContainerProgram();

  /**
   * Creates a new embedded filter-mapping
   */
  public FilterMappingEmbed()
  {
  }

  /**
   * Creates a new embedded filter-mapping
   *
   * @param filterName the filter-name
   */
  public FilterMappingEmbed(String filterName)
  {
    setFilterName(filterName);
  }

  /**
   * Creates a new embedded filter-mapping
   *
   * @param filterName the filter-name
   * @param urlPattern the url-pattern
   */
  public FilterMappingEmbed(String filterName,
                            String urlPattern)
  {
    setFilterName(filterName);
    setUrlPattern(urlPattern);
  }

  /**
   * Creates a new embedded filter-mapping
   *
   * @param filterName the filter-name
   * @param urlPattern the url-pattern
   * @param filterClass the filter-class
   */
  public FilterMappingEmbed(String filterName,
                            String urlPattern,
                            String filterClass)
  {
    setFilterName(filterName);
    setUrlPattern(urlPattern);
    setFilterClass(filterClass);
  }

  /**
   * The filter-name
   */
  public void setFilterName(String filterName)
  {
    _filterName = filterName;
  }

  /**
   * The filter-name
   */
  public String getFilterName()
  {
    return _filterName;
  }

  /**
   * The filter-class
   */
  public void setFilterClass(String filterClass)
  {
    _filterClass = filterClass;
  }

  /**
   * The filter-class
   */
  public String getFilterClass()
  {
    return _filterClass;
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
   * The servlet-name
   */
  public void setServletName(String servletName)
  {
    _servletName = servletName;
  }

  /**
   * Sets an init-param.
   */
  public void setInitParam(String name, String value)
  {
    _initParamMap.put(name, value);
  }

  /**
   * Adds a property.
   */
  public void addProperty(String name, Object value)
  {
    _init.addProgram(new PropertyValueProgram(name, value));
  }

  protected void configure(FilterMapping filterMapping)
  {
    try {
      if (_urlPattern != null)
        filterMapping.createUrlPattern().addText(_urlPattern).init();

      if (_servletName != null)
        filterMapping.addServletName(_servletName);
    
      filterMapping.setFilterName(_filterName);

      if (_filterClass != null)
        filterMapping.setFilterClass(_filterClass);

      for (Map.Entry<String,String> entry : _initParamMap.entrySet()) {
        filterMapping.setInitParam(entry.getKey(), entry.getValue());
      }

      filterMapping.setInit(_init);

      // filterMapping.init();
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}
