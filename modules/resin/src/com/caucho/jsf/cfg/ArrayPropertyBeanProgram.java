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

package com.caucho.jsf.cfg;

import com.caucho.config.*;
import com.caucho.util.L10N;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.faces.context.*;

public class ArrayPropertyBeanProgram extends BeanProgram
{
  private static final Logger log
    = Logger.getLogger(ArrayPropertyBeanProgram.class.getName());
  private static final L10N L = new L10N(ArrayPropertyBeanProgram.class);

  private Method _getter;
  private Method _setter;
  private List<AbstractValue> _values;
  private Class _baseType;
  private String _propertyName;

  public ArrayPropertyBeanProgram(Method getter, Method setter,
                                List<AbstractValue> values, String propertyName)
  {
    _getter = getter;
    _setter = setter;
    _values = values;
    _propertyName = propertyName;

    if (getter != null)
      _baseType = getter.getReturnType().getComponentType();
    else
      _baseType = setter.getParameterTypes()[0].getComponentType();
  }

  /**
   * Configures the object.
   */
  public void configure(FacesContext context, Object bean)
    throws ConfigException
  {
    try {
      Object list = null;

      if (_setter == null) {
        if (log.isLoggable(Level.CONFIG)) {
          log.log(Level.CONFIG,
                  L.l("Setter for {0} not found in type {1}",
                      _propertyName,
                      bean.getClass().getName()));
        }

        return;
      }

      if (_getter != null)
        list = _getter.invoke(bean);

      final int length;

      if (list != null) {
        length = Array.getLength(list);

        Object newList = Array.newInstance(_baseType, length + _values.size());

        System.arraycopy(list, 0, newList, 0, length);

        list = newList;
      }
      else {
        length = 0;
        list = Array.newInstance(_baseType, _values.size());
      }

      for (int i = 0; i < _values.size(); i++) {
        AbstractValue value = _values.get(i);
        Array.set(list, length + i, value.getValue(context));
      }

      _setter.invoke(bean, new Object[]{list});
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}