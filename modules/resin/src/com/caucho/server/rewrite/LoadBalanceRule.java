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

import com.caucho.server.webapp.WebApp;
import com.caucho.server.dispatch.ServletConfigImpl;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoadBalanceRule
  extends AbstractRuleWithConditions
{
  private static final L10N L = new L10N(LoadBalanceRule.class);
  private static final Logger log = Logger.getLogger(LoadBalanceRule.class.getName());

  private final WebApp _webApp;

  private ServletConfigImpl _servlet;

  private ContainerProgram _program = new ContainerProgram();

  LoadBalanceRule(RewriteDispatch rewriteDispatch, WebApp webApp)
  {
    super(rewriteDispatch);

    _webApp = webApp;
  }

  public String getTagName()
  {
    return "load-balance";
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
    return _servlet.createServletChain();
  }

  @Override
  public void init()
    throws ConfigException
  {
    super.init();

    try {
      _servlet = new ServletConfigImpl();

      _servlet.setServletName("resin-dispatch-lb");
      Class cl = Class.forName("com.caucho.servlets.LoadBalanceServlet");
      _servlet.setServletClass("com.caucho.servlets.LoadBalanceServlet");

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
}
