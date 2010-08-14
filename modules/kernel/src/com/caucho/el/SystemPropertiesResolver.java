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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.el;

import javax.el.ELContext;
import javax.el.ELResolver;

import java.util.*;

/**
 * Variable resolver using the system properties (@link System#getProperty()}
 * and the environment {@link System@getenv()}.
 *
 */
public class SystemPropertiesResolver extends AbstractVariableResolver {
  /**
   * Creates the resolver
   */
  public SystemPropertiesResolver()
  {
  }
  
  /**
   * Creates the resolver
   */
  public SystemPropertiesResolver(ELResolver next)
  {
    super(next);
  }
  
  /**
   * Returns the named variable value.
   */
  @Override
  public Object getValue(ELContext env,
                         Object base,
                         Object property)
  {
    String var;
    
    if (base == null && property instanceof String)
      var = (String) property;
    else if (base == this && property instanceof String)
      var = (String) property;
    else
      return null;
    
    Object value = System.getProperty(var);

    if (value != null) {
      env.setPropertyResolved(true);
      return value;
    }

    value = System.getenv(var);

    if (value != null) {
      env.setPropertyResolved(true);
      return value;
    }

    if ("Var".equals(var)) {
      // server/10m5
      env.setPropertyResolved(true);
      return new PropertyMap(env);
    }

    return null;
  }

  /**
   * Returns the system property resolver.
   */
  public String toString()
  {
    return "SystemPropertiesResolver[]";
  }

  static class PropertyMap extends AbstractMap {
    private ELContext _env;

    PropertyMap(ELContext env)
    {
      _env = env;
    }
    
    public Object get(Object key)
    {
      return _env.getELResolver().getValue(_env, null, key);
    }
    
    public Set entrySet()
    {
      throw new UnsupportedOperationException();
    }
  }
}
