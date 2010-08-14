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

package com.caucho.server.rewrite;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.caucho.config.ConfigException;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.server.dispatch.ServletConfigImpl;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;

public class ProxyRule
  extends AbstractRuleWithConditions
{
  private static final L10N L = new L10N(ProxyRule.class);
  private static final Logger log = Logger.getLogger(ProxyRule.class.getName());

  private final WebApp _webApp;

  private ServletConfigImpl _servlet;
  private String _target;

  private ContainerProgram _program = new ContainerProgram();

  ProxyRule(RewriteDispatch rewriteDispatch, WebApp webApp)
  {
    super(rewriteDispatch);

    _webApp = webApp;
  }

  public String getTagName()
  {
    return "proxy";
  }

  public void setTarget(String target)
  {
    _target = target;
  }

  public void addBuilderProgram(ConfigProgram program)
  {
    _program.addProgram(program);
  }

  @Override
  public FilterChain dispatch(String uri,
                              String queryString,
                              FilterChain accept,
                              FilterChainMapper next)
    throws ServletException
  {
    if (_target != null)
      return new ProxyFilterChain(_servlet.createServletChain(),
                                  _target, queryString);
    else
      return new ProxyFilterChain(_servlet.createServletChain(),
                                  uri, queryString);
  }

  @Override
  public void init()
    throws ConfigException
  {
    super.init();

    try {
      _servlet = new ServletConfigImpl();

      _servlet.setServletName("resin-dispatch-lb");
      Class cl = Class.forName("com.caucho.servlets.HttpProxyServlet");
      _servlet.setServletClass("com.caucho.servlets.HttpProxyServlet");

      _servlet.setInit(_program);

      _webApp.addServlet(_servlet);
    }
    catch (ServletException ex) {
      throw ConfigException.create(ex);
    }
    catch (ClassNotFoundException e) {
      log.log(Level.FINER, e.toString(), e);

      throw new ConfigException(L.l("load-balance requires Resin Professional"));
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
    public String getRequestURI()
    {
      return _uri;
    }

    /**
     * Returns the proxy query string
     */
    public String getQueryString()
    {
      return _queryString;
    }

    @Override
    public Object getAttribute(String name)
    {
      return null;
    }

    @Override
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _uri + "?" + _queryString + "]";
    }
  }
}
