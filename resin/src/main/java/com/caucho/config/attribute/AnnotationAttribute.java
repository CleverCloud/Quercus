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

package com.caucho.config.attribute;

import com.caucho.config.*;
import com.caucho.config.type.*;
import com.caucho.config.types.AnnotationConfig;
import com.caucho.config.types.RawString;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

public class AnnotationAttribute<T> extends Attribute {
  private static final L10N L = new L10N(AnnotationAttribute.class);

  private String _name;
  private ConfigType<T> _type;

  public AnnotationAttribute(String name, Class<T> type, boolean isEL)
  {
    _name = name;

    if (isEL)
      _type = TypeFactory.getType(type);
    else if (String.class.equals(type))
      _type = (ConfigType) RawStringType.TYPE;
    else if (String[].class.equals(type))
      _type = RawStringArrayType.TYPE;
    else
      _type = TypeFactory.getType(type);
  }
  
  /**
   * Returns the config type of the attribute value.
   */
  public ConfigType<T> getConfigType()
  {
    return _type;
  }

  /**
   * Creates the child bean.
   */
  public Object create(Object parent, QName name)
    throws ConfigException
  {
    // ioc/04f7
    // ejb/1332 - need to refactor to remove isArray test
    /*
    if (_type.isArray())
      return _type.getComponentType().create(parent, name);
    else
      return _type.create(parent, name);
      */
    return _type.create(parent, name);
  }
  
  /**
   * Sets the value of the attribute
   */
  @Override
  public void setText(Object bean, QName name, String value)
    throws ConfigException
  {
    try {
      AnnotationConfig ann = (AnnotationConfig) bean;

      ann.setAttribute(name.getLocalName(), _type.valueOf(value));
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  
  /**
   * Sets the value of the attribute
   */
  @Override
  public void setValue(Object bean, QName name, Object value)
    throws ConfigException
  {
    try {
      AnnotationConfig ann = (AnnotationConfig) bean;

      ann.setAttribute(name.getLocalName(), value);
      //_putMethod.invoke(bean, name.getLocalName(), value);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}
