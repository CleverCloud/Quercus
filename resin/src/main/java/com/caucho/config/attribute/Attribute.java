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
import com.caucho.util.L10N;
import com.caucho.xml.QName;

public abstract class Attribute {
  private static final L10N L = new L10N(Attribute.class);
  
  /**
   * Returns the config type of the attribute value.
   */
  abstract public ConfigType<?> getConfigType();

  /**
   * Returns true for a bean-style attribute.
   */
  public boolean isBean()
  {
    return getConfigType().isBean();
  }

  /**
   * Returns true for an EL attribute.
   */
  public boolean isEL()
  {
    return getConfigType().isEL();
  }

  /**
   * Returns true for a node attribute.
   */
  public boolean isNode()
  {
    return getConfigType().isNode();
  }

  /**
   * Returns true for a program-style attribute.
   */
  public boolean isProgram()
  {
    return getConfigType().isProgram();
  }

  /**
   * True if it allows inline beans
   */
  public boolean isAllowInline()
  {
    return false;
  }

  /**
   * True if the inline type matches
   */
  public boolean isInlineType(ConfigType<?> type)
  {
    return false;
  }

  /**
   * True if it allows text.
   */
  public boolean isAllowText()
  {
    return true;
  }

  /**
   * True if the attribute is annotated with a @Configurable
   */
  public boolean isConfigurable()
  {
    return false;
  }

  public boolean isAssignableFrom(Attribute oldAttr)
  {
    return false;
  }
  
  /**
   * Sets the value of the attribute as text
   */
  public void setText(Object bean, QName name, String value)
    throws ConfigException
  {
    if (value.trim().equals("")) {
      Object childBean = create(bean, name);

      if (childBean != null) {
        ConfigType<?> type = TypeFactory.getType(childBean.getClass());

        type.init(childBean);

        Object newBean = replaceObject(childBean);

        setValue(bean, name, newBean);

        return;
      }
      
    }

    throw new ConfigException(L.l("{0}: '{1}' does not allow text for attribute {2}.",
                                  this,
                                  getConfigType().getTypeName(),
                                  name));
  }
  
  /**
   * Sets the value of the attribute
   */
  abstract public void setValue(Object bean, QName name, Object value)
    throws ConfigException;

  /**
   * Returns true for attributes which create objects.
   */
  public boolean isSetter()
  {
    return true;
  }

  /**
   * Creates the child bean.
   */
  public Object create(Object parent, QName name, ConfigType<?> type)
    throws ConfigException
  {
    return create(parent, name);
  }

  /**
   * Creates the child bean.
   */
  public Object create(Object parent, QName name)
    throws ConfigException
  {
    return null;
  }

  /**
   * Replaces the given bean.
   */
  public Object replaceObject(Object bean)
  {
    return getConfigType().replaceObject(bean);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getConfigType() + "]";
  }
}
