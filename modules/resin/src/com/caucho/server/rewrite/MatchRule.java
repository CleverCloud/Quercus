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
 * @author Sam
 */

package com.caucho.server.rewrite;

import com.caucho.config.ConfigException;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;
import com.caucho.util.*;
import com.caucho.make.*;
import com.caucho.vfs.*;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.logging.Logger;

public class MatchRule
  extends AbstractRuleWithConditions
  implements AlarmListener
{
  private static final L10N L = new L10N(MatchRule.class);

  private static final Logger log
    = Logger.getLogger(MatchRule.class.getName());

  private DependencyContainer _depend = new DependencyContainer();

  private ArrayList<Rule> _ruleList = new ArrayList<Rule>();

  private FilterChainMapper _lastFilterChainMapper = new LastFilterChainMapper();
  private Rule _firstRule;
  private Rule _lastRule;

  private Alarm _alarm;

  protected MatchRule(RewriteDispatch rewriteDispatch)
  {
    super(rewriteDispatch);

    super.setPassFilterChainMapper(new FirstFilterChainMapper());
  }

  public String getTagName()
  {
    return "match";
  }

  public boolean isModified()
  {
    return _depend.isModified();
  }

  public void addDependency(PersistentDependency depend)
  {
    _depend.add(depend);
  }

  public void setPassFilterChainMapper(FilterChainMapper nextFilterChainMapper)
  {
    // overriden and set in constructor
  }

  private void add(Rule rule)
  {
    if (_firstRule == null)
      _firstRule = rule;

    if (_lastRule != null) {
      _lastRule.setPassFilterChainMapper(rule);
      _lastRule.setFailFilterChainMapper(rule);
    }

    rule.setPassFilterChainMapper(_lastFilterChainMapper);
    rule.setFailFilterChainMapper(_lastFilterChainMapper);

    _lastRule = rule;

    _ruleList.add(rule);

  }

  /**
   * Adds a dispatch.
   */
  public DispatchRule createDispatch()
  {
    return new DispatchRule(getRewriteDispatch());
  }

  public void addDispatch(DispatchRule dispatch)
  {
    add(dispatch);
  }

  /**
   * Adds a forbidden.
   */
  public ErrorRule createForbidden()
  {
    return new ErrorRule(getRewriteDispatch(), HttpServletResponse.SC_FORBIDDEN);
  }

  public void addForbidden(ErrorRule forbidden)
  {
    add(forbidden);
  }

  /**
   * Adds a forward.
   */
  public ForwardRule createForward()
  {
    return new ForwardRule(getRewriteDispatch());
  }

  public void addForward(ForwardRule forward)
  {
    add(forward);
  }

  /**
   * Adds a gone.
   */
  public ErrorRule createGone()
  {
    return new ErrorRule(getRewriteDispatch(), HttpServletResponse.SC_GONE);
  }

  public void addGone(ErrorRule gone)
  {
    add(gone);
  }

  public ImportRule createImport()
  {
    return new ImportRule(getRewriteDispatch());
  }

  public void addImport(ImportRule importRule)
  {
    add(importRule);
  }

  /**
   * Adds a load-balance
   */
  public LoadBalanceRule createLoadBalance()
  {
    WebApp webApp = getRewriteDispatch().getWebApp();

    if (webApp == null)
      throw new ConfigException(L.l("<load-balance> requires a web-app.  Host-based <rewrite-dispatch> can not use <load-balance>."));

    return new LoadBalanceRule(getRewriteDispatch(), webApp);
  }

  public void addLoadBalance(LoadBalanceRule loadBalance)
  {
    add(loadBalance);
  }

  /**
   * Adds a proxy
   */
  public ProxyRule createProxy()
  {
    WebApp webApp = getRewriteDispatch().getWebApp();

    if (webApp == null)
      throw new ConfigException(L.l("<proxy> requires a web-app.  Host-based <rewrite-dispatch> can not use <proxy>."));

    return new ProxyRule(getRewriteDispatch(), webApp);
  }

  public void addProxy(ProxyRule proxy)
  {
    add(proxy);
  }

  public MatchRule createMatch()
  {
    return new MatchRule(getRewriteDispatch());
  }

  public void addMatch(MatchRule match)
  {
    add(match);
  }

  /**
   * Adds a moved permanently (301)
   */
  public MovedRule createMovedPermanently()
  {
    return new MovedRule(getRewriteDispatch(), HttpServletResponse.SC_MOVED_PERMANENTLY);
  }

  public void addMovedPermanently(MovedRule moved)
  {
    add(moved);
  }

  /**
   * Adds a not-found.
   */
  public ErrorRule createNotFound()
  {
    return new ErrorRule(getRewriteDispatch(), HttpServletResponse.SC_NOT_FOUND);
  }

  public void addNotFound(ErrorRule notFound)
  {
    add(notFound);
  }

  /**
   * Adds a redirect.
   */
  public RedirectRule createRedirect()
  {
    return new RedirectRule(getRewriteDispatch());
  }

  public void addRedirect(RedirectRule redirect)
  {
    add(redirect);
  }

  /**
   * Adds a rewrite
   */
  public RewriteRule createRewrite()
  {
    return new RewriteRule(getRewriteDispatch());
  }

  public void addRewrite(RewriteRule rewrite)
  {
    add(rewrite);
  }

  /**
   * Adds a set
   */
  public SetRule createSet()
  {
    return new SetRule(getRewriteDispatch());
  }

  public void addSet(SetRule set)
  {
    add(set);
  }

  @Override
  public void init()
  {
    super.init();

    _ruleList.trimToSize();

    register();

    if (_depend.size() > 0) {
      _alarm = new Alarm(this);
      
      handleAlarm(_alarm);
    }
  }

  public String rewriteUri(String uri, String queryString)
  {
    return uri;
  }

  public FilterChain dispatch(String uri,
                              String queryString,
                              FilterChain accept,
                              FilterChainMapper next)
    throws ServletException
  {
    return null;
  }

  @Override
  synchronized public void register()
  {
    super.register();

    ArrayList<Rule> ruleList = new ArrayList<Rule>();

    if (_ruleList != null)
      ruleList.addAll(_ruleList);

    for (Rule rule : ruleList) {
      rule.register();
    }
  }

  @Override
  synchronized public void unregister()
  {
    ArrayList<Rule> ruleList = new ArrayList<Rule>();

    if (_ruleList != null)
      ruleList.addAll(_ruleList);

    for (Rule rule : ruleList) {
      rule.unregister();
    }

    super.unregister();
  }

  public void handleAlarm(Alarm alarm)
  {
    if (_ruleList == null) {
    }
    else if (_depend.isModified()) {
      getRewriteDispatch().clearCache();
    }
    else {
      long time = _depend.getCheckInterval();
      if (time >= 0 && time < 5000)
        time = 5000;

      if (time > 0) {
        alarm.queue(time);
      }
    }
  }

  @Override
  public void destroy()
  {
    unregister();

    ArrayList<Rule> ruleList = new ArrayList<Rule>();

    if (_ruleList != null)
      ruleList.addAll(_ruleList);

    _ruleList = null;

    for (Rule rule : ruleList) {
      // XXX: s/b  Config.destroy(rule);
      rule.destroy();
    }

    Alarm alarm = _alarm;
    _alarm = null;

    if (alarm != null)
      alarm.dequeue();

    super.destroy();
  }

  private class FirstFilterChainMapper
    implements FilterChainMapper
  {
    public FilterChain map(String uri, String queryString, FilterChain accept)
      throws ServletException
    {
      if (_firstRule != null)
        return _firstRule.map(uri, queryString, accept);
      else
        return _lastFilterChainMapper.map(uri, queryString, accept);
    }
  }

  private class LastFilterChainMapper
    implements FilterChainMapper
  {
    public FilterChain map(String uri, String queryString, FilterChain accept)
      throws ServletException
    {
      FilterChainMapper failFilterChainMapper = getFailFilterChainMapper();

      if (failFilterChainMapper != null)
        return failFilterChainMapper.map(uri, queryString, accept);
      else
        return accept;
    }
  }
}
