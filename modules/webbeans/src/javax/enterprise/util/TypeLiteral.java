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

package javax.enterprise.util;

import java.lang.reflect.*;

/**
 * Convenience API to create runtime parameterized types.
 *
 * <code><pre>
 * new TypeLiteral&lt;ArrayList&lt;String>>() {}
 * </pre></code>
 */
public abstract class TypeLiteral<T>
{
  private transient Type _type;
  
  public final Type getType()
  {
    if (_type == null) {
      Type type = getClass().getGenericSuperclass();

      if (type instanceof ParameterizedType) {
        ParameterizedType pType = (ParameterizedType) type;

        _type = pType.getActualTypeArguments()[0];
      }
      else
        throw new UnsupportedOperationException(type.toString());
    }
    
    return _type;
  }

  @SuppressWarnings("unchecked")
  public final Class<T> getRawType()
  {
    Type type = getType();

    if (type instanceof Class<?>)
      return (Class<T>) type;
    else if (type instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) type;

      return (Class<T>) pType.getRawType();
    }
    else
      throw new UnsupportedOperationException(type.toString());
  }

  @Override
  public int hashCode()
  {
    return getType().hashCode();
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof TypeLiteral<?>))
      return false;

    TypeLiteral<?> lit = (TypeLiteral<?>) o;

    return getType().equals(lit.getType());
  }

  @Override
  public String toString()
  {
    return "TypeLiteral[" + getType() + "]";
  }
}
