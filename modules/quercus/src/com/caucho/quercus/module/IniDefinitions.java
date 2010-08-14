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
 * @author Sam
 */

package com.caucho.quercus.module;

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.NullValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class IniDefinitions {
  public static final IniDefinitions EMPTY = new IniDefinitions();

  private HashMap<String, IniDefinition> _defaultMap;

  /**
   * Add an ini default for an ini that has a boolean value.
   */
  public IniDefinition add(String name, boolean deflt, int scope)
  {
    return add(name,
               IniDefinition.Type.BOOLEAN,
               BooleanValue.create(deflt),
               scope);
  }

  /**
   * Add an ini default for an ini that has a long value.
   */
  public IniDefinition add(String name, long deflt, int scope)
  {
    return add(name, IniDefinition.Type.LONG, LongValue.create(deflt), scope);
  }

  /**
   * Add an ini default for an ini that has a string value.
   */
  public IniDefinition add(String name, String deflt, int scope)
  {
    return add(name,
               IniDefinition.Type.STRING,
               StringValue.create(deflt),
               scope);
  }

  private IniDefinition add(String name,
                            IniDefinition.Type type,
                            Value deflt,
                            int scope)
  {
    return add(new IniDefinition(name, type, deflt, scope));
  }

  private IniDefinition add(IniDefinition iniDefinition)
  {
    if (_defaultMap == null)
      _defaultMap = new HashMap<String, IniDefinition>();

    _defaultMap.put(iniDefinition.getName(), iniDefinition);

    return iniDefinition;
  }

  /**
   * Add an unsupported ini default for an ini that has a boolean value.
   */
  public IniDefinition addUnsupported(String name, boolean deflt, int scope)
  {
    return addUnsupported(name,
                          IniDefinition.Type.BOOLEAN,
                          BooleanValue.create(deflt),
                          scope);
  }

  /**
   * Add an unsupported ini default for an ini that has a long value.
   */
  public IniDefinition addUnsupported(String name, long deflt, int scope)
  {
    return addUnsupported(name,
                          IniDefinition.Type.LONG,
                          LongValue.create(deflt),
                          scope);
  }

  /**
   * Add an unsupported ini default for an ini that has a string value.
   */
  public IniDefinition addUnsupported(String name, String deflt, int scope)
  {
    return addUnsupported(name,
                          IniDefinition.Type.STRING,
                          StringValue.create(deflt),
                          scope);
  }

  private IniDefinition addUnsupported(String name,
                                       IniDefinition.Type type,
                                       Value deflt,
                                       int scope)
  {
    return add(new IniDefinition.Unsupported(name, type, deflt, scope));
  }

  public void addAll(IniDefinitions iniDefinitions)
  {
    if (iniDefinitions._defaultMap == null)
      return;

    if (_defaultMap == null)
      _defaultMap = new HashMap<String, IniDefinition>();
    
    _defaultMap.putAll(iniDefinitions._defaultMap);
  }

  /**
   * Return a set of all of the names.
   */
  public Set<String> getNames()
  {
    if (_defaultMap == null)
      return Collections.emptySet();
    else
      return _defaultMap.keySet();
  }
  
  /*
   * Returns the set of all ini name/value pairs.
   */
  public Set<Map.Entry<String, IniDefinition>> entrySet()
  {
    if (_defaultMap == null)
      return null;
    else
      return _defaultMap.entrySet();
  }

  public IniDefinition get(String name)
  {
    IniDefinition iniDefinition = _defaultMap == null
                                  ? null
                                  : _defaultMap.get(name);

    if (iniDefinition == null) {
      iniDefinition = new IniDefinition.Runtime(name);

      add(iniDefinition);
    }

    return iniDefinition;
  }
}
