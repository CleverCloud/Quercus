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
import java.beans.FeatureDescriptor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Variable resolver using an underlying Map.
 */
public class MapVariableResolver extends ELResolver {
  private Map<String,Object> _map;
  
  /**
   * Creates the resolver
   */
  public MapVariableResolver(Map<String,Object> map)
  {
    if (map == null)
      map = new HashMap<String,Object>();

    _map = map;
  }
  
  /**
   * Creates the resolver
   */
  public MapVariableResolver()
  {
    this(new HashMap<String,Object>());
  }
  
  /**
   * Returns the named variable value.
   */
  @Override
  public Object getValue(ELContext context, Object base, Object property)
  {
    if (base != null || ! (property instanceof String))
      return null;

    String var = (String) property;
    
    Object value = _map.get(var);

    if (value != null) {
      context.setPropertyResolved(true);
      
      return value;
    }

    return null;
  }
  
  /**
   * Returns the named variable value.
   */
  @Override
  public void setValue(ELContext context,
                       Object base,
                       Object property,
                       Object value)
  {
    if (! (base instanceof String) || property != null)
      return;
    
    String var = (String) base;

    context.setPropertyResolved(true);
    
    _map.put(var, value);
  }
  
  /**
   * Sets the named variable value.
   */
  public Object put(String var, Object value)
  {
    return _map.put(var, value);
  }

  /**
   * Returns true for read-only.
   */
  @Override
  public boolean isReadOnly(ELContext context, Object base, Object property)
  {
    if (property != null || ! (base instanceof String))
      return true;

    context.setPropertyResolved(true);

    return false;
  }
  
  /**
   * Returns the named variable value.
   */
  @Override
  public Class<?> getType(ELContext context,
                        Object base,
                        Object property)
  {
    Object value = getValue(context, base, property);

    if (value != null)
      return value.getClass();
    else
      return null;
  }

  public Class<?> getCommonPropertyType(ELContext context,
                                        Object base)
  {
    return null;
  }

  public Iterator<FeatureDescriptor>
    getFeatureDescriptors(ELContext context, Object base)
  {
    return null;
  }

  /**
   * Gets the map.
   */
  public Map<String,Object> getMap()
  {
    return _map;
  }

  /**
   * Sets the map.
   */
  public void setMap(Map<String,Object> map)
  {
    _map = map;
  }

  public String toString()
  {
    return "MapVariableResolver[" + _map + "]";
  }
}
