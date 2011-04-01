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

package com.caucho.inject;

import java.lang.annotation.Annotation;
import java.util.HashMap;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import com.caucho.util.L10N;

/**
 * The ThreadContext works with @ThreadScoped.
 */
public class ThreadContext {
  private static final L10N L = new L10N(ThreadContext.class);
  
  private static final ThreadLocal<HashMap<Contextual<?>,Object>> _threadMap
    = new  ThreadLocal<HashMap<Contextual<?>,Object>>();
  
  private static ThreadContextImpl _context = new ThreadContextImpl();
  
  public static void begin()
  {
    if (_threadMap.get() != null)
      throw new IllegalStateException(L.l("ThreadContext begin() must not be recursive"));
    
    HashMap<Contextual<?>,Object> map = new HashMap<Contextual<?>,Object>(8);
    
    _threadMap.set(map);
  }
  
  public static void end()
  {
    if (_threadMap.get() == null)
      throw new IllegalStateException(L.l("ThreadContext end() does not have a matching begin"));
    
    _threadMap.set(null);
  }
  
  public static Context getContext()
  {
    return _context;
  }
  
  static class ThreadContextImpl implements Context {
    @Override
    public <T> T get(Contextual<T> bean)
    {
      HashMap<Contextual<?>,Object> map = _threadMap.get();

      if (map == null)
        throw new IllegalStateException(L.l("@ThreadScoped is not active for {0}",
                                            Thread.currentThread()));
      
      return (T) map.get(bean);
    }

    @Override
    public <T> T get(Contextual<T> bean, CreationalContext<T> env)
    {
      HashMap<Contextual<?>,Object> map = _threadMap.get();

      if (map == null)
        throw new IllegalStateException(L.l("@ThreadScoped is not active for {0}",
                                            Thread.currentThread()));
      
      T instance = (T) map.get(bean);
      
      if (instance == null) {
        instance = bean.create(env);
        
        map.put(bean, instance);
      }
      
      return instance;
    }

    @Override
    public Class<? extends Annotation> getScope()
    {
      return ThreadScoped.class;
    }

    @Override
    public boolean isActive()
    {
      return _threadMap.get() != null;
    }
  }
}
