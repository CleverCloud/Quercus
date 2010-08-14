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

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.server.rewrite.SetHeaderFilterChain;
import com.caucho.util.L10N;

/**
 * Sets a response header in a rewrite rule or as a top-level filter.
 *
 * <pre>
 * &lt;resin:SetHeader url-pattern="/foo/*"
 *                  name="Foo" value="bar"/>
 * </pre>
 */
@Configurable
public class SetHeader extends AbstractRewriteFilter
{
  private static final L10N L = new L10N(SetHeader.class);

  private String _name;
  private String _value;

  /**
   * Sets the HTTP header name
   */
  @Configurable
  public void setName(String name)
  {
    _name = name;
  }
  
  /**
   * Sets the HTTP header value
   */
  public void setValue(String value)
  {
    _value = value;
  }

  //  @Override
  public void init()
    throws ConfigException
  {
    if (_name == null) {
      throw new ConfigException(L.l("'name' is a required attribute of '{0}'.",
                                    getClass().getSimpleName()));
    }
    
    if (_value == null) {
      throw new ConfigException(L.l("'value' is a required attribute of '{0}'.",
                                    getClass().getSimpleName()));
    }
  }

  @Override
  protected FilterChain createFilterChain(String uri,
                                          String queryString,
                                          FilterChain next)
  {
    return new SetHeaderFilterChain(next, _name, _value);
  }
}

