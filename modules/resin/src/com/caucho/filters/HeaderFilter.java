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

package com.caucho.filters;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * Adds custom headers to a response
 *
 * @since Resin 3.2.1
 */
public class HeaderFilter implements Filter
{
  private ArrayList<Header> _headerList = new ArrayList<Header>();

  public void addHeader(Header header)
  {
    _headerList.add(header);
  }
  
  public void init(FilterConfig config)
    throws ServletException
  {
  }
  
  /**
   * Creates a wrapper to compress the output.
   */
  public void doFilter(ServletRequest request,
                       ServletResponse response,
                       FilterChain nextFilter)
    throws ServletException, IOException
  {
    HttpServletResponse res = (HttpServletResponse) response;
    
    int size = _headerList.size();
    
    for (int i = 0; i < size; i++) {
      Header header = _headerList.get(i);

      res.addHeader(header.getName(), header.getValue());
    }

    nextFilter.doFilter(request, response);
  }
  
  /**
   * Any cleanup for the filter.
   */
  public void destroy()
  {
  }

  public static class Header {
    private String _name;
    private String _value;

    public void setName(String name)
    {
      _name = name;
    }

    public String getName()
    {
      return _name;
    }

    public void setValue(String value)
    {
      _value = value;
    }

    public String getValue()
    {
      return _value;
    }
  }
}
