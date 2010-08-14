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

package com.caucho.config.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Decorator;

import com.caucho.config.reflect.BaseType;
import com.caucho.inject.Module;

/**
 * Represents an introspected Decorator
 */
@Module
public class DecoratorEntry<X> {
  private Decorator<X> _decorator;

  private ArrayList<QualifierBinding> _bindings
    = new ArrayList<QualifierBinding>();

  private BaseType _delegateType;
  
  private boolean _isEnabled;
  
  private Set<BaseType> _decoratedTypes = new LinkedHashSet<BaseType>();

  public DecoratorEntry(InjectManager manager, Decorator<X> decorator,
                        BaseType delegateType)
  {
    _decorator = decorator;
    _delegateType = delegateType;

    for (Annotation ann : decorator.getDelegateQualifiers()) {
      _bindings.add(new QualifierBinding(ann));
    }

    if (_bindings.size() == 0)
      _bindings.add(new QualifierBinding(DefaultLiteral.DEFAULT));
    
    for (Type type: decorator.getDecoratedTypes()) {
      _decoratedTypes.add(manager.createSourceBaseType(type));
    }
  }

  public Decorator<X> getDecorator()
  {
    return _decorator;
  }

  public BaseType getDelegateType()
  {
    return _delegateType;
  }
  
  public Set<BaseType> getDecoratedTypes()
  {
    return _decoratedTypes;
  }

  public boolean isEnabled()
  {
    return _isEnabled;
  }
  
  public void setEnabled(boolean isEnabled)
  {
    _isEnabled = isEnabled;
  }
  
  public boolean isMatch(Annotation []bindingAnn)
  {
    if (! _isEnabled)
      return false;
    
    for (QualifierBinding binding : _bindings) {
      if (! isMatch(binding, bindingAnn)) {
        return false;
      }
    }

    return true;
  }

  public boolean isMatch(QualifierBinding binding, Annotation []bindingAnn)
  {
    for (Annotation ann : bindingAnn) {
      if (binding.isMatch(ann))
        return true;
    }

    return false;
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _decorator + "]";
  }
}
