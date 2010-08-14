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

package com.caucho.rewrite;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;

/**
 * URL rewriting and request dispatching based on URL and query string,
 * configured in the resin-web.xml.  Basically, a replacement for
 * mod_rewrite capabilities in Resin.
 *
 * <p>DispatchRules generally have a regular expression and a target
 * (defined in {@link com.caucho.rewrite.AbstractTargetDispatchRule}).  They have optional
 * {@link com.caucho.rewrite.RequestPredicate} conditions to check
 request headers, so
 * dispatching can be browser-specific.
 *
 * <p>Custom DispatchRules can be made by extending
 * {@link com.caucho.rewrite.AbstractTargetDispatchRule} and implementing
 * the <code>createDispatch</code> method.
 *
 * <pre>
 * &lt;web-app xmlns="http://caucho.com/ns/resin"
 *             xmlns:resin="urn:java:com.caucho.resin">
 *
 *   &lt;resin:Dispatch regexp="\.(php|jpg|gif|js|css)"/>
 *
 *   &lt;resin:Dispatch regexp="^" target="/index.php"/>
 *
 * &lt;/web-app>
 * </pre>
 */
public interface DispatchRule
{
  public boolean isRequest();
  
  public boolean isInclude();
  
  public boolean isForward();

  /**
   * Rewrites the URI for further processing.  Rules following the current
   * one will use the new URI.
   */
  public String rewriteUri(String uri, String queryString);

  /**
   * Creates a FilterChain for the action based on the uri and query string.
   *
   * Matching requests will use <code>tail</code>, and mismatching
   * requests will use <code>next</code>.  <code>tail</code> is the
   * plain servlet/filter chain without any rewriting.  <code>next</code>
   * is the next rewrite dispatch
   *
   * @param uri the request URI to match against
   * @param queryString the request query string to match against
   * @param next the next rewrite FilterChain dispatch
   * @param tail the plain servlet/filter chain for a match
   */
  public FilterChain map(String uri,
                         String queryString,
                         FilterChain next,
                         FilterChain tail)
    throws ServletException;
}
