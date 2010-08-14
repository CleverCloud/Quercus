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

package com.caucho.server.dispatch;

import com.caucho.servlet.comet.CometFilter;
import com.caucho.servlet.comet.CometFilterChain;
import com.caucho.util.L10N;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Manages dispatching: servlets and filters.
 */
public class FilterMapper {
  private static final Logger log
    = Logger.getLogger(FilterMapper.class.getName());
  private static final L10N L = new L10N(FilterMapper.class);

  private ServletContext _servletContext;

  private int _unique;
  private FilterManager _filterManager;

  private ArrayList<FilterMapping> _filterMap
  = new ArrayList<FilterMapping>();

  private ArrayList<FilterChainBuilder> _topFilters
  = new ArrayList<FilterChainBuilder>();

  private ArrayList<FilterChainBuilder> _bottomFilters
  = new ArrayList<FilterChainBuilder>();

  /**
   * Sets the servlet context.
   */
  public void setServletContext(ServletContext servletContext)
  {
    _servletContext = servletContext;
  }

  /**
   * Gets the servlet context.
   */
  public ServletContext getServletContext()
  {
    return _servletContext;
  }

  /**
   * Returns the filter manager.
   */
  public FilterManager getFilterManager()
  {
    return _filterManager;
  }

  /**
   * Sets the filter manager.
   */
  public void setFilterManager(FilterManager manager)
  {
    _filterManager = manager;
  }

  /**
   * Adds a filter mapping
   */
  public void addFilterMapping(FilterMapping mapping)
    throws ServletException
  {
    try {
      String filterName = mapping.getFilterName();

      if (filterName == null)
        filterName = mapping.getFilterClassName();

      if (mapping.getFilterClassName() != null
          && _filterManager.getFilter(filterName) == null) {
        _filterManager.addFilter(mapping);
      }

      if (_filterManager.getFilter(filterName) == null)
        throw new ServletConfigException(L.l("'{0}' is an unknown filter-name.  filter-mapping requires that the named filter be defined in a <filter> configuration before the <filter-mapping>.", filterName));

      _filterMap.add(mapping);

      log.fine("filter-mapping " + mapping + " -> " + filterName);
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  /**
   * Adds a top-filter.  Top filters are added to every request
   * in the filter chain.
   */
  public void addTopFilter(FilterChainBuilder filterBuilder)
  {
    _topFilters.add(filterBuilder);
  }

  /**
   * Fills in the invocation.
   */
  public void buildDispatchChain(Invocation invocation,
                                 FilterChain chain)
    throws ServletException
  {
    synchronized (_filterMap) {
      for (int i = _filterMap.size() - 1; i >= 0; i--) {
        FilterMapping map = _filterMap.get(i);

        if (map.isMatch(invocation.getServletName())) {
          String filterName = map.getFilterName();

          Filter filter = _filterManager.createFilter(filterName);
          FilterConfigImpl config = _filterManager.getFilter(filterName);

          if (! config.isAsyncSupported())
            invocation.clearAsyncSupported();

          chain = addFilter(chain, filter);
        }
      }
    }

    synchronized (_filterMap) {
      for (int i = _filterMap.size() - 1; i >= 0; i--) {
        FilterMapping map = _filterMap.get(i);

        if (map.isMatch(invocation)) {
          String filterName = map.getFilterName();

          Filter filter = _filterManager.createFilter(filterName);
          FilterConfigImpl config = _filterManager.getFilter(filterName);

          if (! config.isAsyncSupported())
            invocation.clearAsyncSupported();

          chain = addFilter(chain, filter);
        }
      }
    }

    for (int i = 0; i < _topFilters.size(); i++) {
      FilterChainBuilder filterBuilder;
      filterBuilder = _topFilters.get(i);

      chain = filterBuilder.build(chain, invocation);
    }

    invocation.setFilterChain(chain);
  }

  /**
   * Fills in the invocation.
   */
  public FilterChain buildFilterChain(FilterChain chain,
                                      String servletName)
    throws ServletException
  {
    synchronized (_filterMap) {
      for (int i = _filterMap.size() - 1; i >= 0; i--) {
        FilterMapping map = _filterMap.get(i);

        if (map.isMatch(servletName)) {
          String filterName = map.getFilterName();

          Filter filter = _filterManager.createFilter(filterName);

          chain = addFilter(chain, filter);
        }
      }
    }

    return chain;
  }

  private FilterChain addFilter(FilterChain chain,
                                Filter filter)
  {
    return new FilterFilterChain(chain, filter);
  }
}
