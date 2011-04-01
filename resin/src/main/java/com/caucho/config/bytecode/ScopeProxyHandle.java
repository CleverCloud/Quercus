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

package com.caucho.config.bytecode;

import java.io.Serializable;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.PassivationCapable;

import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.InjectManager.ReferenceFactory;
import com.caucho.util.L10N;

/**
 * Scope adapting
 */
@SuppressWarnings("serial")
public class ScopeProxyHandle implements Serializable {
  private static final L10N L = new L10N(ScopeProxyHandle.class);
  
  private String _id;
  
  public ScopeProxyHandle(ReferenceFactory factory)
  {
    Bean<?> bean = factory.getBean();
    
    if (bean instanceof PassivationCapable) {
      _id = ((PassivationCapable) bean).getId();
    }
    else
      throw new IllegalStateException(L.l("serializing bean '{0}' that is not passivating",
                                          bean));
  }
  
  private Object readResolve()
  {
    InjectManager manager = InjectManager.getCurrent();
    
    Bean<?> bean = manager.getPassivationCapableBean(_id);
    
    if (bean == null)
      throw new IllegalStateException(L.l("No passivation bean exists with hash {0}",
                                          _id));
    
    return manager.getReference(bean);
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }
}
