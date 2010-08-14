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

package com.caucho.server.security;

import com.caucho.server.dispatch.ErrorFilterChain;
import com.caucho.server.dispatch.FilterChainBuilder;
import com.caucho.server.dispatch.ForwardFilterChain;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Manages security constraint.
 */
public class ConstraintManager extends FilterChainBuilder {
  private static L10N L = new L10N(ConstraintManager.class);

  private ArrayList<SecurityConstraint> _constraints
    = new ArrayList<SecurityConstraint>();

  public void addConstraint(SecurityConstraint constraint)
  {
    _constraints.add(constraint);
  }

  public FilterChainBuilder getFilterBuilder()
  {
    return this;
    /*
    if (_constraints.size() > 0)
      return this;
    else
      return null;
    */
  }
  
  /**
   * Builds a filter chain dynamically based on the invocation.
   *
   * @param next the next filter in the chain.
   * @param invocation the request's invocation.
   */
  public FilterChain build(FilterChain next, Invocation invocation)
  {
    String uri = invocation.getContextURI();

    WebApp app = invocation.getWebApp();
    if (app == null)
      return next;

    String lower = uri.toLowerCase();

    if (lower.startsWith("/web-inf")
        || lower.startsWith("/meta-inf")) {
      return new ErrorFilterChain(HttpServletResponse.SC_NOT_FOUND);
    }

    ArrayList<AbstractConstraint> constraints;
    constraints = new ArrayList<AbstractConstraint>();
    
    HashMap<String,AbstractConstraint[]> methodMap;
    methodMap = new HashMap<String,AbstractConstraint[]>();

    loop:
    for (int i = 0; i < _constraints.size(); i++) {
      SecurityConstraint constraint = _constraints.get(i);

      if (constraint.isMatch(uri)) {
        AbstractConstraint absConstraint = constraint.getConstraint();

        if (absConstraint != null) {
          ArrayList<String> methods = constraint.getMethods(uri);

          for (int j = 0; methods != null && j < methods.size(); j++) {
            String method = methods.get(j);

            AbstractConstraint []methodList = methodMap.get(method);

            if (methodList == null)
              methodList = absConstraint.toArray();
            // server/12ba - the first constraint matches, following are
            // ignored
            /*
            else {

              AbstractConstraint []newMethods = absConstraint.toArray();

              AbstractConstraint []newList;

              newList = new AbstractConstraint[methodList.length
                                               + newMethods.length];

              System.arraycopy(methodList, 0, newList, 0, methodList.length);
              System.arraycopy(newMethods, 0, newList,
                               methodList.length, newMethods.length);

              methodList = newList;
            }
            */

            methodMap.put(method, methodList);
          }
          
          if (methods == null || methods.size() == 0) {
            AbstractConstraint []constArray = absConstraint.toArray();
            for (int k = 0; k < constArray.length; k++)
              constraints.add(constArray[k]);

            // server/12ba - the first constraint matches, following are
            // ignored

            if (! constraint.isFallthrough())
              break loop;
          }
        }
        else {
          // server/1233

          if (! constraint.isFallthrough())
            break loop;
        }
      }
    }

    if (uri.endsWith("/j_security_check")
        && app.getLogin() instanceof FormLogin) {
      RequestDispatcher disp = app.getNamedDispatcher("j_security_check");
      if (disp == null)
        throw new IllegalStateException(L.l("j_security_check is an undefined servlet"));

      next = new ForwardFilterChain(disp);
    }

    if (constraints.size() > 0 || methodMap.size() > 0) {
      SecurityFilterChain filterChain = new SecurityFilterChain(next);
      filterChain.setWebApp(invocation.getWebApp());
      if (methodMap.size() > 0)
        filterChain.setMethodMap(methodMap);
      filterChain.setConstraints(constraints);

      return filterChain;
    }

    return next;
  }

  public boolean hasConstraintForUrlPattern(String pattern) {
    for (SecurityConstraint constraint : _constraints) {
      if (constraint.isMatch(pattern))
        return true;
    }

    return false;
  }
}
