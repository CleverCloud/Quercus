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
import java.lang.reflect.*;

import javax.enterprise.context.spi.CreationalContext;

import com.caucho.config.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.attribute.Attribute;
import com.caucho.config.cfg.BeanConfig;
import com.caucho.config.type.*;
import com.caucho.config.types.AnnotationConfig;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

public class XmlBeanAttribute extends Attribute {
  private static final L10N L = new L10N(XmlBeanAttribute.class);

  public static final XmlBeanAttribute ATTRIBUTE
    = new XmlBeanAttribute();

  private final ConfigType<?> _configType;
  private final Method _setMethod;

  private XmlBeanAttribute()
  {
    this(null, TypeFactory.getType(XmlBeanConfig.class));
  }

  public XmlBeanAttribute(Method setMethod, ConfigType<?> configType)
  {
    _configType = configType;
    _setMethod = setMethod;
  }

  public ConfigType<?> getConfigType()
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
    String uri = qName.getNamespaceURI();
    String localName = qName.getLocalName();

    if (uri.equals("urn:java:com.caucho.config.driver")) {
      TypeFactory factory = TypeFactory.getFactory();

      Class<?> api;

      if (parent instanceof BeanConfig) {
        api = ((BeanConfig) parent).getBeanConfigClass();
      }
      else if (_setMethod != null)
        api = _setMethod.getParameterTypes()[0];
      else
        api = _configType.getType();

      Class<?> cl = factory.getDriverClassByScheme(api, localName);

      return new XmlBeanConfig(qName, cl);
    }

    if (! uri.startsWith("urn:java:"))
      throw new IllegalStateException(L.l("'{0}' is an unexpected namespace, expected 'urn:java:...'", uri));

    String pkg = uri.substring("uri:java:".length());

    Class<?> cl = TypeFactory.loadClass(pkg, localName);

    if (cl == null) {
      ConfigType<?> type = TypeFactory.getFactory().getEnvironmentType(qName);

      if (type != null)
        return type.create(parent, qName);

      throw new ConfigException(L.l("'{0}.{1}' is an unknown class for element '{2}'",
                                    pkg, localName, qName));
    }

    if (Annotation.class.isAssignableFrom(cl)) {
      return new AnnotationConfig(cl);
    }
    else {
      XmlBeanConfig config = new XmlBeanConfig(qName, cl);

      // config.setScope("singleton");

      return config;
    }
  }

  /**
   * Sets the value of the attribute as text
   */
  @Override
  public void setText(Object bean, QName name, String value)
    throws ConfigException
  {
    Object beanChild = create(bean, name);

    if (beanChild instanceof XmlBeanConfig) {
      XmlBeanConfig config = (XmlBeanConfig) beanChild;

      if (! value.trim().equals("")) {
        config.addArg(new TextArgProgram(value));
      }

      config.init();
    }

    setValue(bean, name, beanChild);
  }


  /**
   * Sets the value of the attribute
   */
  @Override
  public void setValue(Object bean, QName name, Object value)
    throws ConfigException
  {
    try {
      if (value instanceof AnnotationConfig)
        value = ((AnnotationConfig) value).replace();

      if (_setMethod != null && value != null) {
        if (! _setMethod.getParameterTypes()[0].isAssignableFrom(value.getClass()))
          throw new ConfigException(L.l("'{0}.{1}' is not assignable from {2}",
                                        _setMethod.getDeclaringClass().getSimpleName(),
                                        _setMethod.getName(),
                                        value));


        _setMethod.invoke(bean, value);
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  static class TextArgProgram extends ConfigProgram {
    private String _arg;

    TextArgProgram(String arg)
    {
      _arg = arg;
    }

    @Override
    public <T> void inject(T bean, CreationalContext<T> env)
    {
      throw new UnsupportedOperationException(getClass().getName());
    }

    @Override
    public <T> T create(ConfigType<T> type, CreationalContext<T> env)
      throws ConfigException
    {
      return (T) type.valueOf(_arg);
    }
  }
}
