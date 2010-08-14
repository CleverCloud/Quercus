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

package com.caucho.security;

import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.rewrite.RequestPredicate;
import com.caucho.server.security.AbstractConstraint;
import com.caucho.server.security.AuthorizationResult;

/**
 * The &lt;sec:Require> tag authorizes requests for a set of url-patterns.
 * If the request URL matches, &lt;sec:Require> checks all its children
 * ServletReqestPredicate for matches, and if any children don't match, the
 * request is forbidden.
  *
 * <code><pre>
 * &lt;web-app xmlns="http://caucho.com/ns/resin"
 *          xmlns:sec="urn:java:com.caucho.security">
 *
 *   &lt;sec:Require>
 *     &lt;sec:url-pattern>/admin/*&lt;sec:url-pattern>
 *     &lt;sec:url-pattern>/security/*&lt;sec:url-pattern>
 *
 *     &lt;sec:IfNetwork>192.168.0.1&lt;/sec:IfNetwork>
 *   &lt;/sec:Require>
 *
 * &lt;/web-app>
 * </pre></code>
 */
public class Require extends Allow
{
  /**
   * return the constraint
   */
  @Override
  public AbstractConstraint getConstraint()
  {
    return new RequireConstraint(getPredicateList());
  }

  class RequireConstraint extends AbstractConstraint
  {
    private RequestPredicate []_predicateList;

    RequireConstraint(ArrayList<RequestPredicate> predicateList)
    {
      _predicateList = new RequestPredicate[predicateList.size()];
      predicateList.toArray(_predicateList);
    }

    public AuthorizationResult isAuthorized(HttpServletRequest request,
                                            HttpServletResponse response,
                                            ServletContext webApp)
    {
      for (RequestPredicate predicate : _predicateList) {
        if (! predicate.isMatch(request)) {
          return AuthorizationResult.DENY;
        }
      }

      return AuthorizationResult.DEFAULT_ALLOW;
    }
  }
}
