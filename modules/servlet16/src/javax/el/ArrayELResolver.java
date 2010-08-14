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

package javax.el;

import java.beans.FeatureDescriptor;
import java.lang.reflect.Array;
import java.util.Iterator;

/**
 * Resolves properties based on arrays.
 */
public class ArrayELResolver extends ELResolver {
  private final boolean _isReadOnly;
  
  public ArrayELResolver()
  {
    _isReadOnly = false;
  }
  
  public ArrayELResolver(boolean isReadOnly)
  {
    _isReadOnly = isReadOnly;
  }

  @Override
  public Class<?> getCommonPropertyType(ELContext context, Object base)
  {
    if (base == null)
      return null;
    else if (base.getClass().isArray())
      return base.getClass().getComponentType();
    else
      return null;
  }

  @Override
  public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context,
                                                           Object base)
  {
    if (base == null)
      return null;
    else if (base.getClass().isArray()) {
      context.setPropertyResolved(true);

      return null;
    }
    else
      return null;
  }

  @Override
  public Class<?> getType(ELContext context,
                          Object base,
                          Object property)
  {
    if (base == null)
      return null;
    else if (base.getClass().isArray()) {
      context.setPropertyResolved(true);

      int index = getIndex(property);

      if (index < 0 || Array.getLength(base) <= index)
        throw new PropertyNotFoundException("array index '" + index + "' is invalid");

      return base.getClass().getComponentType();
    }
    else
      return null;
  }

  /**
   *
   * @param context
   * @param base
   * @param property
   * @return If the <code>propertyResolved</code> property of
     *     <code>ELContext</code> was set to <code>true</code>, then
     *     the value at the given index or <code>null</code>
     *     if the index was out of bounds. Otherwise, undefined.
   */
  @Override
  public Object getValue(ELContext context,
                         Object base,
                         Object property)
  {
    if (base == null)
      return null;
    else if (base.getClass().isArray()) {
      context.setPropertyResolved(true);

      int index = getIndex(property);

      if (0 <= index && index < Array.getLength(base))
        return Array.get(base, index);
      else
        return null;
    }
    else {
      return null;
    }
  }

  @Override
  public boolean isReadOnly(ELContext context,
                            Object base,
                            Object property)
  {
    if (base == null)
      return false;
    else if (base.getClass().isArray()) {
      context.setPropertyResolved(true);

      int index = getIndex(property);

      if (index < 0 || index >= Array.getLength(base))
        throw new PropertyNotFoundException("array index '" + index + "' is invalid");

      return _isReadOnly;
    }
    else
      return false;
  }

  @Override
  public void setValue(ELContext context,
                       Object base,
                       Object property,
                       Object value)
  {
    if (base == null) {
    }
    else if (base.getClass().isArray()) {
      context.setPropertyResolved(true);

      if (_isReadOnly)
        throw new PropertyNotWritableException("resolver is read-only");

      int index = getIndex(property);

      Class componentType
        = base.getClass().getComponentType();

      if (value != null &&
        ! componentType.isAssignableFrom(value.getClass())) {
        throw new ClassCastException(value.getClass().getName()
          + " cannot be cast to "
          + componentType.getName());
      } else if (0 <= index && index < Array.getLength(base))
        Array.set(base, index, value);
      else
        throw new PropertyNotFoundException("array index '" + index + "' is invalid");
    }
  }

  static int getIndex(Object property)
  {
    if (property instanceof Number)
      return ((Number) property).intValue();
    else if (property instanceof String) {
      try {
        return Integer.parseInt((String) property);
      } catch (Exception e) {
        throw new IllegalArgumentException("can't convert '" + property + "' to long.");
      }
    }
    else
      throw new IllegalArgumentException("can't convert '" + property + "' to long.");
  }
}
