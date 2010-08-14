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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.el;

import javax.el.*;
import java.util.Map;

/**
 * Creates a variable resolver based on the classloader.
 */
public class EnvironmentContext extends ELContext {
  private final StackELResolver _elResolver = new StackELResolver();
  
  /**
   * Creates the resolver
   */
  public EnvironmentContext()
  {
    this(Thread.currentThread().getContextClassLoader());
  }
  
  /**
   * Creates the resolver
   */
  public EnvironmentContext(ClassLoader loader)
  {
    _elResolver.push(new ArrayELResolver());
    _elResolver.push(new MapELResolver());
    _elResolver.push(new ListELResolver());
    _elResolver.push(new BeanELResolver());
    _elResolver.push(new SystemPropertiesResolver());

    _elResolver.push(EnvironmentELResolver.create(loader));
  }
  
  /**
   * Creates the resolver
   */
  public EnvironmentContext(Map<String,Object> map)
  {
    this();

    _elResolver.push(new MapVariableResolver(map));
  }

  public ELResolver getELResolver()
  {
    return _elResolver;
  }

  public FunctionMapper getFunctionMapper()
  {
    return null;
  }

  public VariableMapper getVariableMapper()
  {
    return null;
  }
}
