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

import com.caucho.config.program.ContainerProgram;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.*;
import com.caucho.rewrite.DispatchRule;
import com.caucho.rewrite.RewriteFilter;
import com.caucho.server.dispatch.DispatchServer;
import com.caucho.server.cluster.Server;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Configuration for a rewrite-dispatch
 */
public class RewriteDispatch
{
  private static final L10N L = new L10N(RewriteDispatch.class);
  private static final Logger log
    = Logger.getLogger(RewriteDispatch.class.getName());

  private final WebApp _webApp;
  private final Server _server;

  private HashSet<String> _dispatcherTypes = new HashSet<String>();

  private MatchRule _matchRule;

  private ContainerProgram _program = new ContainerProgram();

  private ArrayList<DispatchRule> _ruleList
    = new ArrayList<DispatchRule>();

  private ArrayList<RewriteFilter> _filterList
    = new ArrayList<RewriteFilter>();

  private final boolean _isFiner;
  private final boolean _isFinest;

  public RewriteDispatch(Server server)
  {
    this(server, null);
  }

  public RewriteDispatch(WebApp webApp)
  {
    this(null, webApp);
  }

  private RewriteDispatch(Server server, WebApp webApp)
  {
    _server = server;
    _webApp = webApp;

    _isFiner = log.isLoggable(Level.FINER);
    _isFinest = log.isLoggable(Level.FINEST);
  }

  public WebApp getWebApp()
  {
    return _webApp;
  }

  /**
   * Sets the dispatcher type: REQUEST, INCLUDE, FORWARD
   */
  public void addDispatcherType(String type)
  {
    if ("REQUEST".equals(type)
        || "FORWARD".equals(type)
        || "INCLUDE".equals(type)) {
      _dispatcherTypes.add(type);
    }
    else
      throw new ConfigException(L.l("'{0} is an unknown dispatcher-type.  Valid types are 'REQUEST', 'FORWARD', and 'INCLUDE'",
                                    type));
  }

  /**
   * Returns true for a request dispatcher
   */
  public boolean isRequest()
  {
    return (_dispatcherTypes.contains("REQUEST")
            || _dispatcherTypes.size() == 0);
  }

  /**
   * Returns true for an include dispatcher
   */
  public boolean isInclude()
  {
    return _dispatcherTypes.contains("INCLUDE");
  }

  /**
   * Returns true for a forward dispatcher
   */
  public boolean isForward()
  {
    return _dispatcherTypes.contains("FORWARD");
  }

  /**
   * Returns true for an error dispatcher
   */
  public boolean isError()
  {
    return _dispatcherTypes.contains("ERROR");
  }

  /**
   * Adds to the builder program.
   */
  public void addBuilderProgram(ConfigProgram program)
  {
    _program.addProgram(program);
  }

  public void addRule(DispatchRule rule)
  {
    _ruleList.add(rule);
  }

  public void addAction(RewriteFilter action)
  {
    _filterList.add(action);
  }

  @PostConstruct
  public void init()
  {
    _matchRule = new MatchRule(this);
    _matchRule.setRegexp(Pattern.compile(".*"));

    _program.configure(_matchRule);

    _matchRule.init();
  }

  public FilterChain map(String uri,
                         String queryString,
                         FilterChain chain)
  {
    try {
      if (_isFinest)
        log.finest("rewrite-dispatch check uri '" + uri + "'");

      /*
        if (_matchRule == null || _matchRule.isModified()) {
        if (_matchRule != null)
        _matchRule.destroy();
      
        _matchRule = new MatchRule(this);
        _matchRule.setRegexp(Pattern.compile(".*"));

        _program.configure(_matchRule);

        _matchRule.init();
        }
      */

      if (_matchRule != null) {
        // uri = _matchRule.rewriteUri(uri, queryString);
      
        chain = _matchRule.map(uri, queryString, chain);
      }

      chain = mapChain(0, uri, queryString, chain);

      for (int i = _filterList.size() - 1; i >= 0; i--) {
        chain = _filterList.get(i).map(uri, queryString, chain);
      }

      return chain;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  private FilterChain mapChain(int index,
                               String uri, String queryString,
                               FilterChain chain)
    throws ServletException
  {
    if (_ruleList.size() <= index)
      return chain;

    DispatchRule rule = _ruleList.get(index);
    
    uri = rule.rewriteUri(uri, queryString);

    FilterChain next = mapChain(index + 1, uri, queryString, chain);

    return rule.map(uri, queryString, next, chain);
  }

  public void clearCache()
  {
    if (_webApp != null)
      _webApp.clearCache();
    else if (_server != null)
      _server.clearCache();
  }

  /*
  @PreDestroy
  public void destroy()
  {
    _matchRule.destroy();
  }
  */
}
  
