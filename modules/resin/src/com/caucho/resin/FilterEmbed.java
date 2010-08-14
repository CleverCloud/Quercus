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

import java.util.*;

/**
 * Embeddable version of a filter
 *
 * <code><pre>
 * ResinEmbed resin = new ResinEmbed();
 *
 * WebAppEmbed webApp = new WebAppEmbed("/foo", "/var/www/foo");
 *
 * FilterEmbed myFilter = new FilterEmbed("my-filter", "qa.MyFilter");
 * webApp.addFilter(myFilter);
 *
 * resin.addWebApp(webApp);
 * </pre></code>
 */
public class FilterEmbed
{
  private String _filterName;
  private String _filterClass;
  
  private HashMap<String,String> _initParamMap = new HashMap<String,String>();
  private ContainerProgram _init = new ContainerProgram();

  /**
   * Creates a new embedded filter
   */
  public FilterEmbed()
  {
  }

  /**
   * Creates a new embedded filter
   *
   * @param filterClass the filter-class
   */
  public FilterEmbed(String filterClass)
  {
    setFilterClass(filterClass);
  }

  /**
   * Creates a new embedded filter
   *
   * @param filterClass the filter-class
   * @param filterName the filter-name
   */
  public FilterEmbed(String filterClass, String filterName)
  {
    setFilterClass(filterClass);
    setFilterName(filterName);
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

  protected void configure(FilterConfigImpl filterConfig)
  {
    try {
      filterConfig.setFilterName(_filterName);
      filterConfig.setFilterClass(_filterClass);

      for (Map.Entry<String,String> entry : _initParamMap.entrySet()) {
        filterConfig.setInitParam(entry.getKey(), entry.getValue());
      }

      filterConfig.setInit(_init);

      // filterConfig.init();
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}
