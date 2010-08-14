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

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;

import com.caucho.config.Configurable;
import com.caucho.server.dispatch.MovedFilterChain;

/**
 * Sends a HTTP moved-permanently response
 *
 * <pre>
 * &lt;web-app xmlns:resin="urn:java:com.caucho.resin">
 *
 *   &lt;resin:MovedPermanently regexp="^/hidden" target="/bar"/>
 *
 * &lt;/web-app>
 * </pre>
 */
@Configurable
public class MovedPermanently extends AbstractTargetDispatchRule
{
  @Override
  public FilterChain createDispatch(String uri,
                                    String queryString,
                                    String target,
                                    FilterChain next)
  {
    return new MovedFilterChain(HttpServletResponse.SC_MOVED_PERMANENTLY, target);
  }
}
