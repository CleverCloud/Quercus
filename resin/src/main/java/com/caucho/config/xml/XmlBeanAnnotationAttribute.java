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

package com.caucho.config.xml;

import java.lang.annotation.Annotation;

import com.caucho.config.ConfigException;
import com.caucho.config.attribute.Attribute;
import com.caucho.config.type.ConfigType;
import com.caucho.config.type.TypeFactory;
import com.caucho.config.types.AnnotationConfig;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

/**
 * Attribute for configuring an XML annotation's value.
 * 
 * The XML equivalent for an attribute @mypkg.MyAttribute(myfield="my-value")
 * is the following:
 * 
 * <code><pre>
 * &lt;mypkg:MyAttribute>
 *   &lt;myfield>my-value&lt;/myfield>
 * &lt;/mypkg:MyAttribute>
 * </pre></code>
 * 
 */
public class XmlBeanAnnotationAttribute<T> extends Attribute {
  private static final L10N L = new L10N(XmlBeanAnnotationAttribute.class);

  private static final QName VALUE = new QName("value");

  private final ConfigType<T> _configType;

  public XmlBeanAnnotationAttribute(Class<T> cl)
  {
    _configType = TypeFactory.getType(cl);
  }

  @Override
  public ConfigType<T> getConfigType()
  {
    return _configType;
  }

  /**
   * Creates the child bean.
   */
  @Override
  public Object create(Object parent, QName qName)
    throws ConfigException
  {
    return _configType.create(parent, qName);
  }

  /**
   * Sets the value of the attribute
   */
  @Override
  public void setValue(Object bean, QName name, Object value)
    throws ConfigException
  {
    XmlBeanConfig<T> customBean = (XmlBeanConfig<T>) bean;

    if (value instanceof Annotation) {
      customBean.addAnnotation((Annotation) value);
    }
    else {
      AnnotationConfig annConfig = (AnnotationConfig) value;
      customBean.addAnnotation(annConfig.replace());
    }
  }

  /**
   * Sets the value of the attribute
   */
  @Override
  public void setText(Object parent, QName name, String text)
    throws ConfigException
  {
    Object bean = create(parent, name);

    Attribute attr = _configType.getAttribute(VALUE);

    if (attr != null) {
      attr.setText(bean, VALUE, text);

      setValue(parent, name, bean);
    }
    else if (text == null || "".equals(text)) {
      // server/2pad
      setValue(parent, name, bean);
    }
    else {
      throw new ConfigException(L.l("'{0}' does not have a 'value' attribute, so it cannot have a text value.",
                                    name));
    }
  }
}
