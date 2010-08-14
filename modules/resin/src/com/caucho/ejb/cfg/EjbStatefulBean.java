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

package com.caucho.ejb.cfg;

import java.lang.annotation.Annotation;

import javax.ejb.Stateful;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.inject.Module;

/**
 * Configuration for an ejb stateless session bean.
 */
@Module
public class EjbStatefulBean<X> extends EjbSessionBean<X> {
  /**
   * Creates a new session bean configuration.
   */
  public EjbStatefulBean(EjbConfig ejbConfig,
                         AnnotatedType<X> rawAnnType,
                         AnnotatedType<X> annType,
                         String moduleName)
  {
    super(ejbConfig, rawAnnType, annType, moduleName);
  }
  
  @Override
  public Class<? extends Annotation> getSessionType()
  {
    return Stateful.class;
  }

  /**
   * Returns the kind of bean.
   */
  @Override
  public String getEJBKind()
  {
    return "stateful";
  }
}
