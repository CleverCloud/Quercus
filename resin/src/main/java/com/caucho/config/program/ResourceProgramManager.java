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
 * @author Scott Ferguson;
 */

package com.caucho.config.program;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.util.FreeList;

/**
 * JavaEE resource program
 */
public class ResourceProgramManager {
  private final ConcurrentHashMap<Class<?>,ArrayList<ResourceInjectionTargetProgram>> _programMap
    = new ConcurrentHashMap<Class<?>,ArrayList<ResourceInjectionTargetProgram>>();
  
  private final FreeList<TargetKey> _freeList = new FreeList<TargetKey>(16); 
  
  public void addResource(ResourceInjectionTargetProgram resource)
  {
    ArrayList<ResourceInjectionTargetProgram> programList;
    
    programList = _programMap.get(resource.getTargetClass());
    
    if (programList == null) {
      programList = new ArrayList<ResourceInjectionTargetProgram>();
      
      _programMap.put(resource.getTargetClass(), programList);
    }
    
    programList.add(resource);
  }
  
  public void buildInject(Class<?> type,
                          ArrayList<ConfigProgram> injectProgramList)
  {
    if (type == null || type.equals(Object.class))
      return;
    
    buildInject(type.getSuperclass(), injectProgramList);
    
    ArrayList<ResourceInjectionTargetProgram> programList;
    programList = _programMap.get(type);
    
    if (programList == null)
      return;
    
    for (ResourceInjectionTargetProgram program : programList) {
      injectProgramList.add(program);
    }
  }
 
  static class TargetKey {
    private Class<?> _targetClass;
    private String _targetName;
    
    TargetKey()
    {
    }
    
    TargetKey(Class<?> targetClass, String targetName)
    {
      _targetClass = targetClass;
      _targetName = targetName;
    }
    
    public void init(Class<?> targetClass, String targetName)
    {
      _targetClass = targetClass;
      _targetName = targetName;
    }
    
    @Override
    public int hashCode()
    {
      return 65521 * _targetClass.hashCode() + _targetName.hashCode();
    }
    
    @Override
    public boolean equals(Object o)
    {
      if (this == o)
        return true;
      else if (! (o instanceof TargetKey)) {
        return false;
      }
      
      TargetKey key = (TargetKey) o;
      
      return (_targetClass.equals(key._targetClass)
              && _targetName.equals(key._targetName));
    }
  }
}
