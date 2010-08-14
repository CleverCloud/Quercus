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

package com.caucho.config.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.ObserverMethod;

import com.caucho.inject.Module;

/**
 * Common bean introspection for Produces and ManagedBean.
 */
@Module
abstract public class AbstractObserverMethod<T>
  implements ObserverMethod<T>
{
  @Override
  public Class<?> getBeanClass()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public Type getObservedType()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public Set<Annotation> getObservedQualifiers()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public Reception getReception()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public TransactionPhase getTransactionPhase()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  abstract public void notify(T event);
}
