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

package com.caucho.ejb.session;

import com.caucho.config.inject.InjectManager;
import com.caucho.config.xml.XmlConfigContext;

import java.lang.annotation.*;
import java.util.Set;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

/**
 * Component for session beans
 */
public class StatefulComponent<X> implements InjectionTarget<X> {
  private final StatefulProvider _provider;
  private final Class _beanClass;

  public StatefulComponent(StatefulProvider provider,
                           Class beanClass)
  {
    _provider = provider;
    _beanClass = beanClass;
  }

  /**
   * Creates a new instance of the component
   */
  public X produce(CreationalContext<X> env)
  {
    return (X) _provider.__caucho_createNew(this, env);
  }
  
  /**
   * Inject the bean.
   */
  public void inject(X instance, CreationalContext<X> ctx)
  {
  }
  
  /**
   * PostConstruct initialization
   */
  public void postConstruct(X instance)
  {
  }
  
  /**
   * Call pre-destroy
   */
  public void dispose(X instance)
  {
  }
  
  /**
   * Call destroy
   */
  public void preDestroy(X instance)
  {
  }

  /**
   * Returns the injection points.
   */
  public Set<InjectionPoint> getInjectionPoints()
  {
    return null;
  }
}
