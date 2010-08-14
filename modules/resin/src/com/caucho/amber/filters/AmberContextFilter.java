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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.filters;

import com.caucho.amber.AmberContext;
import com.caucho.amber.AmberFactory;
import com.caucho.config.types.JndiBuilder;
import com.caucho.util.L10N;

import javax.naming.NamingException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * Context for the amber filter.
 */
public class AmberContextFilter implements Filter {
  private static final L10N L = new L10N(AmberContextFilter.class);

  private AmberFactory _factory;

  /**
   * Sets the factory.
   */
  public void setAmberFactory(JndiBuilder jndiBuilder)
    throws NamingException
  {
    _factory = (AmberFactory) jndiBuilder.getObject();
  }

  /**
   * Initializes the filter.
   */
  public void init(FilterConfig config)
    throws ServletException
  {
    if (_factory == null)
      throw new ServletException(L.l("amber-factory must be set"));
  }

  /**
   * Handles the filter request.
   */
  public void doFilter(ServletRequest request, ServletResponse response,
                       FilterChain next)
    throws ServletException, IOException
  {
    AmberContext.create(_factory);
    
    try {
      next.doFilter(request, response);
    } finally {
      AmberContext.close();
    }
  }

  /**
   * Destroys the filter.
   */
  public void destroy()
  {
  }
}
