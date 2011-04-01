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
 */

package com.caucho.config.inject;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import com.caucho.inject.Module;

/**
 * Stack of partially constructed beans.
 */
@Module
public class CreationalContextImpl<T> implements CreationalContext<T> {
  public static final Object NULL = new Object();
  
  private final Contextual<T> _bean;
  private final CreationalContextImpl<?> _parent; // parent in the creation chain
  
  private T _value;
  
  protected CreationalContextImpl(Contextual<T> bean,
                                  CreationalContextImpl<?> parent)
  {
    _bean = bean;
    
    if (parent instanceof CreationalContextImpl<?>)
      _parent = (CreationalContextImpl<?>) parent;
    else
      _parent = null;
  }
  
  protected boolean isTop()
  {
    return false;
  }
  
  protected OwnerCreationalContext<?> getOwner()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  protected DependentCreationalContext<?> getNext()
  {
    return null;
  }
  
  protected InjectionPoint getInjectionPoint()
  {
    return null;
  }
  
  public void setInjectionPoint(InjectionPoint ip)
  {
  }
  
  public T getValue()
  {
    return _value;
  }
  
  public Object getParentValue()
  {
    return _parent.getValue();
  }
  
  public <X> X get(Contextual<X> bean)
  {
    return find(this, bean);    
  }
  
  @SuppressWarnings("unchecked")
  public
  static <X> X find(CreationalContextImpl<?> ptr, Contextual<X> bean)
  {
    for (; ptr != null; ptr = ptr._parent) {
      Contextual<?> testBean = ptr._bean;
      
      if (testBean == bean && ptr._value != null) {
        return (X) ptr._value;
      }
    }
    
    return null;
  }
  
  @SuppressWarnings("unchecked")
  public
  static <X> X findWithNull(CreationalContextImpl<?> ptr, Contextual<X> bean)
  {
    for (; ptr != null; ptr = ptr._parent) {
      Contextual<?> testBean = ptr._bean;
      
      if (testBean != bean) {
        
      }
      else if (ptr._value != null) {
        return (X) ptr._value;
      }
      else
        return (X) NULL;
    }
    
    return null;
  }

  /**
   * Find any bean, for disposers.
   */
  public <X> X getAny(Contextual<X> bean)
  {
    return findAny(getOwner(), bean);    
  }
  
  @SuppressWarnings("unchecked")
  public
  static <X> X findAny(CreationalContextImpl<?> ptr, Contextual<X> bean)
  {
    if (ptr == null)
      return null;
    
    for (ptr = ptr.getOwner(); ptr != null; ptr = ptr.getNext()) {
      Contextual<?> testBean = ptr._bean;
      
      if (testBean == bean) {
        return (X) ptr._value;
      }
    }
    
    return null;
  }
  
  public static Object findByName(CreationalContextImpl<?> ptr, String name)
  {
    for (; ptr != null; ptr = ptr._parent) {
      Contextual<?> testBean = ptr._bean;
      
      if (! (testBean instanceof Bean<?>))
        continue;
      
      Bean<?> bean = (Bean<?>) testBean;

      if (name.equals(bean.getName())) {
        return ptr._value;
      }
    }
    
    return null;
  }
  
  public InjectionPoint findInjectionPoint()
  {
    CreationalContextImpl<?> ptr = this; 
    
    while (ptr != null) {
      if (ptr instanceof CreationalContextImpl<?>) {
        CreationalContextImpl<?> env = (CreationalContextImpl<?>) ptr;

        InjectionPoint ip = env.getInjectionPoint();
        
        if (ip != null)
          return ip;
        
        ptr = env._parent;
      }
      else
        ptr = null;
    }
    
    return null;
  }
  
  public Object getDelegate()
  {
    CreationalContextImpl<?> ptr = this; 
    
    while (ptr != null) {
      if (ptr instanceof CreationalContextImpl<?>) {
        CreationalContextImpl<?> env = (CreationalContextImpl<?>) ptr;
        
        if (env._bean == DelegateProxyBean.BEAN)
          return env._value;
        
        ptr = env._parent;
      }
      else
        ptr = null;
    }
    
    return null;
  }

  @Override
  public void push(T value)
  {
    _value = value;
  }
  
  @Module
  public void clearTarget()
  {
    _value = null;
  }
  
  @Override
  public void release()
  {
    T value = _value;
    _value = null;
    
    if (value != null)
      _bean.destroy(value, this);
    else {
      CreationalContextImpl<?> next = getNext();
      
      if (next != null)
        next.release();
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _bean + "," + _value + ",parent=" + _parent + "]";
  }
}
