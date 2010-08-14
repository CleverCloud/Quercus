/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.jsf.cfg;

import java.lang.reflect.*;
import java.util.*;

import javax.el.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.convert.*;
import javax.faces.el.*;
import javax.faces.event.*;
import javax.faces.validator.*;

import com.caucho.config.*;
import com.caucho.config.types.*;
import com.caucho.util.*;

public class PropertyValue implements AbstractValue
{
  private static final L10N L = new L10N(PropertyValue.class);
  
  private String _valueString;
  private Class _type;
  
  private Object _value;

  private PropertyValue(String valueString, Class type)
  {
    _valueString = valueString;
    _type = type;
  }

  public static AbstractValue create(String valueString, Class type)
  {
    if (valueString.indexOf("#{") >= 0)
      return new ELValue(valueString, type);
    else
      return new PropertyValue(valueString, type);
  }
  
  public Object getValue(FacesContext context)
  {
    if (_value != null)
      return _value;
    else if (_valueString == null)
      return _valueString;
    else {
      Application app = context.getApplication();
      Converter converter = app.createConverter(_type);

      if (converter != null) {
        _value = converter.getAsObject(context,
                                       context.getViewRoot(),
                                       _valueString);
      }
      else {
        if (! _type.isAssignableFrom(String.class))
          throw new ConfigException(L.l("'{0}' cannot have a string value.",
                                        _type.getName()));

        _value = _valueString;
      }

      return _value;
    }
  }
}
