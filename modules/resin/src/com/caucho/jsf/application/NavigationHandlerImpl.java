/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.jsf.application;

import javax.faces.application.*;
import javax.faces.context.*;

import java.util.*;
import java.util.logging.*;

import com.caucho.jsf.cfg.*;

public class NavigationHandlerImpl extends NavigationHandler
{
  private static final Logger log
    = Logger.getLogger(NavigationHandlerImpl.class.getName());
  
  private ArrayList<NavigationRule> _ruleList
    = new ArrayList<NavigationRule>();
  
  private HashMap<String,NavigationRule[]> _ruleMap
    = new HashMap<String,NavigationRule[]>();

  public void addRule(NavigationRule rule)
  {
    _ruleList.add(rule);
  }
  
  public void handleNavigation(FacesContext context,
                               String fromAction,
                               String outcome)
  {
    if (outcome == null) {
      if (log.isLoggable(Level.FINE))
        log.fine("Jsf[" + context.getViewRoot().getViewId() + "] action " + fromAction + " has no outcome");
      
      return;
    }
    
    NavigationRule []ruleList;

    synchronized (_ruleMap) {
      String viewId = context.getViewRoot().getViewId();
      
      ruleList = _ruleMap.get(viewId);

      if (ruleList== null) {
        ruleList = findRuleList(viewId);
      }

      _ruleMap.put(viewId, ruleList);
    }

    for (int i = 0; i < ruleList.length; i++) {
      if (ruleList[i].handleNavigation(context, fromAction, outcome))
        return;
    }
    
    if (log.isLoggable(Level.FINE)) {
      log.fine("Jsf[" + context.getViewRoot().getViewId() + "] action:" + fromAction + " outcome:" + outcome + " has no matching navigation rule");
    }
  }

  private NavigationRule []findRuleList(String viewId)
  {
    ArrayList<NavigationRule> ruleList = new ArrayList<NavigationRule>();

    for (int i = 0; i < _ruleList.size(); i++) {
      NavigationRule rule = _ruleList.get(i);

      if (rule.isMatch(viewId))
        ruleList.add(rule);
    }

    NavigationRule []rules = new NavigationRule[ruleList.size()];
    ruleList.toArray(rules);

    Arrays.sort(rules);

    return rules;
  }

  public String toString()
  {
    return "NavigationHandlerImpl[]";
  }
}
