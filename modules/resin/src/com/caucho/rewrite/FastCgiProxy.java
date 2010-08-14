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

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.types.Period;
import com.caucho.server.dispatch.ServletConfigImpl;
import com.caucho.server.webapp.WebApp;
import com.caucho.servlets.FastCGIServlet;

/**
 * Dispatches a request to a backend server using FastCGI as the proxy
 * protocol.
 *
 * <pre>
 * &lt;web-app xmlns:resin="urn:java:com.caucho.resin">
 *
 *   &lt;resin:FastCgiProxy regexp="^/remote">
 *     &lt;address>127.0.0.1:8080&lt;/address>
 *   &lt;/resin:FastCgiProxy>
 *
 * &lt;/web-app>
 * </pre>
 */
@Configurable
public class FastCgiProxy extends AbstractTargetDispatchRule
{
  private FastCGIServlet _proxyServlet;
  private ServletConfigImpl _servlet;

  public FastCgiProxy()
  {
    _proxyServlet = new FastCGIServlet();

    _servlet = new ServletConfigImpl();

    _servlet.setServletName("resin-dispatch-lb");
    _servlet.setServlet(_proxyServlet);
  }

  /**
   * Adds a backend FastCGI server address like "127.0.0.1:8081"
   *
   * @param address the backend address likst "127.0.0.1:8081"
   */
  @Configurable
  public void addAddress(String address)
  {
    _proxyServlet.addAddress(address);
  }

  /**
   * Sets the timeout to recover from a failed connection to the backend.
   *
   * @param period the recover timeout
   */
  @Configurable
  public void setFailRecoverTime(Period period)
  {
    _proxyServlet.setFailRecoverTime(period);
  }

  @PostConstruct
  public void init()
    throws ConfigException
  {
    try {
      WebApp webApp = WebApp.getCurrent();

      _proxyServlet.init(webApp);

      if (webApp != null)
        webApp.addServlet(_servlet);
    }
    catch (ServletException ex) {
      throw ConfigException.create(ex);
    }
  }

  @Override
  public FilterChain createDispatch(String uri,
                                    String queryString,
                                    String target,
                                    FilterChain next)
  {
    try {
      return new ProxyFilterChain(_servlet.createServletChain(),
                                  uri, queryString);
    } catch (ServletException e) {
      throw ConfigException.create(e);
    }
  }

  public static class ProxyFilterChain implements FilterChain {
    private final FilterChain _next;
    private final String _uri;
    private final String _queryString;

    ProxyFilterChain(FilterChain next, String uri, String queryString)
    {
      _next = next;

      _uri = uri;
      _queryString = queryString;
    }

    @Override
    public void doFilter(ServletRequest req,
                         ServletResponse res)
      throws IOException, ServletException
    {
      _next.doFilter(new ProxyRequest(req, _uri, _queryString), res);
    }
  }

  public static class ProxyRequest extends HttpServletRequestWrapper {
    private String _uri;
    private String _queryString;

    ProxyRequest(ServletRequest req,
                 String uri,
                 String queryString)
    {
      super((HttpServletRequest) req);

      _uri = uri;
      _queryString = queryString;
    }

    /**
     * Returns the proxy uri
     */
    @Override
    public String getRequestURI()
    {
      return _uri;
    }

    /**
     * Returns the proxy query string
     */
    @Override
    public String getQueryString()
    {
      return _queryString;
    }
  }
}
