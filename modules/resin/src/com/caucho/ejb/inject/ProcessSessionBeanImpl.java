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

package com.caucho.ejb.inject;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.ProcessSessionBean;
import javax.enterprise.inject.spi.SessionBeanType;

import com.caucho.config.extension.ProcessManagedBeanImpl;
import com.caucho.config.inject.InjectManager;
import com.caucho.inject.Module;

/**
 * Internal implementation for a Bean
 */
@Module
public class ProcessSessionBeanImpl<X> extends ProcessManagedBeanImpl<Object>
  implements ProcessSessionBean<X>
{
  private String _ejbName;
  private SessionBeanType _sessionBeanType;
  
  public ProcessSessionBeanImpl(InjectManager manager,
                                Bean<Object> bean,
                                AnnotatedType<Object> beanAnnType,
                                String ejbName,
                                SessionBeanType type)
  {
    super(manager, bean, beanAnnType);
    
    _ejbName = ejbName;
    _sessionBeanType = type;
  }

  @Override
  public String getEjbName()
  {
    return _ejbName;
  }

  @Override
  public SessionBeanType getSessionBeanType()
  {
    return _sessionBeanType;
  }
}
