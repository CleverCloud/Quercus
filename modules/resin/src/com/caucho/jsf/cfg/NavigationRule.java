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

package com.caucho.jsf.cfg;

import java.io.IOException;
import java.util.*;
import java.util.regex.*;
import java.util.logging.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.convert.*;
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.validator.*;

import com.caucho.config.*;
import com.caucho.util.*;

public class NavigationRule implements Comparable<NavigationRule>
{
  private static final Logger log
    = Logger.getLogger(NavigationRule.class.getName());
  
  private String _id;
  
  private String _fromViewId;
  private Pattern _fromViewIdPattern;

  private int _cost;

  private ArrayList<NavigationCase> _caseList
    = new ArrayList<NavigationCase>();

  public void setId(String id)
  {
    _id = id;
  }

  public void setDescription(String description)
  {
  }

  public void setDisplayName(String displayName)
  {
  }

  public int getCost()
  {
    return _cost;
  }

  public int compareTo(NavigationRule rule)
  {
    return rule.getCost() - _cost;
  }

  public void setFromViewId(String viewId)
  {
    _fromViewId = viewId;

    _cost = viewId.length();
    
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < viewId.length(); i++) {
      char ch = viewId.charAt(i);

      switch (ch) {
      case '*':
        if (i < _cost)
          _cost = i;

        sb.append(".*");
        break;
      case '.': case '?': case '+': case '|': case '[': case ']':
      case '$': case '^': case '\\': case '(': case ')':
        sb.append("\\");
        sb.append(ch);
        break;
      default:
        sb.append(ch);
        break;
      }
    }

    _fromViewIdPattern = Pattern.compile(sb.toString());
  }

  public boolean isMatch(String url)
  {
    return (_fromViewIdPattern == null
            || _fromViewIdPattern.matcher(url).matches());
  }

  public void addNavigationCase(NavigationCase navCase)
  {
    _caseList.add(navCase);
  }

  public boolean handleNavigation(FacesContext context,
                               String action,
                               String outcome)
  {
    NavigationCase navCase = findCase(action, outcome);

    if (navCase != null) {
      if (log.isLoggable(Level.FINE))
        log.fine("Jsf[" + context.getViewRoot().getViewId() + "] navigation action:" + action + " outcome:" + outcome + " matches " + navCase);

      navCase.handleNavigation(context);
      return true;
    }

    return false;
  }

  private NavigationCase findCase(String action, String outcome)
  {
    NavigationCase bestCase = null;
    int bestCost = -1;
    
    for (int i = 0; i < _caseList.size(); i++) {
      NavigationCase navCase = _caseList.get(i);

      int cost = 0;

      if (navCase.getFromAction() == null) {
      }
      else if (action != null && action.equals(navCase.getFromAction()))
        cost |= 1;
      else
        continue;

      if (navCase.getFromOutcome() == null) {
      }
      else if (outcome != null && outcome.equals(navCase.getFromOutcome()))
        cost |= 2;
      else
        continue;

      if (cost == 3)
        return navCase;
      else if (bestCost < cost) {
        bestCost = cost;
        bestCase = navCase;
      }
    }

    return bestCase;
  }

  public String toString()
  {
    return "NavigationRule[" + _fromViewIdPattern + "]";
  }

  public static class NavigationCase {
    private String _fromAction;
    private String _fromOutcome;

    private String _toViewId;

    private boolean _isRedirect;
    
    public void setId(String id)
    {
    }

    public void setDescription(String description)
    {
    }

    public void setDisplayName(String displayName)
    {
    }
    
    public void setFromAction(String expr)
    {
      _fromAction = expr;
    }
    
    public String getFromAction()
    {
      return _fromAction;
    }
  
    public void setFromOutcome(String expr)
    {
      _fromOutcome = expr;
    }
  
    public String getFromOutcome()
    {
      return _fromOutcome;
    }
  
    public void setToViewId(String viewId)
    {
      _toViewId = viewId;
    }
  
    public void setRedirect(Redirect redirect)
    {
      _isRedirect = true;
    }

    public void handleNavigation(FacesContext context)
    {
      if (_isRedirect) {
        try {
          ExternalContext extContext = context.getExternalContext();
          ViewHandler viewHandler = context.getApplication().getViewHandler();

          String actionUrl = viewHandler.getActionURL(context, _toViewId);

          extContext.redirect(extContext.encodeActionURL(actionUrl));

          context.responseComplete();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      else {
        UIViewRoot oldView = context.getViewRoot();

        ViewHandler view = context.getApplication().getViewHandler();

        UIViewRoot viewRoot = view.createView(context, _toViewId);

        // XXX: is this in spec?
        if (oldView != null)
          viewRoot.setLocale(oldView.getLocale());

        context.setViewRoot(viewRoot);
      }
    }

    public String toString()
    {
      if (_isRedirect)
        return "NavCase[redirect," + _toViewId + "]";
      else
        return "NavCase[" + _toViewId + "]";
    }
  }

  public static class Redirect {
    public void setId(String id)
    {
    }
  }
}
