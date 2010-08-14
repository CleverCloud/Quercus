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

import com.caucho.loader.EnvironmentClassLoader;

import javax.el.ELContext;
import javax.el.ELResolver;

/**
 * Creates a variable resolver based on the classloader.
 */
public class EnvironmentResolver extends AbstractVariableResolver {
  private ClassLoader _loader;
  
  /**
   * Creates the resolver
   */
  public EnvironmentResolver(ClassLoader loader, ELResolver parent)
  {
    super(parent);
    
    _loader = loader;
  }
  
  /**
   * Returns the named variable value.
   */
  public Object getValue(ELContext context,
                         Object base,
                         Object property)
  {
    String var = (String) base;
    
    for (ClassLoader loader = _loader;
         loader != null;
         loader = loader.getParent()) {
      if (loader instanceof EnvironmentClassLoader) {
        Object value = EL.getLevelVar(var, loader);

        if (value == EL.NULL)
          return null;
        else if (value != null)
          return value;
      }
    }

    Object value = EL.getLevelVar(var, null);
    
    if (value == EL.NULL)
      return null;
    else if (value != null)
      return value;
    else
      return super.getValue(context, var, property);
  }
}
