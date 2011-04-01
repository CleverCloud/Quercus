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
 * @author Scott Ferguson;
 */

package com.caucho.config.program;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.spi.CreationalContext;
import javax.rmi.PortableRemoteObject;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;


public class MethodGeneratorProgram extends ConfigProgram
{
  private static final Logger log
    = Logger.getLogger(MethodGeneratorProgram.class.getName());
  private static final L10N L = new L10N(MethodGeneratorProgram.class);

  private Method _method;
  private ValueGenerator _gen;

  public MethodGeneratorProgram(Method method, ValueGenerator gen)
  {
    _method = method;
    _method.setAccessible(true);

    _gen = gen;
  }

  @Override
  public String getName()
  {
    return _method.getName();
  }

  Class<?> getType()
  {
    return _method.getParameterTypes()[0];
  }

  @Override
  public Class<?> getDeclaringClass()
  {
    return _method.getDeclaringClass();
  }

  @Override
  public <T> void inject(T bean, CreationalContext<T> env)
    throws ConfigException
  {
    Class<?> type = getType();
    
    Object value = _gen.create();
    
    try {
      // XXX TCK: ejb30/bb/session/stateless/sessioncontext/descriptor/getBusinessObjectLocal1, needs QA
      if (! type.isAssignableFrom(value.getClass())) {
        try {
          value = PortableRemoteObject.narrow(value, getType());
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);
        }
      }

      if (! type.isAssignableFrom(value.getClass())) {
      }

      _method.invoke(bean, value);
    } catch (InvocationTargetException e) {
      throw new ConfigException(location() + e.getCause().getMessage(),
                                e.getCause());
    } catch (IllegalArgumentException e) {

      throw new ConfigException(location()
                                + L.l("Resource type {0} is not assignable to method '{1}' of type {2}.",
                                      value.getClass().getName(),
                                      _method.getName(),
                                      type.getName()),
                                e);
    } catch (Exception e) {
      throw new ConfigException(location() + e.getMessage(), e);
    }
  }

  private String location()
  {
    return _method.getDeclaringClass().getName() + "." + _method.getName() + ": ";
  }
}
