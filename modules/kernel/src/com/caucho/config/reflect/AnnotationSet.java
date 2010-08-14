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

import java.lang.annotation.Annotation;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

import com.caucho.inject.Module;

/**
 * Abstract introspected view of a Bean
 */
@Module
public class AnnotationSet extends AbstractSet<Annotation>
{
  private static final Annotation []ZERO_SET = new Annotation[0];
  private static final NullAnnotationSetIterator NULL_ITER
    = new NullAnnotationSetIterator();
  
  private Annotation [] _annSet = ZERO_SET;
  private int _size;

  public AnnotationSet()
  {
  }

  public AnnotationSet(Collection<Annotation> set)
  {
    for (Annotation ann : set) {
      add(ann);
    }
  }

  @Override
  public int size()
  {
    return _size;
  }
  
  public boolean isAnnotationPresent(Class<? extends Annotation> annType)
  {
    Annotation []annSet = _annSet;

    for (int i = _size - 1; i >= 0; i--) {
      Annotation ann = annSet[i];

      if (ann.annotationType().equals(annType)) {
        return true;
      }
    }

    return false;
  }
  
  public Annotation getAnnotation(Class<? extends Annotation> annType)
  {
    Annotation []annSet = _annSet;

    for (int i = _size - 1; i >= 0; i--) {
      Annotation ann = annSet[i];

      if (ann.annotationType().equals(annType)) {
        return ann;
      }
    }

    return null;
  }
 
  public void replace(Annotation newAnn)
  {
    Annotation []annSet = _annSet;

    for (int i = _size - 1; i >= 0; i--) {
      Annotation oldAnn = annSet[i];

      if (oldAnn.annotationType().equals(newAnn.annotationType())) {
        annSet[i] = newAnn;
        return;
      }
    }

    addImpl(newAnn);
  }
  
  public boolean add(Annotation newAnn)
  {
    Annotation []annSet = _annSet;

    for (int i = _size - 1; i >= 0; i--) {
      Annotation oldAnn = annSet[i];

      if (oldAnn.equals(newAnn)) {
        return false;
      }
    }

    addImpl(newAnn);
    
    return true;
  }
  
  public boolean remove(Annotation newAnn)
  {
    Annotation []annSet = _annSet;

    for (int i = _size - 1; i >= 0; i--) {
      Annotation oldAnn = annSet[i];

      if (oldAnn.equals(newAnn)) {
        System.arraycopy(annSet, i + 1, annSet, i, annSet.length - i - 1);
        _size--;
        
        return true;
      }
    }
    
    return false;
  }
  
  private void addImpl(Annotation newAnn)
  {
    if (_annSet.length <= _size + 1) {
      int newSize = 2 * (_size + 1);
      
      if (newSize < 16)
        newSize = 16;
      
      Annotation []annSet = new Annotation[newSize];
      System.arraycopy(_annSet, 0, annSet, 0, _annSet.length);
      
      _annSet = annSet;
    }
    
    _annSet[_size++] = newAnn;
  }

  @Override
  public void clear()
  {
    for (int i = _size - 1; i >= 0; i--)
      _annSet[i] = null;
    
    _size = 0;
  }

  @Override
  public Iterator<Annotation> iterator()
  {
    if (_size > 0)
      return new AnnotationSetIterator();
    else
      return NULL_ITER;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    
    sb.append(getClass().getSimpleName());
    sb.append("[");
    
    for (int i = 0; i < _size; i++) {
      if (i > 0)
        sb.append(", ");
      
      sb.append(_annSet[i]);
    }
    sb.append("]");
    
    return sb.toString();
  }
  
  class AnnotationSetIterator implements Iterator<Annotation> {
    int _index;
    
    @Override
    public boolean hasNext()
    {
      return _index < _size;
    }
    
    @Override
    public Annotation next()
    {
      if (_index < _size)
        return _annSet[_index++];
      else
        return null;
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException(getClass().getName());
    }
  }
  
  static class NullAnnotationSetIterator implements Iterator<Annotation> {
    @Override
    public boolean hasNext()
    {
      return false;
    }
    
    @Override
    public Annotation next()
    {
      return null;
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException(getClass().getName());
    }
  }
}
