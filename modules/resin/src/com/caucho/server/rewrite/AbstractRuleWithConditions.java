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

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import com.caucho.vfs.CaseInsensitive;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractRuleWithConditions
  extends AbstractRule
{
  private static final L10N L = new L10N(AbstractRuleWithConditions.class);
  private static final Logger log = Logger.getLogger(AbstractRuleWithConditions.class.getName());

  private final boolean _isFiner;
  private final boolean _isFinest;

  private Pattern _regexp;
  private Pattern _urlRegexp;

  private ArrayList<Condition> _conditionList = new ArrayList<Condition>();

  private Condition []_conditions;


  protected AbstractRuleWithConditions(RewriteDispatch rewriteDispatch)
  {
    super(rewriteDispatch);

    _isFinest = log.isLoggable(Level.FINEST);
    _isFiner = log.isLoggable(Level.FINER);
  }

  /**
   * Sets the regular expression pattern that must match the uri for the
   * rule to match, required.
   */
  public void setRegexp(Pattern regexp)
  {
    _regexp = regexp;
  }

  public Pattern getRegexp()
  {
    return _regexp;
  }

  /**
   * Sets the regular expression pattern that must match the uri for the
   * rule to match, required.
   */
  public void setFullUrlRegexp(Pattern regexp)
  {
    _urlRegexp = regexp;
  }

  public Pattern getFullUrlRegexp()
  {
    return _urlRegexp;
  }

  /**
   * Add a list of conditions that must pass for the rule to match.
   */
  public void addAnd(AndConditions condition)
  {
    _conditionList.add(condition);
  }

  /**
   * Add a list of conditions one of which must pass for the rule to match.
   */
  public void addOr(OrConditions condition)
  {
    _conditionList.add(condition);
  }

  /**
   * Add a list of conditions that must not pass for the rule to match.
   */
  public void addNot(NotConditions condition)
  {
    _conditionList.add(condition);
  }

  /**
   * Add a condition that must pass for the rule to match.
   */
  public void addWhen(ConditionConfig condition)
  {
    _conditionList.add(condition.getCondition());
  }

  /**
   * Add a condition that must not pass for the rule to match.
   */
  public void addUnless(ConditionConfig condition)
  {
    NotConditions not = new NotConditions();
    not.add(condition.getCondition());
    Config.init(not);

    _conditionList.add(not);
  }

  /**
   * Throws an exception if the passed value is null.
   */
  protected void required(Object value, String name)
    throws ConfigException
  {
    if (value == null)
      throw new ConfigException(L.l("{0} requires '{1}' attribute.",
                                    getTagName(), name));
  }

  @Override
  public void init()
    throws ConfigException
  {
    if (_regexp != null)
      setLogPrefix(getLogPrefix() + " " + _regexp.pattern());

    if (_regexp != null && CaseInsensitive.isCaseInsensitive())
      _regexp = Pattern.compile(_regexp.pattern(), Pattern.CASE_INSENSITIVE);

    if (_conditionList.size() > 0) {
      _conditions = new Condition[_conditionList.size()];
      _conditionList.toArray(_conditions);
    }

    _conditionList = null;

    super.init();
  }

  public FilterChain map(String uri,
                         String queryString,
                         FilterChain accept)
    throws ServletException
  {

    if (!isEnabled()) {
      if (_isFinest)
        log.finest(getLogPrefix() + " not enabled, no match");

      return getFailFilterChainMapper().map(uri, queryString, accept);
    }

    Matcher matcher;

    if (_regexp != null) {
      matcher = _regexp.matcher(uri);

      if (!matcher.find()) {
        if (_isFinest)
          log.finest(getLogPrefix() + " does not match " + uri);

        return getFailFilterChainMapper().map(uri, queryString, accept);
      }
    }
    else if (_urlRegexp != null) {
      String fullUrl;
      
      if (queryString != null)
        fullUrl = uri + '?' + queryString;
      else
        fullUrl = uri;
      
      matcher = _urlRegexp.matcher(fullUrl);

      if (! matcher.find()) {
        return getFailFilterChainMapper().map(uri, queryString, accept);
      }
    }
    else
      matcher = null;

    String targetUri = rewrite(uri, matcher);

    FilterChain ruleChain = dispatch(targetUri, queryString,
                                     accept, getPassFilterChainMapper());

    Condition []conditions = _conditions;
    
    if (conditions == null) {
      if (_isFiner)
        log.finer(getLogPrefix() + " '" + uri + "' --> '" + targetUri + "'");

      if (ruleChain == null)
        return getPassFilterChainMapper().map(uri, queryString, accept);
      else
        return ruleChain;
    }
    else {
      FilterChain passChain = ruleChain;

      if (passChain == null) {
        passChain = new ContinueMapFilterChain(targetUri,
                                               queryString,
                                               accept,
                                               getPassFilterChainMapper());
      }

      FilterChain failChain
        = new ContinueMapFilterChain(uri,
                                     queryString,
                                     accept,
                                     getFailFilterChainMapper());

      return new ConditionFilterChain(getLogPrefix(),
                                      uri,
                                      targetUri,
                                      conditions,
                                      passChain,
                                      failChain);

    }
  }

  /**
   * Return a rewritten uri to use for the rest of the processing of
   * rewrite-dispatch.
   *
   * @param matcher a Matcher obtained from doing a regexp comparison, or null
   * if there was no regexp comparison
   */
  public String rewrite(String uri, Matcher matcher)
  {
    return uri;
  }

  /**
   * Returns the FilterChain to invoke if the rule is successful, null indicates
   * that the rule does not invoke a FilterChain.
   *
   * @param targetUri the target uri, possibly rewritten
   * @param accept a FilterChain that stops evaluation of rewrite rules and
   * @param next
   */
  abstract public FilterChain dispatch(String targetUri,
                                       String queryString,
                                       FilterChain accept,
                                       FilterChainMapper next)
    throws ServletException;

  @Override
  public void destroy()
  {
    Condition[] conditions = _conditions;
    _conditions = null;

    if (conditions != null) {

      for (Condition condition : conditions) {
        // XXX: s/b Config.destroy()
        try {
          condition.destroy();
        }
        catch (Exception ex) {
          log.log(Level.FINER, ex.toString(), ex);
        }
      }
    }

    super.destroy();
  }

}
