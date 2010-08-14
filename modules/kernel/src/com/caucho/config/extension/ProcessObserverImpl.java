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

package com.caucho.config.extension;

import java.util.logging.Logger;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessObserverMethod;

import com.caucho.config.inject.InjectManager;
import com.caucho.inject.Module;

@Module

public class ProcessObserverImpl<T,X> implements ProcessObserverMethod<T,X>
{
  private static final Logger log
    = Logger.getLogger(ProcessObserverImpl.class.getName());
  
  private InjectManager _cdiManager;
  private AnnotatedMethod<X> _method;
  private ObserverMethod<T> _observer;
  private Throwable _definitionError;
  
  ProcessObserverImpl(InjectManager cdiManager,
                      ObserverMethod<T> observer,
                      AnnotatedMethod<X> method)
                      
  {
    _cdiManager = cdiManager;
    _method = method;
    _observer = observer;
  }
  
  @Override
  public AnnotatedMethod<X> getAnnotatedMethod()
  {
    return _method;
  }
  
  @Override
  public void addDefinitionError(Throwable t)
  {
    log.info("DEFINITION: " + t);
    _cdiManager.addDefinitionError(t);
  }

  @Override
  public ObserverMethod<T> getObserverMethod()
  {
    return _observer;
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _method + "]";
  }
}
