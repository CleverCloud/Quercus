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

package com.caucho.rewrite;

import com.caucho.server.dispatch.FilterConfigImpl;
import com.caucho.server.dispatch.FilterFilterChain;
import com.caucho.server.webapp.WebApp;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;

/**
 * Wraps a Java filter in a RewriteFilter
 */
public class RewriteFilterAdapter implements RewriteFilter
{
  private Filter _filter;

  public RewriteFilterAdapter(Filter filter)
    throws ServletException
  {
    WebApp webApp = WebApp.getCurrent();
    FilterConfigImpl filterConfig = new FilterConfigImpl();
    filterConfig.setServletContext(webApp);

    filter.init(filterConfig);

    _filter = filter;
  }
  
  public boolean isRequest()
  {
    return true;
  }
  
  public boolean isInclude()
  {
    return false;
  }
  
  public boolean isForward()
  {
    return false;
  }

  public FilterChain map(String uri,
                         String queryString,
                         FilterChain next)
  {
    return new FilterFilterChain(next, _filter);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _filter + "]";
  }
}
