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
 * @author Sam
 */

package com.caucho.config.types;

import java.util.Hashtable;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.NamingException;

import com.caucho.inject.Module;
import com.caucho.naming.ObjectProxy;

/**
 * Object proxy to create instance of a bean.
 */
@Module
public class BeanJndiProxy implements ObjectProxy {
  private BeanManager _manager;
  private Bean<?> _bean;

  public BeanJndiProxy(BeanManager manager, Bean<?> bean)
  {
    if (manager == null)
      throw new NullPointerException();
    if (bean == null)
      throw new NullPointerException();
    
    _manager = manager;
    _bean = bean;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object createObject(Hashtable env) 
    throws NamingException
  {
    CreationalContext<?> cxt = _manager.createCreationalContext(_bean);
    Class<?> type = _bean.getBeanClass();
    
    return _manager.getReference(_bean, type, cxt);
  }

  public String toString()
  {
    return getClass().getSimpleName() +  "[" + _bean + "]";
  }
}
