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

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.faces.context.*;
import javax.faces.FacesException;

public class PropertyBeanProgram extends BeanProgram
{
  private static final Logger log
    = Logger.getLogger(PropertyBeanProgram.class.getName());
  private static final L10N L = new L10N(PropertyBeanProgram.class);

  private Method _method;
  private AbstractValue _value;
  private String _name;
  private boolean _isValid;

  public PropertyBeanProgram(Method method, AbstractValue value)
   {
    _method = method;
    _value = value;
    _isValid = true;
  }

  public PropertyBeanProgram(String name, boolean valid)
  {
    _name = name;
    _isValid = valid;                                                                                       
  }

  /**
   * Configures the object.
   */
  public void configure(FacesContext context, Object bean)
    throws ConfigException
  {
    if (! _isValid)
      throw new FacesException(L.l("'{0}' is unknown property of '{1}'",
                                   _name,
                                   bean.getClass()));

    try {
      _method.invoke(bean, _value.getValue(context));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}
