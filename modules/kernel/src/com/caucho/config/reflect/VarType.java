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

package com.caucho.config.reflect;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import com.caucho.inject.Module;

/**
 * class type matching
 */
@Module
public class VarType<D extends GenericDeclaration> extends BaseType
  implements TypeVariable<D>
{
  private String _name;
  private BaseType []_bounds;

  public VarType(String name, BaseType []bounds)
  {
    _name = name;
    _bounds = bounds;
  }

  @Override
  public String getName()
  {
    return _name;
  }

  @Override
  public D getGenericDeclaration()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public boolean isWildcard()
  {
    // ioc/024j vs ioc/024k
    return false;
  }
  
  @Override
  public boolean isGeneric()
  {
    return true;
  }
  
  @Override
  public boolean isVariable()
  {
    return true;
  }

  @Override
  public Type []getBounds()
  {
    Type []bounds = new Type[_bounds.length];

    for (int i = 0; i < bounds.length; i++) {
      bounds[i] = _bounds[i].toType();
    }

    return bounds;
  }
  
  @Override
  protected BaseType []getWildcardBounds()
  {
    return _bounds;
  }
  
  @Override
  public Class<?> getRawClass()
  {
    return Object.class; // technically bounds
  }

  public Type getGenericComponentType()
  {
    return null;
  }

  @Override
  public Type toType()
  {
    return this;
  }

  @Override
  public boolean isAssignableFrom(BaseType type)
  {
    if (type.isWildcard())
      return true;
    
    for (BaseType bound : _bounds) {
      if (! bound.isAssignableFrom(type)) {
        return false;
      }
    }
    
    return true;
  }
  
  @Override
  public boolean isParamAssignableFrom(BaseType type)
  {
    // ioc/0i3m
    return isAssignableFrom(type);
  }

  @Override
  public int hashCode()
  {
    return 17 + 37 * _name.hashCode();
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this)
      return true;
    else if (o instanceof TypeVariable<?>) {
      // TypeVariable<?> var = (TypeVariable<?>) o;

      return true;
    }
    else
      return false;
  }

  public String toString()
  {
    if (_bounds.length == 0)
      return _name;
    
    StringBuilder sb = new StringBuilder(_name);
    
    for (BaseType type : _bounds) {
      if (! type.getRawClass().equals(Object.class))
        sb.append(" extends ").append(type);
    }
    
    return sb.toString();
  }
}
