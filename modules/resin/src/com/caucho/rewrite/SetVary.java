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
import com.caucho.server.rewrite.AddHeaderFilterChain;
import com.caucho.util.L10N;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sets a response Vary header to control caching based on input headers,
 * e.g. varying depending on the Locale.
 *
 * <pre>
 * &lt;web-app xmlns="http://caucho.com/ns/resin"
 *            xmlns:resin="urn:java:com.caucho.resin">
 *
 *   &lt;resin:SetVary regexp="^/foo" value="Bar"/>
 *
 * &lt;/web-app>
 * </pre>
 */
@Configurable
public class SetVary extends AbstractRewriteFilter
{
  private static final L10N L = new L10N(SetVary.class);

  private String _value;
  
  /**
   * Sets the Vary header
   */
  public void setValue(String value)
  {
    _value = value;
  }

  //  @Override
  public void init()
    throws ConfigException
  {
    if (_value == null) {
      throw new ConfigException(L.l("'value' is a required attribute of '{0}'.",
                                    getClass().getSimpleName()));
    }
  }

  protected FilterChain createFilterChain(String uri,
                                          String queryString,
                                          FilterChain next)
  {
    return new AddHeaderFilterChain(next, "Vary", _value);
  }
}

