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

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.server.dispatch.*;
import com.caucho.server.webapp.*;
import com.caucho.server.rewrite.SetHeaderFilterChain;
import com.caucho.util.L10N;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Filter container which matches URLs and conditions and contains child
 * actions.
 *
 * <pre>
 * &lt;web-app xmlns="http://caucho.com/ns/resin"
 *        xmlns:resin="urn:java:com.caucho.resin">
 *
 * &lt;resin:Location regexp="^/admin">
 *  &lt;resin:IfSecure/>
 *  &lt;resin:SetHeader name="Foo" value="bar"/>
 * &lt;/resin:Location>
 *
 * &lt;/web-app>
 * </pre>
 */
@Configurable
public class Location extends AbstractDispatchRule
{
  private ArrayList<DispatchRule> _ruleList
    = new ArrayList<DispatchRule>();

  private DispatchRule []_rules = new DispatchRule[0];

  /**
   * Adds a child dispatch rule
   */
  public void add(DispatchRule rule)
  {
    _ruleList.add(rule);
    _rules = new DispatchRule[_ruleList.size()];
    _ruleList.toArray(_rules);
  }

  @Override
  public FilterChain map(String uri,
                         String queryString,
                         FilterChain next,
                         FilterChain tail)
    throws ServletException
  {
    return super.map(uri, queryString, next, next);
  }

  @Override
  protected FilterChain createDispatch(String uri,
                                       String queryString,
                                       String target,
                                       FilterChain next)
  {
    return mapChain(0, uri, queryString, next);
  }

  private FilterChain mapChain(int index,
                               String uri, String queryString,
                               FilterChain chain)
  {
    try {
      if (_rules.length <= index)
        return chain;

      DispatchRule rule = _rules[index];
    
      uri = rule.rewriteUri(uri, queryString);

      FilterChain next = mapChain(index + 1, uri, queryString, chain);

      return rule.map(uri, queryString, next, chain);
    } catch (ServletException e) {
      throw ConfigException.create(e);
    }
  }
}

